-- Flyway migration V2: Add cart_items table for US-11 (Semester Cart)

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_items_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_cart_items_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_student ON cart_items(student_id);

