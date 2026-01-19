# 기술 의사결정 기록 (ADR)

AI Chatbot Service 개발 중 내린 주요 기술 의사결정을 기록합니다.

---

## 1. SSE 스트리밍 대신 동기 HTTP 방식 채택

### 날짜
2026-01-19

### 상태
**승인됨**

### 배경
초기 설계에서는 OpenAI API 응답을 클라이언트에게 실시간으로 전달하기 위해 SSE(Server-Sent Events) 스트리밍을 도입하려 했습니다.

### 문제점
SSE 스트리밍 구현 과정에서 다음과 같은 문제들이 발생했습니다:

1. **Spring Security 인증 문제**
   - SSE는 별도 스레드에서 실행되어 `SecurityContext`가 전파되지 않음
   - async dispatch 시 `Access Denied` 에러 발생
   
2. **트랜잭션 관리 복잡성**
   - `@Transactional` 어노테이션이 비동기 스레드에서 동작하지 않음
   - `TransactionTemplate` 사용 시 read-only 트랜잭션 상속 문제
   - 명시적 트랜잭션 관리 코드 필요
   
3. **에러 처리 복잡성**
   - `SseEmitter`의 연결 상태 관리
   - 클라이언트 연결 끊김 시 예외 처리

### 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| SSE 스트리밍 | 실시간 응답, UX 향상 | 복잡한 구현, 보안/트랜잭션 이슈 |
| WebSocket | 양방향 통신 | 더 복잡한 구현, 상태 관리 필요 |
| 동기 HTTP | 단순한 구현, 안정적 | 응답 대기 시간 존재 |

### 결정
**MVP 단계에서는 동기 HTTP 방식만 사용**

1. 구현 복잡도 대폭 감소
2. 트랜잭션 및 보안 이슈 해결
3. 테스트 용이성 향상
4. 추후 필요 시 SSE 재도입 가능

### 변경 사항
- `ChatController`에서 `/stream` 엔드포인트 제거
- `ChatService`에서 `createChatWithStreaming()` 메서드 제거
- `AiClient` 인터페이스에서 `streamCompletion()` 제거
- `CreateChatRequest`에서 `isStreaming` 필드 제거

### 향후 계획
정식 출시 후 사용자 피드백에 따라 SSE 스트리밍 재도입 검토:
- Spring Security의 `SecurityContextHolder` MODE 변경
- `DelegatingSecurityContextExecutor` 활용
- WebSocket 도입 검토

---

## 2. 기본 AI 모델로 gpt-4o-mini 선택

### 날짜
2026-01-19

### 상태
**승인됨**

### 배경
OpenAI API는 다양한 모델을 제공하며, 각 모델은 성능과 비용이 다릅니다.

### 결정
**기본 모델: gpt-4o-mini**

### 이유
1. **무료 티어 지원**: API 사용 비용 절감
2. **충분한 성능**: MVP 수준의 대화에 적합
3. **빠른 응답 속도**: 경량 모델로 응답 시간 단축
4. **유연성**: 필요 시 요청 파라미터로 다른 모델 지정 가능

### 지원 모델
```kotlin
when (model?.lowercase()) {
    "gpt-4" -> ChatModel.GPT_4
    "gpt-4o" -> ChatModel.GPT_4O
    "gpt-4o-mini" -> ChatModel.GPT_4O_MINI
    "gpt-4-turbo" -> ChatModel.GPT_4_TURBO
    "gpt-3.5-turbo" -> ChatModel.GPT_3_5_TURBO
    else -> ChatModel.GPT_4O_MINI // 기본값
}
```

---

## 3. OpenAI 공식 SDK 사용

### 날짜
2026-01-19

### 상태
**승인됨**

### 배경
OpenAI API와 통신하기 위한 방법을 선택해야 했습니다.

### 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| RestTemplate/WebClient 직접 구현 | 완전한 제어 | 유지보수 부담, API 변경 대응 |
| 커뮤니티 라이브러리 | 빠른 도입 | 지원 불확실, 비공식 |
| OpenAI 공식 SDK | 공식 지원, 타입 안전성 | 의존성 추가 |

### 결정
**openai-java-spring-boot-starter 사용**

```kotlin
implementation("com.openai:openai-java-spring-boot-starter:4.15.0")
```

### 이유
1. **공식 지원**: OpenAI에서 직접 개발 및 유지보수
2. **Spring Boot 통합**: 자동 설정 및 Bean 주입
3. **타입 안전성**: 모든 API가 타입화됨
4. **스트리밍 지원**: 향후 SSE 재도입 시 활용 가능

---

## 4. Value Object를 통한 도메인 모델링

### 날짜
2026-01-18

### 상태
**승인됨**

### 결정
핵심 개념을 Value Object로 모델링하여 타입 안전성과 비즈니스 로직 캡슐화 달성

### 적용된 Value Objects
- `Email`: 이메일 형식 검증
- `Password`: 암호화 및 검증 로직
- `Question`: 질문 유효성 검증
- `Answer`: 응답 데이터 캡슐화
- `DateRange`: 날짜 범위 연산

### 장점
1. **유효성 검증 일원화**: 생성 시점에 검증
2. **불변성 보장**: 데이터 무결성 유지
3. **비즈니스 로직 캡슐화**: 도메인 규칙 명시적 표현
4. **테스트 용이성**: 독립적인 단위 테스트 가능

---

## 5. MVP 단계에서 기본 역할을 ADMIN으로 설정

### 날짜
2026-01-19

### 상태
**승인됨 (임시)**

### 배경
MVP 단계에서 모든 기능을 테스트하고 검증하기 위해서는 관리자 권한이 필요합니다.

### 문제점
- 기본 역할이 `MEMBER`인 경우 관리자 전용 API 테스트 불가
- 별도의 관리자 계정 생성 로직 필요
- MVP 단계에서 역할 구분은 불필요한 복잡도

### 결정
**회원가입 시 기본 역할을 ADMIN으로 설정**

```kotlin
companion object {
    fun create(
        email: Email,
        rawPassword: String,
        name: String,
        encoder: PasswordEncoder,
        role: Role = Role.ADMIN // MVP: 모든 사용자를 ADMIN으로 가입
    ): User {
        // ...
    }
}
```

### 이유
1. **테스트 용이성**: 모든 API 엔드포인트 즉시 테스트 가능
2. **개발 속도**: 별도 관리자 생성 로직 불필요
3. **MVP 집중**: 핵심 기능에 집중

### 변경된 파일
- `User.kt`: `User.create()` 팩토리 메서드의 기본 role 파라미터 변경

### 향후 계획
정식 출시 전 반드시 아래 사항 적용:
1. 기본 역할을 `MEMBER`로 복원
2. 관리자 계정 생성용 별도 API 또는 시드 스크립트 작성
3. 역할 기반 접근 제어(RBAC) 테스트 추가

> ⚠️ **주의**: 이 설정은 MVP/개발 단계 전용입니다. 프로덕션 배포 전 반드시 변경해야 합니다.

---

## 6. Thymeleaf 데모 페이지 도입

### 날짜
2026-01-19

### 상태
**승인됨**

### 배경
Swagger UI는 개발자에게 편리하지만, 비개발자 고객사에게 API를 시연하기에는 직관적이지 않습니다.

### 결정
**Thymeleaf를 사용한 간단한 데모 웹페이지 구현**

### 구현 내용
1. **메인 페이지** (`/demo`): 기능 네비게이션
2. **회원가입/로그인** (`/demo/login`): JWT 토큰 발급 및 저장
3. **AI 채팅** (`/demo/chat`): GPT와 대화, 대화 기록 조회, 피드백
4. **관리자 대시보드** (`/demo/admin`): 통계, 피드백 관리, CSV 내보내기

### 기술 스택
- **Thymeleaf**: Spring Boot와 자연스러운 통합
- **Vanilla JavaScript**: 추가 프레임워크 없이 API 호출
- **localStorage**: JWT 토큰 클라이언트 저장
- **Modern CSS**: 다크 테마, 반응형 디자인

### 추가된 파일
```
src/main/kotlin/sionic/demo/DemoController.kt
src/main/resources/templates/demo/
  ├── index.html
  ├── login.html
  ├── chat.html
  └── admin.html
src/main/resources/static/css/demo.css
```

### 장점
1. **직관적 시연**: 비개발자도 쉽게 이해 가능
2. **즉시 테스트**: 회원가입부터 모든 기능 테스트 가능
3. **브랜딩**: 전문적인 프레젠테이션 효과
4. **독립적**: API 서버와 동일 프로젝트에서 실행

### 접근 URL
- 데모 홈: `http://localhost:8080/demo`
- Swagger: `http://localhost:8080/swagger-ui.html`

