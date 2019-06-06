package com.receipty.bantu.core
package message.action

import akka.actor.Actor

import message.action.MessageAction
import com.receipty.bantu.core.message.action.MessageParser.{ParseMessageRequest, ParseMessageResponse}
import db.mysql.service.MysqlDbService.{Data, ItemDbEntry, Sale, UserDbEntry}

class MessageParserException(action : MessageAction.Value, message : String) extends RuntimeException(message) {
  def getAction = action
}
object MessageParser {

  type Items = List[ItemDbEntry]
  case class ParseMessageRequest(msg : String , userItems : List[ItemDbEntry],user : UserDbEntry)
  case class ParseMessageResponse(action : Option[MessageAction.Value] , result: Option[List[Data]] = None , error : Option[String] = None)
}
class MessageParser extends Actor {


  private def getUserItemsFromInput(userItems : List[ItemDbEntry], itemsNumList : List[Int]) = {
    userItems.foldLeft((List[ItemDbEntry](), 1)) {
      case ((itms, ind), entry) =>
        if (itemsNumList.contains(ind)) {
          (itms :+ entry, ind + 1)
        } else {
          (itms, ind + 1)
        }
    }._1
  }

  def receive = {

    case req : ParseMessageRequest =>
      val currentSender = sender()
      //format for making a sale via message is sell#1,2,3,4#560.0#08112172624
      //we have confirmed that we want to sell
      try {
        val entries = req.msg.split('#')
        val action  = entries(0)
        MessageAction.getAction(action) match {
          case Some(res) => res match {
            case MessageAction.Add =>

            case MessageAction.Sell =>
              val itemEntry = entries(1)
              itemEntry.forall(x => {x.isDigit || x == ','}) match {
                case true  =>
                  val itemsNumList = itemEntry.split(",").map(_.toInt)
                  !(itemsNumList.max > req.userItems.length) match {
                    case true  =>
                      val amount = entries(2)
                      try {
                        amount.toDouble
                        val number = entries(3)
                        number.forall(_.isDigit) match {
                          case true  =>
                            val userItemsToSell = getUserItemsFromInput(req.userItems,itemsNumList.toList)
                            val sale = Sale(
                              total  = amount.toDouble,
                              phone  = number,
                              items  = userItemsToSell,
                              userId = req.user.id
                            )
                            currentSender ! ParseMessageResponse(Some(MessageAction.Sell),Some(List(sale)),None)
                          case false =>
                            currentSender ! ParseMessageResponse(Some(MessageAction.Sell),None,Some(s"Invalid Entry, expected a phone number but got ${number} instead"))
                        }
                      }catch {
                        case e : NumberFormatException =>
                          currentSender ! ParseMessageResponse(Some(MessageAction.Sell),None,Some(s"Invalid Entry, expected a Number but got ${amount} instead"))
                      }


                    case false =>
                      throw new MessageParserException(MessageAction.Sell,s"Invalid Entry, item number selected has exceeded maximum value of items of ${req.userItems.length}")
                  }

                case false =>
                  throw new MessageParserException(MessageAction.Sell,"Invalid Entry, expecting only Numbers separated by a ',' for list of items")
              }
          }
          case None =>
        }
      }catch {
        case ex : MessageParserException =>
          currentSender ! ParseMessageResponse(Some(ex.getAction),None,Some(ex.getMessage))
      }
  }
}
