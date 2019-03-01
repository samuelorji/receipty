package com.receipty.bantu.service.Messaging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader

import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.service.Messaging.MessageGateway.SendMessage
import com.receipty.bantu.service.util.HttpClient

object MessageGateway {
  case class SendMessage(id : Int, phoneNumber : String)
}
private[Messaging] class MessageGateway extends Actor
  with ActorLogging
  with HttpClient  {

  implicit val system: ActorSystem = context.system
  def receive = {

    case req : SendMessage =>
      val msg      = s"Welcome To Receipty , Your user Id is ${req.id}, To add items, Life sucks "
      val response = for {
      resp <- makeHttpRequest(
        HttpRequest(
        HttpMethods.POST,
        uri      = ReceiptyConfig.MessageEndpoint,
        entity   = FormData("username" -> ReceiptyConfig.username,"to" -> req.phoneNumber,"message" -> msg).toEntity(HttpCharsets.`UTF-8`)
       ).withHeaders(List(
          RawHeader("apikey", ReceiptyConfig.apikey),
          RawHeader("Content-Type", "application/x-www-form-urlencoded")
        ))
      )
    } yield resp

    response onComplete {
      case Success(res) => res.status.isSuccess() match {
        case true  =>
          log.info("Successfully sent message to user id:{}, phoneNumber :{} , msgReceived : {}",req.id,req.phoneNumber, res.data)
        case false =>
          log.info("Failure sending message to user id:{}, phoneNumber :{}, msgReceived : {} ",req.id,req.phoneNumber,res.data)
      }
      case Failure(ex)  =>
        log.error("Error contacting Message Broker for user id:{}, phoneNumber :{}, Error : {} ", req.id,req.phoneNumber,ex.getMessage)
    }


  }

}

