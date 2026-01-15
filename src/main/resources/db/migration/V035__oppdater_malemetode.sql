-- En del grenser har fått målemetodekodeid fra matrikkelen, oppdaterer til geonorgekoder
UPDATE GRENSE SET maalemetode = '637d1a12-cdeb-4798-8833-1ccfbb23dd5a' where maalemetode is null or maalemetode = ''; --99 Ukjent målemetode

UPDATE GRENSE SET maalemetode = '128c4ce0-7a16-4e46-b088-a098269d0b0f' where maalemetode = '1'; -- 11 TerrengmaltTotalstasjon
UPDATE GRENSE SET maalemetode = '030cfd2d-ace5-4407-966d-fcffae6ae527' where maalemetode = '2'; -- 12 TerrengmaltTeodolittElektronisk
UPDATE GRENSE SET maalemetode = '6f8330f6-b39d-4159-8835-351b575fde1d' where maalemetode = '10'; -- 22 StereoinstrumentAnalytiskPlotter
UPDATE GRENSE SET maalemetode = '3c3658b3-5762-41aa-942f-40aa05a732c0' where maalemetode = '12'; -- 24 DigitaltStereoinstrument
UPDATE GRENSE SET maalemetode = '2511eb3b-1e36-4917-9dad-7e938ffd0793' where maalemetode = '15'; -- 32 ScannetRissfolie
UPDATE GRENSE SET maalemetode = '94c9906a-b454-4489-ab3f-280f297b1156' where maalemetode = '20'; -- 40 DigitalisertOrtofotoFlybilde
UPDATE GRENSE SET maalemetode = '83049530-dabe-42a0-8711-dffc7a06d36f' where maalemetode = '27'; -- 50 DigitalisertStrekKart
UPDATE GRENSE SET maalemetode = 'b6269623-afaf-4af3-884d-97e7e401d0ca' where maalemetode = '28'; -- 51 DigitalisertStrekKartBlyantorig
UPDATE GRENSE SET maalemetode = '1813215a-b38b-41f3-8ca4-53fb568de568' where maalemetode = '30'; -- 53 DigitalisertTransparentGod
UPDATE GRENSE SET maalemetode = 'bc54000f-a3b4-4ba1-80fc-70be6640996c' where maalemetode = '32'; -- 55 DigitalisertStrekKartPapikopi
UPDATE GRENSE SET maalemetode = '5bf7d23e-2fe3-493c-a677-df5b2469765b' where maalemetode = '33'; -- 56 DigitalisertSkjermScannet
UPDATE GRENSE SET maalemetode = '14201fb6-fdb0-41c7-83ef-d8f16e14cfc6' where maalemetode = '37'; -- 63 GenerertSirkelgeometri
UPDATE GRENSE SET maalemetode = 'feb4251e-7d1c-49c3-9022-ba2230602006' where maalemetode = '43'; -- 69 Beregnet
UPDATE GRENSE SET maalemetode = '578c5646-5e91-403e-bd3b-c531aea204ce' where maalemetode = '49'; -- 78 FastsattVedDom
UPDATE GRENSE SET maalemetode = '17bec036-73dd-4547-a4e5-ef2e20ad54f3' where maalemetode = '50'; -- 79 SpesielleMetoderAnnet
UPDATE GRENSE SET maalemetode = '57828895-e43b-4b14-8208-b333321b652c' where maalemetode = '52'; -- 81 FrihandstegningKrokering
UPDATE GRENSE SET maalemetode = 'e8947d32-56c9-4f87-a248-1defed464caf' where maalemetode = '53'; -- 82 FrihandstegningSkjerm
UPDATE GRENSE SET maalemetode = '3cef2463-d9fa-46ec-9329-97b6803725b7' where maalemetode = '60'; -- 96 GPSKinematisk
UPDATE GRENSE SET maalemetode = '637d1a12-cdeb-4798-8833-1ccfbb23dd5a' where maalemetode = '62'; -- 99 Ukjent

