package com.receipty.bantu.service

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor

import com.receipty._

import bantu.core.db.mysql.mapper.ReceiptyMapper
import bantu.core.db.mysql.service.MysqlDbService.UserDbEntry


object DbService {
  case class AddUser(user: UserDbEntry)
  case class AddUserResponse(status : Boolean , msg : String)
}

class DbService extends Actor {

  import DbService._

  def receive = {
    case req: AddUser =>
      val currentSender = sender()
      ReceiptyMapper.insertUserIntoDb(req.user) onComplete {
        case Success(qr) => if (qr.rowsAffected > 0) {

          currentSender ! AddUserResponse(true, qr.statusMessage)
        } else {

          currentSender ! AddUserResponse(false, qr.statusMessage)

        }
        case Failure(ex) =>
          currentSender ! AddUserResponse(false, ex.getMessage)
      }


  }

}

