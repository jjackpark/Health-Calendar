// Intentionally empty by default.
// Add Drizzle tables here when the site actually needs a database.
// See examples/d1/db/schema.ts for an opt-in example.
import { sqliteTable,text,integer,primaryKey,uniqueIndex } from 'drizzle-orm/sqlite-core';
export const records=sqliteTable('health_records',{userId:text('user_id').notNull(),date:text('date').notNull(),metric:text('metric').notNull(),value:integer('value').notNull(),note:text('note').notNull().default(''),kind:text('kind').notNull().default(''),abv:integer('abv').notNull().default(0),source:text('source').notNull().default('manual'),updatedAt:text('updated_at').notNull()},t=>[primaryKey({columns:[t.userId,t.date,t.metric]})]);
export const preferences=sqliteTable('health_preferences',{userId:text('user_id').primaryKey(),goal:integer('goal').notNull().default(8000)});
export const pairingCodes=sqliteTable('health_pairing_codes',{
 userId:text('user_id').primaryKey(),codeHash:text('code_hash').notNull(),expiresAt:integer('expires_at').notNull()
},t=>[uniqueIndex('health_pairing_code_hash').on(t.codeHash)]);
export const devices=sqliteTable('health_devices',{
 userId:text('user_id').primaryKey(),tokenHash:text('token_hash').notNull(),name:text('name').notNull(),createdAt:text('created_at').notNull(),lastSyncAt:text('last_sync_at'),lastObservedAt:integer('last_observed_at').notNull().default(0),background:integer('background').notNull().default(0),timezone:text('timezone').notNull().default('Asia/Seoul')
},t=>[uniqueIndex('health_device_token_hash').on(t.tokenHash)]);
