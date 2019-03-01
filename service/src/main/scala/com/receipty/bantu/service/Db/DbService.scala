package com.receipty.bantu.service.Db

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging}

import com.receipty._
import com.receipty.bantu.core.db.mysql.mapper.ReceiptyMapper
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}


object DbService {
  case class AddUser(user: UserDbEntry)
  case class AddUserResponse(status : Boolean , msg : String)
  case class GetUserId(phoneNumber : String)
  case class GetUserIdResponse(status : Boolean, id : Int)

  case class AddItems(items : List[ItemDbEntry])
  case class AddItemsResponse(status : Boolean , msg : String)
}

class DbService extends Actor with ActorLogging{

  import DbService._
  def receive = {
    case req: AddUser =>
      val currentSender = sender()
      ReceiptyMapper.insertUserIntoDb(req.user) onComplete {
        case Success(qr) => if (qr.rowsAffected > 0) {
          currentSender ! AddUserResponse(
            status   = true,
            msg      = qr.statusMessage
          )
        } else {
          currentSender ! AddUserResponse(
            status = false,
            msg    = qr.statusMessage
          )
        }
        case Failure(ex) =>
          currentSender ! AddUserResponse(
            status = false,
            msg    = ex.getMessage)
      }

    case req : AddItems =>
      val currentSender = sender()
      ReceiptyMapper.addItemsIntoDb(req.items) onComplete{
        case Success(qr) => if (qr.rowsAffected > 0) {
          currentSender ! AddItemsResponse(
            status   = true,
            msg      = qr.statusMessage
          )
        } else {
          currentSender ! AddItemsResponse(
            status = false,
            msg    = qr.statusMessage
          )
        }
        case Failure(ex) =>
          currentSender ! AddItemsResponse(
            status = false,
            msg    = ex.getMessage)
      }

    case req : GetUserId =>
      val currentSender = sender()
      ReceiptyMapper.findUserById(req.phoneNumber) onComplete{
        case Success(res) =>
          res match {
            case Some(payload) => currentSender ! GetUserIdResponse(
              status = true,
              id     = payload.id
            )
            case None           => currentSender ! GetUserIdResponse(
              status = false,
              id     = 0
            )
          }
        case Failure(ex) =>
          log.error(s"Error fetching user detail from Database. for phone number :{${req.phoneNumber}}, error : {}", ex.getMessage)
          currentSender ! GetUserIdResponse(
            status = false,
            id     = 0)
      }
  }

}

