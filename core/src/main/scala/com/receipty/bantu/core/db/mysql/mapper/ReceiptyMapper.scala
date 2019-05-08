package com.receipty.bantu.core.db.mysql.mapper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import com.github.mauricio.async.db.RowData
import com.receipty._
import com.receipty.bantu.core.db.mysql.ReceiptyMySqlDb
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import org.joda.time.{DateTime, LocalDateTime}

object ReceiptyMapper extends ReceiptyMapperT {


}


private[mysql] trait ReceiptyMapperT extends ReceiptyMySqlDb  {

  val FetchAllUsersSql  = "SELECT * FROM user"
  val fetchAllItemsSql  = "SELECT * FROM item"

  def fetchAllItems: Future[List[ItemDbEntry]] = {
    pool.sendPreparedStatement(fetchAllItemsSql) map{ queryResult =>
      queryResult .rows match {
          case Some(rows) => rows.toList.map(x => rowToItemModel(x))
          case None       => List()
        }
    }
  }

  def fetchAllUsers : Future[List[UserDbEntry]] = {
    pool.sendPreparedStatement(FetchAllUsersSql) map{ queryResult =>
      queryResult .rows match {
        case Some(rows) => rows.toList.map(x => rowToUserModel(x))
        case None => List()
      }
    }
  }

  def deleteItemsFromDb(items: List[ItemDbEntry]) = {
    println(items.mkString(","))
    val query = s"DELETE from item where iid in (${items.map(_.id).mkString(",")})"
    pool.sendPreparedStatement(query)
  }

  def addToOrderTable(sid : Int ,items: List[ItemDbEntry]) = {
    val query = s"INSERT INTO `order` (sid,iid) VALUES " + items.foldLeft(("",1)){
      case ((str,ind), item) =>
        if(ind < items.length) {
          (str + s"(${sid},${item.id}),",ind+1)
        }else{
          (str + s"(${sid},${item.id});",ind+1)
        }
    }._1
    pool.sendPreparedStatement(query)
  }
  def addToSaleTable(total: Double, phone: String, userId : Int) = {
    val query = s"INSERT INTO sale (cust_num,total,uid) VALUES('$phone',$total,$userId)"
    pool.sendPreparedStatement(query)
  }

  def rowToSID(row: RowData) : Int =  {
    row("sid").asInstanceOf[Int]
  }

  def findSaleId(userId: Int) = {
    val query = s"SELECT sid from sale where uid=$userId ORDER BY date DESC LIMIT 1"
    pool.sendPreparedStatement(query) map { queryResult =>
      queryResult.rows match {
        case Some(rows) => println(Some(rowToSID(rows.toList.head))) ; Some(rowToSID(rows.toList.head))
        case None       => None
      }
    }
  }

  def addItemsIntoDb(items : List[ItemDbEntry]) = {
    val query = s"INSERT INTO item (description,owner) VALUES " + items.foldLeft(("",1)){
      case ((str,ind), item) =>
        if(ind < items.length) {
          (str + s"('${item.description}',${item.owner}),",ind+1)
        }else{
          (str + s"('${item.description}',${item.owner});",ind+1)
        }
    }._1
    pool.sendPreparedStatement(query)
  }

  def insertUserIntoDb(user : UserDbEntry) = {
    val query = s"INSERT INTO user (phone,password,province,county) VALUES ('${user.phoneNumber}','${user.password}',${user.province},${user.county})"
    pool.sendPreparedStatement(query)
  }

  def findUserById(phoneNumber : String) = {
    val query = s"SELECT * FROM user where phone = '$phoneNumber'"
    pool.sendPreparedStatement(query)map { queryResult =>
      queryResult.rows match {
        case Some(rows) => Some(rowToUserModel(rows.toList.head))
        case None       => None
      }
    }
  }

   def rowToItemModel(row: RowData): ItemDbEntry = {

    ItemDbEntry(
      id          = row("iid").asInstanceOf[Int],
      description = row("description").asInstanceOf[String],
      owner       = row("owner").asInstanceOf[Int],
      added       = row("added").asInstanceOf[LocalDateTime].toString("yyyy-MM-dd")
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


