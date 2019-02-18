package com.receipty.receipty.core
package db.mysql.service

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import akka.pattern.pipe

import com.receipty.receipty.core.db.mysql.cache.InnerWorkings.MySqlDbCacheEntry
import com.receipty.receipty.core.db.mysql.mapper.ReceiptyMapper
import com.receipty.receipty.core.db.mysql.service.MysqlDbService.{ItemDbEntry, ItemFetchDbQuery, UserDbEntry, UserFetchDbQuery}

object MysqlDbService{
  case class UserDbEntry (
    id: Int,
    phoneNumber: String,
    password: String,
    province : Int ,
    county :Int ,
    joined : String
  ) extends MySqlDbCacheEntry

  case class ItemDbEntry(
   id : Int ,
   description : String,
   owner  : Int,
   added : String
   )extends MySqlDbCacheEntry

  case object UserFetchDbQuery
  case object ItemFetchDbQuery
}
class MysqlDbService extends Actor {

  def receive = {

    case UserFetchDbQuery =>
      ReceiptyMapper.fetchAllUsers.mapTo[List[UserDbEntry]] pipeTo sender()

    case ItemFetchDbQuery =>
      ReceiptyMapper.fetchAllItems.mapTo[List[ItemDbEntry]] pipeTo sender()
  }
}
