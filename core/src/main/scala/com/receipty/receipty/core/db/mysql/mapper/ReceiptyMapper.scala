package com.receipty.receipty.core
package db.mysql.mapper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import com.github.mauricio.async.db.RowData
import com.receipty._
import com.receipty.receipty.core.db.mysql.cache.InnerWorkings.MySqlDbCacheEntry
import com.receipty.receipty.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import org.joda.time.LocalDateTime
import receipty.core.db.mysql.ReceiptyMySqlDb

private[mysql] object ReceiptyMapper extends ReceiptyMySqlDb  {

  //used for the database request

  val FetchAllUSersSql  = "SELECT * FROM user"
  val fetchAllItemsSql = "SELECT * FROM item"

  def fetchAllItems: Future[List[ItemDbEntry]] = {

    pool.sendPreparedStatement(fetchAllItemsSql) map{ queryResult =>
      queryResult .rows match {
          case Some(rows) => rows.toList.map(x => rowToItemModel(x))
          case None => List()
        }
    }

  }

  def fetchAllUsers : Future[List[UserDbEntry]] = {

    pool.sendPreparedStatement(FetchAllUSersSql) map{ queryResult =>
      queryResult .rows match {
        case Some(rows) => rows.toList.map(x => rowToUserModel(x))
        case None => List()
      }
    }
  }

   def rowToItemModel(row: RowData): ItemDbEntry = {

    ItemDbEntry(
      id = row("iid").asInstanceOf[Int],
      description = row("description").asInstanceOf[String],
      owner = row("owner").asInstanceOf[Int],
      added = row("added").asInstanceOf[LocalDateTime].toString("yyyy-MM-dd")
    )

  }
  def rowToUserModel(row: RowData): UserDbEntry = {

    UserDbEntry(
      id          = row("uid").asInstanceOf[Int],
      phoneNumber = row("phone").asInstanceOf[String],
      password    = row("password").asInstanceOf[String],
      county      = row("county").asInstanceOf[Int],
      province    = row("province").asInstanceOf[Int],
      joined      = row("joined").asInstanceOf[LocalDateTime].toString("yyyy-MM-dd")
    )

  }
}


