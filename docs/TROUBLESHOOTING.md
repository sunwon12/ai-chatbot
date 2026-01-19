# Troubleshooting Guide

AI Chatbot Service 개발 중 발생한 문제들과 해결 방법을 정리한 문서입니다.

---

## 1. SSE 스트리밍 시 Access Denied 에러

### 증상
```
org.springframework.security.access.AccessDeniedException: Access Denied
    at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(AuthorizationFilter.java:98)
```

SSE 스트리밍 API (`/api/v1/chats/stream`) 호출 시 OpenAI 응답은 정상적으로 수신되지만, 응답 완료 후 위 에러가 발생함.

### 원인
- SSE 스트리밍은 별도 스레드(`Executors.newCachedThreadPool()`)에서 실행됨
- `@Transactional` 어노테이션은 해당 스레드에서 동작하지 않음
- `SecurityContext`가 비동기 스레드로 전파되지 않음
- 스트리밍 완료 후 Tomcat의 async dispatch가 발생할 때 인증 정보가 없어 `Access Denied` 발생

### 해결 방법

**Before (문제 코드):**
```kotlin
@Transactional
fun createChatWithStreaming(userId: Long, request: CreateChatRequest, emitter: SseEmitter) {
    val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
    // ...
    
    executor.execute {
        // @Transactional이 여기서 동작하지 않음!
        // SecurityContext도 없음!
        val answer = aiClient.streamCompletion(...) { chunk ->
            emitter.send(SseEmitter.event().data(chunk))
        }
        chat.updateAnswer(answer)
        chatRepository.save(chat)  // 트랜잭션 없이 실행됨
        emitter.complete()  // async dispatch 시 Access Denied
    }
}
```

**After (해결 코드):**
```kotlin
fun createChatWithStreaming(userId: Long, request: CreateChatRequest, emitter: SseEmitter) {
    // 1. 메인 스레드에서 TransactionTemplate으로 데이터 조회
    val chatData = transactionTemplate.execute {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val thread = threadManager.getOrCreateThread(user, Instant.now())
        val chat = Chat.create(thread, question)
        chatRepository.save(chat)
        Triple(chat.id, context, questionText)
    }!!

    // 2. 비동기 스레드에서 AI 호출
    executor.execute {
        try {
            val answer = aiClient.streamCompletion(...) { chunk ->
                try {
                    emitter.send(SseEmitter.event().data(chunk))
                } catch (e: Exception) {
                    // 클라이언트 연결 끊김 무시
                }
            }
            
            // 3. TransactionTemplate으로 별도 트랜잭션에서 저장
            transactionTemplate.execute {
                val chat = chatRepository.findById(chatId).orElseThrow()
                chat.updateAnswer(answer)
                chatRepository.save(chat)
            }
            
            emitter.complete()
        } catch (e: Exception) {
            try {
                emitter.completeWithError(e)
            } catch (ignored: Exception) {}
        }
    }
}
```

### 핵심 포인트
1. `@Transactional` 대신 `TransactionTemplate` 사용
2. 메인 스레드에서 필요한 데이터를 미리 조회
3. 비동기 스레드에서 명시적 트랜잭션 관리
4. `emitter.send()` 호출 시 try-catch로 연결 끊김 처리

> **최종 결정**: SSE 스트리밍은 MVP 단계에서 복잡도가 높아 제거하고, 동기 HTTP 방식만 사용하기로 결정함. [의사결정.md](./decision_making ) 참고.

---

## 2. TransactionTemplate에서 Read-Only 트랜잭션 에러

### 증상
```
org.postgresql.util.PSQLException: ERROR: cannot execute INSERT in a read-only transaction
```

`TransactionTemplate.execute()` 내에서 INSERT 쿼리 실행 시 위 에러 발생.

### 원인
- `ChatService` 클래스에 `@Transactional(readOnly = true)`가 선언되어 있음
- `TransactionTemplate`은 기본적으로 상위 트랜잭션 속성을 상속받음
- 결과적으로 새 트랜잭션도 read-only로 설정되어 INSERT가 실패함

### 해결 방법

**방법 1: TransactionTemplate에 명시적 속성 설정**
```kotlin
@Service
class ChatService(
    private val transactionManager: PlatformTransactionManager
) {
    private val writeTransactionTemplate = TransactionTemplate(transactionManager).apply {
        isReadOnly = false
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }
    
    fun createChat(...) {
        writeTransactionTemplate.execute {
            // INSERT 가능
        }
    }
}
```

**방법 2: 메서드에 별도 @Transactional 선언 (권장)**
```kotlin
@Service
@Transactional(readOnly = true)
class ChatService(...) {
    
    @Transactional  // readOnly = false (기본값)
    fun createChat(...) {
        // INSERT 가능
    }
}
```

### 핵심 포인트
- 클래스 레벨의 `@Transactional(readOnly = true)`는 모든 메서드에 적용됨
- 쓰기 작업이 필요한 메서드에는 별도로 `@Transactional` 선언 필요
- `TransactionTemplate` 사용 시 트랜잭션 속성을 명시적으로 설정해야 함

---

## 3. Swagger UI 접근 시 401 Unauthorized

### 증상
`http://localhost:8080/swagger-ui.html` 접속 시 로그인 필요 에러 발생

### 원인
Spring Security 설정에서 Swagger 관련 경로가 `permitAll()`에 포함되지 않음

### 해결 방법
`SecurityConfig.kt`에 Swagger 경로 추가:

```kotlin
.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
        // ...
}
```

---

## 3. JPA 엔티티 변경 감지 실패 (Dirty Checking)

### 증상
`@Transactional` 메서드 내에서 엔티티 필드를 수정했지만 DB에 반영되지 않음

### 원인
- Kotlin의 `data class` 사용 시 `allOpen` 플러그인이 없으면 final 클래스로 컴파일됨
- JPA는 프록시를 생성해야 하므로 open 클래스가 필요

### 해결 방법
`build.gradle.kts`에 `allOpen` 설정 추가:

```kotlin
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

---

## 4. Password Embeddable 기본 생성자 오류

### 증상
```
org.hibernate.InstantiationException: No default constructor for entity
```

### 원인
JPA는 엔티티/Embeddable 인스턴스 생성 시 기본 생성자가 필요하지만, `Password` Value Object에 private 생성자만 있음

### 해결 방법
`kotlin("plugin.jpa")` 플러그인이 자동으로 no-arg 생성자를 생성함. 플러그인이 제대로 적용되었는지 확인:

```kotlin
plugins {
    kotlin("plugin.jpa") version "1.9.22"
}
```

---

## 5. OpenAI API 호출 실패

### 증상
```
OpenAI API 호출 실패: Connection refused
```

### 원인
- 네트워크 문제
- API 키가 잘못됨
- Rate limit 초과

### 해결 방법
1. `application.yml`에서 API 키 확인:
   ```yaml
   openai:
     api-key: sk-proj-xxx...
     base-url: https://api.openai.com/v1
   ```

2. API 키 유효성 확인 (OpenAI 대시보드에서 확인)

3. 기본 모델이 무료 모델인지 확인:
   ```kotlin
   else -> ChatModel.GPT_4O_MINI // 기본값: 무료 모델
   ```

---

## 6. Testcontainers PostgreSQL 연결 실패

### 증상
테스트 실행 시 PostgreSQL 컨테이너에 연결되지 않음

### 원인
Docker가 실행되지 않거나, Testcontainers 설정이 잘못됨

### 해결 방법
1. Docker Desktop이 실행 중인지 확인

2. `src/test/resources/application.yml` 또는 테스트 클래스에서 동적 프로퍼티 설정:
   ```kotlin
   companion object {
       @Container
       @JvmStatic
       val postgres = PostgreSQLContainer("postgres:15")
       
       @DynamicPropertySource
       @JvmStatic
       fun configureProperties(registry: DynamicPropertyRegistry) {
           registry.add("spring.datasource.url") { postgres.jdbcUrl }
           registry.add("spring.datasource.username") { postgres.username }
           registry.add("spring.datasource.password") { postgres.password }
       }
   }
   ```

---

## 7. JWT 토큰 검증 실패

### 증상
유효한 토큰인데도 `InvalidTokenException` 발생

### 원인
- JWT secret key가 256비트(32바이트) 미만
- 토큰 만료
- secret key 변경

### 해결 방법
1. `application.yml`에서 secret key 길이 확인 (최소 256비트):
   ```yaml
   jwt:
     secret: sionic-ai-secret-key-for-jwt-token-generation-minimum-256-bits
     expiration: 86400000
   ```

2. 토큰 재발급 필요 시 `/api/v1/auth/login` 재호출

---

## 참고 자료

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OpenAI Java SDK](https://github.com/openai/openai-java)
- [Spring Boot SSE Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
