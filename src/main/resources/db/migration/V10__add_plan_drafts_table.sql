-- US-22: Save multiple named semester & roadmap plan drafts per student
CREATE TABLE IF NOT EXISTS plan_drafts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    pacing_strategy VARCHAR(40),
    total_credits INTEGER NOT NULL DEFAULT 0,
    items_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
