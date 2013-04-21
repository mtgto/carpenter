# --- !Ups
CREATE TABLE `users` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `password` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `projects` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `hostname` VARCHAR(255) NOT NULL,
  `recipe` TEXT NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `source_repositories` (
  `project_id` CHAR(36) NOT NULL UNIQUE,
  `software` VARCHAR(63) NOT NULL,
  `url` VARCHAR(1023) NOT NULL,
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`)
);
CREATE TABLE `jobs` (
  `id` CHAR(36) NOT NULL,
  `project_id` CHAR(36) NOT NULL,
  `user_id` CHAR(36) NOT NULL,
  `task` VARCHAR(255) NOT NULL,
  `exit_code` INT NOT NULL,
  `log` TEXT NOT NULL,
  `execute_time` DATETIME NOT NULL,
  `execute_duration` INT NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `snapshots` (
  `job_id` CHAR(36) NOT NULL UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `revision` VARCHAR(63) NOT NULL,
  `branch_type` VARCHAR(63) NOT NULL,
  FOREIGN KEY (`job_id`) REFERENCES `jobs`(`id`)
);
CREATE TABLE `authorities` (
  `user_id` CHAR(36) NOT NULL,
  `can_login` TINYINT NOT NULL DEFAULT '1',
  `can_create_user` TINYINT NOT NULL DEFAULT '0',
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
);
# --- !Downs
DROP TABLE `users`;
DROP TABLE `projects`;
DROP TABLE `source_repositories`;
DROP TABLE `jobs`;
DROP TABLE `snapshots`;
DROP TABLE `authorities`;
