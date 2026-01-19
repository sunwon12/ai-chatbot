# Implementation Todo List (객체지향 심화 버전)

본 문서는 `docs/new_required.md`의 요구사항을 기반으로 작성되었으며, 요청된 TDD 및 계층별 구현 순서(Controller -> Service Pseudo -> Domain -> Service -> Repository)를 준수합니다.

> **[!IMPORTANT] 객체지향 설계 원칙**
> - **Rich Domain Model**: 비즈니스 로직은 최대한 Domain 객체 내부에 응집시킵니다.
> - **Value Object 분리**: 의미 있는 값은 원시 타입 대신 Value Object로 감쌉니다.
> - **일급 컬렉션**: 컬렉션을 감싸는 일급 컬렉션으로 관련 로직을 응집시킵니다.
> - **Tell, Don't Ask**: 객체에게 상태를 묻지 말고, 행동을 요청합니다.

---

## 1. Environment & Project Setup

### 1.1 Gradle Dependencies & Configuration
- [ ] **build.gradle.kts** 설정
    - **Plugin**: Spring Boot, Dependency Management, Kotlin Spring/JPA
    - **Dependencies**:
        - `implementation`: Spring Web, Data JPA, Security, Validation, Actuator, PostgreSQL Driver
        - `runtimeOnly`: H2 Database
        - `testImplementation`: Spring Boot Starter Test, Spring Security Test, Testcontainers (PostgreSQL)
- [ ] **application.yml** 설정
    - DataSource (Local/Test Setup)
    - JPA (Hibernate DDL Auto, Show SQL)
    - Logging (Level)

### 1.2 Common & Infrastructure
- [ ] **Global Exception Handler**: `ErrorResponse` DTO 및 `@ControllerAdvice` 구현
- [ ] **Common Response Wrapper**: API 공통 응답 포맷 정의
- [ ] **Security Config**: PasswordEncoder Bean, SecurityFilterChain(CSRF disable, Session Stateless), JWT Filter 기본 틀

---

## 2. Feature: User Management & Authentication

### 2.1 Domain 객체 설계 (User)

#### Value Objects
- [ ] **Email** (Value Object)
    - 불변 객체로 설계
    - `validate()`: 이메일 형식 검증 (정규식)
    - `getValue()`: 문자열 반환
    - **테스트**: `EmailTest` - 유효/무효 이메일 형식 검증

- [ ] **Password** (Value Object)
    - `encode(rawPassword, encoder)`: 평문 -> 암호화 (정적 팩토리)
    - `matches(rawPassword, encoder)`: 평문과 암호화된 패스워드 일치 여부
    - `getValue()`: 암호화된 문자열 반환
    - **테스트**: `PasswordTest` - 암호화 및 일치 검증

- [ ] **Role** (Enum)
    - `MEMBER`, `ADMIN`
    - `isAdmin()`: 관리자 권한 여부 반환
    - `canAccessAllResources()`: 모든 리소스 접근 가능 여부

#### Entity
- [ ] **User** (Entity)
    - 필드: `id`, `email: Email`, `password: Password`, `name: String`, `role: Role`, `createdAt`
    - `create(email, rawPassword, name, encoder)`: 정적 팩토리 메서드 (회원가입용)
    - `authenticate(rawPassword, encoder)`: 로그인 시 비밀번호 검증 (내부적으로 `Password.matches` 호출)
    - `isOwnerOf(resourceOwnerId)`: 리소스 소유자 확인
    - `canAccess(resourceOwnerId)`: 본인 또는 관리자인 경우 true
    - **테스트**: `UserTest` - 생성, 인증, 권한 검증

### 2.2 회원가입 (Signup)
- [ ] **[TDD] Controller**: `AuthControllerTest.signup_success()` 작성 (Failing)
    - `POST /api/v1/auth/signup` 요청에 대한 201 Created 및 응답 검증
    - **Edge Case 테스트**: 중복 이메일, 잘못된 이메일 형식, 빈 패스워드
- [ ] **Controller**: `AuthController.signup()` 구현
    - Request DTO 검증 및 Service 호출부 작성
- [ ] **[TDD] Service**: `AuthServiceTest.signup()` 작성 (Failing)
    - Mock Repository를 이용한 비즈니스 로직 테스트
- [ ] **Service Pseudo**: `AuthService.signup()` 수도 코드 작성
    ```kotlin
    fun signup(request: SignupRequest): SignupResponse {
        // 1. Email VO 생성 (내부에서 형식 검증)
        val email = Email(request.email)
        
        // 2. 중복 체크
        if (userRepository.existsByEmail(email)) throw DuplicateEmailException()
        
        // 3. User 엔티티 생성 (정적 팩토리, 내부에서 Password 암호화)
        val user = User.create(email, request.password, request.name, passwordEncoder)
        
        // 4. 저장 및 반환
        return SignupResponse.from(userRepository.save(user))
    }
    ```
- [ ] **Service**: `AuthService.signup()` 실제 로직 구현
- [ ] **Repository**: `UserRepository.existsByEmail(email: Email)`, `save()` 구현
- [ ] **Verify**: 모든 테스트 통과 확인

### 2.3 로그인 (Login)
- [ ] **[TDD] Controller**: `AuthControllerTest.login_success()` 작성 (Failing)
    - `POST /api/v1/auth/login` 요청에 대한 200 OK 및 JWT 토큰 반환 검증
    - **Edge Case 테스트**: 존재하지 않는 이메일, 잘못된 패스워드
- [ ] **Controller**: `AuthController.login()` 구현
- [ ] **[TDD] Service**: `AuthServiceTest.login()` 작성 (Failing)
- [ ] **Service Pseudo**: `AuthService.login()` 수도 코드 작성
    ```kotlin
    fun login(request: LoginRequest): LoginResponse {
        // 1. 유저 조회
        val user = userRepository.findByEmail(Email(request.email))
            ?: throw UserNotFoundException()
        
        // 2. 비밀번호 검증 (User 엔티티에게 위임)
        user.authenticate(request.password, passwordEncoder)
        
        // 3. 토큰 생성
        val token = jwtProvider.createToken(user)
        
        return LoginResponse(token)
    }
    ```
- [ ] **Service**: `AuthService.login()` 실제 로직 구현
- [ ] **Repository**: `UserRepository.findByEmail(email: Email)` 구현
- [ ] **Verify**: 로그인 및 인증 필터 동작 확인

---

## 3. Feature: Chat & Thread Management

### 3.1 Domain 객체 설계 (Thread & Chat)

#### Value Objects
- [ ] **ThreadTimeout** (Value Object 또는 상수)
    - `TIMEOUT_MINUTES = 30`
    - 설정 변경 용이성을 위해 분리

- [ ] **Question** (Value Object)
    - `value: String`
    - `validate()`: 빈 문자열 검증
    - **테스트**: `QuestionTest`

- [ ] **Answer** (Value Object)
    - `value: String`
    - `appendChunk(chunk: String)`: 스트리밍 응답 조각 추가 (불변 유지하며 새 객체 반환)
    - `isEmpty()`: 답변 존재 여부
    - **테스트**: `AnswerTest`

#### Entities
- [ ] **Thread** (Entity)
    - 필드: `id`, `user: User`, `lastMessageAt: Instant`, `createdAt`
    - `isExpired(now: Instant)`: 마지막 메시지로부터 30분 경과 여부 (`now - lastMessageAt > 30분`)
    - `updateLastMessageTime(now: Instant)`: 마지막 상호작용 시간 갱신
    - `isOwnedBy(userId: Long)`: 소유자 확인
    - `validateOwnership(userId: Long)`: 소유자가 아니면 예외 발생
    - **테스트**: `ThreadTest` - 만료 로직, 소유권 검증

- [ ] **Chat** (Entity)
    - 필드: `id`, `thread: Thread`, `question: Question`, `answer: Answer`, `createdAt`
    - `create(thread, question)`: 정적 팩토리 (답변은 빈 상태로 시작)
    - `updateAnswer(answer: Answer)`: 스트리밍 완료 후 최종 답변 저장
    - `belongsTo(userId: Long)`: 해당 유저의 채팅인지 확인 (`thread.isOwnedBy(userId)`)
    - **테스트**: `ChatTest` - 생성, 답변 업데이트

#### Domain Service
- [ ] **ThreadManager** (Domain Service)
    - `getOrCreateThread(user: User, now: Instant)`: 유저의 최근 스레드가 만료되었으면 새로 생성, 아니면 기존 스레드 반환
    - 이 로직은 Thread 엔티티 하나만으로 결정할 수 없고, Repository 조회가 필요하므로 Domain Service로 분리
    - **테스트**: `ThreadManagerTest` - 30분 규칙 검증

### 3.2 대화 생성 (Create Chat)
- [ ] **[TDD] Controller**: `ChatControllerTest.createChat_success()` 작성 (Failing)
    - `POST /api/v1/chats` (SSE Stream) 동작 검증
    - 파라미터: `isStreaming`, `model`
- [ ] **Controller**: `ChatController.createChat()` 구현
    - `SseEmitter` 반환 및 비동기 처리 구조 잡기
- [ ] **[TDD] Service**: `ChatServiceTest.createChat()` 작성 (Failing)
- [ ] **Service Pseudo**: `ChatService.createChat()` 수도 코드 작성
    ```kotlin
    fun createChat(userId: Long, request: CreateChatRequest, emitter: SseEmitter) {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException()
        
        // 1. 스레드 결정 (Domain Service에 위임)
        val thread = threadManager.getOrCreateThread(user, Instant.now())
        
        // 2. 채팅 생성 (빈 답변으로 시작)
        val question = Question(request.question)
        val chat = Chat.create(thread, question)
        chatRepository.save(chat)
        
        // 3. AI 호출 (스트리밍)
        aiClient.streamCompletion(thread, question, request.model) { chunk ->
            emitter.send(chunk)
        }.also { fullAnswer ->
            // 4. 최종 답변 저장
            chat.updateAnswer(fullAnswer)
            chatRepository.save(chat)
            emitter.complete()
        }
    }
    ```
- [ ] **Service**: `ChatService` 구현
- [ ] **Repository**: `ThreadRepository`, `ChatRepository` 구현
- [ ] **Infrastructure**: `AiClient` 인터페이스 및 Mock 구현체
- [ ] **Verify**: SSE 스트리밍 및 스레드 생성 로직(30분 규칙) 검증

### 3.3 대화 목록 조회 (List Chats)

#### 일급 컬렉션
- [ ] **Chats** (일급 컬렉션)
    - `List<Chat>`를 감싸는 객체
    - `groupByThread()`: 스레드별로 그룹화된 Map 반환
    - `filterByUser(userId: Long)`: 특정 유저의 채팅만 필터링
    - **테스트**: `ChatsTest`

#### 구현
- [ ] **[TDD] Controller**: `ChatControllerTest.getChats()` 작성 (Failing)
- [ ] **Controller**: `ChatController.getChats()` 구현 (Pageable 파라미터)
- [ ] **[TDD] Service**: `ChatServiceTest.getChats()` 작성 (Failing)
- [ ] **Service Pseudo**: `ChatService.getChats()` 수도 코드 작성
    ```kotlin
    fun getChats(userId: Long, user: User, pageable: Pageable): Page<ThreadWithChatsDto> {
        val chats = if (user.role.isAdmin()) {
            chatRepository.findAll(pageable)
        } else {
            chatRepository.findAllByThreadUserId(userId, pageable)
        }
        
        // 일급 컬렉션을 통한 그룹화
        return Chats(chats).groupByThread().toPageDto()
    }
    ```
- [ ] **Service**: 실제 조회 로직 구현
- [ ] **Repository**: `ChatRepository.findAllByThreadUserId()` (Pagination) 구현
- [ ] **Verify**: 페이징 및 정렬, 권한별(본인/관리자) 조회 확인

### 3.4 스레드 삭제 (Delete Thread)
- [ ] **[TDD] Controller**: `ThreadControllerTest.deleteThread()` 작성 (Failing)
- [ ] **Controller**: `ThreadController.deleteThread()` 구현
- [ ] **[TDD] Service**: `ThreadServiceTest.deleteThread()` 작성 (Failing)
- [ ] **Service Pseudo**: `ThreadService.deleteThread()` 수도 코드 작성
    ```kotlin
    fun deleteThread(threadId: Long, userId: Long) {
        val thread = threadRepository.findById(threadId) ?: throw ThreadNotFoundException()
        
        // Domain에게 권한 검증 위임 (Tell, Don't Ask)
        thread.validateOwnership(userId)
        
        threadRepository.delete(thread) // Cascade로 Chat도 삭제
    }
    ```
- [ ] **Service**: 삭제 트랜잭션 구현
- [ ] **Repository**: `ThreadRepository.delete()` 구현 (Cascade)
- [ ] **Verify**: 삭제 후 Chat 데이터 연쇄 삭제 확인

---

## 4. Feature: Feedback Management

### 4.1 Domain 객체 설계 (Feedback)

#### Value Objects
- [ ] **FeedbackStatus** (Enum)
    - `PENDING`, `RESOLVED`
    - `canTransitionTo(next: FeedbackStatus)`: 상태 전이 가능 여부 (필요시)
    - `isResolved()`: 해결 여부

- [ ] **FeedbackType** (Enum 또는 Boolean Wrapper)
    - `POSITIVE`, `NEGATIVE`
    - `isPositive()`: 긍정 피드백 여부

#### Entity
- [ ] **Feedback** (Entity)
    - 필드: `id`, `chat: Chat`, `user: User`, `type: FeedbackType`, `status: FeedbackStatus`, `createdAt`
    - `create(chat, user, type)`: 정적 팩토리
    - `updateType(newType: FeedbackType)`: 피드백 유형 변경 (기존 피드백 수정 시)
    - `resolve()`: 상태를 RESOLVED로 변경
    - `isOwnedBy(userId: Long)`: 해당 피드백의 소유자 확인
    - `validateAdminAccess(user: User)`: 관리자만 상태 변경 가능
    - **테스트**: `FeedbackTest` - 생성, 상태 변경, 권한 검증

### 4.2 피드백 생성 (Create Feedback)
- [ ] **[TDD] Controller**: `FeedbackControllerTest.createFeedback()` 작성 (Failing)
    - **Edge Case 테스트**: 중복 피드백, 타인 채팅에 피드백 시도
- [ ] **Controller**: `FeedbackController.createFeedback()` 구현
- [ ] **[TDD] Service**: `FeedbackServiceTest.createFeedback()` 작성 (Failing)
- [ ] **Service Pseudo**: `FeedbackService.createFeedback()` 수도 코드 작성
    ```kotlin
    fun createOrUpdateFeedback(userId: Long, request: CreateFeedbackRequest): FeedbackResponse {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException()
        val chat = chatRepository.findById(request.chatId) ?: throw ChatNotFoundException()
        
        // 권한 체크: 본인 채팅 또는 관리자
        if (!user.canAccess(chat.belongsToUserId())) {
            throw AccessDeniedException()
        }
        
        // Upsert 로직
        val feedback = feedbackRepository.findByChatIdAndUserId(chat.id, user.id)
            ?.apply { updateType(FeedbackType.from(request.isPositive)) }
            ?: Feedback.create(chat, user, FeedbackType.from(request.isPositive))
        
        return FeedbackResponse.from(feedbackRepository.save(feedback))
    }
    ```
- [ ] **Service**: 중복 방지 로직(`upsert`) 구현
- [ ] **Repository**: `FeedbackRepository.findByChatIdAndUserId()` 구현
- [ ] **Verify**: 1인 1피드백 제약조건 확인

### 4.3 피드백 목록 조회 및 상태 변경
- [ ] **[TDD] Controller**: `FeedbackControllerTest` (List & UpdateStatus) 작성 (Failing)
- [ ] **Controller**: `listFeedbacks()`, `updateStatus()` 구현
- [ ] **[TDD] Service**: 서비스 테스트 작성
- [ ] **Service Pseudo**: 수도 코드 작성
    ```kotlin
    fun updateStatus(feedbackId: Long, user: User, newStatus: FeedbackStatus) {
        val feedback = feedbackRepository.findById(feedbackId) ?: throw FeedbackNotFoundException()
        
        // Domain에게 권한 검증 위임
        feedback.validateAdminAccess(user)
        
        // Domain에게 상태 변경 위임
        feedback.resolve() // 또는 feedback.updateStatus(newStatus)
        
        feedbackRepository.save(feedback)
    }
    ```
- [ ] **Service**: 로직 구현
- [ ] **Repository**: 필터링(긍정/부정/상태) 쿼리 구현
- [ ] **Verify**: 관리자 권한 동작 확인

---

## 5. Feature: Analytics & Reporting

### 5.1 Domain 객체 설계 (Stats)

#### Value Objects
- [ ] **DateRange** (Value Object)
    - `from: Instant`, `to: Instant`
    - `of(now: Instant)`: 현재 시점 기준 24시간 범위 생성 (정적 팩토리)
    - `contains(instant: Instant)`: 특정 시점이 범위 내인지 확인
    - **테스트**: `DateRangeTest`

- [ ] **ActivityStats** (Value Object / DTO)
    - `signupCount: Long`, `loginCount: Long`, `chatCount: Long`
    - 불변 객체로 설계
    - `toResponse()`: API 응답 DTO로 변환

### 5.2 활동 기록 통계 (Stats)
- [ ] **[TDD] Controller**: `AdminStatsControllerTest.getStats()` 작성 (Failing)
- [ ] **Controller**: `AdminStatsController.getStats()` 구현
- [ ] **[TDD] Service**: `AdminStatsServiceTest.getStats()` 작성 (Failing)
- [ ] **Service Pseudo**: 수도 코드 작성
    ```kotlin
    fun getStats(): ActivityStats {
        val range = DateRange.of(Instant.now())
        
        return ActivityStats(
            signupCount = userRepository.countByCreatedAtBetween(range.from, range.to),
            loginCount = loginLogRepository.countByCreatedAtBetween(range.from, range.to),
            chatCount = chatRepository.countByCreatedAtBetween(range.from, range.to)
        )
    }
    ```
- [ ] **Service**: 각 Repository Count 메소드 호출 및 집계
- [ ] **Repository**: `countByCreatedAtBetween` 등 통계 쿼리 최적화
- [ ] **Verify**: 날짜 범위(24시간) 정확도 확인

### 5.3 CSV 보고서 생성 (Report)

#### Domain Service
- [ ] **CsvReportGenerator** (Domain Service)
    - `generate(chats: Stream<Chat>)`: 채팅 데이터를 CSV 형식으로 변환
    - CSV 라이브러리 연동, 이스케이프 처리 등 기술적 로직 캡슐화
    - **테스트**: `CsvReportGeneratorTest` - 특수문자 이스케이프, 빈 데이터 처리

#### 구현
- [ ] **[TDD] Controller**: `AdminReportControllerTest.downloadCsv()` 작성 (Failing)
- [ ] **Controller**: `AdminReportController.downloadCsv()` 구현 (Response Header 설정)
- [ ] **[TDD] Service**: `AdminReportServiceTest` 작성
- [ ] **Service Pseudo**: 수도 코드 작성
    ```kotlin
    fun generateReport(outputStream: OutputStream) {
        val range = DateRange.of(Instant.now())
        val chatsStream = chatRepository.streamAllByCreatedAtBetween(range.from, range.to)
        
        csvReportGenerator.generate(chatsStream, outputStream)
    }
    ```
- [ ] **Service**: `Stream`을 이용한 데이터 조회 및 CSV 라이브러리 연동
- [ ] **Repository**: `streamAllByCreatedAtBetween` (Stream 반환) 구현
- [ ] **Verify**: 대량 데이터 처리 시 메모리 사용량 및 파일 포맷 검증
