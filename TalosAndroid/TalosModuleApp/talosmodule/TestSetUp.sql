DROP SCHEMA IF EXISTS TalosTestDB;
CREATE SCHEMA IF NOT EXISTS `TalosTestDB` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `TalosTestDB` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `TalosTestDB`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(100)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`));
  
CREATE  TABLE IF NOT EXISTS `TalosTestDB`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `TalosTestDB`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
PRIMARY KEY (`id`));

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `TalosTestDB`.`TestTable` (
  `UID` INT  NOT NULL,
  `ColumnA_RND` decimal(39,0) NOT NULL,
  `ColumnA_HOM` VARCHAR(101) NOT NULL,
  `ColumnB_DET` BIGINT(8) UNSIGNED  NOT NULL,
  `ColumnC_RND` TEXT  NOT NULL);

CREATE  TABLE IF NOT EXISTS `TalosTestDB`.`TestOPE` (
  `UID` INT  NOT NULL,
  `VAL_DET` BIGINT(8) UNSIGNED NOT NULL,
  `VAL_OPE` BIGINT(8) UNSIGNED  NOT NULL,
  INDEX `opeINDEX` (`VAL_OPE`));


DROP procedure IF EXISTS `insertTest`;
DELIMITER $$
USE `TalosTestDB`$$
CREATE PROCEDURE insertTest(IN userid INT, 
              IN aRND decimal(39,0),
              IN aHOM VARCHAR(101),
              IN bDET BIGINT(8) UNSIGNED,
              IN cRND TEXT)
BEGIN
  INSERT INTO TestTable VALUES(userid, aRND, aHOM, bDET, cRND);
END
$$
DELIMITER ;

DROP procedure IF EXISTS `insertOPE`;
DELIMITER $$
USE `TalosTestDB`$$
CREATE PROCEDURE insertOPE(IN userid INT, 
              IN VAL_DET BIGINT(8) UNSIGNED,
              IN VAL_OPE BIGINT(8) UNSIGNED)
BEGIN
  INSERT INTO TestOPE VALUES(userid, VAL_DET, VAL_OPE);
END
$$
DELIMITER ;

DROP procedure IF EXISTS `queryOPE`;
DELIMITER $$
USE `TalosTestDB`$$
CREATE PROCEDURE queryOPE(IN userid INT, 
              IN VAL_OPE_FROM BIGINT(8) UNSIGNED,
              IN VAL_OPE_TO BIGINT(8) UNSIGNED)
BEGIN
  SELECT * FROM TestOPE WHERE TestOPE.VAL_OPE >= VAL_OPE_FROM AND TestOPE.VAL_OPE <= VAL_OPE_TO AND UID = userid;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `searchTest`;
DELIMITER $$
USE `TalosTestDB`$$
CREATE PROCEDURE searchTest(IN userid INT, 
              IN bDET BIGINT(8) UNSIGNED)
BEGIN
  SELECT * FROM TestTable WHERE TestTable.UID = userid AND TestTable.ColumnB_DET = bDET;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `computeSum`;
DELIMITER $$
USE `TalosTestDB`$$
CREATE PROCEDURE computeSum(IN userid INT)
BEGIN
  SELECT ECElGamal_Agr(TestTable.ColumnA_HOM) FROM TestTable WHERE TestTable.UID = userid;
END
$$
DELIMITER ;

INSERT INTO Commands VALUES(0, 'insertTest','u,i,s,i,s','insertTest(?,?,?,?,?)',5,FALSE);
INSERT INTO Commands VALUES(0, 'searchTest','u,i','searchTest(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'computeSum','u','computeSum(?)',1,TRUE);
INSERT INTO Commands VALUES(0, 'insertOPE','u,i,i','insertOPE(?, ?, ?)',3,FALSE);
INSERT INTO Commands VALUES(0, 'queryOPE','u,i,i','queryOPE(?, ?, ?)',3,TRUE);

INSERT INTO TreeIndexes VALUES(1, 'TestOPE', 'VAL_DET', 'VAL_OPE');