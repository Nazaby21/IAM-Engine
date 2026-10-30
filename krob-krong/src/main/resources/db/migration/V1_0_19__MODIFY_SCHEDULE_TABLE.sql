alter table attendance
    add column reason varchar(255);

alter table attendance
    drop constraint if exists attendance_user_id_fkey;

alter table attendance
    drop column user_id;

alter table attendance
    rename column date to attendance_date;

alter table attendance
    add column student_id uuid references app_user(id);

alter table attendance
    drop column enrollment_id;

alter table subject
    add constraint uq_subject_name unique (name);

alter table schedule
    alter column day_of_week type varchar(150);

alter table class_group
    add column schedule_id uuid references schedule(id);

alter table payment
    drop constraint if exists payment_enrollment_id_fkey;

alter table attendance
    drop constraint if exists attendance_enrollment_id_fkey;

alter table enrollment
    alter column id type varchar(15)
        using id::varchar(15);

alter table payment
    alter column id type varchar(30)
        using id::varchar(30);

alter table payment
    alter column enrollment_id type varchar(15)
        using id::varchar(15);

alter table payment
    add constraint payment_enrollment_id_fkey
        foreign key (enrollment_id)
            references enrollment(id);

alter table schedule
    add column course_id uuid references course(id);

alter table schedule
    rename column day_of_week to days_of_week;

alter table schedule
    alter column days_of_week type jsonb
        using case
                  when days_of_week is null then null
                  else to_jsonb(string_to_array(days_of_week, ','))
        end;

alter table schedule
    add column max_students integer,
    add column status varchar(30);

alter table schedule
    add column "name" varchar(255);

alter table schedule
    drop column class_group_id;

alter table schedule
    drop column classroom_id;

alter table schedule
    add column subject_id uuid references subject(id);

alter table course
    drop constraint course_type_check;

alter table course
    add constraint course_type_check
        check ( type in ('OFFLINE','ONLINE') );

alter table class_group
    drop column course_id;

alter table class_group
    drop column subject_id;

alter table attendance
    drop constraint attendance_status_check;

alter table attendance
    add constraint attendance_status_check
        check ( status in ('PRESENT','LATE', 'PERMISSION', 'ABSENT') );
commit;
