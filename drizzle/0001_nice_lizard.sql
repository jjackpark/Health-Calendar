CREATE TABLE `health_devices` (
	`user_id` text PRIMARY KEY NOT NULL,
	`token_hash` text NOT NULL,
	`name` text NOT NULL,
	`created_at` text NOT NULL,
	`last_sync_at` text,
	`last_observed_at` integer DEFAULT 0 NOT NULL,
	`background` integer DEFAULT 0 NOT NULL,
	`timezone` text DEFAULT 'Asia/Seoul' NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `health_device_token_hash` ON `health_devices` (`token_hash`);--> statement-breakpoint
CREATE TABLE `health_pairing_codes` (
	`user_id` text PRIMARY KEY NOT NULL,
	`code_hash` text NOT NULL,
	`expires_at` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `health_pairing_code_hash` ON `health_pairing_codes` (`code_hash`);