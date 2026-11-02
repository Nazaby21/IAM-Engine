alter table assessment
    alter column started_date type date using started_date::date,
    alter column ended_date type date using ended_date::date;