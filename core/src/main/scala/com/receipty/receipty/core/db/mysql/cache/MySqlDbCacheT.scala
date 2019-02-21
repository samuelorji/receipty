package com.receipty.receipty.core
package db.mysql.cache

import akka.actor.{ ActorRef, Props }

import com.receipty._

import receipty.core.db.mysql.cache.mechanics.{MySqlDbCache, MySqlDbCacheEntry}
import receipty.core.db.mysql.service.MysqlDbService

trait MySqlDbCacheT[T <: MySqlDbCacheEntry] extends MySqlDbCache[T] {
  override def createMySqlDbService: ActorRef = context.actorOf(Props[MysqlDbService])
}
