DROP DATABASE IF EXISTS `receipty`;
CREATE DATABASE `receipty` DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_unicode_ci;
USE `receipty`;

DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `item`;
DROP TABLE IF EXISTS `sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `joined` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `phone` varchar(15) NOT NULL,
  `password` varchar(40) NOT NULL,
  `province` int(5) NOT NULL,
  `county` int(5) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

/*
CREATE TABLE user (uid int(11) NOT NULL AUTO_INCREMENT,joined timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,phone varchar(15) NOT NULL,password varchar(64) NOT NULL,province int(5) NOT NULL,county int(5) NOT NULL,PRIMARY KEY (uid),UNIQUE KEY (uid));

*/
CREATE TABLE `item` (
  `iid` int(11) NOT NULL AUTO_INCREMENT,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `description` varchar(25) NOT NULL,
  `owner` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY `owner` REFERENCES user(`owner`),
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

/*
CREATE TABLE item (   iid int(11) NOT NULL AUTO_INCREMENT,   added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,   description varchar(25) NOT NULL,   owner int(11) NOT NULL,   PRIMARY KEY (iid),   FOREIGN KEY (owner) REFERENCES user(uid) );
*/
CREATE TABLE `sale` (
  `sid` int(11) NOT NULL AUTO_INCREMENT,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uid` int(11) NOT NULL,
  `iid` int(11) NOT NULL,
  `quantity` int(5) NOT NULL,
  `total` int(10) NOT NULL,
  `item` varchar(25) NOT NULL,
  `customer_num` varchar(15) NOT NULL,
  PRIMARY KEY (`sid`),
  FOREIGN KEY `uid` REFERENCES user(`uid`),
  FOREIGN KEY `iid` REFERENCES item(`iid`),
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
