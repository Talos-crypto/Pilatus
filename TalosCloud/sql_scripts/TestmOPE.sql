# noinspection SqlNoDataSourceInspectionForFile
DROP SCHEMA IF EXISTS OPETest;
CREATE SCHEMA IF NOT EXISTS `OPETest` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `OPETest` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `OPETest`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(100)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`));
  
CREATE  TABLE IF NOT EXISTS `OPETest`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `OPETest`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `OPETest`.`TabmOPE` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `UID` INT  NOT NULL,
  `val1` VARCHAR(100) NOT NULL,
  `ope1` BIGINT(8) UNSIGNED NOT NULL,
  `val2` VARCHAR(100) NOT NULL,
  `ope2` BIGINT(8) UNSIGNED NOT NULL,
PRIMARY KEY (`id`));




DROP procedure IF EXISTS `storemOPE`;
DELIMITER $$
USE `OPETest`$$
CREATE PROCEDURE storemOPE(IN userid INT, 
              IN val1  VARCHAR(100), 
              IN ope1  BIGINT(8) UNSIGNED,
              IN val2  VARCHAR(100) ,
              IN ope2  BIGINT(8) UNSIGNED)
BEGIN
  INSERT INTO TabmOPE VALUES (0, userid, val1, ope1, val2, ope2);
END
$$
DELIMITER ;


DROP procedure IF EXISTS `rangemOPE`;
DELIMITER $$
USE `OPETest`$$
CREATE PROCEDURE rangemOPE(IN userid INT,
                            IN opeFrom BIGINT(8) UNSIGNED,
                            IN opeTo BIGINT(8) UNSIGNED)
BEGIN
  SELECT TabmOPE.val1, TabmOPE.ope1 FROM TabmOPE WHERE TabmOPE.ope1>=opeFrom AND TabmOPE.ope1<=opeTo AND TabmOPE.UID=userid ORDER BY TabmOPE.id ASC;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `deleteOPE`;
DELIMITER $$
USE `OPETest`$$
CREATE PROCEDURE deleteOPE(IN userid INT,
                            IN val1I  VARCHAR(100),
                            IN ope1I BIGINT(8) UNSIGNED,
                            IN val2I  VARCHAR(100),
                            IN ope2I BIGINT(8) UNSIGNED)
BEGIN
  DELETE FROM TabmOPE WHERE TabmOPE.ope2 = ope2I AND TabmOPE.val1 = val1I AND TabmOPE.val2 = val2I AND TabmOPE.ope1 = ope1I AND TabmOPE.UID=userid;
END
$$
DELIMITER ;


INSERT INTO Commands VALUES(0, 'storemOPE','u,s,i,s,i','storemOPE(?,?,?,?,?)',5,FALSE);
INSERT INTO Commands VALUES(0, 'deleteOPE','u,s,i,s,i','deleteOPE(?,?,?,?,?)',5,FALSE);
INSERT INTO Commands VALUES(0, 'rangemOPE','u,i,i','rangemOPE(?,?,?)',3,TRUE);

INSERT INTO TreeIndexes VALUES(1, 'TabmOPE', 'val1', 'ope1');
INSERT INTO TreeIndexes VALUES(2, 'TabmOPE', 'val2', 'ope2');