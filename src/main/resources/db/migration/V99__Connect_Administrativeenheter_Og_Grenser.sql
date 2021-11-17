insert into kommune_kommunegrense (kommune_fk, kommunegrense_fk)
select k.objid, kg.objid from kommune k, kommunegrense kg
where k.navn = 'Ringerike'
  and kg.lokalid = 'ringerike';

insert into kommune_kommunegrense (kommune_fk, kommunegrense_fk)
select k.objid, kg.objid from kommune k, kommunegrense kg
where k.navn = 'Ringerike'
  and kg.lokalid = 'ringerike_og_hole';

insert into kommune_kommunegrense (kommune_fk, kommunegrense_fk)
select k.objid, kg.objid from kommune k, kommunegrense kg
where k.navn = 'Hole'
  and kg.lokalid = 'ringerike_og_hole';

insert into kommune_kommunegrense (kommune_fk, kommunegrense_fk)
select k.objid, kg.objid from kommune k, kommunegrense kg
where k.navn = 'Hole'
  and kg.lokalid = 'hole';

insert into fylke_fylkesgrense (fylke_fk, fylkesgrense_fk)
select f.objid, fg.objid from fylke f, fylkesgrense fg
where f.navn = 'Viken'
  and fg.lokalid = 'viken_grense';

insert into fylke_fylkesgrense (fylke_fk, fylkesgrense_fk)
select f.objid, fg.objid from fylke f, fylkesgrense fg
where f.navn = 'Viken'
  and fg.lokalid = 'viken_og_innlandet_grense';

insert into fylke_fylkesgrense (fylke_fk, fylkesgrense_fk)
select f.objid, fg.objid from fylke f, fylkesgrense fg
where f.navn = 'Innlandet'
  and fg.lokalid = 'innlandet_grense';

insert into fylke_fylkesgrense (fylke_fk, fylkesgrense_fk)
select f.objid, fg.objid from fylke f, fylkesgrense fg
where f.navn = 'Innlandet'
  and fg.lokalid = 'viken_og_innlandet_grense';