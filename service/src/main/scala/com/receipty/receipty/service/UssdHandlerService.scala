package com.receipty.receipty.service

import akka.actor.Actor

object USSDHandlerService {
  case class UserInput(
    input : String
   )
}

class USSDHandlerService extends Actor {

  import USSDHandlerService._
  def receive = {
    case req : UserInput =>
  }

}
