DROP SCHEMA IF EXISTS Authtest;
CREATE SCHEMA IF NOT EXISTS `Authtest` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `Authtest` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `Authtest`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(100)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  `pk` BLOB NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `Authtest`.`Shared` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `fromid` INT  NOT NULL,
  `toid` INT NOT NULL,
  `shareuser` INT NOT NULL,
  PRIMARY KEY (`id`));
  
CREATE  TABLE IF NOT EXISTS `Authtest`.`Commands` (
	`id` INT  NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
	`description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));
  