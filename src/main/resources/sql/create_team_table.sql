CREATE TABLE IF NOT EXISTS `lux_teams`(
  `name` VARCHAR(32) UNIQUE NOT NULL,
  `created_at` INTEGER NOT NULL,
  `friendly_fire` BOOLEAN NOT NULL,
  `home_world` VARCHAR(32) NULL,
  `home_position` INTEGER NULL,
  `home_yaw` FLOAT NULL,
  `home_pitch` FLOAT NULL
);
