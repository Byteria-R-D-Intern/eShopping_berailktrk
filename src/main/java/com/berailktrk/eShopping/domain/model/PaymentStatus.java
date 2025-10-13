package com.berailktrk.eShopping.domain.model;


//Ödeme durumu enum'u - PostgreSQL payment_status type ile eşleşir
public enum PaymentStatus {
    //Henüz ödeme yapılmadı    
    NONE,
    
    //Ödeme yetkilendirildi (authorized) - kart bilgileri doğrulandı, tutar rezerve edildi    
    AUTHORIZED,
    
    
    //Ödeme tahsil edildi (captured) - para hesaptan çekildi    
    CAPTURED,
    
    
    //Ödeme iade edildi    
    REFUNDED
}
