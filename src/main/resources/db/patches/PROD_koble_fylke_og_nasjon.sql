update fylke set nasjon_fk = (select objid from nasjon)
where nasjon_fk is null;
