package com.receipty.bantu.service.Messaging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.service.Messaging.MessageGateway
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageToClient, SendMessageToClientResponse}
import com.receipty.bantu.service.util.{HttpClient, MessageParser}

object MessageGateway {
  case class SendMessageToClient( id : Int, phoneNumber : String, msg : String)
  case class SendMessageToClientResponse(status : Boolean)

}
private[Messaging] class MessageGateway extends Actor
  with ActorLogging
  with HttpClient  {

  implicit val system: ActorSystem = context.system
  def receive = {
    case req : SendMessageToClient =>
      val currentSender = sender()
      val response = for {
      resp <- makeHttpRequest(
        HttpRequest(
        HttpMethods.POST,
        uri      = ReceiptyConfig.MessageEndpoint,
        entity   = FormData("username" -> ReceiptyConfig.username,"to" -> req.phoneNumber,"message" -> req.msg).toEntity(HttpCharsets.`UTF-8`)
       ).withHeaders(List(
          RawHeader("apikey", ReceiptyConfig.apikey),
          RawHeader("Content-Type", "application/x-www-form-urlencoded")
        ))
      )
    } yield resp

    response onComplete {
      case Success(res) => res.status.isSuccess() match {
        case true  =>
          MessageParser.getMessageStatusCode(res.data) match {
            case x if x.contains("10") =>
              currentSender ! SendMessageToClientResponse(true)
              log.info("Successfully sent message to user id:{}, phoneNumber :{} , msgReceived : {}",req.id,req.phoneNumber, res.data)
            case _                      =>
              currentSender ! SendMessageToClientResponse(false)
              log.info("problem sending message to user id:{}, phoneNumber :{} , msgReceived : {}",req.id,req.phoneNumber, res.data)
          }


        case false =>
          currentSender ! SendMessageToClientResponse(false)
          log.info("Failure sending message to user id:{}, phoneNumber :{}, msgReceived : {} ",req.id,req.phoneNumber,res.data)
      }
      case Failure(ex)  =>
        currentSender ! SendMessageToClientResponse(false)
        log.error("Error contacting Message Broker for user id:{}, phoneNumber :{}, Error : {} ", req.id,req.phoneNumber,ex.getMessage)
    }



  }

}

