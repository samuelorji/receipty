package com.receipty.bantu.service.Db

import akka.actor.Props
import com.receipty.bantu.core.db.mysql.cache.{ItemDbCache, UserDbCache}

import scala.concurrent.duration._
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import com.receipty.bantu.service.Db.DbService._
import com.receipty.bantu.service.test.TestServiceT
import com.receipty.bantu.service.util.ReceiptyUtils

class DbServiceSpec extends TestServiceT {
  val dbService = system.actorOf(Props[DbService])
//  val userId = 5
//  val userItems = ItemDbCache.getUserItems(userId)

//  println(userItems)
  "DB service " must {
    "Properly add a valid user " in {
      dbService ! AddUserRequest(
        user = UserDbEntry(
          phoneNumber = "+2348020686607",
          password = ReceiptyUtils.hashPassword("1234"),
          province = 1,
          county = 2
        )
      )
      val result = expectMsgClass(
        5 seconds,
        classOf[AddUserResponse]
      )

      result.status should be(true)
    }
    "Properly Add Item Request with valid description of less thsean 25 characters  " in {
      dbService ! AddItemsRequest(
        items = List(
          ItemDbEntry(
            id = 0,
            description = "Pick and Peel",
            owner = 5,
            added = ""
          )
        )
      )
      val result = expectMsgClass(
        5 seconds,
        classOf[AddItemsResponse]
      )

      result.status should be(true)
    }

    "Fail when adding items of more than 25 characters  " in {
      dbService ! AddItemsRequest(
        items = List(
          ItemDbEntry(
            id = 0,
            description = "Pick and Peelllllllllllllllllllllllllllllllllllllllllll",
            owner = 5,
            added = ""
          )
        )
      )
      val result = expectMsgClass(
        5 seconds,
        classOf[AddItemsResponse]
      )

      result.status should be(false)
    }

  }
  "Properly Delete Item Request with valid Item Id  " in {
    dbService ! DeleteItemsRequest(
      items = List(
        ItemDbEntry(
          id = 25,
          description = "Pick and Peel",
          owner = 5,
          added = ""
        )
      )
    )
    val result = expectMsgClass(
      5 seconds,
      classOf[DeleteItemsResponse]
    )

    result.status should be(true)
  }

  "Fail when deleting items that do not exist  " in {
    dbService ! DeleteItemsRequest(
      items = List(
        ItemDbEntry(
          id = 10000000,
          description = "Pick and Peel",
          owner = 5,
          added = ""
        )
      )
    )
    val result = expectMsgClass(
      5 seconds,
      classOf[DeleteItemsResponse]
    )

    result.status should be(false)
  }

  "Find a valid user Id " in {
    dbService ! GetUserIdRequest(
      phoneNumber = "+2348020686607"
    )
    val result = expectMsgClass(
      5 seconds,
      classOf[GetUserIdResponse]
    )

    result.status should be(true)
  }
  "Not Find a valid user Id " in {
    dbService ! GetUserIdRequest(
      phoneNumber = "+23480206866507"
    )
    val result = expectMsgClass(
      5 seconds,
      classOf[GetUserIdResponse]
    )

    result.status should be(false)
  }

  "Add Sale To Db " in {

    dbService ! SellItemsRequest(
      sale = Sale(
        total = 30.0,
        phone = "+2348112172625",
        userId = 5,
        items = List(ItemDbEntry(
          id = 21,
          description = "",
          owner = 5,
          added = ""
        ),
          ItemDbEntry(
            id = 22,
            description = "",
            owner = 5,
            added = ""
          ))
      )
    )

    val result = expectMsgClass(
      5 seconds,
      classOf[SellItemResponse]
    )

    result.status should be(true)

  }
}

