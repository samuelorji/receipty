package com.receipty.bantu.web.service

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{extractRequest, formFields, path, post, _}
import akka.pattern.ask
import akka.util.Timeout
import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.service.Messaging.MessagingService
import com.receipty.bantu.service.Messaging.MessagingService.CustomerMessage
import com.receipty.bantu.service.Ussd.UssdService
import com.receipty.bantu.service.Ussd.UssdService.UssdRequest

import scala.concurrent.duration._


trait ReceiptyWebServiceT {

  implicit val timeout = Timeout(20 seconds)

  implicit def actorRefFactory: ActorSystem

  private val ussdService = createUssdService
  def createUssdService   = actorRefFactory.actorOf(Props[UssdService])

  private val messagingService = createMessagingService
  def createMessagingService        = actorRefFactory.actorOf(Props[MessagingService])


  lazy val routes = {
    path("ussd" / "callback") {
      post {
        logRequest("ussd:callback", Logging.InfoLevel) {
          extractRequest { _: HttpRequest =>
            formFields('sessionId, 'phoneNumber, 'text) { (sessionid, phoneNumber, input) =>
              //there should be a way to persist the session Id to the database for ech session Id
              complete((
                ussdService ? UssdRequest(
                  sessionID   = sessionid,
                  phoneNumber = phoneNumber.trim,
                  input       = input
              )).mapTo[String])
            }
          }
        }
      }
    } ~ {
      path("messaging" / "callback"){
        post {
          logRequest("messaging:callback",Logging.InfoLevel){
            extractRequest { _: HttpRequest => {
              formFields('from , 'text) {(phoneNumber, text) =>
                messagingService ! CustomerMessage(
                  msg   = text,
                  phone = phoneNumber
                )
                complete(StatusCodes.OK)
              }
            }
            }
          }
        }
      }
    }
  }
}