package com.receipty.bantu.core
package message.action

import scala.util.Try

import akka.actor.Actor

import com.receipty.bantu.core.config.ReceiptyConfig
import message.action.MessageAction
import com.receipty.bantu.core.message.action.MessageParser.{ParseMessageRequest, ParseMessageResponse}
import db.mysql.service.MysqlDbService.{Data, ItemDbEntry, Sale, UserDbEntry}

class MessageParserException(action : MessageAction.Value, message : String) extends RuntimeException(message) {
  def getAction = action
}
object MessageParser {

  type Items = List[ItemDbEntry]
  case class ParseMessageRequest(msg : String , userItems : List[ItemDbEntry],user : UserDbEntry)
  case class ParseMessageResponse(action : Option[MessageAction.Value] , result: List[Data]  , error : Option[String] = None)
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
  private def parseDouble(num : String) : Option[Double] = {
    Try(num.toDouble).toOption
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
              println("I am in Add ")
              val numItemsLength = entries.tail.length
              if(numItemsLength < ReceiptyConfig.maxItemsCount) {
                if (req.userItems.length + numItemsLength < ReceiptyConfig.maxItemsCount) {

                  val aliasPattern = """\([a-zA-Z0-9_-[\s]]+\)""".r
                  val itemPattern  = """\#[a-zA-Z0-9_-[\s]]+\(""".r

                  val itemListMatched  = itemPattern.findAllMatchIn(req.msg).toList.map(_.toString).map(_.replace("#","").replace("(",""))
                  val aliasListMatched = aliasPattern.findAllMatchIn(req.msg).toList.map(_.toString).map(_.replace("(","").replace(")",""))

                  itemListMatched zip aliasListMatched match {
                    case list  if list.nonEmpty =>
                      if(list.map(_._1.length).max < ReceiptyConfig.itemMaxCharacterCount
                        && list.map(_._2.length).max < ReceiptyConfig.itemAliasMaxCharacterCount
                        && itemListMatched.length == aliasListMatched.length ){
                        val items = list.foldLeft(List.empty[ItemDbEntry]) {
                          case (l, entry) =>
                            if (entry._1.toLowerCase.contains("add") || entry._1.toLowerCase.contains("help")) {
                              l
                            } else {
                              val item = ItemDbEntry(
                                id          = 0,
                                description = entry._1.trim.toLowerCase,
                                alias       = entry._2.trim.toLowerCase,
                                owner       = req.user.id,
                                added       = ""
                              )
                              l :+ item
                            }
                        }
                        currentSender ! ParseMessageResponse(Some(MessageAction.Add),items,None)
                      }else{
                        throw new MessageParserException(MessageAction.Add,s"Either item character length is longer than ${ReceiptyConfig.itemMaxCharacterCount}" +
                          s" or alias character length of an item  is longer than ${ReceiptyConfig.itemAliasMaxCharacterCount}" +
                          s" or the number of aliases do not match the number of Items")
                      }
                    case _                      =>
                      throw new MessageParserException(MessageAction.Sell,"Invalid Items format, please type add#[item name of 30 characters(item alias of 10 characters)]")
                  }
                }else{
                  throw new MessageParserException(MessageAction.Sell,s"ERROR:\nItem Limit is ${ReceiptyConfig.maxItemsCount}, you already have ${req.userItems.length} items in store, you can only add ${ReceiptyConfig.maxItemsCount - req.userItems.length}")
                }
              }else{
                throw new MessageParserException(MessageAction.Sell,s"Invalid Entry, Maximum number of items to be added is ${ReceiptyConfig.maxItemsCount}")

              }
            case MessageAction.Sell =>
              val itemEntry = entries(1)
              !req.userItems.isEmpty match {
                case true  =>
                  itemEntry.forall(x => {x.isDigit || x == ','}) match {
                    case true  =>
                      val itemsNumList = itemEntry.split(",").map(_.toInt)
                      !(itemsNumList.max > req.userItems.length) match {
                        case true  =>
                          val amount = entries(2)

                          parseDouble(amount) match {
                            case Some(amnt) =>
                              val number = entries(3)
                              (number.forall(_.isDigit) && number.length == 10) match {
                                case true =>
                                  val userItemsToSell = getUserItemsFromInput(req.userItems, itemsNumList.toList)
                                  val sale = Sale(
                                    total = amnt,
                                    phone = number,
                                    items = userItemsToSell,
                                    userId = req.user.id
                                  )
                                  currentSender ! ParseMessageResponse(Some(MessageAction.Sell), List(sale), None)

                                case false =>
                                  currentSender ! ParseMessageResponse(Some(MessageAction.Sell),List(),Some(s"Invalid Entry, expected a valid Customer phone number but got ${number} instead"))
                              }

                            case None      =>
                              currentSender ! ParseMessageResponse(Some(MessageAction.Sell),List(),Some(s"Invalid Entry, expected a Number but got ${amount} instead"))
                          }
                        case false =>
                          throw new MessageParserException(MessageAction.Sell,s"Invalid Entry, item number selected has exceeded maximum value of items of ${req.userItems.length}")
                      }

                    case false =>
                      throw new MessageParserException(MessageAction.Sell,"Invalid Entry, expecting only Numbers separated by a ',' for list of items")
                  }
                case false => throw new MessageParserException(MessageAction.Sell,"No Items in Store to Sell")
              }


            case MessageAction.List =>
              currentSender ! ParseMessageResponse(Some(MessageAction.List),List(),None)
          }

          case None =>
            currentSender ! ParseMessageResponse(None,List(),Some("Invalid Message Action"))
        }
      }catch {
        case ex : MessageParserException =>
          currentSender ! ParseMessageResponse(Some(ex.getAction),List(),Some(ex.getMessage))
      }
  }
}
