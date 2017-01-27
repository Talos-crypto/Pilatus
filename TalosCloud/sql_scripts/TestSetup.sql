DROP procedure IF EXISTS `demoSp`;
DELIMITER $$
USE `Authtest`$$
CREATE PROCEDURE demoSp(IN userid INT)
BEGIN
  SELECT * FROM Users WHERE Users.id = userid;
END
$$
DELIMITER ;

INSERT INTO Users VALUES(0,'lubu','lubu@gmail.com');
INSERT INTO Commands VALUES(0, 'demoSp','int','demoSp(?)',1,TRUE);