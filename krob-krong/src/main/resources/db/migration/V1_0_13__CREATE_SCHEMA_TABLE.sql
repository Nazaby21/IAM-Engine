ALTER TABLE app_user
    add column school_id       uuid REFERENCES school (id),
    add column role_id         uuid REFERENCES role (id),
    add column first_name      VARCHAR(100),
    add column last_name       VARCHAR(100),
    add column gender          VARCHAR(20),
    add column dob             DATE,
    add column image_url       VARCHAR(500),
    add column created_by      uuid REFERENCES app_user (id),
    add column updated_at      timestamptz,
    add column updated_by      uuid REFERENCES app_user (id);

CREATE TABLE course
(
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id   uuid         NOT NULL REFERENCES branch (id),
    name        varchar(255) NOT NULL,
    type        varchar(20)  NOT NULL CHECK (type IN ('offline', 'online')),
    description text,
    status      varchar(30)  NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  uuid         NOT NULL REFERENCES app_user (id),
    updated_at  timestamptz,
    updated_by  uuid REFERENCES app_user (id),
    deleted_at  timestamptz,
    deleted_by  uuid REFERENCES app_user (id)
);

CREATE TABLE subject
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id  uuid         NOT NULL REFERENCES branch (id),
    name       varchar(255) NOT NULL,
    credit     varchar(50),
    status     varchar(30)  NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid         NOT NULL REFERENCES app_user (id),
    updated_at timestamptz,
    updated_by uuid REFERENCES app_user (id),
    deleted_at timestamptz,
    deleted_by uuid REFERENCES app_user (id)
);

CREATE TABLE course_subject
(
    course_id     uuid NOT NULL REFERENCES course (id),
    subject_id    uuid NOT NULL REFERENCES subject (id),
    instructor_id uuid REFERENCES app_user (id),
    code          varchar(50),
    price         decimal(12, 2),
    price_type    varchar(30),
    description   text,
    started_at    timestamptz,
    ended_at      timestamptz,
    currency      varchar(10),
    status        varchar(30) NOT NULL,
    is_public     boolean     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_course_subject PRIMARY KEY (course_id, subject_id),

    CONSTRAINT uq_course_subject_code UNIQUE (course_id, code),

    CONSTRAINT chk_course_subject_dates
        CHECK (
            ended_at IS NULL
                OR started_at IS NULL
                OR ended_at >= started_at
            ),

    CONSTRAINT chk_course_subject_price
        CHECK (price IS NULL OR price >= 0)
);

CREATE TABLE classroom
(
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id  uuid         NOT NULL REFERENCES school (id),
    branch_id  uuid         NOT NULL REFERENCES branch (id),
    name       varchar(255) NOT NULL,
    number     varchar(50),
    floor      varchar(50),
    capacity   integer,
    class_type varchar(30),
    status     varchar(30)  NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by uuid         NOT NULL REFERENCES app_user (id),
    updated_at timestamptz,
    updated_by uuid REFERENCES app_user (id),
    deleted_at timestamptz,
    deleted_by uuid REFERENCES app_user (id),

    CONSTRAINT chk_classroom_capacity
        CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE class_group
(
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id     uuid  NOT NULL REFERENCES course (id),
    subject_id    uuid  NOT NULL REFERENCES subject (id),
    classroom_id  uuid REFERENCES classroom (id),
    instructor_id uuid REFERENCES app_user (id),
    name          varchar(100) NOT NULL,
    capacity      integer,
    status        varchar(30)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    uuid         NOT NULL REFERENCES app_user (id),
    updated_at    timestamptz,
    updated_by    uuid REFERENCES app_user (id),
    deleted_at    timestamptz,
    deleted_by    uuid REFERENCES app_user (id),

    CONSTRAINT fk_class_group_course_subject
        FOREIGN KEY (course_id, subject_id)
            REFERENCES course_subject (course_id, subject_id),

    CONSTRAINT chk_class_group_capacity
        CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE schedule
(
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    class_group_id uuid NOT NULL
        REFERENCES class_group (id),
    classroom_id   uuid REFERENCES classroom (id),
    day_of_week    varchar(20) NOT NULL,
    start_time     time        NOT NULL,
    end_time       time        NOT NULL,
    description    text,
    created_at     timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     uuid        NOT NULL REFERENCES app_user (id),
    updated_at     timestamptz,
    updated_by     uuid REFERENCES app_user (id),

    CONSTRAINT chk_schedule_time
        CHECK (end_time > start_time)
);

CREATE TABLE enrollment
(
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid        NOT NULL REFERENCES app_user (id),
    class_group_id uuid NOT NULL REFERENCES class_group (id),
    status         varchar(30) NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     uuid        NOT NULL REFERENCES app_user (id),
    updated_at     timestamptz,
    updated_by     uuid REFERENCES app_user (id),

    CONSTRAINT uq_enrollment_user_class_group
        UNIQUE (user_id, class_group_id)
);

CREATE TABLE payment
(
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id   uuid    NOT NULL REFERENCES enrollment (id),
    amount          decimal(12, 2) NOT NULL,
    currency        varchar(10)    NOT NULL,
    method          varchar(30)    NOT NULL,
    status          varchar(30)    NOT NULL,
    reference_image varchar(500),
    created_at      timestamptz    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      uuid           NOT NULL REFERENCES app_user (id),
    updated_at      timestamptz,
    updated_by      uuid REFERENCES app_user (id),

    CONSTRAINT chk_payments_amount
        CHECK (amount >= 0)
);

CREATE TABLE attendance
(
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id   uuid NOT NULL REFERENCES schedule (id),
    enrollment_id uuid NOT NULL REFERENCES enrollment (id),
    user_id       uuid        NOT NULL REFERENCES app_user (id),
    date          date        NOT NULL,
    status        varchar(20) NOT NULL
        CHECK (status IN ('present', 'absent', 'late', 'permission')),
    created_at    timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    uuid        NOT NULL REFERENCES app_user (id),
    updated_at    timestamptz,
    updated_by    uuid REFERENCES app_user (id),

    CONSTRAINT uq_attendance_schedule_enrollment_date
        UNIQUE (schedule_id, enrollment_id, date)
);

CREATE TABLE assessment
(
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   uuid NOT NULL REFERENCES course (id),
    subject_id  uuid NOT NULL REFERENCES subject (id),
    type        varchar(30) NOT NULL,
    description text,
    start_date  timestamptz,
    end_date    timestamptz,
    duration    integer,
    total_score integer,
    percentage  integer,
    status      varchar(30) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  uuid        NOT NULL REFERENCES app_user (id),
    updated_at  timestamptz,
    updated_by  uuid REFERENCES app_user (id),

    CONSTRAINT fk_assessment_course_subject
        FOREIGN KEY (course_id, subject_id)
            REFERENCES course_subject (course_id, subject_id),

    CONSTRAINT chk_assessment_dates
        CHECK (
            end_date IS NULL
                OR start_date IS NULL
                OR end_date >= start_date
            ),

    CONSTRAINT chk_assessment_duration
        CHECK (duration IS NULL OR duration > 0),

    CONSTRAINT chk_assessment_total_score
        CHECK (total_score IS NULL OR total_score >= 0),

    CONSTRAINT chk_assessment_percentage
        CHECK (percentage IS NULL OR percentage BETWEEN 0 AND 100)
);

CREATE TABLE score
(
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id uuid NOT NULL REFERENCES assessment (id),
    student_id    uuid        NOT NULL REFERENCES app_user (id),
    score         integer,
    grade         varchar(10),
    created_at    timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    uuid        NOT NULL REFERENCES app_user (id),
    updated_at    timestamptz,
    updated_by    uuid REFERENCES app_user (id),

    CONSTRAINT uq_score_assessment_student
        UNIQUE (assessment_id, student_id),

    CONSTRAINT chk_score_score
        CHECK (score IS NULL OR score >= 0)
);

CREATE TABLE event
(
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    uuid         NOT NULL REFERENCES school (id),
    title        varchar(255) NOT NULL,
    description  text,
    event_date   timestamptz,
    status       varchar(30)  NOT NULL,
    poster_image varchar(500),
    created_at   timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   uuid         NOT NULL references app_user (id),
    updated_at   timestamptz,
    updated_by   uuid references app_user (id),
    deleted_at   timestamptz,
    deleted_by   uuid references app_user (id)
);

CREATE TABLE announcement
(
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      uuid         NOT NULL REFERENCES school (id),
    event_id       uuid REFERENCES event (id),
    class_group_id uuid REFERENCES class_group (id),
    title          varchar(255) NOT NULL,
    description    text,
    type           varchar(30),
    attachment     varchar(500),
    created_at     timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     uuid         NOT NULL references app_user (id),
    updated_at     timestamptz,
    updated_by     uuid         NOT NULL references app_user (id),
    deleted_at     timestamptz,
    deleted_by     uuid         NOT NULL references app_user (id)
);