package com.berailktrk.eShopping.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.AuditLog;

/**
 * AuditLog repository interface
 * 
 * ÖNEMLİ: Sadece INSERT ve SELECT yapılabilir
 * UPDATE ve DELETE database trigger'ları tarafından engellenir
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Kullanıcıya göre audit log'ları getirir
     * 
     * @param actorUserId kullanıcı ID
     * @return audit log'lar
     */
    List<AuditLog> findByActorUserId(UUID actorUserId);

    /**
     * Action type'a göre audit log'ları getirir
     * 
     * @param actionType işlem tipi
     * @return audit log'lar
     */
    List<AuditLog> findByActionType(String actionType);

    /**
     * Kaynak tipine ve ID'ye göre audit log'ları getirir
     * 
     * @param resourceType kaynak tipi
     * @param resourceId kaynak ID
     * @return audit log'lar
     */
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId);

    /**
     * Belirli tarihten sonraki audit log'ları getirir
     * 
     * @param startTime başlangıç zamanı
     * @return audit log'lar
     */
    List<AuditLog> findByCreatedAtAfter(Instant startTime);

    /**
     * Tarih aralığındaki audit log'ları getirir
     * 
     * @param startTime başlangıç zamanı
     * @param endTime bitiş zamanı
     * @return audit log'lar
     */
    List<AuditLog> findByCreatedAtBetween(Instant startTime, Instant endTime);
}
