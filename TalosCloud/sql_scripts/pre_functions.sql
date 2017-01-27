
CREATE  TABLE IF NOT EXISTS `<DBNAME>`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(200)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  `pk` BLOB NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `<DBNAME>`.`Share` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `fromid` INT  NOT NULL,
  `toid` INT NOT NULL,
  `combined` INT NOT NULL,
  `replicated` INT NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `<DBNAME>`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `<DBNAME>`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
  `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
  `numargs` INT NOT NULL,
  `isquery` BOOL NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `<DBNAME>`.`TabmOPE` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `UID` INT  NOT NULL,
  `val1` VARCHAR(100) NOT NULL,
  `ope1` BIGINT(8) UNSIGNED NOT NULL,
  `val2` VARCHAR(100) NOT NULL,
  `ope2` BIGINT(8) UNSIGNED NOT NULL,
  PRIMARY KEY (`id`));


CREATE TABLE `pre_db`.`testsum` (
  `id` INT NOT NULL,
  `uid` INT NULL,
  `add` BLOB NULL,
  PRIMARY KEY (`id`));

DROP procedure IF EXISTS `reEncrypt`;
DELIMITER $$
USE `<DBNAME>`$$
CREATE PROCEDURE reEncrypt(in useridold INT, in useridnew INT, in token BLOB)
  BEGIN
    CREATE TEMPORARY TABLE temp_table AS SELECT * FROM testsum WHERE uid=useridold;
    UPDATE temp_table SET temp_table.id=0, temp_table.uid=useridnew, temp_table.add=PRE_REL_REENC(temp_table.add, token);
    INSERT INTO testsum SELECT * FROM temp_table;
    DROP TABLE temp_table;
  END
$$
DELIMITER ;