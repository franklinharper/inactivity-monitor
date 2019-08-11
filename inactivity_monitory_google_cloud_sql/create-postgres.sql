-- Initialize PostgreSQL
CREATE TYPE ActivityType AS ENUM ( 'IN_VEHICLE', 'ON_BICYCLE', 'ON_FOOT', 'STILL', 'WALKING', 'RUNNING', 'OTHER' );
CREATE TYPE TransitionType AS ENUM ('ENTER');
CREATE TABLE transition ( id SERIAL PRIMARY KEY,
  activityType ActivityType,
  transitionType TransitionType,
  start BIGINT
);
-- INSERT INTO transition (activityType, transitionType, start) values ('WALKING', 'ENTER', 123);
