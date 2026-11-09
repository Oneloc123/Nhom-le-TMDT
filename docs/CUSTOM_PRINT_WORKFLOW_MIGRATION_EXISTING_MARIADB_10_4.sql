-- PrintKraft custom print workflow migration for an EXISTING legacy database.
-- Target: MariaDB 10.4.32; expected schema name: printkraft.
-- Run manually in stages after taking a full backup. This file has NOT been run.
-- Do not run CUSTOM_PRINT_WORKFLOW_SCHEMA_PROPOSAL.sql against the legacy table:
-- CREATE TABLE IF NOT EXISTS does not upgrade an existing custom_print_requests.
--
-- Safety: additive columns/tables/indexes and CHECK-constraint replacement only.
-- This migration never drops a table/column and never deletes request/history data.
-- The only UPDATE below fills missing request_code values with a stable legacy ID.
-- Select the intended database in your SQL client; there is deliberately no USE.
-- Execution: run Stage 0 by itself and inspect every result. Run later stages
-- separately; do not submit this whole file as one batch. After Stage 2, run
-- the duplicate/orphan/FK preflights and continue only if their stop conditions
-- are clear. Some SQL clients continue after SELECT/error output and cannot
-- conditionally abort a script; in those clients, stop manually between stages.
-- Finish with Stage 6. Confirm DATABASE() is printkraft, version is 10.4.x,
-- tables are InnoDB, referenced IDs have matching type/signedness, and the
-- history actor/old_status columns permit NULL before running the later stages.

-- ============================================================================
-- STAGE 0: PREFLIGHT (read-only). Stop if the selected DB/version/tables differ.
-- ============================================================================
SELECT VERSION() AS mariadb_version, DATABASE() AS selected_database;

SELECT TABLE_NAME, ENGINE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('users','design_files','print_shops','orders','order_items',
                     'order_status_history','production_updates','reviews',
                     'custom_print_requests','custom_print_request_history');

-- Confirm the legacy tables' keys, foreign keys, column types/nullability and
-- CHECK expressions before proceeding. In particular, custom_request_id,
-- user_id, design_file_id, shop_id, status, technology, material, quantity,
-- quoted_amount, and the history columns used below must exist as described.
SHOW CREATE TABLE design_files;
SHOW CREATE TABLE print_shops;
SHOW CREATE TABLE orders;
SHOW CREATE TABLE order_items;
SHOW CREATE TABLE order_status_history;
SHOW CREATE TABLE production_updates;
SHOW CREATE TABLE reviews;
SHOW CREATE TABLE custom_print_requests;
SHOW CREATE TABLE custom_print_request_history;

-- Full check-constraint expressions, including the legacy quantity 1..100 rule.
SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
 AND cc.TABLE_NAME = tc.TABLE_NAME
WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
  AND tc.TABLE_NAME IN ('custom_print_requests','custom_print_request_history')
  AND tc.CONSTRAINT_TYPE = 'CHECK'
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;

-- Check column types that should not be silently rewritten by an additive patch.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'custom_print_requests' AND COLUMN_NAME IN
        ('custom_request_id','user_id','design_file_id','shop_id','technology','material',
         'quantity','status','quoted_amount','request_code','order_id','created_at','updated_at'))
    OR (TABLE_NAME = 'custom_print_request_history' AND COLUMN_NAME IN
        ('history_id','custom_request_id','changed_by_user_id','old_status','new_status','created_at')))
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- For old_status/changed_by_user_id, verify the existing definitions allow NULL.
-- The initial history row and scheduled quote expiry intentionally use NULL.
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='custom_print_request_history'
  AND COLUMN_NAME IN ('changed_by_user_id','old_status');

-- Inspect existing indexes before the migration. The conditional index stage
-- below recognizes an existing leftmost-column prefix, even under another name.
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('design_files','print_shops','orders','order_items',
                     'order_status_history','production_updates','reviews',
                     'custom_print_requests','custom_print_request_history')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Pricing-table preflight. Missing tables may be created by Stage 2. If a table
-- already exists, the audit below reports missing/wrongly typed columns and
-- missing full unique keys. Stop before Stage 2 if an existing table has any
-- mismatch: CREATE TABLE IF NOT EXISTS will not repair it. Compare defaults,
-- ON UPDATE attributes and foreign keys with the CREATE definitions in Stage 2.
SELECT e.table_name, t.ENGINE,
       CASE WHEN t.TABLE_NAME IS NULL THEN 'TABLE_MISSING_WILL_BE_CREATED'
            WHEN UPPER(t.ENGINE)<>'INNODB' THEN 'STOP_ENGINE_MUST_BE_INNODB'
            ELSE 'OK' END AS audit_result
FROM (
  SELECT 'shop_print_price_profiles' table_name
  UNION ALL SELECT 'shop_print_price_options'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers'
) e
LEFT JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name;

SELECT e.table_name, e.column_name, e.expected_type,
       e.expected_char_length, e.expected_precision, e.expected_scale,
       e.expected_unsigned, e.expected_nullable,
       CASE
         WHEN c.COLUMN_NAME IS NULL THEN 'MISSING_COLUMN'
         WHEN LOWER(c.DATA_TYPE) <> e.expected_type THEN 'TYPE_MISMATCH'
         WHEN e.expected_char_length IS NOT NULL
              AND c.CHARACTER_MAXIMUM_LENGTH <> e.expected_char_length THEN 'LENGTH_MISMATCH'
         WHEN e.expected_precision IS NOT NULL
              AND c.NUMERIC_PRECISION <> e.expected_precision THEN 'PRECISION_MISMATCH'
         WHEN e.expected_scale IS NOT NULL
              AND c.NUMERIC_SCALE <> e.expected_scale THEN 'SCALE_MISMATCH'
         WHEN IF(LOCATE('unsigned',LOWER(c.COLUMN_TYPE))>0,'YES','NO') <> e.expected_unsigned THEN 'SIGNEDNESS_MISMATCH'
         WHEN c.IS_NULLABLE <> e.expected_nullable THEN 'NULLABILITY_MISMATCH'
         ELSE 'OK'
       END AS audit_result,
       c.COLUMN_TYPE AS actual_type, c.IS_NULLABLE AS actual_nullable,
       c.COLUMN_DEFAULT AS actual_default, c.EXTRA AS actual_extra
FROM (
  SELECT 'shop_print_price_profiles' table_name,'profile_id' column_name,'bigint' expected_type,NULL expected_char_length,NULL expected_precision,NULL expected_scale,'NO' expected_unsigned,'NO' expected_nullable
  UNION ALL SELECT 'shop_print_price_profiles','shop_id','bigint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','printing_method','varchar',100,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','material','varchar',120,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','material_price_per_gram','decimal',NULL,12,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','material_density_g_cm3','decimal',NULL,10,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','material_waste_factor','decimal',NULL,8,5,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','machine_cost_per_hour','decimal',NULL,12,2,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','deposition_rate_cm3_per_hour','decimal',NULL,10,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','wall_thickness_mm','decimal',NULL,8,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','support_material_factor','decimal',NULL,8,5,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','infill_usage_factor','decimal',NULL,8,5,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','reference_layer_height_mm','decimal',NULL,8,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','technology_factor','decimal',NULL,8,5,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','complexity_factor','decimal',NULL,8,5,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','setup_hours','decimal',NULL,8,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','post_processing_fee','decimal',NULL,12,2,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_order_price','decimal',NULL,12,2,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_layer_height_mm','decimal',NULL,8,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','maximum_layer_height_mm','decimal',NULL,8,4,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_infill_percent','smallint',NULL,NULL,NULL,'YES','NO'
  UNION ALL SELECT 'shop_print_price_profiles','maximum_infill_percent','smallint',NULL,NULL,NULL,'YES','NO'
  UNION ALL SELECT 'shop_print_price_profiles','production_days','smallint',NULL,NULL,NULL,'YES','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_material_cost','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_machine_cost','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_infill_factor','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_layer_height_factor','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_technology_factor','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_complexity_factor','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_post_processing_fee','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_quantity_discount','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','use_minimum_price','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','is_active','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','created_at','datetime',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_profiles','updated_at','datetime',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','option_id','bigint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','profile_id','bigint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','option_type','varchar',20,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','option_value','varchar',100,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','label','varchar',120,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','fee','decimal',NULL,12,2,'NO','NO'
  UNION ALL SELECT 'shop_print_price_options','is_active','tinyint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','discount_id','bigint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id','bigint',NULL,NULL,NULL,'NO','NO'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','minimum_quantity','int',NULL,NULL,NULL,'YES','NO'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','discount_rate','decimal',NULL,6,5,'NO','NO'
) e
JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME=e.table_name AND c.COLUMN_NAME=e.column_name
WHERE c.COLUMN_NAME IS NULL
   OR LOWER(c.DATA_TYPE) <> e.expected_type
   OR (e.expected_char_length IS NOT NULL AND c.CHARACTER_MAXIMUM_LENGTH <> e.expected_char_length)
   OR (e.expected_precision IS NOT NULL AND c.NUMERIC_PRECISION <> e.expected_precision)
   OR (e.expected_scale IS NOT NULL AND c.NUMERIC_SCALE <> e.expected_scale)
   OR IF(LOCATE('unsigned',LOWER(c.COLUMN_TYPE))>0,'YES','NO') <> e.expected_unsigned
   OR c.IS_NULLABLE <> e.expected_nullable
ORDER BY e.table_name,e.column_name;

-- Inspect all existing pricing defaults and EXTRA attributes. In particular,
-- profiles.created_at/updated_at must retain the defaults declared in Stage 2.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
       CHARACTER_SET_NAME, COLLATION_NAME, COLUMN_COMMENT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('shop_print_price_profiles','shop_print_price_options',
                     'shop_print_quantity_discount_tiers')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME,
       CASE WHEN LOWER(REPLACE(REPLACE(COLUMN_DEFAULT,'()',''),' ',''))='current_timestamp'
              AND (COLUMN_NAME<>'updated_at' OR LOWER(EXTRA) LIKE '%on update current_timestamp%')
            THEN 'OK' ELSE 'STOP_DEFAULT_OR_ON_UPDATE_MISMATCH' END AS audit_result,
       COLUMN_NAME, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='shop_print_price_profiles'
  AND COLUMN_NAME IN ('created_at','updated_at');

-- Numeric defaults used by the pricing DAO/model are part of the expected
-- schema too. Report mismatches instead of assuming CREATE IF NOT EXISTS fixed
-- them. Resolve any returned row manually before enabling the pricing UI.
SELECT e.table_name, e.column_name, e.expected_default,
       c.COLUMN_DEFAULT AS actual_default, 'STOP_DEFAULT_MISMATCH' AS audit_result
FROM (
  SELECT 'shop_print_price_profiles' table_name,'material_waste_factor' column_name,'0.05000' expected_default
  UNION ALL SELECT 'shop_print_price_profiles','wall_thickness_mm','1.2000'
  UNION ALL SELECT 'shop_print_price_profiles','support_material_factor','0.05000'
  UNION ALL SELECT 'shop_print_price_profiles','infill_usage_factor','1.00000'
  UNION ALL SELECT 'shop_print_price_profiles','reference_layer_height_mm','0.2000'
  UNION ALL SELECT 'shop_print_price_profiles','technology_factor','1.00000'
  UNION ALL SELECT 'shop_print_price_profiles','complexity_factor','1.00000'
  UNION ALL SELECT 'shop_print_price_profiles','setup_hours','0.0000'
  UNION ALL SELECT 'shop_print_price_profiles','post_processing_fee','0.00'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_order_price','0.00'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_layer_height_mm','0.0800'
  UNION ALL SELECT 'shop_print_price_profiles','maximum_layer_height_mm','0.4000'
  UNION ALL SELECT 'shop_print_price_profiles','minimum_infill_percent','0'
  UNION ALL SELECT 'shop_print_price_profiles','maximum_infill_percent','100'
  UNION ALL SELECT 'shop_print_price_profiles','production_days','3'
  UNION ALL SELECT 'shop_print_price_profiles','use_material_cost','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_machine_cost','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_infill_factor','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_layer_height_factor','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_technology_factor','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_complexity_factor','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_post_processing_fee','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_quantity_discount','1'
  UNION ALL SELECT 'shop_print_price_profiles','use_minimum_price','1'
  UNION ALL SELECT 'shop_print_price_profiles','is_active','1'
  UNION ALL SELECT 'shop_print_price_options','fee','0.00'
  UNION ALL SELECT 'shop_print_price_options','is_active','1'
) e
JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME=e.table_name AND c.COLUMN_NAME=e.column_name
WHERE c.COLUMN_NAME IS NULL OR c.COLUMN_DEFAULT IS NULL
   OR c.COLUMN_DEFAULT NOT REGEXP '^[+-]?[0-9]+([.][0-9]+)?$'
   OR CAST(c.COLUMN_DEFAULT AS DECIMAL(30,10)) <> CAST(e.expected_default AS DECIMAL(30,10));

-- Required pricing inputs and labels are defined without implicit defaults.
SELECT e.table_name, e.column_name, c.COLUMN_DEFAULT AS unexpected_default,
       'STOP_UNEXPECTED_DEFAULT' AS audit_result
FROM (
  SELECT 'shop_print_price_profiles' table_name,'shop_id' column_name
  UNION ALL SELECT 'shop_print_price_profiles','printing_method'
  UNION ALL SELECT 'shop_print_price_profiles','material'
  UNION ALL SELECT 'shop_print_price_profiles','material_price_per_gram'
  UNION ALL SELECT 'shop_print_price_profiles','material_density_g_cm3'
  UNION ALL SELECT 'shop_print_price_profiles','machine_cost_per_hour'
  UNION ALL SELECT 'shop_print_price_profiles','deposition_rate_cm3_per_hour'
  UNION ALL SELECT 'shop_print_price_options','profile_id'
  UNION ALL SELECT 'shop_print_price_options','option_type'
  UNION ALL SELECT 'shop_print_price_options','option_value'
  UNION ALL SELECT 'shop_print_price_options','label'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','minimum_quantity'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','discount_rate'
) e
JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME=e.table_name AND c.COLUMN_NAME=e.column_name
WHERE c.COLUMN_DEFAULT IS NOT NULL;

-- Required full unique keys for DAO upserts. Prefix keys do not satisfy these.
SELECT e.table_name, e.expected_columns,
       CASE WHEN t.TABLE_NAME IS NULL THEN 'TABLE_MISSING_WILL_BE_CREATED'
            WHEN i.TABLE_NAME IS NULL THEN 'MISSING_OR_NOT_FULL_UNIQUE_KEY'
            ELSE 'OK' END AS audit_result
FROM (
  SELECT 'shop_print_price_profiles' table_name,'shop_id,printing_method,material' expected_columns
  UNION ALL SELECT 'shop_print_price_options','profile_id,option_type,option_value'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id,minimum_quantity'
) e
LEFT JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name
LEFT JOIN (
  SELECT TABLE_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS indexed_columns,
         MAX(NON_UNIQUE) AS NON_UNIQUE,
         IF(MAX(SUB_PART) IS NULL,0,1) AS has_prefix
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE()
  GROUP BY TABLE_NAME,INDEX_NAME
) i ON i.TABLE_NAME=e.table_name AND i.indexed_columns=e.expected_columns
   AND i.NON_UNIQUE=0 AND i.has_prefix=0;

-- Generated primary IDs are required by the profile/option/discount DAOs.
SELECT e.table_name, e.column_name,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING_ID_COLUMN'
            WHEN c.COLUMN_KEY<>'PRI' OR LOWER(c.EXTRA) NOT LIKE '%auto_increment%' THEN 'STOP_ID_MUST_BE_AUTO_INCREMENT_PRIMARY_KEY'
            ELSE 'OK' END AS audit_result,
       c.COLUMN_TYPE, c.COLUMN_KEY, c.EXTRA
FROM (
  SELECT 'shop_print_price_profiles' table_name,'profile_id' column_name
  UNION ALL SELECT 'shop_print_price_options','option_id'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','discount_id'
) e
JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA=DATABASE() AND t.TABLE_NAME=e.table_name
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME=e.table_name AND c.COLUMN_NAME=e.column_name;

-- Existing pricing tables must retain the declared parent relationships too.
-- A missing table is expected to be created in dependency order in Stage 2;
-- a missing FK on an existing table is a structural mismatch and must be
-- repaired manually before the app uses it (this migration will not guess).
SELECT e.child_table, e.child_column, e.parent_table, e.parent_column,
       CASE WHEN ct.TABLE_NAME IS NULL THEN 'TABLE_MISSING_WILL_BE_CREATED'
            WHEN k.CONSTRAINT_NAME IS NULL THEN 'STOP_MISSING_FOREIGN_KEY_ON_EXISTING_PRICING_TABLE'
            WHEN rc.CONSTRAINT_NAME IS NULL THEN 'STOP_FOREIGN_KEY_METADATA_MISSING'
            WHEN (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE k2
                  WHERE k2.TABLE_SCHEMA=k.TABLE_SCHEMA AND k2.TABLE_NAME=k.TABLE_NAME
                    AND k2.CONSTRAINT_NAME=k.CONSTRAINT_NAME)<>1 THEN 'STOP_FOREIGN_KEY_MUST_BE_SINGLE_COLUMN'
            WHEN rc.UPDATE_RULE<>e.expected_update_rule OR rc.DELETE_RULE<>e.expected_delete_rule THEN 'STOP_FOREIGN_KEY_RULE_MISMATCH'
            ELSE 'OK' END AS audit_result
FROM (
  SELECT 'shop_print_price_profiles' child_table,'shop_id' child_column,'print_shops' parent_table,'shop_id' parent_column,'CASCADE' expected_update_rule,'RESTRICT' expected_delete_rule
  UNION ALL SELECT 'shop_print_price_options','profile_id','shop_print_price_profiles','profile_id','CASCADE','CASCADE'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id','shop_print_price_profiles','profile_id','CASCADE','CASCADE'
) e
LEFT JOIN information_schema.TABLES ct
  ON ct.TABLE_SCHEMA=DATABASE() AND ct.TABLE_NAME=e.child_table
LEFT JOIN information_schema.KEY_COLUMN_USAGE k
  ON k.TABLE_SCHEMA=DATABASE() AND k.TABLE_NAME=e.child_table AND k.COLUMN_NAME=e.child_column
 AND k.REFERENCED_TABLE_NAME=e.parent_table AND k.REFERENCED_COLUMN_NAME=e.parent_column
LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
  ON rc.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND rc.CONSTRAINT_NAME=k.CONSTRAINT_NAME;

-- New pricing profile table has a FK to this parent. This must be OK before
-- Stage 2 creates it; Stage 5 repeats the full child/parent audit.
SELECT pt.ENGINE AS parent_engine, pc.COLUMN_TYPE AS parent_key_type,
       CASE WHEN pt.ENGINE <> 'InnoDB' THEN 'STOP_ENGINE_MUST_BE_INNODB'
            WHEN LOWER(pc.DATA_TYPE) <> 'bigint' THEN 'STOP_PARENT_KEY_MUST_BE_BIGINT'
            WHEN LOCATE('unsigned',LOWER(pc.COLUMN_TYPE)) > 0 THEN 'STOP_PARENT_SIGNEDNESS_MISMATCH_WITH_NEW_BIGINT_CHILD'
            WHEN NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS s
                             WHERE s.TABLE_SCHEMA=DATABASE() AND s.TABLE_NAME='print_shops'
                               AND s.COLUMN_NAME='shop_id' AND s.SEQ_IN_INDEX=1
                               AND s.NON_UNIQUE=0 AND s.SUB_PART IS NULL) THEN 'STOP_PARENT_KEY_NOT_FULL_UNIQUE'
            ELSE 'OK' END AS audit_result
FROM information_schema.TABLES pt
JOIN information_schema.COLUMNS pc
  ON pc.TABLE_SCHEMA=pt.TABLE_SCHEMA AND pc.TABLE_NAME=pt.TABLE_NAME AND pc.COLUMN_NAME='shop_id'
WHERE pt.TABLE_SCHEMA=DATABASE() AND pt.TABLE_NAME='print_shops';

SELECT custom_request_id, quantity
FROM custom_print_requests
WHERE quantity IS NOT NULL AND (quantity < 1 OR quantity > 100000);

-- ============================================================================
-- STAGE 1: ADD MISSING COLUMNS. Existing known columns are left untouched.
-- ============================================================================

-- design_files already has mime_type, file_size_bytes, sha256_hash, deleted_at
-- in the live database. Add only analyzer output columns used by DesignFileDao.
ALTER TABLE design_files
  ADD COLUMN IF NOT EXISTS analysis_status VARCHAR(32) NULL,
  ADD COLUMN IF NOT EXISTS source_unit VARCHAR(8) NULL DEFAULT 'mm',
  ADD COLUMN IF NOT EXISTS width_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS depth_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS height_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS volume_cm3 DECIMAL(18,6) NULL,
  ADD COLUMN IF NOT EXISTS surface_area_cm2 DECIMAL(18,6) NULL,
  ADD COLUMN IF NOT EXISTS triangle_count BIGINT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS analysis_message VARCHAR(500) NULL;

-- The live print_shops table already contains contact/capability/build-volume
-- fields. Add its three confirmed gaps only.
ALTER TABLE print_shops
  ADD COLUMN IF NOT EXISTS maximum_custom_print_quantity INT UNSIGNED NOT NULL DEFAULT 100000,
  ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7) NULL;

-- Orders already has tracking and snapshot columns in the live database. These
-- IF NOT EXISTS clauses cover only genuinely missing checkout/custom-link fields.
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS shipping_latitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS shipping_longitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS delivery_note_snapshot VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS customer_note VARCHAR(1000) NULL,
  ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS tracking_token CHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS estimated_delivery_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS custom_print_request_id BIGINT NULL;

-- ============================================================================
-- STAGE 2: PRICING TABLES AND IN-PLACE UPGRADE OF LEGACY REQUEST/HISTORY.
-- ============================================================================

-- Created only if missing. Existing tables/data are not replaced.
CREATE TABLE IF NOT EXISTS shop_print_price_profiles (
  profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  shop_id BIGINT NOT NULL,
  printing_method VARCHAR(100) NOT NULL,
  material VARCHAR(120) NOT NULL,
  material_price_per_gram DECIMAL(12,4) NOT NULL,
  material_density_g_cm3 DECIMAL(10,4) NOT NULL,
  material_waste_factor DECIMAL(8,5) NOT NULL DEFAULT 0.05000,
  machine_cost_per_hour DECIMAL(12,2) NOT NULL,
  deposition_rate_cm3_per_hour DECIMAL(10,4) NOT NULL,
  wall_thickness_mm DECIMAL(8,4) NOT NULL DEFAULT 1.2000,
  support_material_factor DECIMAL(8,5) NOT NULL DEFAULT 0.05000,
  infill_usage_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  reference_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.2000,
  technology_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  complexity_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  setup_hours DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  post_processing_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  minimum_order_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  minimum_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.0800,
  maximum_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.4000,
  minimum_infill_percent SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  maximum_infill_percent SMALLINT UNSIGNED NOT NULL DEFAULT 100,
  production_days SMALLINT UNSIGNED NOT NULL DEFAULT 3,
  use_material_cost BOOLEAN NOT NULL DEFAULT TRUE,
  use_machine_cost BOOLEAN NOT NULL DEFAULT TRUE,
  use_infill_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_layer_height_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_technology_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_complexity_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_post_processing_fee BOOLEAN NOT NULL DEFAULT TRUE,
  use_quantity_discount BOOLEAN NOT NULL DEFAULT TRUE,
  use_minimum_price BOOLEAN NOT NULL DEFAULT TRUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_shop_print_profile (shop_id, printing_method, material),
  CONSTRAINT fk_shop_print_profile_shop FOREIGN KEY (shop_id)
    REFERENCES print_shops(shop_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_print_price_options (
  option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  option_type VARCHAR(20) NOT NULL,
  option_value VARCHAR(100) NOT NULL,
  label VARCHAR(120) NOT NULL,
  fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_shop_print_option (profile_id, option_type, option_value),
  CONSTRAINT fk_shop_print_option_profile FOREIGN KEY (profile_id)
    REFERENCES shop_print_price_profiles(profile_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_print_quantity_discount_tiers (
  discount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  minimum_quantity INT UNSIGNED NOT NULL,
  discount_rate DECIMAL(6,5) NOT NULL,
  UNIQUE KEY uq_shop_print_discount (profile_id, minimum_quantity),
  CONSTRAINT fk_shop_print_discount_profile FOREIGN KEY (profile_id)
    REFERENCES shop_print_price_profiles(profile_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- The table already exists in the legacy database. Add only fields read/written
-- by CustomPrintWorkflowDao and OrderService; existing technology/material/
-- quantity/status/quoted_amount and all existing keys/data remain in place.
ALTER TABLE custom_print_requests
  ADD COLUMN IF NOT EXISTS request_code VARCHAR(40) NULL,
  ADD COLUMN IF NOT EXISTS workflow_type VARCHAR(16) NULL,
  ADD COLUMN IF NOT EXISTS shop_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS infill_percent SMALLINT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS layer_height_mm DECIMAL(8,4) NULL,
  ADD COLUMN IF NOT EXISTS color_option VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS post_processing_option VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS customer_note VARCHAR(1000) NULL,
  ADD COLUMN IF NOT EXISTS dimensions_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS estimated_total DECIMAL(12,2) NULL,
  ADD COLUMN IF NOT EXISTS seller_response_note VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS production_days SMALLINT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS quote_valid_until DATETIME NULL,
  ADD COLUMN IF NOT EXISTS order_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS submitted_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS quoted_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS accepted_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS declined_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ADD COLUMN IF NOT EXISTS does not widen an existing VARCHAR(24). Read its
-- metadata and widen only when needed, retaining nullability, default, charset,
-- collation, and comment. MODIFY keeps the column in place and does not rewrite
-- any nonblank legacy request_code value.
SET @cp_request_code_data_type := (
  SELECT LOWER(DATA_TYPE) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_length := (
  SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_charset := (
  SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_collation := (
  SELECT COLLATION_NAME FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_default := (
  SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_comment := (
  SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
    AND COLUMN_NAME='request_code'
);
SET @cp_request_code_sql := CASE
  WHEN @cp_request_code_data_type IS NULL THEN
    'SELECT ''STOP: request_code column is missing after Stage 2'' AS migration_note'
  WHEN @cp_request_code_data_type <> 'varchar' THEN
    'SELECT ''STOP: request_code is not VARCHAR; inspect its definition before backfill'' AS migration_note'
  WHEN @cp_request_code_length < 40 THEN CONCAT(
    'ALTER TABLE `custom_print_requests` MODIFY COLUMN `request_code` VARCHAR(40)',
    IF(@cp_request_code_charset IS NULL, '', CONCAT(' CHARACTER SET ', @cp_request_code_charset)),
    IF(@cp_request_code_collation IS NULL, '', CONCAT(' COLLATE ', @cp_request_code_collation)),
    IF(@cp_request_code_nullable = 'YES', ' NULL', ' NOT NULL'),
    IF(@cp_request_code_default IS NULL, '', CONCAT(' DEFAULT ', QUOTE(@cp_request_code_default))),
    IF(@cp_request_code_comment IS NULL OR @cp_request_code_comment = '', '',
       CONCAT(' COMMENT ', QUOTE(@cp_request_code_comment)))
  )
  ELSE 'SELECT ''request_code is already VARCHAR(40) or wider; definition retained'' AS migration_note'
END;
PREPARE cp_request_code_stmt FROM @cp_request_code_sql;
EXECUTE cp_request_code_stmt;
DEALLOCATE PREPARE cp_request_code_stmt;

-- Backfill only NULL/blank codes after the column is wide enough. All existing
-- nonblank request codes are left byte-for-byte unchanged.
UPDATE custom_print_requests
SET request_code = CONCAT('PK3D-LEGACY-', custom_request_id)
WHERE request_code IS NULL OR TRIM(request_code) = '';

-- Review legacy business states before the CHECK/index stages. Existing status,
-- quote amounts, and quote dates are intentionally not reinterpreted or renewed.
SELECT status, workflow_type, COUNT(*) AS request_count,
       SUM(quoted_amount IS NOT NULL) AS rows_with_quote_amount
FROM custom_print_requests
GROUP BY status, workflow_type
ORDER BY status, workflow_type;

-- Upgrade the existing history table in place. The actor and old status are
-- nullable because automated quote expiry and the initial DRAFT event use NULL.
ALTER TABLE custom_print_request_history
  ADD COLUMN IF NOT EXISTS changed_by_user_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS old_status VARCHAR(40) NULL,
  ADD COLUMN IF NOT EXISTS new_status VARCHAR(40) NULL,
  ADD COLUMN IF NOT EXISTS note VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Review these after the backfill and before Stage 4/5. The request_code query
-- must return no rows before its unique index is added. Resolve duplicates
-- deliberately; do not rewrite legacy codes or delete requests/history.
SELECT request_code, COUNT(*) AS duplicate_rows
FROM custom_print_requests
GROUP BY request_code HAVING COUNT(*) > 1;

SELECT order_id, COUNT(*) AS duplicate_rows
FROM custom_print_requests
WHERE order_id IS NOT NULL
GROUP BY order_id HAVING COUNT(*) > 1;

SELECT custom_print_request_id, COUNT(*) AS duplicate_rows
FROM orders
WHERE custom_print_request_id IS NOT NULL
GROUP BY custom_print_request_id HAVING COUNT(*) > 1;

-- A nonzero orphan count means stop before Stage 5 and retain/investigate rows.
SELECT 'request_user_orphans' check_name, COUNT(*) orphan_rows
FROM custom_print_requests r LEFT JOIN users u ON u.user_id = r.user_id
WHERE r.user_id IS NOT NULL AND u.user_id IS NULL
UNION ALL
SELECT 'request_file_orphans', COUNT(*)
FROM custom_print_requests r LEFT JOIN design_files f ON f.design_file_id = r.design_file_id
WHERE r.design_file_id IS NOT NULL AND f.design_file_id IS NULL
UNION ALL
SELECT 'request_shop_orphans', COUNT(*)
FROM custom_print_requests r LEFT JOIN print_shops s ON s.shop_id = r.shop_id
WHERE r.shop_id IS NOT NULL AND s.shop_id IS NULL
UNION ALL
SELECT 'request_order_orphans', COUNT(*)
FROM custom_print_requests r LEFT JOIN orders o ON o.order_id = r.order_id
WHERE r.order_id IS NOT NULL AND o.order_id IS NULL
UNION ALL
SELECT 'history_request_orphans', COUNT(*)
FROM custom_print_request_history h LEFT JOIN custom_print_requests r
  ON r.custom_request_id = h.custom_request_id
WHERE h.custom_request_id IS NOT NULL AND r.custom_request_id IS NULL
UNION ALL
SELECT 'history_actor_orphans', COUNT(*)
FROM custom_print_request_history h LEFT JOIN users u ON u.user_id = h.changed_by_user_id
WHERE h.changed_by_user_id IS NOT NULL AND u.user_id IS NULL
UNION ALL
SELECT 'order_request_orphans', COUNT(*)
FROM orders o LEFT JOIN custom_print_requests r ON r.custom_request_id = o.custom_print_request_id
WHERE o.custom_print_request_id IS NOT NULL AND r.custom_request_id IS NULL;

-- Foreign-key preflight after Stage 2, before Stage 5. Each planned parent/child
-- key pair must exist, use InnoDB on both tables, match DATA_TYPE, numeric
-- precision/scale, character length/charset/collation, UNSIGNED, and reference
-- a full single-column unique key.
-- A mismatch sets @cp_fk_metadata_ok=0; Stage 5 skips every ADD FOREIGN KEY.
-- Do not alter types/data to force a match: stop and reconcile manually.
SET @cp_fk_metadata_issues := (
  SELECT COUNT(*) FROM (
    SELECT CASE
      WHEN ct.ENGINE IS NULL OR pt.ENGINE IS NULL OR cc.COLUMN_NAME IS NULL OR pc.COLUMN_NAME IS NULL THEN 'MISSING_TABLE_OR_COLUMN'
      WHEN UPPER(ct.ENGINE)<>'INNODB' OR UPPER(pt.ENGINE)<>'INNODB' THEN 'ENGINE_MISMATCH'
      WHEN LOWER(cc.DATA_TYPE)<>LOWER(pc.DATA_TYPE)
        OR NOT (cc.NUMERIC_PRECISION <=> pc.NUMERIC_PRECISION)
        OR NOT (cc.NUMERIC_SCALE <=> pc.NUMERIC_SCALE)
        OR NOT (cc.CHARACTER_MAXIMUM_LENGTH <=> pc.CHARACTER_MAXIMUM_LENGTH)
        OR NOT (cc.CHARACTER_SET_NAME <=> pc.CHARACTER_SET_NAME)
        OR NOT (cc.COLLATION_NAME <=> pc.COLLATION_NAME)
        OR (LOCATE('unsigned',LOWER(cc.COLUMN_TYPE))>0)<>(LOCATE('unsigned',LOWER(pc.COLUMN_TYPE))>0) THEN 'TYPE_OR_SIGNEDNESS_MISMATCH'
      WHEN NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS s
        WHERE s.TABLE_SCHEMA=DATABASE() AND s.TABLE_NAME=p.parent_table
          AND s.COLUMN_NAME=p.parent_column AND s.SEQ_IN_INDEX=1
          AND s.NON_UNIQUE=0 AND s.SUB_PART IS NULL
          AND (SELECT COUNT(*) FROM information_schema.STATISTICS s2
               WHERE s2.TABLE_SCHEMA=s.TABLE_SCHEMA AND s2.TABLE_NAME=s.TABLE_NAME
                 AND s2.INDEX_NAME=s.INDEX_NAME)=1
      ) THEN 'PARENT_KEY_NOT_FULL_UNIQUE'
      WHEN p.child_table='custom_print_request_history' AND p.child_column='changed_by_user_id' AND cc.IS_NULLABLE<>'YES' THEN 'NULLABILITY_MISMATCH_FOR_SET_NULL_FK'
      ELSE 'OK'
    END AS pair_status
    FROM (
      SELECT 'custom_print_requests' child_table,'user_id' child_column,'users' parent_table,'user_id' parent_column
      UNION ALL SELECT 'custom_print_requests','design_file_id','design_files','design_file_id'
      UNION ALL SELECT 'custom_print_requests','shop_id','print_shops','shop_id'
      UNION ALL SELECT 'custom_print_requests','order_id','orders','order_id'
      UNION ALL SELECT 'custom_print_request_history','custom_request_id','custom_print_requests','custom_request_id'
      UNION ALL SELECT 'custom_print_request_history','changed_by_user_id','users','user_id'
      UNION ALL SELECT 'orders','custom_print_request_id','custom_print_requests','custom_request_id'
      UNION ALL SELECT 'shop_print_price_profiles','shop_id','print_shops','shop_id'
      UNION ALL SELECT 'shop_print_price_options','profile_id','shop_print_price_profiles','profile_id'
      UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id','shop_print_price_profiles','profile_id'
    ) p
    LEFT JOIN information_schema.TABLES ct
      ON ct.TABLE_SCHEMA=DATABASE() AND ct.TABLE_NAME=p.child_table
    LEFT JOIN information_schema.TABLES pt
      ON pt.TABLE_SCHEMA=DATABASE() AND pt.TABLE_NAME=p.parent_table
    LEFT JOIN information_schema.COLUMNS cc
      ON cc.TABLE_SCHEMA=DATABASE() AND cc.TABLE_NAME=p.child_table AND cc.COLUMN_NAME=p.child_column
    LEFT JOIN information_schema.COLUMNS pc
      ON pc.TABLE_SCHEMA=DATABASE() AND pc.TABLE_NAME=p.parent_table AND pc.COLUMN_NAME=p.parent_column
  ) checked_pairs
  WHERE pair_status<>'OK'
);
SET @cp_fk_metadata_ok := IF(@cp_fk_metadata_issues=0,1,0);

SELECT p.child_table, p.child_column, p.parent_table, p.parent_column,
       ct.ENGINE AS child_engine, cc.COLUMN_TYPE AS child_type,
       pt.ENGINE AS parent_engine, pc.COLUMN_TYPE AS parent_type,
       CASE
         WHEN ct.ENGINE IS NULL OR pt.ENGINE IS NULL OR cc.COLUMN_NAME IS NULL OR pc.COLUMN_NAME IS NULL THEN 'STOP_MISSING_TABLE_OR_COLUMN'
         WHEN UPPER(ct.ENGINE)<>'INNODB' OR UPPER(pt.ENGINE)<>'INNODB' THEN 'STOP_ENGINE_MISMATCH'
         WHEN LOWER(cc.DATA_TYPE)<>LOWER(pc.DATA_TYPE)
           OR NOT (cc.NUMERIC_PRECISION <=> pc.NUMERIC_PRECISION)
           OR NOT (cc.NUMERIC_SCALE <=> pc.NUMERIC_SCALE)
           OR NOT (cc.CHARACTER_MAXIMUM_LENGTH <=> pc.CHARACTER_MAXIMUM_LENGTH)
           OR NOT (cc.CHARACTER_SET_NAME <=> pc.CHARACTER_SET_NAME)
           OR NOT (cc.COLLATION_NAME <=> pc.COLLATION_NAME)
           OR (LOCATE('unsigned',LOWER(cc.COLUMN_TYPE))>0)<>(LOCATE('unsigned',LOWER(pc.COLUMN_TYPE))>0) THEN 'STOP_TYPE_OR_SIGNEDNESS_MISMATCH'
         WHEN NOT EXISTS (
           SELECT 1 FROM information_schema.STATISTICS s
           WHERE s.TABLE_SCHEMA=DATABASE() AND s.TABLE_NAME=p.parent_table
             AND s.COLUMN_NAME=p.parent_column AND s.SEQ_IN_INDEX=1
             AND s.NON_UNIQUE=0 AND s.SUB_PART IS NULL
             AND (SELECT COUNT(*) FROM information_schema.STATISTICS s2
                  WHERE s2.TABLE_SCHEMA=s.TABLE_SCHEMA AND s2.TABLE_NAME=s.TABLE_NAME
                    AND s2.INDEX_NAME=s.INDEX_NAME)=1
         ) THEN 'STOP_PARENT_KEY_NOT_FULL_UNIQUE'
         WHEN p.child_table='custom_print_request_history' AND p.child_column='changed_by_user_id' AND cc.IS_NULLABLE<>'YES' THEN 'STOP_CHANGED_BY_MUST_ALLOW_NULL'
         ELSE 'OK'
       END AS audit_result
FROM (
  SELECT 'custom_print_requests' child_table,'user_id' child_column,'users' parent_table,'user_id' parent_column
  UNION ALL SELECT 'custom_print_requests','design_file_id','design_files','design_file_id'
  UNION ALL SELECT 'custom_print_requests','shop_id','print_shops','shop_id'
  UNION ALL SELECT 'custom_print_requests','order_id','orders','order_id'
  UNION ALL SELECT 'custom_print_request_history','custom_request_id','custom_print_requests','custom_request_id'
  UNION ALL SELECT 'custom_print_request_history','changed_by_user_id','users','user_id'
  UNION ALL SELECT 'orders','custom_print_request_id','custom_print_requests','custom_request_id'
  UNION ALL SELECT 'shop_print_price_profiles','shop_id','print_shops','shop_id'
  UNION ALL SELECT 'shop_print_price_options','profile_id','shop_print_price_profiles','profile_id'
  UNION ALL SELECT 'shop_print_quantity_discount_tiers','profile_id','shop_print_price_profiles','profile_id'
) p
LEFT JOIN information_schema.TABLES ct
  ON ct.TABLE_SCHEMA=DATABASE() AND ct.TABLE_NAME=p.child_table
LEFT JOIN information_schema.TABLES pt
  ON pt.TABLE_SCHEMA=DATABASE() AND pt.TABLE_NAME=p.parent_table
LEFT JOIN information_schema.COLUMNS cc
  ON cc.TABLE_SCHEMA=DATABASE() AND cc.TABLE_NAME=p.child_table AND cc.COLUMN_NAME=p.child_column
LEFT JOIN information_schema.COLUMNS pc
  ON pc.TABLE_SCHEMA=DATABASE() AND pc.TABLE_NAME=p.parent_table AND pc.COLUMN_NAME=p.parent_column
ORDER BY p.child_table,p.child_column;

-- ============================================================================
-- STAGE 3: REPLACE LEGACY RESTRICTIVE CHECKS WITHOUT TOUCHING FOREIGN KEYS.
-- ============================================================================
-- This removes only a simple quantity 1..100 expression and direct IN-list
-- checks that omit values accepted by CustomPrintController:
-- technology FDM/SLA/SLS; materials PLA, ABS, PETG, TPU, RESIN, RESIN-STD,
-- RESIN-CLEAR, RESIN-FLEX, NYLON, NYLON-PA12, NYLON-GF; request statuses
-- DRAFT, READY_TO_ORDER, PENDING_REVIEW, QUOTED, CUSTOMER_DECLINED, EXPIRED,
-- ORDER_CREATED, CLOSED. Composite/other CHECKs are retained for manual review.
-- The preflight above prints every CHECK_CLAUSE. Review it before this stage.
SET @cp_drop_checks_sql := (
  SELECT IF(COUNT(*) = 0,
    'SELECT ''No recognized incompatible custom-print CHECK constraints'' AS migration_note',
    CONCAT('ALTER TABLE `custom_print_requests` ',
      GROUP_CONCAT(CONCAT('DROP CONSTRAINT `', REPLACE(constraint_name, '`', '``'), '`')
                   ORDER BY constraint_name SEPARATOR ', ')))
  FROM (
    SELECT tc.CONSTRAINT_NAME AS constraint_name,
           LOWER(cc.CHECK_CLAUSE) AS clause_text,
           LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(cc.CHECK_CLAUSE,
             '`',''),' ',''),'\t',''),'\r',''),'\n','')) AS normalized_clause
    FROM information_schema.TABLE_CONSTRAINTS tc
    JOIN information_schema.CHECK_CONSTRAINTS cc
      ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
     AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
     AND cc.TABLE_NAME = tc.TABLE_NAME
    WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
      AND tc.TABLE_NAME = 'custom_print_requests'
      AND tc.CONSTRAINT_TYPE = 'CHECK'
  ) checks_found
  WHERE normalized_clause IN (
      'quantitybetween1and100',
      'quantity>=1andquantity<=100',
      'quantity<=100andquantity>=1',
      'quantity>=1andquantity<101',
      'quantity<101andquantity>=1',
      'quantity>0andquantity<=100',
      'quantity<=100andquantity>0',
      'quantity>0andquantity<101',
      'quantity<101andquantity>0')
     OR (
       normalized_clause LIKE 'technologyin%'
       AND clause_text NOT REGEXP '(^|[^a-z])(and|or)([^a-z]|$)'
       AND (LOCATE("'fdm'", clause_text) = 0 OR LOCATE("'sla'", clause_text) = 0
            OR LOCATE("'sls'", clause_text) = 0))
     OR (
       normalized_clause LIKE 'materialin%'
       AND clause_text NOT REGEXP '(^|[^a-z])(and|or)([^a-z]|$)'
       AND (LOCATE("'pla'", clause_text) = 0 OR LOCATE("'abs'", clause_text) = 0
            OR LOCATE("'petg'", clause_text) = 0 OR LOCATE("'tpu'", clause_text) = 0
            OR LOCATE("'resin'", clause_text) = 0 OR LOCATE("'resin-std'", clause_text) = 0
            OR LOCATE("'resin-clear'", clause_text) = 0 OR LOCATE("'resin-flex'", clause_text) = 0
            OR LOCATE("'nylon'", clause_text) = 0 OR LOCATE("'nylon-pa12'", clause_text) = 0
            OR LOCATE("'nylon-gf'", clause_text) = 0))
     OR (
       normalized_clause LIKE 'statusin%'
       AND clause_text NOT REGEXP '(^|[^a-z])(and|or)([^a-z]|$)'
       AND (LOCATE("'draft'", clause_text) = 0 OR LOCATE("'ready_to_order'", clause_text) = 0
            OR LOCATE("'pending_review'", clause_text) = 0 OR LOCATE("'quoted'", clause_text) = 0
            OR LOCATE("'customer_declined'", clause_text) = 0 OR LOCATE("'expired'", clause_text) = 0
            OR LOCATE("'order_created'", clause_text) = 0 OR LOCATE("'closed'", clause_text) = 0))
);
PREPARE cp_drop_checks_stmt FROM @cp_drop_checks_sql;
EXECUTE cp_drop_checks_stmt;
DEALLOCATE PREPARE cp_drop_checks_stmt;

-- A legacy ENUM cannot store newly accepted values. Convert only ENUM columns,
-- preserving each column's nullability, default, charset, collation and comment.
-- ALTER changes the type only; it does not UPDATE or remap existing row values.
SET @cp_enum_column := 'technology';
SET @cp_enum_data_type := (SELECT LOWER(DATA_TYPE) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_default := (SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_charset := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_collation := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_comment := (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_sql := IF(@cp_enum_data_type='enum', CONCAT('ALTER TABLE `custom_print_requests` MODIFY COLUMN `',@cp_enum_column,'` VARCHAR(255)',IF(@cp_enum_charset IS NULL,'',CONCAT(' CHARACTER SET ',@cp_enum_charset)),IF(@cp_enum_collation IS NULL,'',CONCAT(' COLLATE ',@cp_enum_collation)),IF(@cp_enum_nullable='YES',' NULL',' NOT NULL'),IF(@cp_enum_default IS NULL,'',CONCAT(' DEFAULT ',QUOTE(@cp_enum_default))),IF(@cp_enum_comment IS NULL OR @cp_enum_comment='','',CONCAT(' COMMENT ',QUOTE(@cp_enum_comment)))), 'SELECT ''technology is not ENUM; definition retained'' AS migration_note');
PREPARE cp_enum_stmt FROM @cp_enum_sql; EXECUTE cp_enum_stmt; DEALLOCATE PREPARE cp_enum_stmt;

SET @cp_enum_column := 'material';
SET @cp_enum_data_type := (SELECT LOWER(DATA_TYPE) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_default := (SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_charset := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_collation := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_comment := (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_sql := IF(@cp_enum_data_type='enum', CONCAT('ALTER TABLE `custom_print_requests` MODIFY COLUMN `',@cp_enum_column,'` VARCHAR(255)',IF(@cp_enum_charset IS NULL,'',CONCAT(' CHARACTER SET ',@cp_enum_charset)),IF(@cp_enum_collation IS NULL,'',CONCAT(' COLLATE ',@cp_enum_collation)),IF(@cp_enum_nullable='YES',' NULL',' NOT NULL'),IF(@cp_enum_default IS NULL,'',CONCAT(' DEFAULT ',QUOTE(@cp_enum_default))),IF(@cp_enum_comment IS NULL OR @cp_enum_comment='','',CONCAT(' COMMENT ',QUOTE(@cp_enum_comment)))), 'SELECT ''material is not ENUM; definition retained'' AS migration_note');
PREPARE cp_enum_stmt FROM @cp_enum_sql; EXECUTE cp_enum_stmt; DEALLOCATE PREPARE cp_enum_stmt;

SET @cp_enum_column := 'status';
SET @cp_enum_data_type := (SELECT LOWER(DATA_TYPE) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_default := (SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_charset := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_collation := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_comment := (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME=@cp_enum_column);
SET @cp_enum_sql := IF(@cp_enum_data_type='enum', CONCAT('ALTER TABLE `custom_print_requests` MODIFY COLUMN `',@cp_enum_column,'` VARCHAR(255)',IF(@cp_enum_charset IS NULL,'',CONCAT(' CHARACTER SET ',@cp_enum_charset)),IF(@cp_enum_collation IS NULL,'',CONCAT(' COLLATE ',@cp_enum_collation)),IF(@cp_enum_nullable='YES',' NULL',' NOT NULL'),IF(@cp_enum_default IS NULL,'',CONCAT(' DEFAULT ',QUOTE(@cp_enum_default))),IF(@cp_enum_comment IS NULL OR @cp_enum_comment='','',CONCAT(' COMMENT ',QUOTE(@cp_enum_comment)))), 'SELECT ''status is not ENUM; definition retained'' AS migration_note');
PREPARE cp_enum_stmt FROM @cp_enum_sql; EXECUTE cp_enum_stmt; DEALLOCATE PREPARE cp_enum_stmt;

-- INT UNSIGNED is required for the 100000 quantity contract. Widen only legacy
-- TINYINT/SMALLINT definitions; an INT/MEDIUMINT/BIGINT column already fits.
SET @cp_column_type := (SELECT LOWER(COLUMN_TYPE) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME='quantity');
SET @cp_sql := IF(@cp_column_type LIKE 'tinyint%' OR @cp_column_type LIKE 'smallint%',
  'ALTER TABLE custom_print_requests MODIFY COLUMN quantity INT UNSIGNED NULL',
  'SELECT ''quantity column already supports 100000'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Replace the quantity cap with the workflow cap. NULL remains valid for old
-- drafts; backend validation still requires 1..100000 before submit/order.
SET @cp_has_quantity_check := (
  SELECT COUNT(*)
  FROM information_schema.CHECK_CONSTRAINTS cc
  WHERE cc.CONSTRAINT_SCHEMA = DATABASE()
    AND cc.TABLE_NAME = 'custom_print_requests'
    AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(cc.CHECK_CLAUSE,
        '`',''),' ',''),'\t',''),'\r',''),'\n',''))
        IN ('quantityisnullorquantitybetween1and100000',
            'quantitybetween1and100000')
);
SET @cp_add_quantity_check_sql := IF(@cp_has_quantity_check = 0,
  'ALTER TABLE `custom_print_requests` ADD CONSTRAINT `chk_cp_request_quantity_100000` CHECK (`quantity` IS NULL OR `quantity` BETWEEN 1 AND 100000)',
  'SELECT ''Compatible quantity CHECK already exists'' AS migration_note');
PREPARE cp_add_quantity_check_stmt FROM @cp_add_quantity_check_sql;
EXECUTE cp_add_quantity_check_stmt;
DEALLOCATE PREPARE cp_add_quantity_check_stmt;

-- ============================================================================
-- STAGE 4: CONDITIONAL INDEXES. Existing equivalent leftmost prefixes are kept.
-- ============================================================================
-- Recheck duplicates immediately before index creation. This flag also gates
-- the other Stage 4 unique indexes. Plain SQL cannot force every GUI/client to
-- abort the remaining script after a result; run Stage 4 separately and do not
-- continue to Stage 5 if this count is nonzero. The guards below still prevent
-- unique-index DDL if a client continues or if Stage 4 is submitted alone.
SET @cp_request_code_duplicate_count := (
  SELECT COUNT(*) FROM (
    SELECT request_code FROM custom_print_requests
    GROUP BY request_code HAVING COUNT(*)>1
  ) duplicate_codes
);
SELECT @cp_request_code_duplicate_count AS duplicate_request_code_groups;

-- Only a full, single-column unique index satisfies the code contract.
-- SUB_PART IS NULL excludes unique prefix indexes.
SET @cp_has_idx := (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
  GROUP BY INDEX_NAME HAVING MAX(NON_UNIQUE)=0 AND COUNT(*)=1
    AND MAX(COLUMN_NAME)='request_code' AND MAX(SUB_PART) IS NULL
) i);
SET @cp_sql := IF(COALESCE(@cp_request_code_duplicate_count,1)>0,
  'SELECT ''STOP: duplicate request_code values; Stage 4 unique indexes are skipped'' AS migration_note',
  IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD UNIQUE INDEX uq_cp_request_code (request_code)','SELECT ''full request_code unique index already exists'' AS migration_note'));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- One order per custom request; duplicate preflight must return no rows.
SET @cp_has_idx := (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests'
  GROUP BY INDEX_NAME HAVING MAX(NON_UNIQUE)=0 AND COUNT(*)=1
    AND MAX(COLUMN_NAME)='order_id' AND MAX(SUB_PART) IS NULL
) i);
SET @cp_sql := IF(COALESCE(@cp_request_code_duplicate_count,1)>0,
  'SELECT ''STOP: duplicate request_code values; Stage 4 unique indexes are skipped'' AS migration_note',
  IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD UNIQUE INDEX uq_cp_request_order (order_id)','SELECT ''full request order unique index already exists'' AS migration_note'));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Conditional non-unique index helper pattern: only add when no existing index
-- starts with the same columns. Index names are migration-specific to avoid
-- colliding with unrelated indexes that happen to use a familiar name.

-- custom_print_requests(user_id, created_at, custom_request_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',3)='user_id,created_at,custom_request_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD INDEX idx_cp_request_user_created (user_id,created_at,custom_request_id)','SELECT ''request user index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- custom_print_requests(design_file_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',1)='design_file_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD INDEX idx_cp_request_file (design_file_id)','SELECT ''request file index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- custom_print_requests(shop_id, workflow_type, status, created_at)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',4)='shop_id,workflow_type,status,created_at') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD INDEX idx_cp_request_shop_status (shop_id,workflow_type,status,created_at)','SELECT ''request shop/status index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- custom_print_requests(status, quote_valid_until)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',2)='status,quote_valid_until') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE custom_print_requests ADD INDEX idx_cp_request_expiry (status,quote_valid_until)','SELECT ''request expiry index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- custom_print_request_history(custom_request_id, created_at)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_request_history' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',2)='custom_request_id,created_at') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE custom_print_request_history ADD INDEX idx_cp_history_request_time (custom_request_id,created_at)','SELECT ''custom history index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- orders(custom_print_request_id) must be unique; nullable for ordinary orders.
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' GROUP BY INDEX_NAME HAVING MAX(NON_UNIQUE)=0 AND COUNT(*)=1 AND MAX(COLUMN_NAME)='custom_print_request_id' AND MAX(SUB_PART) IS NULL) i);
SET @cp_sql := IF(COALESCE(@cp_request_code_duplicate_count,1)>0,
  'SELECT ''STOP: duplicate request_code values; Stage 4 unique indexes are skipped'' AS migration_note',
  IF(@cp_has_idx=0,'ALTER TABLE orders ADD UNIQUE INDEX uq_cp_orders_request (custom_print_request_id)','SELECT ''full orders request unique index already exists'' AS migration_note'));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- orders(user_id, placed_at, order_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',3)='user_id,placed_at,order_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE orders ADD INDEX idx_cp_orders_user_placed (user_id,placed_at,order_id)','SELECT ''orders user index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- design_files(user_id, deleted_at)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='design_files' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',2)='user_id,deleted_at') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE design_files ADD INDEX idx_cp_design_owner_deleted (user_id,deleted_at)','SELECT ''design file owner index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- print_shops(owner_user_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='print_shops' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',1)='owner_user_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE print_shops ADD INDEX idx_cp_shops_owner (owner_user_id)','SELECT ''shop owner index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- print_shops(verification_status, status)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='print_shops' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',2)='verification_status,status') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE print_shops ADD INDEX idx_cp_shops_workflow_status (verification_status,status)','SELECT ''shop workflow index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- order_items(order_id): explicitly inspect existing indexes in Stage 0 first.
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order_items' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',1)='order_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE order_items ADD INDEX idx_cp_order_items_order (order_id)','SELECT ''order_items order index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- order_status_history(order_id, changed_at, history_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order_status_history' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',3)='order_id,changed_at,history_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE order_status_history ADD INDEX idx_cp_order_status_history_time (order_id,changed_at,history_id)','SELECT ''order history index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- production_updates(order_item_id, shop_id, created_at, production_update_id)
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='production_updates' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',4)='order_item_id,shop_id,created_at,production_update_id') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE production_updates ADD INDEX idx_cp_production_item_shop_time (order_item_id,shop_id,created_at,production_update_id)','SELECT ''production update index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- reviews(shop_id, is_published) used for shop recommendation and review counts.
SET @cp_has_idx := (SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reviews' GROUP BY INDEX_NAME HAVING SUBSTRING_INDEX(GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','),',',2)='shop_id,is_published') i);
SET @cp_sql := IF(@cp_has_idx=0,'ALTER TABLE reviews ADD INDEX idx_cp_reviews_shop_published (shop_id,is_published)','SELECT ''review shop index prefix already exists'' AS migration_note');
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- ============================================================================
-- STAGE 5: ADD MISSING FOREIGN KEYS ONLY WHEN NO EQUIVALENT EXISTS AND DATA IS
-- CLEAN. Existing FK names/actions are preserved. Nonzero preflight orphan
-- counts must be investigated before running this stage. This stage is also
-- write-gated on clean request_code and FK type/engine/nullability audits.
-- ============================================================================

SET @cp_stage5_allowed := IF(COALESCE(@cp_request_code_duplicate_count,1)=0
  AND COALESCE(@cp_fk_metadata_ok,0)=1,1,0);
SELECT @cp_stage5_allowed AS stage5_write_allowed;

-- Request -> user
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME='user_id' AND REFERENCED_TABLE_NAME='users' AND REFERENCED_COLUMN_NAME='user_id'),
  'SELECT ''request user FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_requests r LEFT JOIN users u ON u.user_id=r.user_id WHERE r.user_id IS NOT NULL AND u.user_id IS NULL),
    'SELECT ''request user FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_requests ADD CONSTRAINT fk_cp_request_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Request -> design file
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME='design_file_id' AND REFERENCED_TABLE_NAME='design_files' AND REFERENCED_COLUMN_NAME='design_file_id'),
  'SELECT ''request file FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_requests r LEFT JOIN design_files f ON f.design_file_id=r.design_file_id WHERE r.design_file_id IS NOT NULL AND f.design_file_id IS NULL),
    'SELECT ''request file FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_requests ADD CONSTRAINT fk_cp_request_file FOREIGN KEY (design_file_id) REFERENCES design_files(design_file_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Request -> shop
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME='shop_id' AND REFERENCED_TABLE_NAME='print_shops' AND REFERENCED_COLUMN_NAME='shop_id'),
  'SELECT ''request shop FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_requests r LEFT JOIN print_shops s ON s.shop_id=r.shop_id WHERE r.shop_id IS NOT NULL AND s.shop_id IS NULL),
    'SELECT ''request shop FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_requests ADD CONSTRAINT fk_cp_request_shop FOREIGN KEY (shop_id) REFERENCES print_shops(shop_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Request -> order
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_requests' AND COLUMN_NAME='order_id' AND REFERENCED_TABLE_NAME='orders' AND REFERENCED_COLUMN_NAME='order_id'),
  'SELECT ''request order FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_requests r LEFT JOIN orders o ON o.order_id=r.order_id WHERE r.order_id IS NOT NULL AND o.order_id IS NULL),
    'SELECT ''request order FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_requests ADD CONSTRAINT fk_cp_request_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- History -> request
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_request_history' AND COLUMN_NAME='custom_request_id' AND REFERENCED_TABLE_NAME='custom_print_requests' AND REFERENCED_COLUMN_NAME='custom_request_id'),
  'SELECT ''history request FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_request_history h LEFT JOIN custom_print_requests r ON r.custom_request_id=h.custom_request_id WHERE h.custom_request_id IS NOT NULL AND r.custom_request_id IS NULL),
    'SELECT ''history request FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_request_history ADD CONSTRAINT fk_cp_history_request FOREIGN KEY (custom_request_id) REFERENCES custom_print_requests(custom_request_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- History -> user (nullable actor supports system-generated expiry events).
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='custom_print_request_history' AND COLUMN_NAME='changed_by_user_id' AND REFERENCED_TABLE_NAME='users' AND REFERENCED_COLUMN_NAME='user_id'),
  'SELECT ''history actor FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM custom_print_request_history h LEFT JOIN users u ON u.user_id=h.changed_by_user_id WHERE h.changed_by_user_id IS NOT NULL AND u.user_id IS NULL),
    'SELECT ''history actor FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE custom_print_request_history ADD CONSTRAINT fk_cp_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- Reverse one-to-one link on orders. Unique request ID is the database-level
-- duplicate-order guard; the request/order transaction remains in the service.
SET @cp_sql := IF(COALESCE(@cp_stage5_allowed,0)=0,
  'SELECT ''STOP: duplicate request_code or FK metadata mismatch; Stage 5 FK additions are skipped'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' AND COLUMN_NAME='custom_print_request_id' AND REFERENCED_TABLE_NAME='custom_print_requests' AND REFERENCED_COLUMN_NAME='custom_request_id'),
  'SELECT ''orders request FK already exists'' AS migration_note',
  IF(EXISTS(SELECT 1 FROM orders o LEFT JOIN custom_print_requests r ON r.custom_request_id=o.custom_print_request_id WHERE o.custom_print_request_id IS NOT NULL AND r.custom_request_id IS NULL),
    'SELECT ''orders request FK skipped: orphan rows exist'' AS migration_note',
    'ALTER TABLE orders ADD CONSTRAINT fk_cp_orders_request FOREIGN KEY (custom_print_request_id) REFERENCES custom_print_requests(custom_request_id) ON UPDATE CASCADE ON DELETE RESTRICT')));
PREPARE cp_stmt FROM @cp_sql; EXECUTE cp_stmt; DEALLOCATE PREPARE cp_stmt;

-- ============================================================================
-- STAGE 6: POSTFLIGHT (read-only). Verify all DAO columns, keys and constraints.
-- ============================================================================
SHOW COLUMNS FROM design_files;
SHOW COLUMNS FROM print_shops;
SHOW COLUMNS FROM orders;
SHOW COLUMNS FROM custom_print_requests;
SHOW COLUMNS FROM custom_print_request_history;
SHOW INDEX FROM design_files;
SHOW INDEX FROM print_shops;
SHOW INDEX FROM orders;
SHOW INDEX FROM order_items;
SHOW INDEX FROM order_status_history;
SHOW INDEX FROM production_updates;
SHOW INDEX FROM reviews;
SHOW INDEX FROM custom_print_requests;
SHOW INDEX FROM custom_print_request_history;
SHOW CREATE TABLE shop_print_price_profiles;
SHOW CREATE TABLE shop_print_price_options;
SHOW CREATE TABLE shop_print_quantity_discount_tiers;
SHOW CREATE TABLE custom_print_requests;
SHOW CREATE TABLE custom_print_request_history;

SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS tc
LEFT JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA
 AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
 AND cc.TABLE_NAME=tc.TABLE_NAME
WHERE tc.CONSTRAINT_SCHEMA=DATABASE()
  AND tc.TABLE_NAME IN ('custom_print_requests','custom_print_request_history','orders')
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_TYPE, tc.CONSTRAINT_NAME;

-- DAO/schema alignment notes:
-- * CustomPrintWorkflowDao reads/writes all request/history columns added above.
-- * DesignFileDao reads/writes the analysis columns and live file metadata.
-- * CustomPrintPricingDao uses the three pricing tables created above.
-- * OrderService/OrderDao link one request to one order and store ETA in orders.
-- * WorkshopDao and customer account/order DAOs read request snapshots, shops,
--   order_items, order_status_history, production_updates and reviews; Stage 0
--   indexes them only when an equivalent index prefix is absent.
-- If a required column already exists with a type/default/nullability different
-- from the DAO contract, stop and inspect SHOW CREATE TABLE before changing it.
