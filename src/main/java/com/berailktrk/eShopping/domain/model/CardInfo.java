package com.berailktrk.eShopping.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


//Kart bilgilerini temsil eden value object
//Güvenlik nedeniyle gerçek kart bilgileri saklanmaz, sadece token kullanılır

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {
    
    //Tokenized kart numarası (gerçek kart numarası değil)
    //Örnek: tok_a94b5c7d8e0f1g2h3i4j5k6l
    
    private String token;
    
    //Kart sahibinin adı
    //Örnek: John Doe
    private String cardholderName;
    
    //Kartın son kullanma tarihi (MM/YY formatında)
    //Örnek: 12/25
    private String expiryDate;
    
    //Kartın son 4 hanesi (maskelenmiş görünüm için)
    //Örnek: ****1234
    private String maskedCardNumber;
    
    //Kart türü (VISA, MASTERCARD, AMEX, etc.)
    //Örnek: VISA
    private String cardType;
    
    //Token'ın oluşturulma tarihi
    //Örnek: 2025-01-01
    private java.time.Instant createdAt;
    
    //Token'ın son kullanma tarihi (opsiyonel)
    //Örnek: 2025-01-01
    private java.time.Instant expiresAt;
}
