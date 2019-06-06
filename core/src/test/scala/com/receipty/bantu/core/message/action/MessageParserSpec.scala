package com.receipty.bantu.core.message.action

import akka.actor.Props

import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, Sale, UserDbEntry}
import com.receipty.bantu.core.message.action.MessageParser.{ParseMessageRequest, ParseMessageResponse}
import com.receipty.bantu.core.test.{HelperT, TestServiceT}

class MessageParserSpec extends TestServiceT with HelperT{

  val messageParser = system.actorOf(Props[MessageParser])

  val mockUser = UserDbEntry(
    id          = 2,
    phoneNumber = "+2348024276976",
    password    = "",
    province    = 1,
    county      = 1,
    joined      = ""
  )
  val userItems = getMockUserItems(10,mockUser.id)

  "The Message Parser Actor " must {
    "properly parse a sale  " in {
      messageParser !  ParseMessageRequest(
        msg       = "sell#1,2,3,4#560.0#08112172624",
        userItems = userItems,
        user      = mockUser
      )
      val sale = getSale(userItems,0,"08112172624",560,1,2,3,4)


      expectMsg(ParseMessageResponse(Some(MessageAction.Sell),Some(List(sale)),None))
    }
  }

}
