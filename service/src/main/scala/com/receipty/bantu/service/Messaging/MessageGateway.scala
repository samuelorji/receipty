package com.receipty.bantu.service.Messaging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.service.Messaging.MessageGateway
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageRequest, SendMessageResponse}
import com.receipty.bantu.service.util.{HttpClient, GatewayXMLParser}

object MessageGateway {
  case class SendMessageRequest(id : Int, recepient : String, msg : String)
  case class SendMessageResponse(status : Boolean)

}
private[Messaging] class MessageGateway extends Actor
  with ActorLogging
  with HttpClient  {

  implicit val system: ActorSystem = context.system
  def receive = {
    case req : SendMessageRequest =>
      val currentSender = sender()
      val response = for {
      resp <- makeHttpRequest(
        HttpRequest(
        HttpMethods.POST,
        uri      = ReceiptyConfig.MessageEndpoint,
        entity   = FormData("username" -> ReceiptyConfig.username,"to" -> req.recepient,"message" -> req.msg).toEntity(HttpCharsets.`UTF-8`)
       ).withHeaders(List(
          RawHeader("apikey", ReceiptyConfig.apikey),
          RawHeader("Content-Type", "application/x-www-form-urlencoded")
        ))
      )
    } yield resp

    response onComplete {
      case Success(res) => res.status.isSuccess() match {
        case true  =>
          GatewayXMLParser.getMessageStatusCode(res.data) match {
            case x if x.contains("10") =>
              currentSender ! SendMessageResponse(true)
              log.info("Successfully sent message to user id:{}, phoneNumber :{} , msgReceived : {}",req.id,req.recepient, res.data)
            case _                      =>
              currentSender ! SendMessageResponse(false)
              log.info("problem sending message to user id:{}, phoneNumber :{} , msgReceived : {}",req.id,req.recepient, res.data)
          }


        case false =>
          currentSender ! SendMessageResponse(false)
          log.info("Failure sending message to user id:{}, phoneNumber :{}, msgReceived : {} ",req.id,req.recepient,res.data)
      }
      case Failure(ex)  =>
        currentSender ! SendMessageResponse(false)
        log.error("Error contacting Message Broker for user id:{}, phoneNumber :{}, Error : {} ", req.id,req.recepient,ex.getMessage)
    }



  }

}

