package com.berailktrk.eShopping.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.AuditLog;

// AuditLog Repository - Audit trail yönetimi
// ÖNEMLİ: Sadece INSERT ve SELECT yapılabilir (UPDATE/DELETE trigger ile engellenir)
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Kullanıcıya göre audit log'ları getir
    List<AuditLog> findByActorUserId(UUID actorUserId);

    // Action type'a göre audit log'ları getir
    List<AuditLog> findByActionType(String actionType);

    // Kaynak tipine ve ID'ye göre audit log'ları getir
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId);

    // Belirli tarihten sonraki audit log'ları getir
    List<AuditLog> findByCreatedAtAfter(Instant startTime);

    // Tarih aralığındaki audit log'ları getir
    List<AuditLog> findByCreatedAtBetween(Instant startTime, Instant endTime);
}
