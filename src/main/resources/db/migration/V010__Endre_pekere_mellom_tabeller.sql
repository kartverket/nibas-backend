CREATE TABLE uniklokalid
(
    lokalid                    text                     NOT NULL,
    flatetype                  text                     NOT NULL
)
    WITH (OIDS= FALSE);

ALTER TABLE uniklokalid ADD PRIMARY KEY (lokalid);
insert into nibas.uniklokalid (lokalid, flatetype)  select distinct lokalid, '0' from nibas.fylke;
insert into nibas.uniklokalid (lokalid, flatetype)  select distinct lokalid, '1' from nibas.kommune;
insert into nibas.uniklokalid (lokalid, flatetype)  select distinct lokalid, '2' from nibas.nasjon;

ALTER TABLE fylke add column nasjonlokalid text;
UPDATE fylke f SET nasjonlokalid = (select n.lokalid FROM nasjon n where n.objid = f.nasjon_fk);
ALTER TABLE fylke drop column nasjon_fk;
create index fylke_nasjon_index on fylke (nasjonlokalid);
create index fylke_lokalid_index on fylke (lokalid);
ALTER TABLE fylke ADD FOREIGN KEY (nasjonlokalid) REFERENCES uniklokalid (lokalid);

ALTER TABLE kommune ADD COLUMN fylkelokalid text;
UPDATE kommune k SET fylkelokalid = (select f.lokalid FROM fylke f where f.objid = k.fylke_fk);
ALTER TABLE kommune drop column fylke_fk;
create index kommune_fylke_index on kommune (fylkelokalid);
create index kommune_lokalid_index on kommune (lokalid);
ALTER TABLE kommune ADD FOREIGN KEY (fylkelokalid) REFERENCES uniklokalid (lokalid);

ALTER TABLE administrativenhetnavn add column administrativenhetlokalid text;
UPDATE administrativenhetnavn a SET administrativenhetlokalid = (select n.lokalid FROM nasjon n where n.objid = a.administrativenhet_nasjon_fk)
                                where a.administrativenhet_nasjon_fk is not null;
UPDATE administrativenhetnavn a SET administrativenhetlokalid = (select f.lokalid FROM fylke f where f.objid = a.administrativenhet_fylke_fk)
                                where a.administrativenhet_fylke_fk is not null;
UPDATE administrativenhetnavn a SET administrativenhetlokalid = (select k.lokalid FROM kommune k where k.objid = a.administrativenhet_kommune_fk)
                                where a.administrativenhet_kommune_fk is not null;

ALTER TABLE administrativenhetnavn
    drop column administrativenhet_fk,
    drop column administrativenhet_fylke_fk,
    drop column administrativenhet_kommune_fk,
    drop column administrativenhet_nasjon_fk;
create index navn_administrativenhet_index on administrativenhetnavn (administrativenhetlokalid);
ALTER TABLE administrativenhetnavn ADD FOREIGN KEY (administrativenhetlokalid) REFERENCES uniklokalid (lokalid);

ALTER TABLE stemmekrets add column kommunelokalid text;
UPDATE stemmekrets s SET kommunelokalid = (select k.lokalid FROM kommune k where k.objid = s.kommune_fk);
ALTER TABLE stemmekrets drop column kommune_fk;
create index stemmekrets_kommune_index on stemmekrets (kommunelokalid);
ALTER TABLE stemmekrets ADD FOREIGN KEY (kommunelokalid) REFERENCES uniklokalid (lokalid);

ALTER TABLE grunnkrets add column kommunelokalid text;
UPDATE grunnkrets g SET kommunelokalid = (select k.lokalid FROM kommune k where k.objid = g.kommune_fk);
ALTER TABLE grunnkrets drop column kommune_fk;
create index grunnkrets_kommune_index on grunnkrets (kommunelokalid);
ALTER TABLE grunnkrets ADD FOREIGN KEY (kommunelokalid) REFERENCES uniklokalid (lokalid);

