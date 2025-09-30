-- eShopping Database Reset Script
-- Bu script database'i temizleyip yeniden oluşturur

-- 1) Tüm tabloları sil (cascade)
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS payment_tokens CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS carts CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 2) Enum tiplerini sil (artık kullanmıyoruz, VARCHAR kullanacağız)
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS payment_status CASCADE;

-- 3) Trigger ve function'ları sil
DROP TRIGGER IF EXISTS trg_cart_items_after_insert ON cart_items CASCADE;
DROP TRIGGER IF EXISTS trg_cart_items_after_update ON cart_items CASCADE;
DROP TRIGGER IF EXISTS trg_cart_items_after_delete ON cart_items CASCADE;
DROP FUNCTION IF EXISTS trg_refresh_cart_updated_at() CASCADE;

DROP TRIGGER IF EXISTS trg_audit_logs_no_update ON audit_logs CASCADE;
DROP TRIGGER IF EXISTS trg_audit_logs_no_delete ON audit_logs CASCADE;
DROP FUNCTION IF EXISTS audit_logs_prevent_modification() CASCADE;
DROP FUNCTION IF EXISTS insert_audit_log(UUID, TEXT, TEXT, UUID, TEXT, JSONB) CASCADE;

-- 4) Extension'ı aktif tut (gerekebilir)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- NOT: Artık Hibernate otomatik olarak tabloları oluşturacak (spring.jpa.hibernate.ddl-auto=update)
-- Enum değil VARCHAR kolonlar oluşturacak
