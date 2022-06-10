alter table kommune drop constraint kommune_land_fkey;
alter table kommune drop constraint kommune_nivaanummer_fkey;

alter table fylke drop constraint fylke_land_fkey;
alter table fylke drop constraint fylke_nivaanummer_fkey;

alter table nasjon drop constraint nasjon_land_fkey;
alter table nasjon drop constraint nasjon_nivaanummer_fkey;

alter table sektoromraade drop constraint sektoromraade_nivaanummer_fkey;

alter table kommune drop column land;
alter table kommune drop column nivaanummer;
alter table kommune drop column navn;
alter table kommune drop column administrativkode;

alter table fylke drop column land;
alter table fylke drop column nivaanummer;
alter table fylke drop column navn;
alter table fylke drop column administrativkode;

alter table nasjon drop column land;
alter table nasjon drop column nivaanummer;
alter table nasjon drop column navn;
alter table nasjon drop column administrativkode;

alter table sektoromraade drop column nivaanummer;
alter table sektoromraade drop column nivaanavn;

drop table nivaanummer;
