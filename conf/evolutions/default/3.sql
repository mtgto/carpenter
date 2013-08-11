# --- !Ups
ALTER TABLE `jobs` ADD FOREIGN KEY (`project_id`) REFERENCES `projects`(`id`);
ALTER TABLE `jobs` ADD FOREIGN KEY (`user_id`) REFERENCES `users`(`id`);
# --- !Downs
ALTER TABLE `jobs` DROP FOREIGN KEY (`project_id`);
ALTER TABLE `jobs` DROP FOREIGN KEY (`user_id`);
