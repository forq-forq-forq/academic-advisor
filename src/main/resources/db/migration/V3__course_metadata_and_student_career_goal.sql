-- Flyway migration V3: Add course difficulty/description metadata and student career_goal
ALTER TABLE courses ADD COLUMN difficulty INTEGER DEFAULT 3;
ALTER TABLE courses ADD COLUMN description VARCHAR(500);
ALTER TABLE students ADD COLUMN career_goal VARCHAR(255);

