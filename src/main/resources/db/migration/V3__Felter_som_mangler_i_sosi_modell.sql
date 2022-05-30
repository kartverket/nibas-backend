ALTER TABLE kommune add column posisjon geometry(Point, 25833);
ALTER TABLE kommune ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE kommune ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);

ALTER TABLE kommune add column flate geometry(MultiPolygon, 25833);
ALTER TABLE kommune ADD CONSTRAINT enforce_dims_flate CHECK (st_ndims(flate) = 2);
ALTER TABLE kommune ADD CONSTRAINT enforce_srid_flate CHECK (st_srid(flate) = 25833);

ALTER TABLE fylke add column posisjon geometry(Point, 25833);
ALTER TABLE fylke ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE fylke ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);

ALTER TABLE fylke add column flate geometry(MultiPolygon, 25833);
ALTER TABLE fylke ADD CONSTRAINT enforce_dims_flate CHECK (st_ndims(flate) = 2);
ALTER TABLE fylke ADD CONSTRAINT enforce_srid_flate CHECK (st_srid(flate) = 25833);

ALTER TABLE nasjon add column posisjon geometry(Point, 25833);
ALTER TABLE nasjon ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE nasjon ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);

ALTER TABLE nasjon add column flate geometry(MultiPolygon, 25833);
ALTER TABLE nasjon ADD CONSTRAINT enforce_dims_flate CHECK (st_ndims(flate) = 2);
ALTER TABLE nasjon ADD CONSTRAINT enforce_srid_flate CHECK (st_srid(flate) = 25833);

ALTER TABLE grunnkrets add column flate geometry(MultiPolygon, 25833);
ALTER TABLE grunnkrets ADD CONSTRAINT enforce_dims_flate CHECK (st_ndims(flate) = 2);
ALTER TABLE grunnkrets ADD CONSTRAINT enforce_srid_flate CHECK (st_srid(flate) = 25833);
ALTER TABLE grunnkrets add column informasjon text;

ALTER TABLE grunnkretsgrense add column informasjon text;

ALTER TABLE stemmekrets add column flate geometry(MultiPolygon, 25833);
ALTER TABLE stemmekrets ADD CONSTRAINT enforce_dims_flate CHECK (st_ndims(flate) = 2);
ALTER TABLE stemmekrets ADD CONSTRAINT enforce_srid_flate CHECK (st_srid(flate) = 25833);

ALTER TABLE stemmekretsgrense add column informasjon text;
