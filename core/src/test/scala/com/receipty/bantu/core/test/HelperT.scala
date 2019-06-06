package com.receipty.bantu.core.test

import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, Sale}

trait HelperT {

  def getMockUserItems(numOfItems : Int,userId : Int ) : List[ItemDbEntry] = {
    (1 to numOfItems).map{x =>
      ItemDbEntry(
        id          = x ,
        description = "mock description",
        owner       = userId,
        added       = ""
      )
    }.toList
  }


  def getSale(userItems : List[ItemDbEntry],userId : Int , phoneNumber : String, amount : Double,itemNums : Int*) : Sale = {
    val itemsToSell = userItems.filter(x => itemNums.contains(x.id))
    Sale(
      total  = amount,
      phone  = phoneNumber,
      userId = userId,
      items = itemsToSell
    )
  }

}
