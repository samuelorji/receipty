package com.receipty.receipty.web
package service

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.util.Timeout
import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{extractRequest, formFields, path, post}
import akka.http.scaladsl.server.Directives._


trait ReceiptyWebServiceT{

  implicit val timeout   = Timeout(20 seconds)
  implicit def actorRefFactory : ActorSystem

  lazy val routes = {
    path("ussd" / "callback"){
      post{

      logRequest("ussd:callback",Logging.InfoLevel) {
        extractRequest{ _: HttpRequest =>
          formFields('sessionId,'phoneNumber ,'serviceCode ,'text) { (sessionId,phoneNumber,serviceCode,input) =>

            complete("Hello")

          }

        }
      }
      }
    }
  }



}
