-- US-18: Persistent 8-semester multi-year degree roadmap placements
CREATE TABLE IF NOT EXISTS roadmap_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    semester INTEGER NOT NULL,
    UNIQUE(student_id, course_id)
);
