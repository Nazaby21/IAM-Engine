alter table attendance
    add column reason varchar(255);

alter table attendance
    rename column user_id to student_id;