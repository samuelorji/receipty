package com.receipty.bantu.core.test

import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{Data, ItemDbEntry, Sale}
import com.receipty.bantu.core.message.action.MessageAction
import com.receipty.bantu.core.message.action.MessageParser.ParseMessageResponse

trait HelperT {

  def getMockUserItems(numOfItems : Int,userId : Int ) : List[ItemDbEntry] = {
    (1 to numOfItems).map{x =>
      ItemDbEntry(
        id          = x ,
        description = "mock description",
        alias       = "mock alias",
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

  def getItemsFromAddMsg(msg : String) = {
    val aliasPattern = """\([a-zA-Z0-9_-[\s]]+\)""".r
    val itemPattern  = """\#[a-zA-Z0-9_-[\s]]+\(""".r


    val itemListMatched  = itemPattern.findAllMatchIn(msg).toList.map(_.toString).map(_.replace("#","").replace("(",""))
    val aliasListMatched = aliasPattern.findAllMatchIn(msg).toList.map(_.toString).map(_.replace("(","").replace(")",""))

    itemListMatched zip aliasListMatched match {
      case list if list.nonEmpty =>
          val items = list.foldLeft(List.empty[ItemDbEntry]) {
            case (l, entry) =>
              if (entry._1.toLowerCase.contains("add") || entry._1.toLowerCase.contains("help")) {
                l
              } else {
                val item = ItemDbEntry(
                  id = 0,
                  description = entry._1.trim.toLowerCase,
                  alias = entry._2.trim.toLowerCase,
                  owner = 2,
                  added = ""
                )
                l :+ item
              }

          }
          items
      case _ => List[Data]()
    }
  }

//ParseMessageResponse(Some(Sell),List(ItemDbEntry(0,nyamachoma,nya-chom,2,), ItemDbEntry(0,riceeeeex,rice,2,), ItemDbEntry(0,beansxxxxxxxxx,beans,2,), ItemDbEntry(0,yamxxxxxxxxxxcc,yam,2,), ItemDbEntry(0,nyamachoma,nya chom,2,), ItemDbEntry(0,spi,sput 0,2,), ItemDbEntry(0,sput,jj 90kg,2,)),None)
//ParseMessageResponse(Some(Add),List(ItemDbEntry(0,nyamachoma,nya-chom,2,), ItemDbEntry(0,riceeeeex,rice,2,), ItemDbEntry(0,beansxxxxxxxxx,beans,2,), ItemDbEntry(0,yamxxxxxxxxxxcc,yam,2,), ItemDbEntry(0,nyamachoma,nya chom,2,), ItemDbEntry(0,spi,sput 0,2,), ItemDbEntry(0,sput,jj 90kg,2,)),None)
}
