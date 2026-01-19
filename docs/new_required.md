# 상세 요구사항 및 분석 (New Requirements)

@[docs/required.md]의 내용을 바탕으로 상세 분석, 엣지 케이스, 확장성 고려사항, 그리고 기술적 의사결정의 근거를 정리합니다.

---

## 1. 개요 및 기술적 접근 전략

### 1.1 기술 스택 및 아키텍처
- **Language/Framework**: Kotlin 1.9.x + Spring Boot 3.x.x
- **Database**: PostgreSQL 15.8+ (JSONB 활용 및 트랜잭션 안전성 확보)
- **Architecture**: Layered Architecture (Controller -> Service -> Repository)
    - **이유**: 익숙한 패턴으로 빠른 개발이 가능하며, 추후 도메인형 구조로 리팩토링하기 용이함.
- **Async/Non-blocking**: `Chat` 스트리밍 응답을 위해 Spring WebFlux 또는 MVC의 `SseEmitter` 고려.
    - **선택**: Spring MVC `SseEmitter` (Server-Sent Events).
    - **근거**: 
        1. **통신 특성**: LLM 챗봇은 "유저 질문 -> AI 답변(스트리밍)"의 순차적(Sequential) 구조입니다. 게임이나 실시간 채팅처럼 양쪽에서 비동기적으로 메시지를 쏟아내는 Full-Duplex 통신이 아닙니다.
        2. **복잡도 관리**: WebSocket은 연결 유지, 헤브비트(Heartbeat), 에러 핸들링, 로드밸런싱 등 인프라 복잡도가 높습니다. 반면 SSE는 표준 HTTP를 사용하므로 구현이 간단하고 기존 인프라(Auth, LB)와 호환성이 좋습니다.
        3. **MVP 효율성**: 제한된 시간(3시간) 내에 구현하기에 SSE가 훨씬 생산적입니다.

### 1.2 확장성 및 성능 고려사항
- **Database Indexing**: 조회 성능을 위해 `created_at`, `user_id`, `thread_id` 등에 인덱스 필수.
- **Reporting**: 대량의 데이터 조회 시 OOM(Out Of Memory) 방지를 위해 `Cursor` 기반 페이징 또는 Stream 처리 필요.

---

## 2. 기능별 상세 요구사항 및 분석

### 2.1 사용자 관리 및 인증 (User & Auth)

#### [분석 및 상세 스펙]
- **인증 방식**: JWT (Access Token)
    - **확장성 고려**: Access Token 탈취 시 보안 위협을 줄이기 위해 `Refresh Token` 도입을 고려할 수 있으나, 현재 "MVP/긴급 시연" 요건상 Access Token(유효기간 적절히 설정) 단일 방식으로 구현하되, 구조는 분리해둠.
- **패스워드 저장**: 평문 저장 금지. `BCrypt` 또는 `Argon2` 해싱 필수.
- **권한 관리**: `Role` 기반 (`MEMBER`, `ADMIN`). API 레벨에서 Annotation(`@PreAuthorize`) 또는 Interceptor로 권한 체크.

#### [엣지 케이스]
- **중복 이메일 가입 시도**: 고유 제약조건(Unique Constraint) 위반 예외 처리 및 사용자 친화적 메시지 응답.
- **잘못된 이메일 형식**: 정규식 검증(`@Valid`, `@Email`).
- **토큰 만료**: 만료된 토큰 요청 시 401 Unauthorized 명확한 응답 코드 반환.

#### [기술적 선택]
- **Spring Security vs Custom Filter**:
    - **선택**: Spring Security.
    - **근거**: 인증/인가 처리에 대한 표준이며, 추후 확장성(Oauth2 등)에 유리함. MVP 구현 시 다소 설정이 복잡할 수 있으나, 보안 사고 방지를 위해 필수.

---

### 2.2 대화(Chat) 및 스레드(Thread) 관리

#### [분석 및 상세 스펙]
- **스레드 생성 로직**:
    - "마지막 질문 후 30분 경과" 로직은 `Thread`의 `last_interaction_at` 필드를 업데이트하며 관리하거나, 가장 최근 `Chat`의 `created_at`을 조회하여 판단.
    - **Data Integrity**: 동시에 여러 요청이 올 경우 스레드가 중복 생성될 수 있음.
- **스트리밍(Streaming)**:
    - OpenAI API의 `stream=true` 옵션을 사용하여, 백엔드에서도 클라이언트로 `Server-Sent Events (SSE)` 형태로 실시간 전송.

#### [엣지 케이스]
- **동시성 이슈 (Concurrency)**:
    - 사용자가 30분 경계선에서 거의 동시에 2개의 질문을 보낼 경우, 스레드가 2개 생성될 수 있음. 트랜잭션 격리 수준 또는 Lock(Optimistic/Pessimistic) 고려. MVP에서는 `Synchronized` 또는 DB Unique Constraint로 방어.
- **OpenAI API 장애**: 외부 API 타임아웃 또는 5xx 에러 시, 적절한 에러 핸들링 및 재시도(Retry) 로직 필요.
- **삭제 정책**: 스레드 삭제 시 하위 채팅 메시지는? -> **Hard Delete** (요구사항 단순화) 또는 **Cascade Delete**.

#### [확장성 고려]
- **채팅 이력 문맥(Context) 관리**: 토큰 제한(Context Window)을 고려하여 프롬프트에 포함할 이전 대화 개수 제한(예: 최근 10개) 필요.

---

### 2.3 사용자 피드백 (Feedback)

#### [분석 및 상세 스펙]
- **제약 조건**: "하나의 대화에 오직 하나의 피드백".
    - DB Unique Index: `(user_id, chat_id)` 복합 유니크 인덱스 생성 필수.
- **권한 분리**: 일반 유저는 본인 채팅만, 관리자는 모든 채팅 피드백 생성 가능.
    - 관리자가 타인의 채팅에 피드백을 남길 때 `user_id`는 관리자 ID로 기록되는가? -> 요구사항 상 "각 사용자는 하나에 대화에 오직 하나의 피드백". 따라서 관리자도 개인 자격으로 피드백을 남기는 것으로 해석.

#### [엣지 케이스]
- **이미 피드백한 대화에 다시 피드백 시도**: 업데이트(Update)로 처리할지, 에러(Error)로 처리할지 결정 필요. -> 일반적으로 "수정" 허용이 UX에 좋으나, 요구사항 엄격성을 위해 "이미 존재함" 에러 처리 후 수정 API 별도 제공 혹은 Upsert 구현. 여기선 **Upsert(존재하면 수정)** 로직 추천.
- **없는 대화 ID로 피드백 요청**: 404 Not Found.

---

### 2.4 분석 및 보고 (Analytics & Report)

#### [분석 및 상세 스펙]
- **집계 기간**:
    - "요청 시점으로부터 하루 동안": `[Request Time - 24 Hours, Request Time]` 범위로 해석.
- **CSV 생성**:
    - 데이터가 많을 경우 메모리에 모두 로드하면 OOM 발생.
    - **Stream 방식**으로 DB에서 데이터를 읽어와 바로 Response OutputStream으로 쓰는 방식 채택.

#### [엣지 케이스]
- **데이터가 0건일 때**: 빈 CSV 파일 생성 또는 "데이터 없음" 메시지? -> 헤더만 있는 빈 CSV 제공.
- **특수문자 처리**: CSV 내용에 쉼표(,)나 개행이 포함된 경우 따옴표(") 이스케이프 처리 필수.

#### [기술적 선택]
- **CSV 라이브러리 vs String Builder**:
    - **선택**: `Apache Commons CSV` 또는 `OpenCSV` 라이브러리.
    - **근거**: 이스케이프 처리 등 엣지 케이스를 직접 구현하는 것보다 라이브러리가 안전함.

---

## 3. 데이터 모델링 (ERD Draft)

### Users
- `id` (PK, UUID or Long)
- `email` (Unique)
- `password` (Encrypted)
- `name`
- `role` (ENUM: MEMBER, ADMIN)
- `created_at`

### Threads
- `id` (PK)
- `user_id` (FK)
- `last_message_at` (Timestamp, 30분 로직 계산용)
- `created_at`

### Chats
- `id` (PK)
- `thread_id` (FK)
- `question` (Text)
- `answer` (Text, Streaming 응답 후 최종 저장)
- `created_at`

### Feedbacks
- `id` (PK)
- `chat_id` (FK)
- `user_id` (FK)
- `is_positive` (Boolean)
- `status` (ENUM: PENDING, RESOLVED)
- `created_at`
- **Unique Constraint**: `(chat_id, user_id)`

---

## 4. API 명세서 요약 (Endpoints)

| Method | URI | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 | Public |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) | Public |
| POST | `/api/v1/chats` | 대화 생성 (SSE Stream 지원) | Member/Admin |
| GET | `/api/v1/chats` | 대화 목록 조회 (Paging) | Member(본인)/Admin(전체) |
| DELETE | `/api/v1/threads/{threadId}` | 스레드 삭제 | Member(본인)/Admin(전체) |
| POST | `/api/v1/feedbacks` | 피드백 생성/수정 | Member/Admin |
| GET | `/api/v1/feedbacks` | 피드백 조회 | Member(본인)/Admin(전체) |
| PUT | `/api/v1/feedbacks/{feedbackId}/status` | 피드백 상태 변경 | Admin Only |
| GET | `/api/v1/admin/stats` | 활동 기록 통계 | Admin Only |
| GET | `/api/v1/admin/reports/csv` | CSV 보고서 다운로드 | Admin Only |
