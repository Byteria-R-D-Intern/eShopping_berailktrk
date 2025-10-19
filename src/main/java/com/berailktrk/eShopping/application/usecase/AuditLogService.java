package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.User;

//Audit log service - log oluşturma ve filtreleme işlemleri
//Append-only: UPDATE/DELETE yok, immutable işlemler
@Service
public class AuditLogService {

    //Action Type sabitleri
    public static final String ACTION_USER_CREATED = "USER_CREATED";
    public static final String ACTION_USER_UPDATED = "USER_UPDATED";
    public static final String ACTION_USER_DELETED = "USER_DELETED";
    public static final String ACTION_USER_LOGIN = "USER_LOGIN";
    public static final String ACTION_USER_LOGOUT = "USER_LOGOUT";
    public static final String ACTION_USER_PASSWORD_CHANGED = "USER_PASSWORD_CHANGED";
    
    public static final String ACTION_PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String ACTION_PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String ACTION_PRODUCT_DELETED = "PRODUCT_DELETED";
    public static final String ACTION_PRODUCT_ACTIVATED = "PRODUCT_ACTIVATED";
    public static final String ACTION_PRODUCT_DEACTIVATED = "PRODUCT_DEACTIVATED";
    
    public static final String ACTION_INVENTORY_UPDATED = "INVENTORY_UPDATED";
    public static final String ACTION_INVENTORY_STOCK_ADDED = "INVENTORY_STOCK_ADDED";
    public static final String ACTION_INVENTORY_STOCK_RESERVED = "INVENTORY_STOCK_RESERVED";
    public static final String ACTION_INVENTORY_STOCK_RELEASED = "INVENTORY_STOCK_RELEASED";
    
    public static final String ACTION_ORDER_CREATED = "ORDER_CREATED";
    public static final String ACTION_ORDER_PAID = "ORDER_PAID";
    public static final String ACTION_ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ACTION_ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ACTION_ORDER_REFUNDED = "ORDER_REFUNDED";
    
    public static final String ACTION_PAYMENT_INITIATED = "PAYMENT_INITIATED";
    public static final String ACTION_PAYMENT_AUTHORIZED = "PAYMENT_AUTHORIZED";
    public static final String ACTION_PAYMENT_CAPTURED = "PAYMENT_CAPTURED";
    public static final String ACTION_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ACTION_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    
    public static final String ACTION_PAYMENT_METHOD_ADDED = "PAYMENT_METHOD_ADDED";
    public static final String ACTION_PAYMENT_METHOD_UPDATED = "PAYMENT_METHOD_UPDATED";
    public static final String ACTION_PAYMENT_METHOD_DELETED = "PAYMENT_METHOD_DELETED";
    
    //Resource Type sabitleri
    public static final String RESOURCE_USER = "USER";
    public static final String RESOURCE_PRODUCT = "PRODUCT";
    public static final String RESOURCE_INVENTORY = "INVENTORY";
    public static final String RESOURCE_ORDER = "ORDER";
    public static final String RESOURCE_CART = "CART";
    public static final String RESOURCE_PAYMENT = "PAYMENT";
    public static final String RESOURCE_PAYMENT_METHOD = "PAYMENT_METHOD";

    //Temel audit log oluştur
    public AuditLog createLog(User actorUser, String actionType, String resourceType,UUID resourceId, String summary) {
        return AuditLog.builder()
            .actorUser(actorUser)
            .actionType(actionType)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .summary(summary)
            .build();
    }

    //Detaylı audit log oluştur (before/after değerleri ile)
    public AuditLog createLogWithDetails(
        User actorUser,
        String actionType,
        String resourceType,
        UUID resourceId,
        String summary,
        Map<String, Object> details
    ) {
        return AuditLog.builder()
            .actorUser(actorUser)
            .actionType(actionType)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .summary(summary)
            .details(sanitizeDetails(details))
            .build();
    }

    //Kullanıcı işlemleri için log oluştur
    public AuditLog logUserAction(User actorUser, String actionType, UUID targetUserId, String summary) {
        return createLog(actorUser, actionType, RESOURCE_USER, targetUserId, summary);
    }

    //Ürün işlemleri için log oluştur
    public AuditLog logProductAction(User actorUser, String actionType, UUID productId, String summary) {
        return createLog(actorUser, actionType, RESOURCE_PRODUCT, productId, summary);
    }

    //Sipariş işlemleri için log oluştur
    public AuditLog logOrderAction(User actorUser, String actionType, UUID orderId, String summary) {
        return createLog(actorUser, actionType, RESOURCE_ORDER, orderId, summary);
    }

    //Sipariş işlemleri için detaylı log oluştur
    public AuditLog logOrderActionWithDetails(
        User actorUser, 
        String actionType, 
        UUID orderId, 
        String summary,
        Map<String, Object> orderDetails
    ) {
        return createLogWithDetails(actorUser, actionType, RESOURCE_ORDER, orderId, summary, orderDetails);
    }

    //Stok işlemleri için log oluştur
    public AuditLog logInventoryAction(
        User actorUser, 
        String actionType, 
        UUID productId, 
        String summary,
        Map<String, Object> inventoryDetails
    ) {
        return createLogWithDetails(actorUser, actionType, RESOURCE_INVENTORY, productId, summary, inventoryDetails);
    }

    //Ödeme işlemleri için log oluştur
    public AuditLog logPaymentAction(
        User actorUser, 
        String actionType, 
        UUID paymentId, 
        String summary
    ) {
        return createLog(actorUser, actionType, RESOURCE_PAYMENT, paymentId, summary);
    }

    //Ödeme işlemleri için detaylı log oluştur
    public AuditLog logPaymentActionWithDetails(
        User actorUser, 
        String actionType, 
        UUID paymentId, 
        String summary,
        Map<String, Object> paymentDetails
    ) {
        return createLogWithDetails(actorUser, actionType, RESOURCE_PAYMENT, paymentId, summary, paymentDetails);
    }

    //Ödeme yöntemi işlemleri için log oluştur
    public AuditLog logPaymentMethodAction(
        User actorUser, 
        String actionType, 
        UUID paymentMethodId, 
        String summary
    ) {
        return createLog(actorUser, actionType, RESOURCE_PAYMENT_METHOD, paymentMethodId, summary);
    }

    //Sistem işlemleri için log oluştur (kullanıcı yok)
    public AuditLog logSystemAction(String actionType, String resourceType, UUID resourceId, String summary) {
        return createLog(null, actionType, resourceType, resourceId, summary);
    }

    //Before/After değişiklikleri için detay map'i oluştur
    public Map<String, Object> createBeforeAfterDetails(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> details = new HashMap<>();
        details.put("before", before);
        details.put("after", after);
        details.put("timestamp", Instant.now().toString());
        return details;
    }

    //Detayları sanitize et (hassas bilgileri temizle)
    private Map<String, Object> sanitizeDetails(Map<String, Object> details) {
        if (details == null) {
            return null;
        }

        Map<String, Object> sanitized = new HashMap<>(details);

        //Hassas alanları temizle
        sanitized.remove("password");
        sanitized.remove("password_hash");
        sanitized.remove("credit_card");
        sanitized.remove("cvv");
        sanitized.remove("card_number");
        sanitized.remove("provider_token");
        sanitized.remove("secret");
        sanitized.remove("api_key");

        //Nested map'lerde de temizle
        sanitized.forEach((key, value) -> {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                sanitized.put(key, sanitizeDetails(nestedMap));
            }
        });

        return sanitized;
    }

    //Log doğrulama
    public void validateLog(AuditLog auditLog) {
        if (auditLog.getActionType() == null || auditLog.getActionType().trim().isEmpty()) {
            throw new IllegalStateException("Action type boş olamaz");
        }

        if (auditLog.getSummary() == null || auditLog.getSummary().trim().isEmpty()) {
            throw new IllegalStateException("Summary boş olamaz");
        }
    }

    //Belirli bir kaynağın audit geçmişini filtrele
    public List<AuditLog> filterByResource(List<AuditLog> auditLogs, String resourceType, UUID resourceId) {
        return auditLogs.stream()
            .filter(log -> resourceType.equals(log.getResourceType()) && 
                          resourceId.equals(log.getResourceId()))
            .toList();
    }

    //Belirli bir kullanıcının işlemlerini filtrele
    public List<AuditLog> filterByUser(List<AuditLog> auditLogs, UUID userId) {
        return auditLogs.stream()
            .filter(log -> log.getActorUser() != null && 
                          userId.equals(log.getActorUser().getId()))
            .toList();
    }

    //Belirli bir işlem tipine göre filtrele
    public List<AuditLog> filterByActionType(List<AuditLog> auditLogs, String actionType) {
        return auditLogs.stream()
            .filter(log -> actionType.equals(log.getActionType()))
            .toList();
    }

    //Belirli bir tarih aralığındaki log'ları filtrele
    public List<AuditLog> filterByDateRange(List<AuditLog> auditLogs, Instant startTime, Instant endTime) {
        return auditLogs.stream()
            .filter(log -> !log.getCreatedAt().isBefore(startTime) && 
                          !log.getCreatedAt().isAfter(endTime))
            .toList();
    }

    //Sistem işlemlerini filtrele (actor_user_id = NULL)
    public List<AuditLog> filterSystemActions(List<AuditLog> auditLogs) {
        return auditLogs.stream()
            .filter(log -> log.getActorUser() == null)
            .toList();
    }

    //Kullanıcı işlemlerini filtrele (actor_user_id != NULL)
    public List<AuditLog> filterUserActions(List<AuditLog> auditLogs) {
        return auditLogs.stream()
            .filter(log -> log.getActorUser() != null)
            .toList();
    }

    //Log özeti oluştur (action_type -> count)
    public Map<String, Long> createActionTypeSummary(List<AuditLog> auditLogs) {
        return auditLogs.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                AuditLog::getActionType,
                java.util.stream.Collectors.counting()
            ));
    }

    //Kullanıcı bazında aktivite özeti (user_id -> action_count)
    public Map<UUID, Long> createUserActivitySummary(List<AuditLog> auditLogs) {
        return auditLogs.stream()
            .filter(log -> log.getActorUser() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getActorUser().getId(),
                java.util.stream.Collectors.counting()
            ));
    }
}
