package com.receipty.bantu.service.Db

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.Actor

import com.receipty._
import com.receipty.bantu.core.db.mysql.mapper.ReceiptyMapper
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.UserDbEntry


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

