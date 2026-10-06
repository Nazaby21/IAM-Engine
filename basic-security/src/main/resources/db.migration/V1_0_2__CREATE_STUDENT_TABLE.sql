create table students (
    student_id varchar(50) primary key,
    full_name varchar(150) not null,
    email varchar(150) not null,
    registered_on date
);

create table student_profiles (
    student_id varchar(50) primary key references students(student_id),
    biography varchar(150) not null,
    learning_goal varchar(250) not null
);

create table instructors (
     instructor_id varchar(50) primary key,
     full_name varchar(225) not null,
     email varchar(225) unique not null
);

create table courses (
    course_id varchar(50) primary key,
    instructor_id varchar(50) references instructors(instructor_id),
    course_code varchar(225) unique not null,
    title varchar(225) not null,
    publication_status varchar(50) not null
);

create table enrollments (
    student_id varchar(50) references students(student_id),
    course_id varchar(50) references courses(course_id),
    enrolled_on date not null,
    status varchar(100) not null,
    final_score decimal not null,
    primary key (student_id, course_id)
);

create table lesson (
    lesson_id varchar(50) primary key,
    course_id varchar(50) references courses(course_id),
    title varchar(225) not null,
    sequence_number integer not null
);