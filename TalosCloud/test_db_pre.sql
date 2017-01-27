# noinspection SqlNoDataSourceInspectionForFile
DROP SCHEMA IF EXISTS pre_db;
CREATE SCHEMA IF NOT EXISTS `pre_db` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `pre_db` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `pre_db`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(200)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  `pk` BLOB NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `pre_db`.`Share` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `fromid` INT  NOT NULL,
  `toid` INT NOT NULL,
  `combined` INT NOT NULL,
  `replicated` INT NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `pre_db`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `pre_db`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
  `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
  `numargs` INT NOT NULL,
  `isquery` BOOL NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `pre_db`.`Dataset` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `UID` INT  NOT NULL,
  `data_HOM` BLOB NOT NULL,
  PRIMARY KEY (`id`));


DROP procedure IF EXISTS `reEncrypt`;
DELIMITER $$
USE `pre_db`$$
CREATE PROCEDURE reEncrypt(in useridold INT, in useridnew INT, in token BLOB)
  BEGIN
    CREATE TEMPORARY TABLE temp_table AS SELECT * FROM Dataset WHERE UID=useridold;
    UPDATE temp_table SET temp_table.id=0, temp_table.UID=useridnew, temp_table.data_HOM=PRE_REL_REENC(temp_table.data_HOM, token);
    INSERT INTO Dataset SELECT * FROM temp_table;
    DROP TABLE temp_table;
  END
$$
DELIMITER ;


DROP procedure IF EXISTS `insertDataset`;
DELIMITER $$
USE `pre_db`$$
CREATE PROCEDURE insertDataset(IN userid INT,
                               IN data_HOM BLOB)
  BEGIN
    INSERT INTO Dataset VALUES (0, userid, data_HOM);
  END
$$
DELIMITER ;


DROP procedure IF EXISTS `getSUM`;
DELIMITER $$
USE `pre_db`$$
CREATE PROCEDURE getSUM(IN userid INT)
  BEGIN
    SELECT PRE_REL_SUM(data_HOM) FROM Dataset WHERE Dataset.UID=userid;
  END
$$
DELIMITER ;


INSERT INTO Commands VALUES(0, 'insertDataset','u,pre','insertDataset(?,?)',2,FALSE);
INSERT INTO Commands VALUES(0, 'getSUM','u', 'getSUM(?)',1,TRUE);