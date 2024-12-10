-- Sett et breakpoint i en test og ta opp testcontaineren i dbeaver. Kjør denne selecten og kopier ut kolonnen med insert-setninger.
-- Husk å bevare setningen med uniklokalid
-- select kommunenummer, "version", 'INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring,
--                            posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet)
-- VALUES (''' ||objid|| ''', ''' ||lokalid|| ''', ''' ||gyldigfra||''', null, null,
--         '''||foerstedigitaliseringsdato||''', null, '''||endretdato||''', '''||endretav||''', '''||typeendring||''', '''|| st_ashexewkb(posisjon)||''',
-- 		'||samiskforvaltningsomraade|| ', ''' ||kommunenummer|| ''', ''' ||kommunenummer|| ''', ''' ||fylkelokalid|| ''', '''||"version"||''', null, '||slettet||');'
-- from kommune k where gyldigtil  is NULL
-- UNION
-- select kommunenummer, "version", 'INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring,
--                            posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet)
-- VALUES (''' ||objid|| ''', ''' ||lokalid|| ''', ''' ||gyldigfra||''', '''||gyldigtil||''', null,
--         '''||foerstedigitaliseringsdato||''', null, '''||endretdato||''', '''||endretav||''', '''||typeendring||''', '''|| st_ashexewkb(posisjon)||''',
-- 		'||samiskforvaltningsomraade|| ', ''' ||kommunenummer|| ''', ''' ||kommunenummer|| ''', ''' ||fylkelokalid|| ''', '''||"version"||''', null, '||slettet||');'
-- from kommune k where gyldigtil  is NOT NULL order by 1, 2;

INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring,
                           posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet, informasjon)
VALUES ('ny-e24e9dcf-4ae9-475c-afc2-1a5c49f166e4', 'ffd39f1e-f20e-4559-80c9-42b911aac29b', '2021-01-01 00:00:00+01', '2022-11-25 00:00:00+01', null, 
        '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import', '0101000020E964000014AE47E130DE1041295C8FE2516F5941',
		false, '4fae75c2-b82d-4cb2-937d-34aacd760fb8', '3032', '47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1', '1', null, false, 'her er informasjon');
INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring, 
                           posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet, informasjon)
VALUES ('e24e9dcf-4ae9-475c-afc2-1a5c49f166e4', 'ffd39f1e-f20e-4559-80c9-42b911aac29b', '2022-11-25 00:00:00+01', null, null, 
        '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import', '0101000020E964000014AE47E130DE1041295C8FE2516F5941',
		false, '4fae75c2-b82d-4cb2-937d-34aacd760fb8', '3032', '47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1', '2', null, false, 'informasjon er her');

INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring,
                           posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet, informasjon)
VALUES ('12a23944-6b81-4e9e-b5f3-5f46f00957af', 'ac7e05cd-fb07-4270-ad95-e3c32c1370d0', '2022-11-25 00:00:00+01', null, null,
        '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import', '0101000020E9640000E17A14AEC7050B41D7A3709DAA075941',
		false, '3a7be64b-2421-451c-a42b-6568e7f9c1ff', '3804', '068f6d7f-2f05-47b2-a6aa-0d9611b89ebf', '1', null, false, 'informasjon er her');

INSERT INTO nibas.kommune (objid, lokalid, gyldigfra, gyldigtil, datafangstdato, foerstedigitaliseringsdato, opphav, endretdato, endretav, typeendring,
                           posisjon, samiskforvaltningsomraade, kommunenummeruuid, kommunenummer, fylkelokalid, version, utledetfra, slettet, informasjon)
VALUES ('ee7a81d3-6321-4157-a364-7fda34919067', '8b023c74-4db4-48f9-8a5c-a3124c75da14', '2022-11-25 00:00:00+01', null, null,
        '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import', '0101000020E9640000C3F5285C6F22F540A4703DBAF0DC5841',
		false, '5d5d1c22-907d-4fde-aca8-387bb7e8717e', '4219', '49b80966-598f-4a93-9960-723b5d34c03d', '1', null, false, 'info her');

insert into nibas.uniklokalid (lokalid, flatetype)  select distinct lokalid, '1' from nibas.kommune;
