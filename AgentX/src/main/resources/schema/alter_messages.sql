ALTER TABLE messages 
ADD COLUMN message_type VARCHAR(50) DEFAULT 'TEXT' AFTER content; 