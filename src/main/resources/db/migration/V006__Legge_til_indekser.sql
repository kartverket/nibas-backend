create index grunnkrets_kommune_fk_index on grunnkrets (kommune_fk);
create index stemmekrets_kommune_fk_index on stemmekrets (kommune_fk);
create index kommune_fylke_fk_index on kommune (fylke_fk);
create index grunnkrets_grense_grense_fk_index ON grunnkrets_grense (grense_fk);
create index stemmekrets_grense_grense_fk_index ON stemmekrets_grense (grense_fk);
