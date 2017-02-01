DROP SCHEMA IF EXISTS SensorAppDB;
CREATE SCHEMA IF NOT EXISTS `SensorAppDB` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `SensorAppDB` ;

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `SensorAppDB`.`Users` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `userid` VARCHAR(100)  NOT NULL,
  `mail` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`));
  
CREATE  TABLE IF NOT EXISTS `SensorAppDB`.`Commands` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100)  NOT NULL,
    `typeinfo` TEXT  NOT NULL,
  `description` TEXT NOT NULL,
    `numargs` INT NOT NULL,
    `isquery` BOOL NOT NULL,
PRIMARY KEY (`id`));

CREATE  TABLE IF NOT EXISTS `SensorAppDB`.`TreeIndexes` (
  `id` INT  NOT NULL AUTO_INCREMENT,
  `table` VARCHAR(100)  NOT NULL,
  `coldet` VARCHAR(100)   NOT NULL,
  `colope` VARCHAR(100)  NOT NULL,
PRIMARY KEY (`id`));

-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `SensorAppDB`.`Sensor` (
  `UID` INT  NOT NULL,
  `nameID_DET` TEXT NOT NULL,
  `name_RND` TEXT NOT NULL,
  `belongsTo_RND` TEXT NOT NULL,
  `vendor_RND` TEXT  NOT NULL,
  `description_RND` TEXT NOT NULL);

CREATE  TABLE IF NOT EXISTS `SensorAppDB`.`Measurement` (
  `UID` INT  NOT NULL,
  `id_PLAIN` INT AUTO_INCREMENT NOT NULL,
  `datatype_DET` TEXT NOT NULL,
  `sensorID_DET` TEXT NOT NULL,
  `timeStamp_RND` TEXT  NOT NULL,
  `data_DET` BIGINT(8) UNSIGNED NOT NULL,
  `data_HOM` BINARY(100) NOT NULL,
  `data_OPE` BIGINT(8) UNSIGNED NOT NULL,
  PRIMARY KEY (`id_PLAIN`));


DROP procedure IF EXISTS `storeMeasurement`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE storeMeasurement(IN userid INT, 
              IN datatype_DET TEXT,
              IN sensorID_DET TEXT,
              IN timeStamp_RND TEXT,
              IN data_DET BIGINT(8) UNSIGNED,
              IN data_HOM BINARY(100),
              IN data_OPE BIGINT(8) UNSIGNED)
BEGIN
  INSERT INTO Measurement VALUES(userid, 0, datatype_DET, sensorID_DET, timeStamp_RND, data_DET, data_HOM, data_OPE);
END
$$
DELIMITER ;

DROP procedure IF EXISTS `sensorExistsInDB`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE sensorExistsInDB(IN userid INT, 
              IN nameID_DET TEXT)
BEGIN
  SELECT Sensor.nameID_DET FROM Sensor WHERE Sensor.nameID_DET=nameID_DET AND Sensor.UID=userid;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `storeSensorInDB`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE storeSensorInDB(IN userid INT, 
              IN nameID_DET TEXT ,
              IN name_RND TEXT ,
              IN belongsTo_RND TEXT ,
              IN vendor_RND TEXT  ,
              IN description_RND TEXT)
BEGIN
  INSERT INTO Sensor VALUES (userid, nameID_DET, name_RND, belongsTo_RND, vendor_RND, description_RND);
END
$$
DELIMITER ;

DROP procedure IF EXISTS `loadSensors`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE loadSensors(IN userid INT)
BEGIN
  SELECT * FROM Sensor WHERE Sensor.UID=userid;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `loadValueTypesAndAverage`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE loadValueTypesAndAverage(IN userid INT,
                            IN nameID_DET TEXT)
BEGIN
  SELECT Measurement.datatype_DET, CRT_GAMAL_SUM(Measurement.data_HOM), COUNT(Measurement.data_HOM)
  FROM Sensor JOIN Measurement ON Measurement.sensorID_DET = Sensor.nameID_DET 
  WHERE Sensor.nameID_DET = nameID_DET AND Sensor.UID=userid AND Measurement.UID =userid GROUP BY Measurement.datatype_DET;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `loadMeasurementsWithTag`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE loadMeasurementsWithTag(IN userid INT,
                            IN nameID_DET TEXT,
                            IN datatype_DET TEXT,
                            IN input INT)
BEGIN
  SELECT Measurement.id_PLAIN, Measurement.datatype_DET, Measurement.sensorID_DET, Measurement.timeStamp_RND, Measurement.data_DET
  FROM Sensor JOIN Measurement ON Measurement.sensorID_DET = Sensor.nameID_DET
  WHERE Sensor.nameID_DET = nameID_DET AND Measurement.datatype_DET = datatype_DET AND Sensor.UID=userid AND Measurement.UID =userid ORDER BY Measurement.id_PLAIN DESC LIMIT input;
END
$$
DELIMITER ;

DROP procedure IF EXISTS `loadMetaOfMeasurementsWithTag`;
DELIMITER $$
USE `SensorAppDB`$$
CREATE PROCEDURE loadMetaOfMeasurementsWithTag(IN userid INT,
                            IN nameID_DET TEXT,
                            IN datatype_DET TEXT)
BEGIN
  SELECT COUNT(Measurement.id_PLAIN), mOPE_MIN(Measurement.data_DET, Measurement.data_OPE), mOPE_MAX(Measurement.data_DET, Measurement.data_OPE)
  FROM Sensor JOIN Measurement ON Measurement.sensorID_DET = Sensor.nameID_DET 
  WHERE Sensor.nameID_DET = nameID_DET AND Measurement.datatype_DET = datatype_DET AND Sensor.UID=userid AND Measurement.UID =userid;
END
$$
DELIMITER ;

INSERT INTO Commands VALUES(0, 'storeMeasurement','u,s,s,s,i,b,i','storeMeasurement(?,?,?,?,?,?,?)',7,FALSE);
INSERT INTO Commands VALUES(0, 'sensorExistsInDB','u,s','sensorExistsInDB(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'storeSensorInDB','u,s,s,s,s,s','storeSensorInDB(?,?,?,?,?,?)',6,FALSE);
INSERT INTO Commands VALUES(0, 'loadSensors','u','loadSensors(?)',1,TRUE);
INSERT INTO Commands VALUES(0, 'loadValueTypesAndAverage','u,s','loadValueTypesAndAverage(?,?)',2,TRUE);
INSERT INTO Commands VALUES(0, 'loadMeasurementsWithTag','u,s,s,i','loadMeasurementsWithTag(?,?,?,?)',4,TRUE);
INSERT INTO Commands VALUES(0, 'loadMetaOfMeasurementsWithTag','u,s,s','loadMetaOfMeasurementsWithTag(?,?,?)',3,TRUE);

INSERT INTO TreeIndexes VALUES(1, 'Measurement', 'data_DET', 'data_OPE');