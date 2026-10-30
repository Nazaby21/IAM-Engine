-- 1. Drop the foreign key referencing enrollment.id
ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS attendance_enrollment_id_fkey;

-- 2. Change attendance.enrollment_id from UUID to VARCHAR(255)
ALTER TABLE attendance
    ALTER COLUMN enrollment_id TYPE VARCHAR(255)
        USING enrollment_id::VARCHAR;

-- 3. Add reason column
ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS reason VARCHAR(255);

-- 4. Add user_id column if missing
ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS user_id UUID;

-- 5. Rename date to attendance_date if not already renamed
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'attendance'
              AND column_name = 'date'
        ) THEN
            ALTER TABLE attendance
                RENAME COLUMN date TO attendance_date;
        END IF;
    END $$;

-- 6. Add unique constraint to subject.name if missing
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_subject_name'
              AND conrelid = 'subject'::regclass
        ) THEN
            ALTER TABLE subject
                ADD CONSTRAINT uq_subject_name UNIQUE (name);
        END IF;
    END $$;

-- 7. Add schedule_id to class_group
ALTER TABLE class_group
    ADD COLUMN IF NOT EXISTS schedule_id UUID;

-- 8. Drop the payment foreign key before changing column types
ALTER TABLE payment
    DROP CONSTRAINT IF EXISTS payment_enrollment_id_fkey;

-- 9. Change enrollment.id to VARCHAR(255)
ALTER TABLE enrollment
    ALTER COLUMN id TYPE VARCHAR(255)
        USING id::VARCHAR;

-- 10. Change payment.enrollment_id to VARCHAR(255)
ALTER TABLE payment
    ALTER COLUMN enrollment_id TYPE VARCHAR(255)
        USING enrollment_id::VARCHAR;

-- 11. Recreate the payment foreign key
ALTER TABLE payment
    ADD CONSTRAINT payment_enrollment_id_fkey
        FOREIGN KEY (enrollment_id)
            REFERENCES enrollment(id);

-- 12. Change payment.id from UUID to VARCHAR(255)
ALTER TABLE payment
    ALTER COLUMN id TYPE VARCHAR(255)
        USING id::VARCHAR;