CREATE TABLE IF NOT EXISTS chat_room (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    is_group BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_room_members (
    id VARCHAR(36) PRIMARY KEY,
    chat_room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(50),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(36) UNIQUE,
    chat_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT,
    type VARCHAR(50),
    sequence INT,
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);