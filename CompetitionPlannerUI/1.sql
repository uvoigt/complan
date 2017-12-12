update User set password = '6G7XMlc9lWEN3jceVYMp+51Q3K61MaaNoBh/6EqDQtknXHQTvS88c1r3HZGhcLBacPkW+UHveZA8
8qlZS4irBA==' where userid='empty'

select length(password) from benutzer where id=1





ALIAS_CATALOG ALIAS_SCHEMA ALIAS_NAME  JAVA_CLASS JAVA_METHOD DATA_TYPE TYPE_NAME COLUMN_COUNT RETURNS_RESULT REMARKS ID  SOURCE                                                                                                                                                              
------------- ------------ ----------- ---------- ----------- --------- --------- ------------ -------------- ------- --- ------------------------------------------------------------------------------------------------------------------------------------------------------------------- 
PLANNERDB     PUBLIC       DATE_FORMAT [null]     [null]      12        VARCHAR   2            2                      107 
String format(java.util.Date date, String format) {
return date != null ? java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(date) : null;
}
 


09:37:49,201
bis
erste Query
09:37:49,207
Namensqueries
09:37:49,317




select user0_.userId as col_0_0_, user0_.email as col_1_0_, user0_.birthDate as col_2_0_, user0_.gender as col_3_0_, 
	role2_.role as col_4_0_, user0_.locked as col_5_0_, 
	user0_.id as col_6_0_, 
	user0_.createTime as created, user0_.createUser as createdBy, user0_.updateTime as updated, user0_.updateUser as updatedBy,
	nvl(u1.firstName, u2.firstName), nvl(u1.lastName, u2.lastName)

from User user0_ 
left outer join User_Role roles1_ on user0_.id=roles1_.User_id 
left outer join Role role2_ on roles1_.role_id=role2_.id 
left outer join User u1 on u1.userid=user0_.createUser
left outer join User u2 on u2.userid=user0_.updateUser
	where user0_.club_id=16










insert into Role (id, role, description, version, createtime, createuser) values(1, 'Admin', 'Administrator', 0, current_timestamp, null);
insert into Role (id, role, description, version, createtime, createuser) values(2, 'Sportwart', 'Sportwart (Kann Ausschreibungen erstellen, Melden und Trainer und Sportler im Verein anlegen und bearbeiten)', 0, current_timestamp, null);
insert into Role (id, role, description, version, createtime, createuser) values(3, 'Trainer', 'Trainer (Kann Sportler melden aber keine zusätzlichen Trainer anlegen)', 0, current_timestamp, null);
insert into Role (id, role, description, version, createtime, createuser) values(4, 'Mastersportler', 'Mastersportler (Kann sich selbst melden)', 0, current_timestamp, null);
insert into Role (id, role, description, version, createtime, createuser) values(4, 'Sportler', 'Sportler (Kann nur Meldungen und Ausschreibungen ansehen)', 0, current_timestamp, null);


-- age types
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler A', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler B', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Schüler C', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Jugend', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Junioren', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Leistungsklasse', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren B', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren C', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren D', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A/B', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren C/D', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 3, 0, 'Senioren A/B/C/D', current_timestamp, null, 0)

-- boat classes
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K1', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K2', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'K4', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C1', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C2', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C4', current_timestamp, null, 0)
insert into enum (id, type, ordinal, name, createtime, createuser, version) values (next value for hibernate_sequence, 4, 0, 'C8', current_timestamp, null, 0)

