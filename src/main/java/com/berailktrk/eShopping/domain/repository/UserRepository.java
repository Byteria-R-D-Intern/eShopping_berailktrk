package com.berailktrk.eShopping.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.User;

/**
 * User repository interface
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Email'e göre kullanıcı bul
     * 
     * @param email kullanıcı email
     * @return kullanıcı (varsa)
     */
    Optional<User> findByEmail(String email);

    /**
     * Email'in var olup olmadığını kontrol et
     * 
     * @param email kontrol edilecek email
     * @return varsa true
     */
    boolean existsByEmail(String email);
}
