package com.receipty.bantu.core.db.mysql.cache

import akka.actor.{ActorRef, Props}

import com.receipty._
import com.receipty.bantu.core.db.mysql.cache.mechanics.{MySqlDbCache, MySqlDbCacheEntry}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService
import bantu.core.db.mysql.cache.mechanics.{MySqlDbCache, MySqlDbCacheEntry}

trait MySqlDbCacheT[T <: MySqlDbCacheEntry] extends MySqlDbCache[T] {
  override def createMySqlDbService: ActorRef = context.actorOf(Props[MysqlDbService])
}
