-- T-06: Role-based access control columns on students and student_accounts
ALTER TABLE students ADD COLUMN role VARCHAR(30) DEFAULT 'STUDENT';
ALTER TABLE student_accounts ADD COLUMN role VARCHAR(30) DEFAULT 'STUDENT';

UPDATE students SET role = 'STUDENT' WHERE role IS NULL;
UPDATE student_accounts SET role = 'STUDENT' WHERE role IS NULL;
