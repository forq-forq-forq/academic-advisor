-- US-16: Add course offering season availability (FALL, SPRING, BOTH)
ALTER TABLE courses ADD COLUMN season VARCHAR(16) DEFAULT 'BOTH';
