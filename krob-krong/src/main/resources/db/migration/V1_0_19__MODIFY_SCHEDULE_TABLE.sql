alter table attendance
    drop constraint attendance_status_check;

alter table attendance
    add constraint attendance_status_check
        check ( status in ('PRESENT','LATE', 'PERMISSION', 'ABSENT') );