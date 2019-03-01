package com.receipty.bantu.service.Messaging

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout

import com.receipty.bantu.service.Db.DbService
import com.receipty.bantu.service.Db.DbService.{GetUserId, GetUserIdResponse}
import com.receipty.bantu.service.Messaging.MessageGateway.SendMessage
import com.receipty.bantu.service.Messaging.MessagingService.SendMessageToUser

object MessagingService {
  case class SendMessageToUser(
    sessionId: String ,
    phoneNumber : String
  )
}

class MessagingService extends Actor with ActorLogging{
  //this class should get the phone number of a registered user , query the database for the user's Id
  //and then send they user a welcome message showing the format to add items


  val dbService        = createDbService
  def createDbService  = context.actorOf(Props[DbService])

  val messageGateway       = createMessageGateway
  def createMessageGateway = context.actorOf(Props[MessageGateway])

  implicit val timeout = Timeout(5 seconds)

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
  }
}
