-- Additive migration for existing installations; compatible with SQLite and PostgreSQL.
-- Run once BEFORE starting the upgraded application (see docs/US-29.md).
ALTER TABLE student_courses ADD COLUMN letter_grade VARCHAR(10);
ALTER TABLE student_courses ADD COLUMN semester_taken VARCHAR(50);
