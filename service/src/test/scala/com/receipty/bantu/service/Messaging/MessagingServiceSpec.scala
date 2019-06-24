package com.receipty.bantu.service.Messaging

import akka.actor.Props
import akka.testkit.TestProbe

import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.cache
import scala.concurrent.duration._

import com.receipty.bantu.core.db.mysql.service.MysqlDbService.UserDbEntry
import com.receipty.bantu.service.Db.DbService.{AddItemsRequest, AddItemsResponse, GetUserIdRequest, GetUserIdResponse}
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageRequest, SendMessageResponse}
import com.receipty.bantu.service.Messaging.MessagingService._
import com.receipty.bantu.service.test._

class MessagingServiceSpec extends TestServiceT {

  val dbService      = TestProbe()
  val messageGateway = TestProbe()
  val messagingService = system.actorOf(Props(new MessagingService {
    override def createDbService      = dbService.ref
    override def createMessageGateway = messageGateway.ref

    override def getItemDbCache = ItemDbCache

    override def getUserDbCache = UserDbCache
  }))

  val phoneNumber = "+2348112172624"
  val welcomeMessage = "Welcome To Receipty, To add items, please send ADD and the items separated by # example ADD#Ugali#Rice. Please ensure that the items do not exceed 10  "

  val validAddMsg = "ADD#Pick and Peel#Strawberry#Jello#Spaghetti"
  val invalidCharacterOutOfRange = "ADD#Pick and Peellllllllllllllllllllllllll#Strawberry#Jello#Spaghetti"
  val extraItems  = "ADD#Pick and Peel#Strawberry#Jello#Spaghetti#Rice#Beans#Remote#Bread#Butter"
  val extraItemsMoreThan10  = "ADD#Pick and Peel#Strawberry#Jello#Spaghetti#Rice#Beans#Remote#Bread#Butter#Cup#Keyboard"

  "The Messaging Service " must {
    "Handle Sending Registration Message " in {
      messagingService ! SendRegistrationMessage(
        sessionId   = "sessionId",
        phoneNumber = phoneNumber,
        user        = UserDbEntry(
          phoneNumber = "",
          password    = ""
        )
      )

      dbService.expectMsg(GetUserIdRequest(
        phoneNumber = "+2348112172624"
      ))
      dbService.reply(GetUserIdResponse(
        status = true ,
        id     = 5
      ))

      messageGateway .expectMsg(SendMessageRequest(
        id          = 5 ,
        recepient = phoneNumber,
        msg         = welcomeMessage
      ))
      messageGateway.reply(SendMessageResponse(
        status = true
      ))
      expectMsg(SendRegistrationMessageResponse(true))
      expectNoMessage(100 milliseconds)
    }

    "Handle Sending Custom Message for a Valid Phone Number " in {
      messagingService ! SendCustomMessageRequest(
        id    = 5,
        msg   = welcomeMessage,
        phone = phoneNumber
      )

      messageGateway .expectMsg(SendMessageRequest(
        id          = 5 ,
        recepient = phoneNumber,
        msg         = welcomeMessage
      ))
      messageGateway.reply(SendMessageResponse(
        status = true
      ))
      expectMsg(SendCustomMessageResponse(true))
      expectNoMessage(100 milliseconds)
    }

    "Handle Sending Custom Message for an InValid Phone Number " in {
      messagingService ! SendCustomMessageRequest(
        id    = 5,
        msg   = "hello",
        phone = phoneNumber + "9090909090"
      )

      messageGateway .expectMsg(SendMessageRequest(
        id          = 5 ,
        recepient = phoneNumber + "9090909090",
        msg         = "hello"
      ))
      messageGateway.reply(SendMessageResponse(
        status = false
      ))
      expectMsg(SendCustomMessageResponse(false))
      expectNoMessage(100 milliseconds)
   }

    "Inform the user that number of items to add is going to exceed 10 " in {
      messagingService ! CustomerMessage(
        msg      = extraItems,
        phone    = phoneNumber
      )
      val errorMsg = s"ERROR:\nItem Limit is ${ReceiptyConfig.maxItemsCount}, you already have ${ItemDbCache.getUserItems(23).length} items in store, you can only add ${ReceiptyConfig.maxItemsCount - ItemDbCache.getUserItems(23).length}"
      messageGateway.expectMsg(SendMessageRequest(
        id          = 5,
        recepient = phoneNumber,
        msg         = errorMsg
      ))
      expectNoMessage(100 milliseconds)
    }

    "Send Error message that number of items to Add is more than 10 for messages with more than 10 items " in {

      messagingService ! CustomerMessage(
        msg = extraItemsMoreThan10,
        phone = phoneNumber
      )

      val errorMsg = s"ERROR:\nHello User ${5}, Number of items to add is more than ${ReceiptyConfig.maxItemsCount}"
      messageGateway.expectMsg(SendMessageRequest(
        id          = 5,
        recepient = phoneNumber,
        msg         = errorMsg
      ))
      expectNoMessage(100 milliseconds)

    }

    "Send Error message when character limit of 25 is reached for any item " in {

      messagingService ! CustomerMessage(
        msg   = invalidCharacterOutOfRange,
        phone = phoneNumber
      )

      val errMsg = "Length of An Item to Add is greater than 20"
       messageGateway.expectMsg(SendMessageRequest(
        id          = 5,
        recepient = phoneNumber,
        msg         = errMsg
      ))
      expectNoMessage(100 milliseconds)

    }

    "Properly store Items that need to be added with valid Add Message " in {
      messagingService ! CustomerMessage(
        msg = validAddMsg,
        phone = phoneNumber
      )

      dbService.expectMsg(AddItemsRequest(
        items = ItemDbCache.getUserItems(23)
      ))

      dbService.reply(AddItemsResponse(
        status = true,
        msg = "success"
      ))
      expectNoMessage(100 milliseconds)

    }



  }
}
