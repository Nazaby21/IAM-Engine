alter table attendance
    drop constraint if exists attendance_user_id_fkey;

alter table attendance
    drop column user_id;

alter table attendance
    add column student_id uuid references app_user(id);

alter table attendance
    drop column enrollment_id;

alter table schedule
    alter column day_of_week type varchar(150);

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
    drop column day_of_week;

alter table schedule
    drop column class_group_id;

alter table schedule
    drop column classroom_id;

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
