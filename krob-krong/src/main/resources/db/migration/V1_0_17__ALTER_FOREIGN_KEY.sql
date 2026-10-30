-- 1. Drop the payment foreign key if it exists
ALTER TABLE payment
    DROP CONSTRAINT IF EXISTS payment_enrollment_id_fkey;

-- 2. Change payment.enrollment_id to VARCHAR(255)
ALTER TABLE payment
    ALTER COLUMN enrollment_id TYPE VARCHAR(255)
        USING enrollment_id::VARCHAR;

-- 3. Recreate the payment foreign key
ALTER TABLE payment
    ADD CONSTRAINT payment_enrollment_id_fkey
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollment(id);

-- 4. Add course_id to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS course_id UUID;

-- 5. Add days_of_week to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS days_of_week VARCHAR(255);

-- 6. Change schedule.days_of_week from VARCHAR to JSONB
ALTER TABLE schedule
    ALTER COLUMN days_of_week TYPE JSONB
        USING days_of_week::JSONB;

-- 7. Add max_students to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS max_students INTEGER NOT NULL DEFAULT 1;

-- 8. Add name to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS name VARCHAR(100) NOT NULL DEFAULT '';

-- 9. Add status to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE';


-- 10. Add subject_id to schedule if it does not exist
ALTER TABLE schedule
    ADD COLUMN IF NOT EXISTS subject_id UUID;