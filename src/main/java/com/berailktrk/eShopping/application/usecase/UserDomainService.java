package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.User;

/**
 * User entity'si için uygulama servis katmanı
 * Durum değişikliklerini ve karmaşık business logic'i yönetir
 */
@Service
public class UserDomainService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * Hesabın şu anda kilitli olup olmadığını kontrol eder
     * 
     * @param user kontrol edilecek kullanıcı
     * @return hesap kilitliyse true, değilse false
     */
    public boolean isLocked(User user) {
        return user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil());
    }

    /**
     * Başarısız giriş sayısını artırır ve eşik değer aşılırsa hesabı kilitler
     * 
     * @param user başarısız giriş sayısı artırılacak kullanıcı
     */
    public void handleFailedLogin(User user) {
        int currentCount = user.getFailedLoginCount();
        user.setFailedLoginCount(currentCount + 1);
        
        System.out.println("DEBUG: Failed login count increased from " + currentCount + " to " + user.getFailedLoginCount());
        
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            lockAccount(user, LOCK_DURATION_MINUTES);
        }
    }

    /**
     * Başarısız giriş sayısını sıfırlar ve hesap kilidini kaldırır
     * 
     * @param user başarısız giriş denemeleri sıfırlanacak kullanıcı
     */
    public void resetFailedLoginAttempts(User user) {
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
    }

    /**
     * Hesabı belirtilen süre boyunca kilitler (dakika cinsinden)
     * 
     * @param user kilitlenecek kullanıcı
     * @param durationMinutes kilitleme süresi (dakika)
     */
    public void lockAccount(User user, int durationMinutes) {
        Instant lockUntil = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        user.setLockedUntil(lockUntil);
    }

    /**
     * Hesabı belirtilen zamana kadar kilitler
     * 
     * @param user kilitlenecek kullanıcı
     * @param until hesabın kilitli kalacağı son zaman
     */
    public void lockAccountUntil(User user, Instant until) {
        user.setLockedUntil(until);
    }

    /**
     * Hesabın kilidini hemen açar
     * 
     * @param user kilidi açılacak kullanıcı
     */
    public void unlockAccount(User user) {
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
    }

    /**
     * Son giriş zamanını günceller ve başarısız denemeleri sıfırlar
     * 
     * @param user başarılı giriş yapan kullanıcı
     */
    public void recordSuccessfulLogin(User user) {
        user.setLastLoginAt(Instant.now());
        resetFailedLoginAttempts(user);
    }

    /**
     * Kullanıcı hesabını devre dışı bırakır
     * 
     * @param user devre dışı bırakılacak kullanıcı
     */
    public void deactivateUser(User user) {
        user.setIsActive(false);
    }

    /**
     * Kullanıcı hesabını aktif hale getirir
     * 
     * @param user aktif hale getirilecek kullanıcı
     */
    public void activateUser(User user) {
        user.setIsActive(true);
        resetFailedLoginAttempts(user);
    }

    /**
     * Hesabın giriş yapıp yapamayacağını doğrular
     * 
     * @param user doğrulanacak kullanıcı
     * @throws IllegalStateException kullanıcı giriş yapamıyorsa
     */
    public void validateCanLogin(User user) {
        if (!user.getIsActive()) {
            throw new IllegalStateException("Hesap aktif değil");
        }
        
        if (isLocked(user)) {
            throw new IllegalStateException("Hesap şu zamana kadar kilitli: " + user.getLockedUntil());
        }
    }
}
