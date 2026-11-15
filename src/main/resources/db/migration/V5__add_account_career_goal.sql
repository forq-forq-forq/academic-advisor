-- US-03: Store career aspiration goal on student_accounts during registration onboarding
ALTER TABLE student_accounts ADD COLUMN career_goal VARCHAR(255);
