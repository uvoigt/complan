update benutzer set password = 'x61Ey612Kl2gpFL56FT9weDnpSo4AV8j8+qx2AuTHdRyY036xxzTTrw10Wq3+4qQyB+XURPWx1ON
xp3Y3pB37A==' where id=1

select length(password) from benutzer where id=1


-- age types
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler A', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler B', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler C', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Jugend', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Junioren', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Leistungsklasse', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren B', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren C', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren D', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A/B', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren C/D', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A/B/C/D', current_timestamp, 'dba', 0)

-- boat classes
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K1', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K2', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K4', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C1', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C2', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C4', current_timestamp, 'dba', 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C8', current_timestamp, 'dba', 0)

