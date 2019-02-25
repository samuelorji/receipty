package com.receipty.bantu.core.config

import com.typesafe.config.ConfigFactory

object ReceiptyConfig {

  val config = ConfigFactory.load

  //MySql
  val mysqlDbHost  = config.getString("receipty.db.mysql.host")
  val mysqlDbPort  = config.getInt("receipty.db.mysql.port")
  val mysqlDbUser  = config.getString("receipty.db.mysql.user")
  val mysqlDbPass  = config.getString("receipty.db.mysql.pass")
  val mysqlDbName  = config.getString("receipty.db.mysql.name")

  val mysqlDbPoolMaxObjects   = config.getInt("receipty.db.mysql.pool.max-objects")
  val mysqlDbPoolMaxIdle      = config.getInt("receipty.db.mysql.pool.max-idle")
  val mysqlDbPoolMaxQueueSize = config.getInt("receipty.db.mysql.pool.max-queue-size")

  //Server

  val host = config.getString("receipty.interface.web.host")
  val port = config.getInt("receipty.interface.web.port")

}
