package com.receipty.bantu.service.Messaging

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout

import com.receipty.bantu.core.db.mysql.cache.UserDbCache
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.ItemDbEntry
import com.receipty.bantu.service.Db.DbService
import com.receipty.bantu.service.Db.DbService.{AddItems, AddItemsResponse, GetUserId, GetUserIdResponse}
import com.receipty.bantu.service.Messaging.MessageGateway.SendMessage
import org.joda.time.DateTime


object MessagingService {
  case class SendMessageToUser(
    sessionId: String ,
    phoneNumber : String
  )
  case class CustomerMessage(msg: String,phone : String)
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
  def receive = {
    case req: SendMessageToUser =>
      //first find user Id from
      (dbService ? GetUserId(
        phoneNumber  = req.phoneNumber.substring(1)
      )).mapTo[GetUserIdResponse] onComplete{
        case Success(res) => res match {
          case GetUserIdResponse(true, id) =>
            if(id != 0){
              //now send user message
              messageGateway ! SendMessage(
                id          = id,
                phoneNumber = req.phoneNumber
              )
            }else{
              log.error(s"Could not fetch data for user: phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
            }
          case GetUserIdResponse(false,_) =>
            log.error(s"Could not fetch data for user: phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
        }
        case Failure(ex) =>
          log.error("Error Finding user info for user with phoneNumber : {}, sessionId : {}, Error : {}",req.phoneNumber,req.sessionId,ex.getMessage)
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
              val numItems = entries.length - 1
             val items =  entries.foldLeft(List.empty[ItemDbEntry]){
                case (list, entry) =>
                  if(entry.toLowerCase.contains("add") || entry.toLowerCase.contains("help")){
                    list
                  }else{
                    val item = ItemDbEntry(
                      id          = 0,
                      description = entry.trim ,
                      owner       = user.id,
                      added       = ""
                    )
                     list :+ item
                  }
              }

             (dbService ? AddItems(items)).mapTo[AddItemsResponse] onComplete{
               case Success(res) => res match {
                 case AddItemsResponse(true, _) =>
                      //TODO ....send message to user that he/she has successfully added items
                   log.info(s"Successfully added items message to user : phone :{} ",req.phone)

                 case AddItemsResponse(false,msg) =>

                   log.error(s"Could Not  Successfully added items for user : phone :{} reason :{}",req.phone, msg)
               }
               case Failure(ex) =>
                 log.error(s"Could Not Successfully added items for user : phone :{} error  :{}",req.phone, ex.getMessage)
             }

              //now tell Db Service to put these items in the DB

            case None  =>
          }


        case "help" =>
          //handle help cases

        case _ => //TODO ...send message to user that entry is incorrect and they should follow protocol
      }

  }
}
