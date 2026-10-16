import fs from "node:fs";

const source = fs.readFileSync("src/main/webapp/admin-ui/js/data.js", "utf8");
const dataUrl = `data:text/javascript;base64,${Buffer.from(source).toString("base64")}`;
const data = await import(dataUrl);
const entityTypes = [
  "users", "sellers", "products", "orders", "transactions", "orderItems",
  "depositTransactions", "commissionRules", "reviews"
];
const slash = String.fromCharCode(92);
const quote = String.fromCharCode(39);
const escapeSql = value => value.replaceAll(slash, slash + slash).replaceAll(quote, quote + quote);

let sql = `-- Sample data for the Admin workspace. Re-running resets only these admin workspace tables.
-- Creates/uses printkraft; it does not drop the database or business tables.
CREATE DATABASE IF NOT EXISTS printkraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE printkraft;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS admin_workspace_records (
    record_type VARCHAR(40) NOT NULL,
    record_id VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    payload JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_type, record_id),
    KEY idx_admin_workspace_type_order (record_type, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_workspace_settings (
    setting_key VARCHAR(80) NOT NULL PRIMARY KEY,
    setting_value JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM admin_workspace_records;
DELETE FROM admin_workspace_settings;
`;

for (const type of entityTypes) {
  const records = data[type] || [];
  if (!records.length) continue;
  sql += `\nINSERT INTO admin_workspace_records (record_type, record_id, sort_order, payload) VALUES\n`;
  sql += records.map((record, index) => {
    const payload = escapeSql(JSON.stringify(record));
    return `('${type}', '${record.id}', ${index + 1}, CAST('${payload}' AS JSON))`;
  }).join(",\n") + ";\n";
}

const settings = { ...data.sampleSettings, commissionRate: 10 };
const insertSetting = (key, value) => `('${key}', CAST('${escapeSql(JSON.stringify(value))}' AS JSON))`;
sql += `\nINSERT INTO admin_workspace_settings (setting_key, setting_value) VALUES\n`;
sql += [insertSetting("marketplace", settings), insertSetting("dashboardSeries", data.dashboardSeries)].join(",\n") + ";\n";

fs.writeFileSync("printkraft_admin_seed.sql", sql, "utf8");
console.log(`Wrote printkraft_admin_seed.sql (${entityTypes.map(type => `${type}=${data[type]?.length || 0}`).join(", ")}).`);
