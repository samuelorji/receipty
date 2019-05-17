package com.receipty.bantu.core
package db.mysql.cache

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, _}

import akka.actor.Props
import akka.pattern.{ask, pipe}

import com.receipty._
import com.receipty.bantu.core.db.mysql.cache.mechanics.{MySqlDbCacheManagerT, UpdateCacheRequestImpl}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, ItemFetchDbQuery, UserDbEntry, UserFetchDbQuery}


object UserDbCache extends UserDbCacheT
object ItemDbCache extends ItemDbCacheT

 trait ItemDbCacheT extends MySqlDbCacheManagerT[ItemDbEntry]{

  def getUserItems(uid : Int) = itemMap.getOrElse(uid,List[ItemDbEntry]())

  override def setEntries(x: List[ItemDbEntry]): Unit = {

    setItemMap(x.foldLeft(Map[Int, List[ItemDbEntry]]()) {
      case (l, entry) => l.updated(
        entry.owner , x.filter(_.owner == entry.owner)
      )
     }
    )
  }

  def props = Props(classOf[ItemDbCache],this)
  private var itemMap = Map[Int/* item Id */, List[ItemDbEntry] /* List of Item */]()

  private def setItemMap(map : Map[Int,List[ItemDbEntry]]): Unit ={
    itemMap = map
  }
}
 trait UserDbCacheT extends MySqlDbCacheManagerT[UserDbEntry]{

  def checkIfUserExists(phoneNumber : String) =
    userMap.get(phoneNumber)

  override def setEntries(x: List[UserDbEntry]): Unit = {
    super.setEntries(x)

    setUserMap(x.foldLeft(Map[String,UserDbEntry]()){
      case (m,entry) => m.updated(
          entry.phoneNumber
        ,entry
      )
    })
  }

  def props = Props(classOf[UserDbCache],this)
  private var userMap = Map[String/* phone number */,UserDbEntry /* user */]()

  private def setUserMap(map : Map[String ,UserDbEntry]): Unit = {
    userMap = map
  }
}

/*
The authentication Db Cache is an Actor because it extends the MysqlDbCacheT which is the actor
that handles the scheduling of caching, this is the actor class that should be spun up on program start
 */
private [core] class UserDbCache(
  val manager : UserDbCacheT
  ) extends MySqlDbCacheT[UserDbEntry]{
  override protected val updateFrequency: FiniteDuration = 1 minute
  override protected def specificReceive: Receive = {
    case UpdateCacheRequestImpl =>
      (mysqlDbService ? UserFetchDbQuery).mapTo[List[UserDbEntry]] pipeTo sender
  }
}

private[core] class ItemDbCache(
 val manager : ItemDbCacheT
 ) extends MySqlDbCacheT[ItemDbEntry]{

  override protected val updateFrequency: FiniteDuration = 1 minute

  override protected def specificReceive: Receive = {
    case UpdateCacheRequestImpl =>
      (mysqlDbService ? ItemFetchDbQuery).mapTo[List[ItemDbEntry]] pipeTo sender()
  }
}
