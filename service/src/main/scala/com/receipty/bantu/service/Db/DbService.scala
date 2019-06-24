package com.receipty.bantu.service.Db

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging}

import com.receipty._
import com.receipty.bantu.core.db.mysql.mapper.ReceiptyMapper
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, Sale, UserDbEntry}
import com.receipty.bantu.core.message.action.MessageParser


object DbService {
  case class AddUserRequest(user: UserDbEntry)
  case class AddUserResponse(status : Boolean , msg : String)
   
  case class GetUserIdRequest(phoneNumber : String)
  case class GetUserIdResponse(status : Boolean, id : Int)

  case class SellItemsRequest(sale : Sale)
  case class SellItemResponse(status : Boolean , msg : String)

  case class AddItemsRequest(items : List[ItemDbEntry])
  case class AddItemsResponse(status : Boolean , msg : String)

  case class DeleteItemsRequest(items : List[ItemDbEntry])
  case class DeleteItemsResponse(status : Boolean , msg : String)



}

class DbService extends Actor with ActorLogging{

  import DbService._
  def receive = {
    case req: AddUserRequest =>
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

    case req : AddItemsRequest =>
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

    case req : DeleteItemsRequest =>
      val currentSender = sender()
      ReceiptyMapper.deleteItemsFromDb(req.items) onComplete{
        case Success(qr) => if (qr.rowsAffected > 0) {
          currentSender ! DeleteItemsResponse(
            status   = true,
            msg      = qr.statusMessage
          )
        } else {
          currentSender ! DeleteItemsResponse(
            status = false,
            msg    = qr.statusMessage
          )
        }
        case Failure(ex) =>
          currentSender ! DeleteItemsResponse(
            status = false,
            msg    = ex.getMessage)
      }

    case req : SellItemsRequest =>
      val currentSender = sender()

      ReceiptyMapper.addToSaleTable(req.sale.total, req.sale.phone, req.sale.userId) onComplete {
        case Success(qr) => if (qr.rowsAffected > 0) {
         //now add to the order table
          ReceiptyMapper.findSaleId(req.sale.userId) onComplete {
            case Success(sid) =>
              sid match {
                case Some(value) =>
                  ReceiptyMapper.addToOrderTable(value , req.sale.items) onComplete {
                    case Success(ord)  =>
                      if(ord.rowsAffected > 0){
                        currentSender ! SellItemResponse(
                          status = true,
                          msg    = "")
                      }else{
                        currentSender ! SellItemResponse(
                          status = false,
                          msg    = "UnSuccessful Bulk insert into Order table")
                      }
                    case Failure(ex)    =>
                      currentSender ! SellItemResponse(
                        status = false,
                        msg    = ex.getMessage)

                  }
                case None        =>
                  currentSender ! SellItemResponse(
                    status = false,
                    msg    = s"Cannot Get SID for user  with user id : [${req.sale.userId}]")
              }

            case Failure(ex)  =>
              currentSender ! SellItemResponse(
                status = false,
                msg    = ex.getMessage)

          }
        } else {
          currentSender ! SellItemResponse(
            status = false,
            msg    = qr.statusMessage
          )
        }
        case Failure(ex) =>
          currentSender ! SellItemResponse(
            status = false,
            msg    = ex.getMessage)
      }

    case req : GetUserIdRequest =>
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

