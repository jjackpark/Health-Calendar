CREATE TABLE `health_preferences` (
	`user_id` text PRIMARY KEY NOT NULL,
	`goal` integer DEFAULT 8000 NOT NULL
);
--> statement-breakpoint
CREATE TABLE `health_records` (
	`user_id` text NOT NULL,
	`date` text NOT NULL,
	`metric` text NOT NULL,
	`value` integer NOT NULL,
	`note` text DEFAULT '' NOT NULL,
	`kind` text DEFAULT '' NOT NULL,
	`abv` integer DEFAULT 0 NOT NULL,
	`source` text DEFAULT 'manual' NOT NULL,
	`updated_at` text NOT NULL,
	PRIMARY KEY(`user_id`, `date`, `metric`)
);
