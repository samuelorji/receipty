package com.receipty.receipty.core
package db.mysql

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.receipty.receipty.core.config.ReceiptyConfig

private[mysql] object ReceiptyMySqlDb{

  private val configuration = new Configuration(
    username = ReceiptyConfig.mysqlDbUser,
    host     = ReceiptyConfig.mysqlDbHost,
    port     = ReceiptyConfig.mysqlDbPort,
    password = Some(ReceiptyConfig.mysqlDbPass),
    database = Some(ReceiptyConfig.mysqlDbName)
  )

  private val poolConfiguration = new PoolConfiguration(
    maxObjects   = ReceiptyConfig.mysqlDbPoolMaxObjects,
    maxIdle      = ReceiptyConfig.mysqlDbPoolMaxIdle,
    maxQueueSize = ReceiptyConfig.mysqlDbPoolMaxQueueSize
  )

  private val factory   = new MySQLConnectionFactory(configuration)
  private lazy val pool = new ConnectionPool(factory, poolConfiguration)
}

private [mysql] trait ReceiptyMySqlDb {
  implicit lazy val pool = ReceiptyMySqlDb.pool
}