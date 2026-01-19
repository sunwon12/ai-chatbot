package sionic.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import sionic.IntegrationTestBase
import sionic.auth.dto.LoginRequest
import sionic.auth.dto.SignupRequest

@AutoConfigureMockMvc
class AuthControllerTest : IntegrationTestBase() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Nested
    @DisplayName("회원가입 시")
    inner class Signup {

        @Test
        @DisplayName("올바른 정보로 회원가입에 성공한다")
        fun signupSuccess() {
            val request = SignupRequest(
                email = "test@example.com",
                password = "password123",
                name = "테스트"
            )

            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
        }

        @Test
        @DisplayName("중복 이메일로 회원가입에 실패한다")
        fun signupFailWithDuplicateEmail() {
            val request = SignupRequest(
                email = "duplicate@example.com",
                password = "password123",
                name = "테스트"
            )

            // 첫 번째 가입
            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isCreated)

            // 중복 가입 시도
            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."))
        }

        @Test
        @DisplayName("잘못된 이메일 형식으로 회원가입에 실패한다")
        fun signupFailWithInvalidEmail() {
            val request = SignupRequest(
                email = "invalid-email",
                password = "password123",
                name = "테스트"
            )

            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("로그인 시")
    inner class Login {

        @Test
        @DisplayName("올바른 정보로 로그인에 성공한다")
        fun loginSuccess() {
            // 먼저 회원가입
            val signupRequest = SignupRequest(
                email = "login@example.com",
                password = "password123",
                name = "테스트"
            )
            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest))
            ).andExpect(status().isCreated)

            // 로그인
            val loginRequest = LoginRequest(
                email = "login@example.com",
                password = "password123"
            )
            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인에 실패한다")
        fun loginFailWithNonExistentEmail() {
            val request = LoginRequest(
                email = "nonexistent@example.com",
                password = "password123"
            )

            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인에 실패한다")
        fun loginFailWithWrongPassword() {
            // 먼저 회원가입
            val signupRequest = SignupRequest(
                email = "wrongpw@example.com",
                password = "password123",
                name = "테스트"
            )
            mockMvc.perform(
                post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signupRequest))
            ).andExpect(status().isCreated)

            // 잘못된 비밀번호로 로그인
            val loginRequest = LoginRequest(
                email = "wrongpw@example.com",
                password = "wrongpassword"
            )
            mockMvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
        }
    }
}
