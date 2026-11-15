-- US-09: Academic course registration windows for proactive dashboard alerts
CREATE TABLE IF NOT EXISTS registration_periods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    term_name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500)
);
