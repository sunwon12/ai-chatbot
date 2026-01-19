package sionic.auth.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sionic.auth.dto.*
import sionic.auth.jwt.JwtProvider
import sionic.common.exception.DuplicateEmailException
import sionic.common.exception.UserNotFoundException
import sionic.user.domain.Email
import sionic.user.domain.User
import sionic.user.repository.UserRepository

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val loginLogService: LoginLogService
) {
    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        // 1. Email VO 생성 (내부에서 형식 검증)
        val email = Email(request.email)

        // 2. 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw DuplicateEmailException()
        }

        // 3. User 엔티티 생성 (정적 팩토리, 내부에서 Password 암호화)
        val user = User.create(
            email = email,
            rawPassword = request.password,
            name = request.name,
            encoder = passwordEncoder
        )

        // 4. 저장 및 반환
        return SignupResponse.from(userRepository.save(user))
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        // 1. 유저 조회
        val user = userRepository.findByEmail(Email(request.email))
            ?: throw UserNotFoundException()

        // 2. 비밀번호 검증 (User 엔티티에게 위임)
        user.authenticate(request.password, passwordEncoder)

        // 3. 로그인 기록 저장 (활동 통계용)
        loginLogService.log(user)

        // 4. 토큰 생성
        val token = jwtProvider.createToken(user)

        return LoginResponse(accessToken = token)
    }
}

