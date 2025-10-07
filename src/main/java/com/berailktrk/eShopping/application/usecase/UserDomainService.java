package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.User;

//User domain service - durum değişiklikleri ve business logic
@Service
public class UserDomainService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    //Hesabın şu anda kilitli olup olmadığını kontrol et
    public boolean isLocked(User user) {
        return user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil());
    }

    //Başarısız giriş sayısını artır ve eşik değer aşılırsa hesabı kilitle
    public void handleFailedLogin(User user) {
        int currentCount = user.getFailedLoginCount();
        user.setFailedLoginCount(currentCount + 1);
        
        System.out.println("DEBUG: Failed login count increased from " + currentCount + " to " + user.getFailedLoginCount());
        
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            lockAccount(user, LOCK_DURATION_MINUTES);
        }
    }

    //Başarısız giriş sayısını sıfırla ve hesap kilidini kaldır
    public void resetFailedLoginAttempts(User user) {
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
    }

    //Hesabı belirtilen süre boyunca kilitle (dakika cinsinden)
    public void lockAccount(User user, int durationMinutes) {
        Instant lockUntil = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        user.setLockedUntil(lockUntil);
    }

    //Hesabı belirtilen zamana kadar kilitle
    public void lockAccountUntil(User user, Instant until) {
        user.setLockedUntil(until);
    }

    //Hesabın kilidini hemen aç
    public void unlockAccount(User user) {
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
    }

    //Son giriş zamanını güncelle ve başarısız denemeleri sıfırla
    public void recordSuccessfulLogin(User user) {
        user.setLastLoginAt(Instant.now());
        resetFailedLoginAttempts(user);
    }

    //Kullanıcı hesabını devre dışı bırak
    public void deactivateUser(User user) {
        user.setIsActive(false);
    }

    //Kullanıcı hesabını aktif hale getir
    public void activateUser(User user) {
        user.setIsActive(true);
        resetFailedLoginAttempts(user);
    }

    //Hesabın giriş yapıp yapamayacağını doğrula
    public void validateCanLogin(User user) {
        if (!user.getIsActive()) {
            throw new IllegalStateException("Hesap aktif değil");
        }
        
        if (isLocked(user)) {
            throw new IllegalStateException("Hesap şu zamana kadar kilitli: " + user.getLockedUntil());
        }
    }
}
