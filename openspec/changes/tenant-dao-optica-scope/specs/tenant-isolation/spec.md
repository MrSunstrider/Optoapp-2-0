# Spec — tenant isolation (Room PK)

## R1 — Scoped getById

WHEN an entity with `opticaId`/`optica_id` is queried by primary key with a different optica  
THEN the DAO returns null (or empty for lists).

WHEN queried with the matching optica  
THEN the row is returned.

## R2 — Scoped delete (regalos)

WHEN deleting a regalo by id  
THEN the delete requires `optica_id` and affects 0 rows for foreign tenant.

## R3 — Sync bump

WHEN BumpEntityStrategy bumps an entity  
THEN it loads via the scoped DAO/repo signature.

## R4 — No venta writer change

Stock single-writer and movement identity remain unchanged.
