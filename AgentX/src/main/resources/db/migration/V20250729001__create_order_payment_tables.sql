-- 订单和支付系统数据库表创建脚本 (PostgreSQL)
-- 作者: Claude Code
-- 创建时间: 2025-07-29
-- 描述: 为AgentX创建订单管理和支付系统相关表

-- 1. 创建订单表 (orders)
-- 存储订单基本信息，支持多种订单类型和支付方式
CREATE TABLE orders (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    order_no VARCHAR(100) NOT NULL UNIQUE,
    order_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amount DECIMAL(20,8) NOT NULL,
    currency VARCHAR(10) DEFAULT 'CNY',
    status INTEGER NOT NULL DEFAULT 1,
    expired_at TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_amount DECIMAL(20,8) DEFAULT 0.00000000,
    payment_platform VARCHAR(50),
    payment_type VARCHAR(50),
    provider_order_id VARCHAR(200),
    metadata JSONB,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 添加索引
-- 订单表索引
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_no ON orders(order_no);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_type ON orders(order_type);
CREATE INDEX idx_orders_payment_platform ON orders(payment_platform);
CREATE INDEX idx_orders_payment_type ON orders(payment_type);
CREATE INDEX idx_orders_provider_order_id ON orders(provider_order_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_expired_at ON orders(expired_at);


-- 添加表注释
COMMENT ON TABLE orders IS '订单表，存储各种类型的订单信息和支付方式';


-- 添加列注释
-- 订单表字段注释
COMMENT ON COLUMN orders.id IS '订单唯一ID';
COMMENT ON COLUMN orders.user_id IS '用户ID';
COMMENT ON COLUMN orders.order_no IS '订单号（唯一）';
COMMENT ON COLUMN orders.order_type IS '订单类型：RECHARGE(充值)、PURCHASE(购买)、SUBSCRIPTION(订阅)';
COMMENT ON COLUMN orders.title IS '订单标题';
COMMENT ON COLUMN orders.description IS '订单描述';
COMMENT ON COLUMN orders.amount IS '订单金额';
COMMENT ON COLUMN orders.currency IS '货币代码，默认CNY';
COMMENT ON COLUMN orders.status IS '订单状态：1-待支付，2-已支付，3-已取消，4-已退款，5-已过期';
COMMENT ON COLUMN orders.expired_at IS '订单过期时间';
COMMENT ON COLUMN orders.paid_at IS '支付完成时间';
COMMENT ON COLUMN orders.cancelled_at IS '取消时间';
COMMENT ON COLUMN orders.refunded_at IS '退款时间';
COMMENT ON COLUMN orders.refund_amount IS '退款金额';
COMMENT ON COLUMN orders.payment_platform IS '支付平台：alipay(支付宝)、wechat(微信支付)、stripe(Stripe)';
COMMENT ON COLUMN orders.payment_type IS '支付类型：web(网页支付)、qr_code(二维码支付)、mobile(移动端支付)、h5(H5支付)、mini_program(小程序支付)';
COMMENT ON COLUMN orders.provider_order_id IS '第三方支付平台的订单ID，用于查询支付状态和对账';
COMMENT ON COLUMN orders.metadata IS '订单扩展信息（JSONB格式）';

-- 注释说明：
-- 1. 订单状态枚举：1-待支付，2-已支付，3-已取消，4-已退款，5-已过期
-- 2. 支付状态枚举：1-创建，2-等待支付，3-支付成功，4-支付失败，5-已取消
-- 3. 订单类型支持：RECHARGE(充值)、PURCHASE(购买)、SUBSCRIPTION(订阅)等
-- 4. 支付平台支持：alipay(支付宝)、wechat(微信支付)、stripe(Stripe)等
-- 5. 支付类型支持：web(网页支付)、qr_code(二维码支付)、mobile(移动端支付)、h5(H5支付)、mini_program(小程序支付)等
-- 6. 所有金额字段使用DECIMAL(20,8)确保精度
-- 7. 使用JSONB存储扩展信息，便于灵活扩展和查询
-- 8. 建立了完善的索引以支持高效查询
-- 9. 支持软删除机制（deleted_at字段）
-- 10. provider_order_id用于与第三方支付平台进行状态同步和对账