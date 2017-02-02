# noinspection SqlNoDataSourceInspectionForFile
DROP SCHEMA IF EXISTS TalosFitbit;
CREATE SCHEMA IF NOT EXISTS `TalosFitbit` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `TalosFitbit` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `TalosFitbit`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(100)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`));
  
CREATE  TABLE IF NOT EXISTS `TalosFitbit`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `TalosFitbit`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `TalosFitbit`.`Dataset` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `UID` INT  NOT NULL,
  `date` DATE  NOT NULL,
  `time` TIME  NOT NULL,
  `datatype_DET` VARCHAR(200)  NOT NULL,
  `data_RND` DECIMAL(39,0)  NOT NULL,
  `data_HOM` BINARY(100)  NOT NULL,
INDEX `dataset_date_idx` (`date`),
PRIMARY KEY (`id`));




DROP procedure IF EXISTS `insertDataset`;
DELIMITER $$
USE `TalosFitbit`$$
CREATE PROCEDURE insertDataset(IN userid INT, 
              IN date  DATE, 
              IN time  Time,
              IN datatype_DET  VARCHAR(200),
              IN data_RND  DECIMAL(39,0),
              IN data_HOM  BINARY(100))
BEGIN
  IF ( SELECT NOT EXISTS (SELECT 1 FROM Dataset WHERE Dataset.UID=userid AND Dataset.date=date AND Dataset.time=time AND Dataset.datatype_DET=datatype_DET) ) THEN 
    INSERT INTO Dataset VALUES (0, userid, date, time, datatype_DET, data_RND, data_HOM);
  ELSE 
    UPDATE Dataset SET Dataset.data_RND=data_RND, Dataset.data_HOM=data_HOM WHERE Dataset.UID=userid AND Dataset.date=date AND Dataset.time=time AND Dataset.datatype_DET=datatype_DET; 
  END IF;
END
$$
DELIMITER ;


DROP procedure IF EXISTS `getValuesByDay`;
DELIMITER $$
USE `TalosFitbit`$$
CREATE PROCEDURE getValuesByDay(IN userid INT,
                          IN fromDATE DATE,
                          IN toDATE DATE,
                          IN datatype VARCHAR(200))
BEGIN
  SELECT Dataset.date, CRT_GAMAL_SUM(data_HOM), COUNT(data_HOM) FROM Dataset WHERE Dataset.UID=userid AND Dataset.datatype_DET=datatype AND Dataset.date>=fromDATE AND Dataset.date<=toDATE GROUP BY Dataset.date;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getValuesDuringDay`;
DELIMITER $$
USE `TalosFitbit`$$
CREATE PROCEDURE getValuesDuringDay(IN userid INT,
                            IN curDate DATE)
BEGIN
  SELECT COUNT(data_HOM), CRT_GAMAL_SUM(data_HOM), Dataset.datatype_DET FROM Dataset WHERE Dataset.UID=userid AND Dataset.date=curDate GROUP BY Dataset.datatype_DET;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `agrDailySummary`;
DELIMITER $$
USE `TalosFitbit`$$
CREATE PROCEDURE agrDailySummary(IN userid INT,
                            IN curDate DATE,
                            IN datatype VARCHAR(200),
                            IN granularity INT)
BEGIN
	SELECT from_unixtime(ROUND(UNIX_TIMESTAMP(CONCAT(Dataset.date,' ',Dataset.time))/(granularity * 60))*(granularity * 60),'%H:%i:%s') AS agrTime, CRT_GAMAL_SUM(data_HOM), COUNT(data_HOM)
	FROM Dataset
    WHERE Dataset.UID=userid AND Dataset.datatype_DET=datatype AND Dataset.date=curDate
	GROUP BY agrTime ORDER BY agrTime;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `getMostActualDate`;
DELIMITER $$
USE `TalosFitbit`$$
CREATE PROCEDURE getMostActualDate(IN userid INT)
BEGIN
	SELECT MAX(Dataset.date) FROM Dataset WHERE Dataset.UID=userid;
END
$$
DELIMITER ;




INSERT INTO Commands VALUES(0, 'insertDataset','u,s,s,s,i,b','insertDataset(?,?,?,?,?,?)',6,FALSE);
INSERT INTO Commands VALUES(0, 'getValuesByDay','u,s,s,s', 'getValuesByDay(?,?,?,?)',4,TRUE);
INSERT INTO Commands VALUES(0, 'agrDailySummary','u,s,s,i', 'agrDailySummary(?,?,?,?)',4,TRUE);
INSERT INTO Commands VALUES(0, 'getValuesDuringDay','u,s','getValuesDuringDay(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'getMostActualDate','u','getMostActualDate(?)',1,TRUE);
