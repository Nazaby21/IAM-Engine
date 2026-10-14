alter table assessment
    rename column start_date to started_date;

alter table assessment
    rename column end_date to ended_date;

alter table assessment
    alter column total_score type numeric(18, 2)
        using total_score::numeric(18, 2),
    alter column percentage type numeric(18, 2)
        using percentage::numeric(18, 2);

alter table schedule
    rename column start_time to started_time;

alter table schedule
    rename column end_time to ended_time;

alter table course_subject
    alter column price type numeric(18, 2)
        using price::numeric(18, 2);

alter table event
    alter column event_date type date;

alter table subject
    alter column credit type numeric(18, 2)
        using credit::numeric(18, 2);