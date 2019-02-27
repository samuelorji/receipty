package com.receipty.bantu.service

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor

import com.receipty._

import bantu.core.db.mysql.mapper.ReceiptyMapper
import bantu.core.db.mysql.service.MysqlDbService.UserDbEntry


object DbService {
  case class AddUser(user: UserDbEntry)
}

class DbService extends Actor {

  import DbService._

  def receive = {
    case req: AddUser =>
      val currentSender = sender()
      ReceiptyMapper.insertUserIntoDb(req.user) onComplete {
        case Success(qr) => if (qr.rowsAffected > 0) {
          currentSender ! true
        } else {

          currentSender ! false
        }
        case Failure(ex) =>
          println(ex)
          currentSender ! false
      }


  }

}

