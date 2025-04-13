CREATE TABLE plugins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50),
    status INT NOT NULL DEFAULT 1,
    version VARCHAR(50),
    repository_url VARCHAR(255) NOT NULL,
    license VARCHAR(50) NOT NULL,
    icon VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_repository_url (repository_url)
);
