-------------------------- Start TEST -------------------------------
--------------------- Start grunnkretser ---------------------
-- grense_objid: e0de108d-bce9-4181-8c62-ba29935b2165, grunnkrets_objid: 8c21d1ac-67dd-4064-b6ed-03b500f190a8
-- grense_objid: e0de108d-bce9-4181-8c62-ba29935b2165, grunnkrets_objid: d82cb741-be86-4baf-8015-54e8b97c9694
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('8c21d1ac-67dd-4064-b6ed-03b500f190a8')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = 'e0de108d-bce9-4181-8c62-ba29935b2165' and grunnkrets_fk = '8c21d1ac-67dd-4064-b6ed-03b500f190a8');

update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('d82cb741-be86-4baf-8015-54e8b97c9694')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = 'e0de108d-bce9-4181-8c62-ba29935b2165' and grunnkrets_fk = 'd82cb741-be86-4baf-8015-54e8b97c9694');

delete from grunnkrets_grense where grense_fk = 'e0de108d-bce9-4181-8c62-ba29935b2165';
delete from grense where objid = 'e0de108d-bce9-4181-8c62-ba29935b2165';

-- grense_objid: 040b44f1-07d6-416a-bfda-fb39ec804316, grunnkrets_objid: 0c3ad3c5-a86a-4d0c-a304-923f72ad740d
-- grense_objid: 040b44f1-07d6-416a-bfda-fb39ec804316, grunnkrets_objid: bc4f9598-2fee-4490-81c7-aaa9047d486f
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('0c3ad3c5-a86a-4d0c-a304-923f72ad740d')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '040b44f1-07d6-416a-bfda-fb39ec804316' and grunnkrets_fk = '0c3ad3c5-a86a-4d0c-a304-923f72ad740d');

update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('bc4f9598-2fee-4490-81c7-aaa9047d486f')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '040b44f1-07d6-416a-bfda-fb39ec804316' and grunnkrets_fk = 'bc4f9598-2fee-4490-81c7-aaa9047d486f');

delete from grunnkrets_grense where grense_fk = '040b44f1-07d6-416a-bfda-fb39ec804316';
delete from grense where objid = '040b44f1-07d6-416a-bfda-fb39ec804316';
--------------------- Slutt grunnkretser ---------------------

--------------------- Start stemmekretser ---------------------
-- grense_objid: fb1e48d8-6a2c-405c-9530-dc49429d1be5, stemmekrets_objid: 77788d35-e50a-4f0f-a346-2624a3e6dc0a
-- grense_objid: fb1e48d8-6a2c-405c-9530-dc49429d1be5, stemmekrets_objid: 7b14ce01-f0c9-4abd-8494-321d732c7d5e
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('77788d35-e50a-4f0f-a346-2624a3e6dc0a')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'fb1e48d8-6a2c-405c-9530-dc49429d1be5' and stemmekrets_fk = '77788d35-e50a-4f0f-a346-2624a3e6dc0a');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('7b14ce01-f0c9-4abd-8494-321d732c7d5e')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'fb1e48d8-6a2c-405c-9530-dc49429d1be5' and stemmekrets_fk = '7b14ce01-f0c9-4abd-8494-321d732c7d5e');

delete from stemmekrets_grense where grense_fk = 'fb1e48d8-6a2c-405c-9530-dc49429d1be5';
delete from grense where objid = 'fb1e48d8-6a2c-405c-9530-dc49429d1be5';

-- grense_objid: ed50833b-b5a5-44c5-9875-0e2dc6ec897c, stemmekrets_objid: f01ec940-d24d-41d7-b481-e42458875ed6
-- grense_objid: ed50833b-b5a5-44c5-9875-0e2dc6ec897c, stemmekrets_objid: a949990e-83c2-4ee9-8415-48a9dba401bd
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('f01ec940-d24d-41d7-b481-e42458875ed6')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'ed50833b-b5a5-44c5-9875-0e2dc6ec897c' and stemmekrets_fk = 'f01ec940-d24d-41d7-b481-e42458875ed6');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('a949990e-83c2-4ee9-8415-48a9dba401bd')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'ed50833b-b5a5-44c5-9875-0e2dc6ec897c' and stemmekrets_fk = 'a949990e-83c2-4ee9-8415-48a9dba401bd');

delete from stemmekrets_grense where grense_fk = 'ed50833b-b5a5-44c5-9875-0e2dc6ec897c';
delete from grense where objid = 'ed50833b-b5a5-44c5-9875-0e2dc6ec897c';

-- grense_objid: c08d2865-ee7e-4b5d-b485-eddc88410f03, stemmekrets_objid: 574ca541-9cb2-4898-a8b2-081285cefb24
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('574ca541-9cb2-4898-a8b2-081285cefb24')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'c08d2865-ee7e-4b5d-b485-eddc88410f03' and stemmekrets_fk = '574ca541-9cb2-4898-a8b2-081285cefb24');

delete from stemmekrets_grense where grense_fk = 'c08d2865-ee7e-4b5d-b485-eddc88410f03';
delete from grense where objid = 'c08d2865-ee7e-4b5d-b485-eddc88410f03';

-- grense_objid: fd7caae7-b8b8-47f8-97b6-d874a0717ada, stemmekrets_objid: 8feb0074-2e26-4ad2-af9f-3a43f2b4e9bb
-- grense_objid: fd7caae7-b8b8-47f8-97b6-d874a0717ada, stemmekrets_objid: d56051ed-35db-49ff-83c6-aade8fd72f37
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('8feb0074-2e26-4ad2-af9f-3a43f2b4e9bb')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'fd7caae7-b8b8-47f8-97b6-d874a0717ada' and stemmekrets_fk = '8feb0074-2e26-4ad2-af9f-3a43f2b4e9bb');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('d56051ed-35db-49ff-83c6-aade8fd72f37')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'fd7caae7-b8b8-47f8-97b6-d874a0717ada' and stemmekrets_fk = 'd56051ed-35db-49ff-83c6-aade8fd72f37');

delete from stemmekrets_grense where grense_fk = 'fd7caae7-b8b8-47f8-97b6-d874a0717ada';
delete from grense where objid = 'fd7caae7-b8b8-47f8-97b6-d874a0717ada';

-- grense_objid: 5e55de30-f771-4998-b4af-0afc240f47ad, stemmekrets_objid: 61cc2bd0-a042-4820-bec3-c4bf4e30ec7e
-- grense_objid: 5e55de30-f771-4998-b4af-0afc240f47ad, stemmekrets_objid: 54ad0956-4082-486b-9135-afa8f429de1c
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('61cc2bd0-a042-4820-bec3-c4bf4e30ec7e')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '5e55de30-f771-4998-b4af-0afc240f47ad' and stemmekrets_fk = '61cc2bd0-a042-4820-bec3-c4bf4e30ec7e');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('54ad0956-4082-486b-9135-afa8f429de1c')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '5e55de30-f771-4998-b4af-0afc240f47ad' and stemmekrets_fk = '54ad0956-4082-486b-9135-afa8f429de1c');

delete from stemmekrets_grense where grense_fk = '5e55de30-f771-4998-b4af-0afc240f47ad';
delete from grense where objid = '5e55de30-f771-4998-b4af-0afc240f47ad';

-- grense_objid: 536e8633-293e-42ea-b745-8b51ca138af2, stemmekrets_objid: 500d6847-0ed5-40f2-8821-ea8d53948871
-- grense_objid: 536e8633-293e-42ea-b745-8b51ca138af2, stemmekrets_objid: 4d7e2814-229f-4861-b5a0-3c27948d7312
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('500d6847-0ed5-40f2-8821-ea8d53948871')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '536e8633-293e-42ea-b745-8b51ca138af2' and stemmekrets_fk = '500d6847-0ed5-40f2-8821-ea8d53948871');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('4d7e2814-229f-4861-b5a0-3c27948d7312')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '536e8633-293e-42ea-b745-8b51ca138af2' and stemmekrets_fk = '4d7e2814-229f-4861-b5a0-3c27948d7312');

delete from stemmekrets_grense where grense_fk = '536e8633-293e-42ea-b745-8b51ca138af2';
delete from grense where objid = '536e8633-293e-42ea-b745-8b51ca138af2';

-- grense_objid: ae203fed-69b1-4929-954a-f7654883a448, stemmekrets_objid: 6984e3e8-23fb-48b4-8f79-b001db50b08d
-- grense_objid: ae203fed-69b1-4929-954a-f7654883a448, stemmekrets_objid: 84b37cfd-81ce-4e0e-b2bd-d14630fa2cfe
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('6984e3e8-23fb-48b4-8f79-b001db50b08d')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'ae203fed-69b1-4929-954a-f7654883a448' and stemmekrets_fk = '6984e3e8-23fb-48b4-8f79-b001db50b08d');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('84b37cfd-81ce-4e0e-b2bd-d14630fa2cfe')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'ae203fed-69b1-4929-954a-f7654883a448' and stemmekrets_fk = '84b37cfd-81ce-4e0e-b2bd-d14630fa2cfe');

delete from stemmekrets_grense where grense_fk = 'ae203fed-69b1-4929-954a-f7654883a448';
delete from grense where objid = 'ae203fed-69b1-4929-954a-f7654883a448';

-- grense_objid: 2a339e8d-da7f-4a61-ae31-77700f52a9c4, stemmekrets_objid: 34fd3d92-7f8e-4e09-a1f2-55f01618eebd
-- grense_objid: 2a339e8d-da7f-4a61-ae31-77700f52a9c4, stemmekrets_objid: 1584c967-c31d-4683-a361-f2d3c5d31121
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('34fd3d92-7f8e-4e09-a1f2-55f01618eebd')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '2a339e8d-da7f-4a61-ae31-77700f52a9c4' and stemmekrets_fk = '34fd3d92-7f8e-4e09-a1f2-55f01618eebd');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('1584c967-c31d-4683-a361-f2d3c5d31121')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '2a339e8d-da7f-4a61-ae31-77700f52a9c4' and stemmekrets_fk = '1584c967-c31d-4683-a361-f2d3c5d31121');

delete from stemmekrets_grense where grense_fk = '2a339e8d-da7f-4a61-ae31-77700f52a9c4';
delete from grense where objid = '2a339e8d-da7f-4a61-ae31-77700f52a9c4';

-- grense_objid: 1e5aac67-01d9-45f0-ac42-3f051eb22cfd, stemmekrets_objid: 243dd1ac-5212-43da-bf99-f6453944746a
-- grense_objid: 1e5aac67-01d9-45f0-ac42-3f051eb22cfd, stemmekrets_objid: f3fa9cfe-d67a-4e1b-91ad-0c7486f20cc8
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('243dd1ac-5212-43da-bf99-f6453944746a')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '1e5aac67-01d9-45f0-ac42-3f051eb22cfd' and stemmekrets_fk = '243dd1ac-5212-43da-bf99-f6453944746a');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('f3fa9cfe-d67a-4e1b-91ad-0c7486f20cc8')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '1e5aac67-01d9-45f0-ac42-3f051eb22cfd' and stemmekrets_fk = 'f3fa9cfe-d67a-4e1b-91ad-0c7486f20cc8');

delete from stemmekrets_grense where grense_fk = '1e5aac67-01d9-45f0-ac42-3f051eb22cfd';
delete from grense where objid = '1e5aac67-01d9-45f0-ac42-3f051eb22cfd';

-- grense_objid: bbae04f8-3f73-41f2-9e1d-2e9767869a54, stemmekrets_objid: 55f7424e-98b9-45d7-9268-87adbc624c2a
-- grense_objid: bbae04f8-3f73-41f2-9e1d-2e9767869a54, stemmekrets_objid: b48b5807-e063-4cbb-b94d-7441fd06e494
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('55f7424e-98b9-45d7-9268-87adbc624c2a')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'bbae04f8-3f73-41f2-9e1d-2e9767869a54' and stemmekrets_fk = '55f7424e-98b9-45d7-9268-87adbc624c2a');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('b48b5807-e063-4cbb-b94d-7441fd06e494')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'bbae04f8-3f73-41f2-9e1d-2e9767869a54' and stemmekrets_fk = 'b48b5807-e063-4cbb-b94d-7441fd06e494');

delete from stemmekrets_grense where grense_fk = 'bbae04f8-3f73-41f2-9e1d-2e9767869a54';
delete from grense where objid = 'bbae04f8-3f73-41f2-9e1d-2e9767869a54';

-- grense_objid: da055e20-eefc-443c-879d-f5595fee7783, stemmekrets_objid: 15905122-8f66-4456-bc31-62a32ef586bc
-- grense_objid: da055e20-eefc-443c-879d-f5595fee7783, stemmekrets_objid: 55230fa6-0fa1-44a6-8954-8bfd87dd64d4
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('15905122-8f66-4456-bc31-62a32ef586bc')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'da055e20-eefc-443c-879d-f5595fee7783' and stemmekrets_fk = '15905122-8f66-4456-bc31-62a32ef586bc');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('55230fa6-0fa1-44a6-8954-8bfd87dd64d4')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'da055e20-eefc-443c-879d-f5595fee7783' and stemmekrets_fk = '55230fa6-0fa1-44a6-8954-8bfd87dd64d4');

delete from stemmekrets_grense where grense_fk = 'da055e20-eefc-443c-879d-f5595fee7783';
delete from grense where objid = 'da055e20-eefc-443c-879d-f5595fee7783';
--------------------- Slutt stemmekretser ---------------------
-------------------------- Slutt TEST -------------------------------
