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