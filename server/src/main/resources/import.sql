-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

insert into users (id, username, email, firstname, lastname, status) values (1, 'lbroudoux', 'laurent@microcks.io', 'Laurent', 'Broudoux', 'REGISTERED');

insert into api_tokens (id, name, token, valid_until, user_id) values (1, 'Dev token', 'my-super-secret-token', '2025-12-24 23:59:59', 1);

insert into expositions (id, name) values (1, 'My exposition');