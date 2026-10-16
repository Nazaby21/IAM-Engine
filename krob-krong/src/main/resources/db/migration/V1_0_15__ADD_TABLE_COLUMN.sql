alter table attendance
    add column reason varchar(255);

alter table attendance
    drop constraint if exists attendance_user_id_fkey;

alter table attendance
    drop column user_id;

alter table attendance
    rename column date to attendance_date;

alter table subject
    add constraint uq_subject_name unique (name);