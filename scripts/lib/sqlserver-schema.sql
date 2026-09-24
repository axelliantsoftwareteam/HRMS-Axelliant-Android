-- Deterministic textual dump of a SQL Server schema, for the schema contract.
-- SQL Server has no pg_dump/mysqldump equivalent that ships in the container and prints a stable
-- result, so this reads the catalog directly and prints one sorted line per object. It is a
-- contract, not a restore script: its only job is to change when the schema changes.
-- The catalog's *_desc and definition columns use a different collation from object names, so
-- they are brought to the database default before concatenation, and the final sort is binary so
-- the order never depends on the server's collation.
SET NOCOUNT ON;
SELECT line FROM (
    SELECT CONCAT('TABLE ', s.name, '.', t.name) AS line
    FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
    UNION ALL
    SELECT CONCAT('COLUMN ', s.name, '.', t.name, '.', c.name, ' ', ty.name,
                  CASE WHEN ty.name IN ('varchar','char','varbinary','binary') THEN CONCAT('(', IIF(c.max_length = -1, 'max', CAST(c.max_length AS varchar(10))), ')')
                       WHEN ty.name IN ('nvarchar','nchar') THEN CONCAT('(', IIF(c.max_length = -1, 'max', CAST(c.max_length / 2 AS varchar(10))), ')')
                       WHEN ty.name IN ('decimal','numeric') THEN CONCAT('(', c.precision, ',', c.scale, ')')
                       WHEN ty.name IN ('datetime2','datetimeoffset','time') THEN CONCAT('(', c.scale, ')')
                       ELSE '' END,
                  IIF(c.is_nullable = 1, ' NULL', ' NOT NULL'),
                  IIF(c.is_identity = 1, ' IDENTITY', ''),
                  CASE WHEN dc.definition IS NOT NULL THEN CONCAT(' DEFAULT ', dc.definition COLLATE DATABASE_DEFAULT) ELSE '' END,
                  CONCAT(' #', FORMAT(c.column_id, '000')))
    FROM sys.columns c
    JOIN sys.tables t ON t.object_id = c.object_id
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.types ty ON ty.user_type_id = c.user_type_id
    LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id
    UNION ALL
    SELECT CONCAT('INDEX ', s.name, '.', t.name, '.', i.name,
                  IIF(i.is_primary_key = 1, ' PRIMARY KEY', IIF(i.is_unique = 1, ' UNIQUE', '')),
                  ' ', i.type_desc COLLATE DATABASE_DEFAULT, ' (',
                  STRING_AGG(CONCAT(c.name, IIF(ic.is_descending_key = 1, ' DESC', '')), ', ')
                      WITHIN GROUP (ORDER BY ic.key_ordinal), ')',
                  CASE WHEN i.filter_definition IS NOT NULL THEN CONCAT(' WHERE ', i.filter_definition COLLATE DATABASE_DEFAULT) ELSE '' END)
    FROM sys.indexes i
    JOIN sys.tables t ON t.object_id = i.object_id
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE i.name IS NOT NULL
    GROUP BY s.name, t.name, i.name, i.is_primary_key, i.is_unique, i.type_desc, i.filter_definition
    UNION ALL
    SELECT CONCAT('FOREIGN KEY ', s.name, '.', t.name, '.', fk.name, ' -> ', rs.name, '.', rt.name,
                  ' ON DELETE ', fk.delete_referential_action_desc COLLATE DATABASE_DEFAULT, ' ON UPDATE ', fk.update_referential_action_desc COLLATE DATABASE_DEFAULT)
    FROM sys.foreign_keys fk
    JOIN sys.tables t ON t.object_id = fk.parent_object_id
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.tables rt ON rt.object_id = fk.referenced_object_id
    JOIN sys.schemas rs ON rs.schema_id = rt.schema_id
    UNION ALL
    SELECT CONCAT('CHECK ', s.name, '.', t.name, '.', cc.name, ' ', cc.definition COLLATE DATABASE_DEFAULT)
    FROM sys.check_constraints cc
    JOIN sys.tables t ON t.object_id = cc.parent_object_id
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    UNION ALL
    SELECT CONCAT(o.type_desc COLLATE DATABASE_DEFAULT, ' ', s.name, '.', o.name)
    FROM sys.objects o JOIN sys.schemas s ON s.schema_id = o.schema_id
    WHERE o.type IN ('V', 'P', 'FN', 'IF', 'TF', 'TR', 'SO') AND o.is_ms_shipped = 0
) AS contract
ORDER BY line COLLATE Latin1_General_BIN2;
