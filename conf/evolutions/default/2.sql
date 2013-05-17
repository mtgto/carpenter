# --- !Ups
CREATE TABLE `subversion_paths` (
  `project_id` CHAR(36) NOT NULL,
  `path` VARCHAR(255) NOT NULL,
  `directory` BOOLEAN NOT NULL,
  UNIQUE (`project_id`, `path`),
  FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`)
);
# --- !Downs
DROP TABLE `subversion_paths`;
