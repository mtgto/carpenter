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
# --- !Downs
DROP TABLE `users`;
DROP TABLE `projects`;