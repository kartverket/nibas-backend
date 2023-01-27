--------------------- Start PROD ---------------------
--------------------- Start grunnkretser ---------------------
-- grense_objid: 501cb86a-8b0e-4dc8-9266-3e619bab5df8, grunnkrets_objid: b3c1a665-10ee-4df8-a71f-5b9d63adcdb0
-- grense_objid: 501cb86a-8b0e-4dc8-9266-3e619bab5df8, grunnkrets_objid: b228d559-32ca-4607-81ff-61b10659c1b9
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('b3c1a665-10ee-4df8-a71f-5b9d63adcdb0')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '501cb86a-8b0e-4dc8-9266-3e619bab5df8' and grunnkrets_fk = 'b3c1a665-10ee-4df8-a71f-5b9d63adcdb0');

update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('b228d559-32ca-4607-81ff-61b10659c1b9')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '501cb86a-8b0e-4dc8-9266-3e619bab5df8' and grunnkrets_fk = 'b228d559-32ca-4607-81ff-61b10659c1b9');

delete from grunnkrets_grense where grense_fk = '501cb86a-8b0e-4dc8-9266-3e619bab5df8';
delete from grense where objid = '501cb86a-8b0e-4dc8-9266-3e619bab5df8';


-- grense_objid: 4f8659c6-c869-4e9a-b6f3-cc7e2c6bfe71, grunnkrets_objid: 14c4658b-c81d-43d5-8a55-6f2792e2df82
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('14c4658b-c81d-43d5-8a55-6f2792e2df82')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '4f8659c6-c869-4e9a-b6f3-cc7e2c6bfe71' and grunnkrets_fk = '14c4658b-c81d-43d5-8a55-6f2792e2df82');

delete from grunnkrets_grense where grense_fk = '4f8659c6-c869-4e9a-b6f3-cc7e2c6bfe71';
delete from grense where objid = '4f8659c6-c869-4e9a-b6f3-cc7e2c6bfe71';

-- grense_objid: 20f4de81-9bc7-43b0-9515-24d8b0792c2a, grunnkrets_objid: cf0ac072-3c38-4dc2-b63f-6cfe5e8257ed
-- grense_objid: 20f4de81-9bc7-43b0-9515-24d8b0792c2a, grunnkrets_objid: 152b90c2-8275-4ae6-b78c-422b5d16bf65
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('cf0ac072-3c38-4dc2-b63f-6cfe5e8257ed')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '20f4de81-9bc7-43b0-9515-24d8b0792c2a' and grunnkrets_fk = 'cf0ac072-3c38-4dc2-b63f-6cfe5e8257ed');

update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('152b90c2-8275-4ae6-b78c-422b5d16bf65')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = '20f4de81-9bc7-43b0-9515-24d8b0792c2a' and grunnkrets_fk = '152b90c2-8275-4ae6-b78c-422b5d16bf65');

delete from grunnkrets_grense where grense_fk = '20f4de81-9bc7-43b0-9515-24d8b0792c2a';
delete from grense where objid = '20f4de81-9bc7-43b0-9515-24d8b0792c2a';



-- grense_objid: a6887fcc-cc46-44f4-9325-a027b9df09a0, grunnkrets_objid: 583295c1-768e-4d11-8752-43896a7ef1e4
-- grense_objid: a6887fcc-cc46-44f4-9325-a027b9df09a0, grunnkrets_objid: 2e676725-e90e-4d17-8724-0b3866f97004
update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('583295c1-768e-4d11-8752-43896a7ef1e4')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = 'a6887fcc-cc46-44f4-9325-a027b9df09a0' and grunnkrets_fk = '583295c1-768e-4d11-8752-43896a7ef1e4');

update grunnkrets_grense gg set rekkefoelge = (rekkefoelge - 1)
where gg.grunnkrets_fk in ('2e676725-e90e-4d17-8724-0b3866f97004')
and rekkefoelge > (select rekkefoelge from grunnkrets_grense where grense_fk = 'a6887fcc-cc46-44f4-9325-a027b9df09a0' and grunnkrets_fk = '2e676725-e90e-4d17-8724-0b3866f97004');

delete from grunnkrets_grense where grense_fk = 'a6887fcc-cc46-44f4-9325-a027b9df09a0';
delete from grense where objid = 'a6887fcc-cc46-44f4-9325-a027b9df09a0';


--------------------- Slutt grunnkretser ---------------------


--------------------- Start stemmekretser ---------------------

-- grense_objid: 76c9ec8f-3978-45b1-96be-ba2860a0ee2f, stemmekrets_objid: cf053d8f-4c3e-4da8-b5c7-f16e7272729f
-- grense_objid: 76c9ec8f-3978-45b1-96be-ba2860a0ee2f, stemmekrets_objid: effb7713-0f77-49e8-9f1a-831aeccd940a
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('cf053d8f-4c3e-4da8-b5c7-f16e7272729f')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '76c9ec8f-3978-45b1-96be-ba2860a0ee2f' and stemmekrets_fk = 'cf053d8f-4c3e-4da8-b5c7-f16e7272729f');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('effb7713-0f77-49e8-9f1a-831aeccd940a')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '76c9ec8f-3978-45b1-96be-ba2860a0ee2f' and stemmekrets_fk = 'effb7713-0f77-49e8-9f1a-831aeccd940a');

delete from stemmekrets_grense where grense_fk = '76c9ec8f-3978-45b1-96be-ba2860a0ee2f';
delete from grense where objid = '76c9ec8f-3978-45b1-96be-ba2860a0ee2f';

-- grense_objid: c5bea7e3-3b3e-4134-8016-0f2ed542e34a, stemmekrets_objid: e6ca2149-c106-4a1e-82a8-4e7c1818d76d
-- grense_objid: c5bea7e3-3b3e-4134-8016-0f2ed542e34a, stemmekrets_objid: 87cc27c6-065c-4a16-af11-94c9454299a9
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('e6ca2149-c106-4a1e-82a8-4e7c1818d76d')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'c5bea7e3-3b3e-4134-8016-0f2ed542e34a' and stemmekrets_fk = 'e6ca2149-c106-4a1e-82a8-4e7c1818d76d');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('87cc27c6-065c-4a16-af11-94c9454299a9')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'c5bea7e3-3b3e-4134-8016-0f2ed542e34a' and stemmekrets_fk = '87cc27c6-065c-4a16-af11-94c9454299a9');

delete from stemmekrets_grense where grense_fk = 'c5bea7e3-3b3e-4134-8016-0f2ed542e34a';
delete from grense where objid = 'c5bea7e3-3b3e-4134-8016-0f2ed542e34a';

-- grense_objid: a4ff299b-b423-4ba0-a6bf-1b803c593b76, stemmekrets_objid: 339ceb87-ea40-41a0-9ad0-20c70bf678cc
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('339ceb87-ea40-41a0-9ad0-20c70bf678cc')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'a4ff299b-b423-4ba0-a6bf-1b803c593b76' and stemmekrets_fk = '339ceb87-ea40-41a0-9ad0-20c70bf678cc');

delete from stemmekrets_grense where grense_fk = 'a4ff299b-b423-4ba0-a6bf-1b803c593b76';
delete from grense where objid = 'a4ff299b-b423-4ba0-a6bf-1b803c593b76';

-- grense_objid: 750955b4-a9cb-4536-9463-4a4052f56bb5, stemmekrets_objid: f2dddf92-1fdc-4a48-811f-c24633270640
-- grense_objid: 750955b4-a9cb-4536-9463-4a4052f56bb5, stemmekrets_objid: ca343821-87e9-4543-bc89-ae2ea8793f46
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('f2dddf92-1fdc-4a48-811f-c24633270640')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '750955b4-a9cb-4536-9463-4a4052f56bb5' and stemmekrets_fk = 'f2dddf92-1fdc-4a48-811f-c24633270640');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('ca343821-87e9-4543-bc89-ae2ea8793f46')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '750955b4-a9cb-4536-9463-4a4052f56bb5' and stemmekrets_fk = 'ca343821-87e9-4543-bc89-ae2ea8793f46');

delete from stemmekrets_grense where grense_fk = '750955b4-a9cb-4536-9463-4a4052f56bb5';
delete from grense where objid = '750955b4-a9cb-4536-9463-4a4052f56bb5';

-- grense_objid: 02096002-4ca4-4378-88ab-b1ffabcbaee0, stemmekrets_objid: 6572535b-98f2-4fb6-85fd-e4daa08f658c
-- grense_objid: 02096002-4ca4-4378-88ab-b1ffabcbaee0, stemmekrets_objid: 0d5397b4-6bcd-4445-b224-d3f630b2d432
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('6572535b-98f2-4fb6-85fd-e4daa08f658c')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '02096002-4ca4-4378-88ab-b1ffabcbaee0' and stemmekrets_fk = '6572535b-98f2-4fb6-85fd-e4daa08f658c');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('0d5397b4-6bcd-4445-b224-d3f630b2d432')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '02096002-4ca4-4378-88ab-b1ffabcbaee0' and stemmekrets_fk = '0d5397b4-6bcd-4445-b224-d3f630b2d432');

delete from stemmekrets_grense where grense_fk = '02096002-4ca4-4378-88ab-b1ffabcbaee0';
delete from grense where objid = '02096002-4ca4-4378-88ab-b1ffabcbaee0';

-- grense_objid: 06ccafca-9eb7-4e79-9601-8e94993196b4, stemmekrets_objid: 085d54db-1128-414a-911a-788b7a767024
-- grense_objid: 06ccafca-9eb7-4e79-9601-8e94993196b4, stemmekrets_objid: c81ff486-8864-4fe3-bb24-8dc303b6acc7
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('085d54db-1128-414a-911a-788b7a767024')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '06ccafca-9eb7-4e79-9601-8e94993196b4' and stemmekrets_fk = '085d54db-1128-414a-911a-788b7a767024');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('c81ff486-8864-4fe3-bb24-8dc303b6acc7')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '06ccafca-9eb7-4e79-9601-8e94993196b4' and stemmekrets_fk = 'c81ff486-8864-4fe3-bb24-8dc303b6acc7');

delete from stemmekrets_grense where grense_fk = '06ccafca-9eb7-4e79-9601-8e94993196b4';
delete from grense where objid = '06ccafca-9eb7-4e79-9601-8e94993196b4';

-- grense_objid: f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7, stemmekrets_objid: 1f5e9da6-fa70-4ba6-86ca-95043e8aa837
-- grense_objid: f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7, stemmekrets_objid: 6ad5226d-c456-47d9-b243-6dd8b266bd44
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('1f5e9da6-fa70-4ba6-86ca-95043e8aa837')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7' and stemmekrets_fk = '1f5e9da6-fa70-4ba6-86ca-95043e8aa837');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('6ad5226d-c456-47d9-b243-6dd8b266bd44')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = 'f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7' and stemmekrets_fk = '6ad5226d-c456-47d9-b243-6dd8b266bd44');

delete from stemmekrets_grense where grense_fk = 'f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7';
delete from grense where objid = 'f5b4835a-5b96-4ea2-a3fe-1fdb2ffdc6c7';

-- grense_objid: 3211c586-83ab-403d-ba44-f1ec85a05fd1, stemmekrets_objid: af2b52fb-44a3-4123-914b-41c3507cd03f
-- grense_objid: 3211c586-83ab-403d-ba44-f1ec85a05fd1, stemmekrets_objid: 33833577-8044-492b-903e-da1a17736d6d
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('af2b52fb-44a3-4123-914b-41c3507cd03f')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '3211c586-83ab-403d-ba44-f1ec85a05fd1' and stemmekrets_fk = 'af2b52fb-44a3-4123-914b-41c3507cd03f');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('33833577-8044-492b-903e-da1a17736d6d')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '3211c586-83ab-403d-ba44-f1ec85a05fd1' and stemmekrets_fk = '33833577-8044-492b-903e-da1a17736d6d');

delete from stemmekrets_grense where grense_fk = '3211c586-83ab-403d-ba44-f1ec85a05fd1';
delete from grense where objid = '3211c586-83ab-403d-ba44-f1ec85a05fd1';

-- grense_objid: 8af281f8-2beb-4ecb-a5a9-d5729a55eedf, stemmekrets_objid: dc48b76b-f53d-4a31-bc69-5ca08bc36970
-- grense_objid: 8af281f8-2beb-4ecb-a5a9-d5729a55eedf, stemmekrets_objid: 598484d3-8133-49c9-a782-5e2b9f34c88f
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('dc48b76b-f53d-4a31-bc69-5ca08bc36970')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '8af281f8-2beb-4ecb-a5a9-d5729a55eedf' and stemmekrets_fk = 'dc48b76b-f53d-4a31-bc69-5ca08bc36970');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('598484d3-8133-49c9-a782-5e2b9f34c88f')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '8af281f8-2beb-4ecb-a5a9-d5729a55eedf' and stemmekrets_fk = '598484d3-8133-49c9-a782-5e2b9f34c88f');

delete from stemmekrets_grense where grense_fk = '8af281f8-2beb-4ecb-a5a9-d5729a55eedf';
delete from grense where objid = '8af281f8-2beb-4ecb-a5a9-d5729a55eedf';

-- grense_objid: 69aba0a6-bce2-48e1-b66b-155984ab6bdf, stemmekrets_objid: 4fd54d3c-227b-4113-9832-0d36fa7881fa
-- grense_objid: 69aba0a6-bce2-48e1-b66b-155984ab6bdf, stemmekrets_objid: 5a37430b-6b3a-478e-9223-ec8a463d1eca
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('4fd54d3c-227b-4113-9832-0d36fa7881fa')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '69aba0a6-bce2-48e1-b66b-155984ab6bdf' and stemmekrets_fk = '4fd54d3c-227b-4113-9832-0d36fa7881fa');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('5a37430b-6b3a-478e-9223-ec8a463d1eca')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '69aba0a6-bce2-48e1-b66b-155984ab6bdf' and stemmekrets_fk = '5a37430b-6b3a-478e-9223-ec8a463d1eca');

delete from stemmekrets_grense where grense_fk = '69aba0a6-bce2-48e1-b66b-155984ab6bdf';
delete from grense where objid = '69aba0a6-bce2-48e1-b66b-155984ab6bdf';

-- grense_objid: 2288b0ae-7427-44b0-bb6b-614ff758b600, stemmekrets_objid: 68baea9b-2e41-48d8-b55f-608337711b44
-- grense_objid: 2288b0ae-7427-44b0-bb6b-614ff758b600, stemmekrets_objid: 2e872718-a492-4de2-af07-c9ae5d2af457
update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('68baea9b-2e41-48d8-b55f-608337711b44')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '2288b0ae-7427-44b0-bb6b-614ff758b600' and stemmekrets_fk = '68baea9b-2e41-48d8-b55f-608337711b44');

update stemmekrets_grense sg set rekkefoelge = (rekkefoelge - 1)
where sg.stemmekrets_fk in ('2e872718-a492-4de2-af07-c9ae5d2af457')
and rekkefoelge > (select rekkefoelge from stemmekrets_grense where grense_fk = '2288b0ae-7427-44b0-bb6b-614ff758b600' and stemmekrets_fk = '2e872718-a492-4de2-af07-c9ae5d2af457');

delete from stemmekrets_grense where grense_fk = '2288b0ae-7427-44b0-bb6b-614ff758b600';
delete from grense where objid = '2288b0ae-7427-44b0-bb6b-614ff758b600';

--------------------- Slutt stemmekretser ---------------------
-------------------------- Slutt PROD -------------------------------
