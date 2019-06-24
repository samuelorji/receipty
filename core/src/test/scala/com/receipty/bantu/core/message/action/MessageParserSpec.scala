package com.receipty.bantu.core.message.action

import akka.actor.Props

import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, Sale, UserDbEntry}
import com.receipty.bantu.core.message.action.MessageParser.{ParseMessageRequest, ParseMessageResponse}
import com.receipty.bantu.core.test.{HelperT, TestServiceT}

class MessageParserSpec extends TestServiceT with HelperT{

  val messageParser = system.actorOf(Props[MessageParser])

  /*

  throw new MessageParserException(MessageAction.Sell,s"Either item character length is longer than ${ReceiptyConfig.itemMaxCharacterCount} " +
  s"or alias character length of an item  is longer than ${ReceiptyConfig.itemAliasMaxCharacterCount}")
  }
  case _                      =>
  throw new MessageParserException(MessageAction.Sell,"Invalid Items format, please type add#[item name of 30 characters(item alias of 10 characters)]")
  }
  }else{
  throw new MessageParserException(MessageAction.Sell,s"ERROR:\nItem Limit is ${ReceiptyConfig.maxItemsCount}, you already have ${req.userItems.length} items in store, you can only add ${ReceiptyConfig.maxItemsCount - req.userItems.length}")
  }
  }else{
  throw new MessageParserException(MessageAction.Sell,s"Invalid Entry, Maximum number of items to be added is ${ReceiptyConfig.maxItemsCount}")
   */

  val mockUser = UserDbEntry(
    id          = 2,
    phoneNumber = "+2348024276976",
    password    = "",
    joined      = ""
  )

//  val aliasPattern = """\([a-zA-Z0-9_-[\s]]+\)""".r
//  val itemPattern  = """\#[a-zA-Z0-9_-[\s]]+\(""".r
//  itemPattern.findAllMatchIn("Add#NyamaChoma(nya-chom)#Riceeeeex(Rice)#Beansxxxxxxxxx(Beans)#Yamxxxxxxxxxxcc(Yam)#NyamaChoma(nya chom)#spi(sput 0)#sput(jj 90kg)").toList match {
//    case l if l.nonEmpty =>
//      l.map(_.toString) foreach(println)
//    case List()      =>
//      println("No Match")
//  }

  val userItems = getMockUserItems(10,mockUser.id)

  "The Message Parser Actor " must {
    "properly parse a sale  " in {
      messageParser !  ParseMessageRequest(
        msg       = "sell#1,2,3,4#560.0#0987654321",
        userItems = userItems,
        user      = mockUser
      )
      val sale = getSale(userItems,2,"0987654321",560,1,2,3,4)


      expectMsg(ParseMessageResponse(Some(MessageAction.Sell),List(sale),None))
    }
    "properly parse Items to Add with valid item and alias names as well as length" in {
      val message = "Add#NyamaChoma(nya-chom)#Riceeeeex(Rice)#Beansxxxxxxxxx(Beans)#Yamxxxxxxxxxxcc(Yam)#NyamaChoma(nya chom)#spi(sput 0)#sput(jj 90kg)"
      messageParser !  ParseMessageRequest(
        msg       = message,
        userItems = List(),
        user      = mockUser
      )
      val items = getItemsFromAddMsg(message)
      expectMsg(ParseMessageResponse(Some(MessageAction.Add),items,None))

    }

    "not parse add msg when alias is more than max amount of characters " in {
      val message = "Add#NyamaChoma(nya-chom)#Riceeeeex(Rice)#Beansxxxxxxxxx(Beanssssssssssssssssss)#Yamxxxxxxxxxxcc(Yam)#NyamaChoma(nya chom)#spi(sput 0)#sput(jj 90kg)"
      messageParser !  ParseMessageRequest(
        msg       = message,
        userItems = List(),
        user      = mockUser
      )

      expectMsg(ParseMessageResponse(Some(MessageAction.Add),List(),Some(s"Either item character length is longer than ${ReceiptyConfig.itemMaxCharacterCount}" +
        s" or alias character length of an item  is longer than ${ReceiptyConfig.itemAliasMaxCharacterCount}" +
        s" or the number of aliases do not match the number of Items")))
    }
    "not parse message when item is more than max characters " in {
      val message = "Add#NyamaChoma(nya-chom)#Riceeeeex(Rice)#Beansxxccccccccccccccccccccccccccccccccccccccccccccccxxxxxxx(Beanssss)#Yamxxxxxxxxxxcc(Yam)#NyamaChoma(nya chom)#spi(sput 0)#sput(jj 90kg)"
      messageParser !  ParseMessageRequest(
        msg       = message,
        userItems = List(),
        user      = mockUser
      )

      expectMsg(ParseMessageResponse(Some(MessageAction.Add),List(),Some(s"Either item character length is longer than ${ReceiptyConfig.itemMaxCharacterCount}" +
        s" or alias character length of an item  is longer than ${ReceiptyConfig.itemAliasMaxCharacterCount}" +
        s" or the number of aliases do not match the number of Items")))
    }
    "not parse message when number of items is equal to number of aliases " in {
      val `messagewith@character` = "Add#NyamaChoma(nya-chom)#Riceeeeex(Rice)#Beansxxccccxxxx(Beans@sss)#Yamxxxxxxxxxxcc(Yam)#NyamaChoma(nya chom)#spi(sput 0)#sput(jj 90kg)"
      messageParser !  ParseMessageRequest(
        msg       =  `messagewith@character`,
        userItems = List(),
        user      = mockUser
      )

      expectMsg(ParseMessageResponse(Some(MessageAction.Add),List(),Some(s"Either item character length is longer than ${ReceiptyConfig.itemMaxCharacterCount}" +
        s" or alias character length of an item  is longer than ${ReceiptyConfig.itemAliasMaxCharacterCount}" +
        s" or the number of aliases do not match the number of Items")))

    }
  }

  //ParseMessageResponse(Some(Sell),List(Sale(560.0,0987654321,0,List(ItemDbEntry(1,mock description,2,), ItemDbEntry(2,mock description,2,), ItemDbEntry(3,mock description,2,), ItemDbEntry(4,mock description,2,)))),None)
} //ParseMessageResponse(Some(Sell),List(Sale(560.0,0987654321,2,List(ItemDbEntry(1,mock description,2,), ItemDbEntry(2,mock description,2,), ItemDbEntry(3,mock description,2,), ItemDbEntry(4,mock description,2,)))),None)
