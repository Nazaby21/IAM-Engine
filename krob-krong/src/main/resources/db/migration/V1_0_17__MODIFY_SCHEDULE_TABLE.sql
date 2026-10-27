alter table course
    drop constraint course_type_check;

alter table course
    add constraint course_type_check
    check ( type in ('OFFLINE','ONLINE') );