package com.receipty.bantu.core.db.mysql.service

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import akka.pattern.pipe

import com.receipty._
import com.receipty.bantu.core.db.mysql.cache.mechanics.MySqlDbCacheEntry
import com.receipty.bantu.core.db.mysql.mapper.ReceiptyMapper
import bantu.core.db.mysql.cache.mechanics.MySqlDbCacheEntry
import bantu.core.db.mysql.mapper.ReceiptyMapper
import bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, ItemFetchDbQuery, UserDbEntry, UserFetchDbQuery}

object MysqlDbService{
  sealed trait Data

  case class UserDbEntry (
     id: Int = 0,
     phoneNumber: String,
     password: String,
     businessName : String = "",
     natureOfBusiness :Int = 0,
     joined : String = ""
  ) extends MySqlDbCacheEntry with Ordered[UserDbEntry] with Data {
    override def compare(that: UserDbEntry): Int = this.id compare  that.id
  }

  case class ItemDbEntry(
   id : Int ,
   description : String,
   alias : String = "",
   owner  : Int,
   added : String
   )extends MySqlDbCacheEntry with Ordered[UserDbEntry] with Data   {
    override def compare(that: UserDbEntry): Int = this.id compare that.id
  }
  case class Sale(
   total : Double,
   phone : String,
   userId : Int,
   items : List[ItemDbEntry]
 ) extends Data


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
