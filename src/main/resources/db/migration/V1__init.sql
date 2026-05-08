-- =====================
-- USERS
-- =====================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    email VARCHAR(100),
    is_new BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================
-- AUTH TOKENS
-- =====================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(100),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================
-- ADDRESSES
-- =====================

CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    label VARCHAR(50),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    latitude DECIMAL NOT NULL,
    longitude DECIMAL NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================
-- PLACES
-- =====================

CREATE TABLE places (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    phone VARCHAR(20) NOT NULL,
    is_pure_veg BOOLEAN NOT NULL DEFAULT FALSE,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    latitude DECIMAL NOT NULL,
    longitude DECIMAL NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_places_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE place_changes (
    id UUID PRIMARY KEY,
    place_id UUID,
    owner_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    phone VARCHAR(20) NOT NULL,
    is_pure_veg BOOLEAN NOT NULL DEFAULT FALSE,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    latitude DECIMAL NOT NULL,
    longitude DECIMAL NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_place_changes_place FOREIGN KEY (place_id) REFERENCES places(id),
    CONSTRAINT fk_place_changes_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_place_changes_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

-- =====================
-- MEDIA
-- =====================

CREATE TABLE media (
    id UUID PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(50),
    size INT,
    created_at TIMESTAMP NOT NULL
);

-- =====================
-- MENU
-- =====================

CREATE TABLE menu_categories (
    id UUID PRIMARY KEY,
    place_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_menu_categories_place FOREIGN KEY (place_id) REFERENCES places(id)
);

CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    place_id UUID NOT NULL,
    category_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    media_id UUID,
    price DECIMAL NOT NULL,
    is_veg BOOLEAN NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flagged_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_menu_items_place FOREIGN KEY (place_id) REFERENCES places(id),
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES menu_categories(id),
    CONSTRAINT fk_menu_items_media FOREIGN KEY (media_id) REFERENCES media(id)
);

-- =====================
-- CART
-- =====================

CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    place_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_carts_place FOREIGN KEY (place_id) REFERENCES places(id)
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- =====================
-- ORDERS
-- =====================

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    place_id UUID NOT NULL,
    address_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_place FOREIGN KEY (place_id) REFERENCES places(id),
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- =====================
-- PRICE BREAKDOWN
-- =====================

CREATE TABLE order_breakdown (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    subtotal DECIMAL NOT NULL,
    tax_amount DECIMAL NOT NULL DEFAULT 0,
    delivery_fee DECIMAL NOT NULL DEFAULT 0,
    discount_amount DECIMAL NOT NULL DEFAULT 0,
    total_amount DECIMAL NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_charges_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- =====================
-- PAYMENTS
-- =====================

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    provider VARCHAR(100),
    amount DECIMAL NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- =====================
-- CANCELLATIONS & REFUNDS
-- =====================

CREATE TABLE cancellations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    cancelled_by VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_cancellations_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    amount DECIMAL NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- =====================
-- DELIVERY
-- =====================

CREATE TABLE delivery_agents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_delivery_agents_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE agent_documents (
    id UUID PRIMARY KEY,

    agent_id UUID NOT NULL,

    document_type VARCHAR(50) NOT NULL,

    document_number VARCHAR(100),

    document_url VARCHAR(500),

    verified BOOLEAN NOT NULL DEFAULT FALSE,
    rejected BOOLEAN NOT NULL DEFAULT FALSE,

    rejection_reason VARCHAR(255),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_agent_documents_agent
    FOREIGN KEY (agent_id) REFERENCES delivery_agents(id)
);

CREATE TABLE deliveries (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP,
    picked_at TIMESTAMP,
    delivered_at TIMESTAMP,
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_deliveries_agent FOREIGN KEY (agent_id) REFERENCES delivery_agents(id)
);

-- =====================
-- USERS
-- =====================

CREATE INDEX idx_users_phone ON users(phone);

-- =====================
-- USER ROLES
-- =====================

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- =====================
-- REFRESH TOKENS
-- =====================

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- =====================
-- ADDRESSES
-- =====================

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- =====================
-- PLACES
-- =====================

CREATE INDEX idx_places_owner_id ON places(owner_id);
CREATE INDEX idx_places_city ON places(city);
CREATE INDEX idx_places_is_open ON places(is_open);

-- =====================
-- MENU
-- =====================

CREATE INDEX idx_menu_categories_place_id ON menu_categories(place_id);

CREATE INDEX idx_menu_items_place_id ON menu_items(place_id);
CREATE INDEX idx_menu_items_category_id ON menu_items(category_id);
CREATE INDEX idx_menu_items_is_available ON menu_items(is_available);

-- =====================
-- CART
-- =====================

CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_place_id ON carts(place_id);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);

-- =====================
-- ORDERS
-- =====================

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_place_id ON orders(place_id);
CREATE INDEX idx_orders_status ON orders(status);

-- =====================
-- ORDER ITEMS
-- =====================

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- =====================
-- PAYMENTS
-- =====================

CREATE INDEX idx_payments_order_id ON payments(order_id);

-- =====================
-- DELIVERY
-- =====================

CREATE INDEX idx_delivery_agents_user_id ON delivery_agents(user_id);

CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX idx_deliveries_agent_id ON deliveries(agent_id);