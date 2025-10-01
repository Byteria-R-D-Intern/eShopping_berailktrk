package com.berailktrk.eShopping.presentation.dto.request;

import com.berailktrk.eShopping.domain.model.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    private String password;

    @NotBlank(message = "Şifre tekrarı boş olamaz")
    private String confirmPassword;

    // Opsiyonel - Verilmezse CUSTOMER olarak atanır
    // NOT: Production'da bu field kaldırılmalı veya sadece admin tarafından ayarlanmalı
    private UserRole role;
}
