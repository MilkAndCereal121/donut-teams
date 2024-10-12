CREATE TABLE IF NOT EXISTS `lux_teams_members`(
  `unique_id` CHAR(36) NOT NULL,
  `name` VARCHAR(16) NOT NULL,
  `team_name` VARCHAR(32) NOT NULL,
  `joined_at` INTEGER NOT NULL,
  `permissions` INTEGER NOT NULL,
  PRIMARY KEY (`unique_id`, `team_name`)
);
