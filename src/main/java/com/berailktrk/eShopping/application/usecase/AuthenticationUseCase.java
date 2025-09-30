package com.berailktrk.eShopping.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.model.UserRole;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.UserRepository;
import com.berailktrk.eShopping.infrastructure.security.JwtProvider;
import com.berailktrk.eShopping.presentation.dto.request.LoginRequest;
import com.berailktrk.eShopping.presentation.dto.request.RegisterRequest;
import com.berailktrk.eShopping.presentation.dto.response.AuthResponse;

import lombok.RequiredArgsConstructor;

/**
 * Authentication use case
 * Kullanıcı kayıt ve giriş işlemlerini yönetir
 */
@Service
@RequiredArgsConstructor
public class AuthenticationUseCase {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserDomainService userDomainService;
    private final AuditLogService auditLogService;

    /**
     * Yeni kullanıcı kaydı
     * 
     * @param request kayıt isteği
     * @return authentication yanıtı
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Şifre eşleşme kontrolü
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Şifreler eşleşmiyor");
        }

        // 2. Email kontrolü
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu email adresi zaten kullanılıyor");
        }

        // 3. Şifreyi hashle
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Kullanıcı oluştur
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(request.getRole() != null ? request.getRole() : UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // 5. JWT token oluştur
        String token = jwtProvider.generateToken(savedUser);

        // 6. Audit log kaydet (kullanıcı kendini kaydetti)
        AuditLog registerLog = auditLogService.logUserAction(
            savedUser, // Actor: Kaydolan kullanıcının kendisi
            AuditLogService.ACTION_USER_CREATED,
            savedUser.getId(),
            String.format("Yeni kullanıcı kaydı: %s", savedUser.getEmail())
        );
        auditLogRepository.save(registerLog);

        // 7. Yanıt oluştur
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    /**
     * Kullanıcı girişi
     * 
     * @param request giriş isteği
     * @return authentication yanıtı
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Kullanıcıyı bul
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email veya şifre hatalı"));

        // 2. Hesap kilidi kontrolü
        if (userDomainService.isLocked(user)) {
            throw new IllegalStateException(
                String.format("Hesabınız %s tarihine kadar kilitli", user.getLockedUntil())
            );
        }

        // 3. Hesap aktif mi kontrolü
        if (!user.getIsActive()) {
            throw new IllegalStateException("Hesabınız aktif değil");
        }

        // 4. Şifre kontrolü
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Başarısız giriş kaydı
            userDomainService.handleFailedLogin(user);
            userRepository.save(user);

            // Başarısız giriş audit log kaydet
            AuditLog failLog = auditLogService.logUserAction(
                null,
                "USER_LOGIN_FAILED",
                user.getId(),
                String.format("Başarısız giriş denemesi: %s (Toplam: %d)", 
                    user.getEmail(), user.getFailedLoginCount())
            );
            auditLogRepository.save(failLog);

            throw new IllegalArgumentException("Email veya şifre hatalı");
        }

        // 5. Başarılı giriş - failed count sıfırla ve last login güncelle
        userDomainService.recordSuccessfulLogin(user);
        userRepository.save(user);

        // 6. JWT token oluştur
        String token = jwtProvider.generateToken(user);

        // 7. Başarılı giriş audit log kaydet
        AuditLog loginLog = auditLogService.logUserAction(
            user,
            AuditLogService.ACTION_USER_LOGIN,
            user.getId(),
            String.format("Başarılı giriş: %s", user.getEmail())
        );
        auditLogRepository.save(loginLog);

        // 8. Yanıt oluştur
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    /**
     * Token doğrulama
     * 
     * @param token JWT token
     * @return geçerliyse kullanıcı
     */
    public User validateToken(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("Geçersiz veya süresi dolmuş token");
        }

        String email = jwtProvider.getEmailFromToken(token);
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
    }

   
}
