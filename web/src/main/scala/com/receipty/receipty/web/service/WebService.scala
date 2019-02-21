package com.receipty.receipty.web
package service

import scala.concurrent.duration._

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{extractRequest, formFields, path, post, _}
import akka.pattern.ask
import akka.util.Timeout

import com.receipty.receipty.service.USSDHandlerService
import com.receipty.receipty.service.USSDHandlerService.UssdRequest


trait ReceiptyWebServiceT {

  implicit val timeout = Timeout(20 seconds)

  implicit def actorRefFactory: ActorSystem

  private val ussdService = createUssdService
  def createUssdService   = actorRefFactory.actorOf(Props[USSDHandlerService])

  lazy val routes = {
    path("ussd" / "callback") {
      post {

        logRequest("ussd:callback", Logging.InfoLevel) {
          extractRequest { _: HttpRequest =>
            formFields('sessionId, 'phoneNumber, 'text) { (sessionid, phoneNumber, input) =>
              //there should be a way to persist the session Id to the database for ech session Id
              complete((createUssdService ? UssdRequest(
                phoneNumber = phoneNumber.trim,
                input       = input
              )).mapTo[String])
            }
          }
        }
      }
    }
  }
}
