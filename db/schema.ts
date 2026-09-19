// Intentionally empty by default.
// Add Drizzle tables here when the site actually needs a database.
// See examples/d1/db/schema.ts for an opt-in example.
import { sqliteTable,text,integer,primaryKey } from 'drizzle-orm/sqlite-core';
export const records=sqliteTable('health_records',{userId:text('user_id').notNull(),date:text('date').notNull(),metric:text('metric').notNull(),value:integer('value').notNull(),note:text('note').notNull().default(''),kind:text('kind').notNull().default(''),abv:integer('abv').notNull().default(0),source:text('source').notNull().default('manual'),updatedAt:text('updated_at').notNull()},t=>[primaryKey({columns:[t.userId,t.date,t.metric]})]);
export const preferences=sqliteTable('health_preferences',{userId:text('user_id').primaryKey(),goal:integer('goal').notNull().default(8000)});
