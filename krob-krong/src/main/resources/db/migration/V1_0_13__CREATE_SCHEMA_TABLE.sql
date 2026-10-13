CREATE TABLE users
(
    id          uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    school_id   uuid, -- FK added after school is created
    role_id     VARCHAR(36),
    first_name  VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    gender      VARCHAR(20),
    dob         DATE,
    image_url   VARCHAR(500),
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(36),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(36),
    UNIQUE (school_id, email)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_school FOREIGN KEY (school_id) REFERENCES school (id);

CREATE TABLE course
(
    id          VARCHAR(36) PRIMARY KEY,
    branch_id   uuid         NOT NULL REFERENCES branch (id),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('offline', 'online')),
    description TEXT,
    status      VARCHAR(30)  NOT NULL,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at  DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(36),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(36),
    UNIQUE (branch_id, code)
);

CREATE TABLE subject
(
    id         VARCHAR(36) PRIMARY KEY,
    branch_id  uuid         NOT NULL REFERENCES branch (id),
    name       VARCHAR(255) NOT NULL,
    credit     VARCHAR(50),
    status     VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE course_subjects
(
    id            VARCHAR(36) PRIMARY KEY,
    course_id     VARCHAR(36) NOT NULL REFERENCES course (id),
    subject_id    VARCHAR(36) NOT NULL REFERENCES subject (id),
    instructor_id uuid REFERENCES users (id),
    price         DECIMAL(12, 2),
    description   TEXT,
    currency      VARCHAR(30), -- khr / USD
    status        VARCHAR(30) NOT NULL,
    UNIQUE (course_id, subject_id)
);

CREATE TABLE classroom
(
    id         VARCHAR(36) PRIMARY KEY,
    branch_id  uuid         NOT NULL REFERENCES branch (id),
    name       VARCHAR(255) NOT NULL,
    number     VARCHAR(50),
    floor      VARCHAR(50),
    capacity   INTEGER,
    class_type VARCHAR(30),
    status     VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_at TIMESTAMP,
    updated_by VARCHAR(36)
);

CREATE TABLE class_group
(
    id                VARCHAR(36) PRIMARY KEY,
    course_subject_id VARCHAR(36)  NOT NULL REFERENCES course_subjects (id),
    classroom_id      VARCHAR(36) REFERENCES classroom (id), -- null for online
    instructor_id     uuid REFERENCES users (id),            -- null = use course_subjects.instructor_id
    name              VARCHAR(100) NOT NULL,
    capacity          INTEGER,
    status            VARCHAR(30)  NOT NULL
);

CREATE TABLE schedule
(
    id             VARCHAR(36) PRIMARY KEY,
    class_group_id VARCHAR(36) NOT NULL REFERENCES class_group (id),
    classroom_id   VARCHAR(36) REFERENCES classroom (id),
    day_of_week    VARCHAR(20) NOT NULL,
    start_time     TIME        NOT NULL,
    end_time       TIME        NOT NULL,
    description    TEXT,
    CHECK (end_time > start_time)
);

CREATE TABLE enrollment
(
    id             VARCHAR(36) PRIMARY KEY,
    user_id        uuid        NOT NULL REFERENCES users (id),
    class_group_id VARCHAR(36) NOT NULL REFERENCES class_group (id),
    status         VARCHAR(30) NOT NULL,
    enrolled_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, class_group_id)
);

CREATE TABLE payment
(
    id            VARCHAR(36) PRIMARY KEY,
    enrollment_id VARCHAR(36)    NOT NULL REFERENCES enrollment (id),
    amount        DECIMAL(12, 2) NOT NULL,
    currency      VARCHAR(10)    NOT NULL,
    method        VARCHAR(30)    NOT NULL,
    status        VARCHAR(30)    NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(36),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(36)
);

CREATE TABLE attendance
(
    id            VARCHAR(36) PRIMARY KEY,
    schedule_id   VARCHAR(36) NOT NULL REFERENCES schedule (id),
    enrollment_id VARCHAR(36) NOT NULL REFERENCES enrollment (id),
    user_id       uuid        NOT NULL REFERENCES users (id),
    date          DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('present', 'absent', 'late')),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(36),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(36),
    UNIQUE (schedule_id, enrollment_id, date)
);

CREATE TABLE assessment
(
    id                VARCHAR(36) PRIMARY KEY,
    course_subject_id VARCHAR(36) NOT NULL REFERENCES course_subjects (id),
    type              VARCHAR(30) NOT NULL,
    description       TEXT,
    start_date        DATE,
    end_date          DATE,
    duration          INTEGER,
    total_score       INTEGER,
    percentage        INTEGER,
    status            VARCHAR(30) NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(36),
    updated_at        TIMESTAMP,
    updated_by        VARCHAR(36)
);

CREATE TABLE score
(
    id            VARCHAR(36) PRIMARY KEY,
    assessment_id VARCHAR(36) NOT NULL REFERENCES assessment (id),
    student_id    uuid        NOT NULL REFERENCES users (id),
    score         INTEGER,
    grade         VARCHAR(10),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(36),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(36),
    UNIQUE (assessment_id, student_id)
);

CREATE TABLE event
(
    id          VARCHAR(36) PRIMARY KEY,
    school_id   uuid         NOT NULL REFERENCES school (id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    event_date  TIMESTAMP,
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE TABLE announcement
(
    id          VARCHAR(36) PRIMARY KEY,
    school_id   uuid         NOT NULL REFERENCES school (id),
    event_id    VARCHAR(36) REFERENCES event (id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    type        VARCHAR(30),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);