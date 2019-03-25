package com.receipty.bantu.service.Messaging

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.cache.{ItemDbCache, UserDbCache}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.ItemDbEntry
import com.receipty.bantu.service.Db.DbService
import com.receipty.bantu.service.Db.DbService.{AddItems, AddItemsResponse, GetUserId, GetUserIdResponse}
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageToClient, SendMessageToClientResponse}



object MessagingService {
  case class SendRegistrationMessage(
    sessionId: String ,
    phoneNumber : String
  )
  case class SendRegistrationMessageResponse(status : Boolean)
  case class CustomerMessage(msg: String,phone : String)
  case class SendCustomMessage(id : Int, msg: String,phone : String)
  case class SendCustomMessageResponse(status : Boolean)
}

class MessagingService extends Actor with ActorLogging{
  //this class should get the phone number of a registered user , query the database for the user's Id
  //and then send they user a welcome message showing the format to add items


  val dbService        = createDbService
  def createDbService  = context.actorOf(Props[DbService])

  val messageGateway       = createMessageGateway
  def createMessageGateway = context.actorOf(Props[MessageGateway])

  implicit val timeout = Timeout(5 seconds)

  import MessagingService._
  private def sendMessage(message : String, id : Int, pNumber : String) = {
    (messageGateway ? SendMessageToClient(
      id = id,
      phoneNumber = pNumber,
      msg = message
    )).mapTo[SendMessageToClientResponse]
  }


  def receive = {
    case req: SendRegistrationMessage =>
      val currentSender =  sender()
      //first find user Id from
      log.info(s"processing request $req")
      (dbService ? GetUserId(
        phoneNumber  = req.phoneNumber.substring(1)
      )).mapTo[GetUserIdResponse] onComplete{
        case Success(res) => res match {
          case GetUserIdResponse(true, id) =>
            if(id != 0){
              //now send user message
              val msg      = s"Welcome To Receipty , Your user Id is ${id}, To add items, Life sucks "
              sendMessage(
                message = msg,
                id      = id,
                pNumber = req.phoneNumber).mapTo[SendMessageToClientResponse] map {
                case SendMessageToClientResponse(true)  =>
                  currentSender ! SendRegistrationMessageResponse(true)
                case SendMessageToClientResponse(false) =>
                  currentSender ! SendRegistrationMessageResponse(false)
              }
            }else{
              currentSender ! SendRegistrationMessageResponse(false)
              log.error(s"Could not fetch data for user: phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
            }
          case GetUserIdResponse(false,_) =>
            currentSender ! SendRegistrationMessageResponse(false)
            log.error(s"Could not fetch data for user: phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
        }
        case Failure(ex) =>
          currentSender ! SendRegistrationMessageResponse(false)
          log.error("Error Finding user info for user with phoneNumber : {}, sessionId : {}, Error : {}",req.phoneNumber,req.sessionId,ex.getMessage)
      }

    case req : SendCustomMessage =>
      val currentSender =  sender()
      sendMessage(
        message = req.msg,
        id      = req.id,
        pNumber = req.phone
      ) map {
        case SendMessageToClientResponse(true)  =>
          currentSender ! SendCustomMessageResponse(true)
        case SendMessageToClientResponse(false) =>
          currentSender ! SendCustomMessageResponse(false)
      }


    case req : CustomerMessage =>
      //TODO ...this should contain detail from registered user for adding items or any other complaint
      //TODO a good format should be either ADD# or HELP#  or any prompt with a # separator

    val separator = '#'
      val entries = req.msg.split(separator)

      entries(0).toLowerCase match {
        case "add" =>
          //now add items from entries(1) to end
          val userexist = UserDbCache.checkIfUserExixsts(req.phone)
          userexist match {
            case Some(user) =>
              val numItems  = entries.length - 1
              val userItems = ItemDbCache.getUserItems(user.id)
              println(s"length of user items is ${userItems.length}")
              if(numItems <= ReceiptyConfig.maxItemsCount) {
                if (userItems.length + numItems > ReceiptyConfig.maxItemsCount) {
                  //user cannot add more items , items left that can be added is
                  //10 -
                  val errorMsg = s"ERROR:\nItem Limit reached, you already have ${ReceiptyConfig.maxItemsCount} items in store"

                  println(errorMsg)
                  sendMessage(
                    message  = errorMsg,
                    id       = user.id,
                    pNumber  = user.phoneNumber
                  )

                } else {
                  val items = entries.foldLeft(List.empty[ItemDbEntry]) {
                    case (list, entry) =>
                      if (entry.toLowerCase.contains("add") || entry.toLowerCase.contains("help")) {
                        list
                      } else {
                        val item = ItemDbEntry(
                          id = 0,
                          description = entry.trim,
                          owner = user.id,
                          added = ""
                        )
                        list :+ item
                      }
                  }

                  (dbService ? AddItems(items)).mapTo[AddItemsResponse] onComplete {
                    case Success(res) => res match {
                      case AddItemsResponse(true, _) =>
                        //TODO ....send message to user that he/she has successfully added items
                        log.info(s"Successfully added items message to user : phone :{} ", req.phone)
                        val msg = s"Succesfully added $numItems items"
                        sendMessage(
                          id = user.id,
                          pNumber = user.phoneNumber,
                          message = msg
                        )

                      case AddItemsResponse(false, err) =>

                        val errorMsg = s"ERROR:\nCould not add items, please retry"
                        sendMessage(
                          id = user.id,
                          pNumber = user.phoneNumber,
                          message = errorMsg
                        )
                        log.error(s"Could Not Successfully add items for user : phone :{} error :{}", req.phone, err)
                    }
                    case Failure(ex) =>
                      val errorMsg = s"ERROR:\nCould not add items, please retry"
                      sendMessage(
                        id = user.id,
                        pNumber = user.phoneNumber,
                        message = errorMsg
                      )
                      log.error(s"Could Not Successfully added items for user : phone :{} error  :{}", req.phone, ex.getMessage)
                  }
                }
              }
              else{
                //TODO send to user that number of items too much
                val errorMsg = s"ERROR:\nHello User ${user.id}, Number of items to add is more than ${ReceiptyConfig.maxItemsCount}"
                println(errorMsg)
                sendMessage(
                  id          = user.id,
                  pNumber     = user.phoneNumber,
                  message     = errorMsg
                )
              }


            case None  =>
              val errorMsg = s"ERROR:\nHello , You Have not been registered "
              println(errorMsg


              )
              messageGateway ! SendCustomMessage(
                phone     = req.phone,
                msg       = errorMsg,
                id        = 0,

              )
          }


        case "help" =>
          //handle help cases

        case _ => //TODO ...send message to user that entry is incorrect and they should follow protocol
      }

  }
}
