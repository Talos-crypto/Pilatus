# noinspection SqlNoDataSourceInspectionForFile
DROP SCHEMA IF EXISTS AvaDataset;
CREATE SCHEMA IF NOT EXISTS `AvaDataset` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `AvaDataset` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `AvaDataset`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(200)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  `pk` BLOB NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `AvaDataset`.`Share` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `fromid` INT  NOT NULL,
  `toid` INT NOT NULL,
  `combined` INT NOT NULL,
  `replicated` INT NOT NULL,
  PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `AvaDataset`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `AvaDataset`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `AvaDataset`.`Dataset` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `UID` INT  NOT NULL,
  `date` DATE  NOT NULL,
  `time` TIME  NOT NULL,
  `setid` INT  NOT NULL,
  `datatype_DET` VARCHAR(200)  NOT NULL,
  `data_HOM` BLOB  NOT NULL,
INDEX `dataset_date_idx` (`date`),
PRIMARY KEY (`id`));


DROP procedure IF EXISTS `reEncrypt`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE reEncrypt(in useridold INT, in useridnew INT, in token BLOB)
  BEGIN
    CREATE TEMPORARY TABLE temp_table AS SELECT * FROM Dataset WHERE uid=useridold;
    UPDATE temp_table SET temp_table.id=0, temp_table.UID=useridnew, temp_table.data_HOM=PRE_REL_REENC(temp_table.data_HOM, token);
    INSERT INTO Dataset SELECT * FROM temp_table;
    DROP TABLE temp_table;
  END
$$
DELIMITER ;


DROP procedure IF EXISTS `insertDataset`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE insertDataset(IN userid INT,
              IN date  DATE,
              IN time  Time,
              IN setid  INT,
              IN datatype_DET VARCHAR(200),
              IN data_HOM BLOB)
BEGIN
  IF ( SELECT NOT EXISTS (SELECT 1 FROM Dataset WHERE Dataset.UID=userid AND Dataset.date=date AND Dataset.time=time AND Dataset.datatype_DET=datatype_DET) ) THEN
    INSERT INTO Dataset VALUES (0, userid, date, time, setid, datatype_DET, data_HOM);
  ELSE
    UPDATE Dataset SET Dataset.data_HOM=data_HOM WHERE Dataset.UID=userid AND Dataset.date=date AND Dataset.time=time AND Dataset.datatype_DET=datatype_DET;
  END IF;
END
$$
DELIMITER ;


DROP procedure IF EXISTS `getValuesByDay`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE getValuesByDay(IN userid INT,
                          IN fromDATE DATE,
                          IN toDATE DATE,
                          IN datatype VARCHAR(200))
BEGIN
  SELECT Dataset.date, PRE_REL_SUM(data_HOM), COUNT(data_HOM) FROM Dataset WHERE Dataset.UID=userid AND setid=0 AND Dataset.datatype_DET=datatype AND Dataset.date>=fromDATE AND Dataset.date<=toDATE GROUP BY Dataset.date;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getValuesForSet`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE getValuesForSet(IN userid INT, IN setinid INT)
BEGIN
  SELECT Dataset.date, Dataset.time, Dataset.datatype_DET, Dataset.data_HOM FROM Dataset WHERE Dataset.UID=userid AND Dataset.setid=setinid;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getValuesDuringDay`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE getValuesDuringDay(IN userid INT,
                            IN curDate DATE)
BEGIN
  SELECT COUNT(data_HOM), PRE_REL_SUM(data_HOM), Dataset.datatype_DET FROM Dataset WHERE Dataset.UID=userid AND setid=0 AND Dataset.date=curDate GROUP BY Dataset.datatype_DET;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `agrDailySummary`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE agrDailySummary(IN userid INT,
                            IN curDate DATE,
                            IN datatype VARCHAR(200),
                            IN granularity INT)
BEGIN
	SELECT from_unixtime(ROUND(UNIX_TIMESTAMP(CONCAT(Dataset.date,' ',Dataset.time))/(granularity * 60))*(granularity * 60),'%H:%i:%s') AS agrTime, PRE_REL_SUM(data_HOM), COUNT(data_HOM)
	FROM Dataset
    WHERE Dataset.UID=userid AND setid=0 AND Dataset.datatype_DET=datatype AND Dataset.date=curDate
	GROUP BY agrTime ORDER BY agrTime;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getMostActualDate`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE getMostActualDate(IN userid INT)
BEGIN
	SELECT Dataset.date FROM Dataset WHERE Dataset.UID=userid AND setid=0 GROUP BY Dataset.date ORDER BY Dataset.date;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getSets`;
DELIMITER $$
USE `AvaDataset`$$
CREATE PROCEDURE getSets(IN userid INT)
BEGIN
	SELECT Dataset.setid FROM Dataset WHERE Dataset.UID=userid GROUP BY Dataset.setid;
END
$$
DELIMITER ;




INSERT INTO Commands VALUES(0, 'insertDataset','u,s,s,i,s,pre','insertDataset(?,?,?,?,?,?)',6,FALSE);
INSERT INTO Commands VALUES(0, 'getValuesByDay','u,s,s,s', 'getValuesByDay(?,?,?,?)',4,TRUE);
INSERT INTO Commands VALUES(0, 'agrDailySummary','u,s,s,i', 'agrDailySummary(?,?,?,?)',4,TRUE);
INSERT INTO Commands VALUES(0, 'getValuesDuringDay','u,s','getValuesDuringDay(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'getMostActualDate','u','getMostActualDate(?)',1,TRUE);

INSERT INTO Commands VALUES(0, 'getValuesForSet','u,i','getValuesForSet(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'getSets','u','getSets(?)',1,TRUE);