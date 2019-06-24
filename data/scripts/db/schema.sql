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
CREATE TABLE user (
uid int(11) NOT NULL AUTO_INCREMENT,
joined timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
phone varchar(15) NOT NULL,
password varchar(64) NOT NULL,
natureOfBusiness int(1) NOT NULL,
businessName varchar(20) NOT NULL,
PRIMARY KEY (uid),
UNIQUE KEY (uid)
);

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
CREATE TABLE item (
iid int(11) NOT NULL AUTO_INCREMENT,
added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
description varchar(20) NOT NULL,
alias varchar(10) NOT NULL,
owner int(11) NOT NULL,
PRIMARY KEY (iid),
FOREIGN KEY (owner) REFERENCES user(uid)
 );
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


//Sale Table 

/*
CREATE TABLE sale (
sid int(11) NOT NULL AUTO_INCREMENT,
date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
cust_num varchar(15) NOT NULL,
total double(11,2) NOT NULL,
uid int(11) NOT NULL,
PRIMARY KEY (sid),
FOREIGN KEY (uid) REFERENCES user(uid)
);
*/



//Order Table

CREATE TABLE `order` (
oid int(11) NOT NULL AUTO_INCREMENT,
sid int(11) NOT NULL,
iid int(11) NOT NULL,
PRIMARY KEY (oid),
FOREIGN KEY (sid) REFERENCES sale(sid),
FOREIGN KEY (iid) REFERENCES item(iid) ON DELETE CASCADE
);

/*
query to fetch all sales for each user in each day ="select cust_num,total,uid,sid from sale where date > (DATE_ADD(NOW(),INTERVAL -1 DAY)) AND uid in (select uid from user);"
*/

/*
query for sales made by each user and item description in the process
"select t1.sid,t1.iid,t1.cust_num,t1.total,t1.uid,item.description,item.owner from (select sale.date,sale.sid,sale.cust_num,sale.total,sale.uid,`order`.iid FROM sale INNER JOIN `order` ON sale.sid=`order`.sid where date > (DATE_ADD(NOW(),INTERVAL -1 DAY))) t1
 INNER JOIN item
 ON t1.iid=item.iid;"
*/