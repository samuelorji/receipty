package com.receipty.bantu.core
package config

import com.typesafe.config.ConfigFactory

object ReceiptyConfig {

  val config = ConfigFactory.load

  //MySql
  val mysqlDbHost  = config.getString("bantu.db.mysql.host")
  val mysqlDbPort  = config.getInt("bantu.db.mysql.port")
  val mysqlDbUser  = config.getString("bantu.db.mysql.user")
  val mysqlDbPass  = config.getString("bantu.db.mysql.pass")
  val mysqlDbName  = config.getString("bantu.db.mysql.name")

  val mysqlDbPoolMaxObjects   = config.getInt("bantu.db.mysql.pool.max-objects")
  val mysqlDbPoolMaxIdle      = config.getInt("bantu.db.mysql.pool.max-idle")
  val mysqlDbPoolMaxQueueSize = config.getInt("bantu.db.mysql.pool.max-queue-size")

  //Server

  val host = config.getString("bantu.interface.web.host")
  val port = config.getInt("bantu.interface.web.port")

}
