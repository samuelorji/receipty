package com.receipty.bantu.service.Messaging

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout

import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.cache.{ItemDbCache, ItemDbCacheT, UserDbCache, UserDbCacheT}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import com.receipty.bantu.core.message.action.MessageParser
import com.receipty.bantu.core.message.action.MessageParser.{ParseMessageRequest, ParseMessageResponse}
import com.receipty.bantu.core.utils.ReceiptyCoreUtils
import com.receipty.bantu.service.Db.DbService
import com.receipty.bantu.service.Db.DbService.{AddItemsRequest, AddItemsResponse, GetUserIdRequest, GetUserIdResponse}
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageRequest, SendMessageResponse}



object MessagingService {
  case class SendRegistrationMessage(
    sessionId: String ,
    phoneNumber : String,
    user : UserDbEntry
  )
  case class SendRegistrationMessageResponse(status : Boolean)
  case class CustomerMessage(msg: String,phone : String)
  case class SendCustomMessageRequest(id : Int, msg: String, phone : String)
  case class SendCustomMessageResponse(status : Boolean)
}

class MessagingService extends Actor with ActorLogging{
  //this class should get the phone number of a registered user , query the database for the user's Id
  //and then send they user a welcome message showing the format to add items


  private val dbService   = createDbService
  def createDbService     = context.actorOf(Props[DbService])

  private val messageGateway  = createMessageGateway
  def createMessageGateway    = context.actorOf(Props[MessageGateway])

  private val messageParser   = createMessageParser
  def createMessageParser     = context.actorOf(Props[MessageParser])

  private val userDbCache : UserDbCacheT = getUserDbCache
  def getUserDbCache      : UserDbCacheT = UserDbCache

  private val itemDbCache : ItemDbCacheT  = getItemDbCache
  def getItemDbCache      : ItemDbCacheT  = ItemDbCache

  implicit val timeout = Timeout(5 seconds)

  import MessagingService._
  private def sendMessage(message : String, id : Int, pNumber : String) = {
    (messageGateway ? SendMessageRequest(
      id = id,
      recepient = pNumber,
      msg = message
    )).mapTo[SendMessageResponse]
  }

  private def showItemList(entries: List[MysqlDbService.ItemDbEntry]) = {
    entries.foldLeft(("",1)){
      case((str, ind),en) =>
        if(ind < entries.length){
          (str + s"${ind}.) ${en.description} \n", ind +1)
        }else{
          (str + s"${ind}.) ${en.description} \n", ind)
        }
    }
  }


  def receive = {
    case req: SendRegistrationMessage =>
      val currentSender =  sender()
      //first find user Id from
      log.info(s"processing request $req")
      (dbService ? GetUserIdRequest(
        phoneNumber  = req.phoneNumber
      )).mapTo[GetUserIdResponse] onComplete{
        case Success(res) => res match {
          case GetUserIdResponse(true, id) =>
            if(id != 0){
              //now send user message
              val msg      = s"Hello ${req.user.businessName},Welcome To Receipty, Your User ID is ${id}\nYour Nature of business is ${ReceiptyCoreUtils.natureOfBusinessMap(req.user.natureOfBusiness)}" +
                s"\nTo add items, please send ADD and the items no more than 20 characters separated by the symbol '#' " +
                s"along with a 8 letter alias to show up in your USSD portal example ADD#Nyama Choma(nya-chom)#Rice and stew (Rice-stew)." +
                s"Please ensure that the items do not exceed 10 "
              sendMessage(
                message = msg,
                id      = id,
                pNumber = req.phoneNumber).mapTo[SendMessageResponse] map {
                case SendMessageResponse(true)  =>
                  currentSender ! SendRegistrationMessageResponse(true)
                case SendMessageResponse(false) =>
                  currentSender ! SendRegistrationMessageResponse(false)
              }
            }else{
              currentSender ! SendRegistrationMessageResponse(false)
              log.error(s"Could not fetch data for user with phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
            }
          case GetUserIdResponse(false,_) =>
            currentSender ! SendRegistrationMessageResponse(false)
            log.error(s"Could not fetch data for user: phone :{},sessionId:{}",req.phoneNumber, req.sessionId)
        }
        case Failure(ex) =>
          currentSender ! SendRegistrationMessageResponse(false)
          log.error("Error Finding user info for user with phoneNumber : {}, sessionId : {}, Error : {}",req.phoneNumber,req.sessionId,ex.getMessage)
      }

    case req : SendCustomMessageRequest =>
      val currentSender =  sender()
      sendMessage(
        message = req.msg,
        id      = req.id,
        pNumber = req.phone
      ) map {
        case SendMessageResponse(true)  =>
          currentSender ! SendCustomMessageResponse(true)
        case SendMessageResponse(false) =>
          currentSender ! SendCustomMessageResponse(false)
      }

    case req : CustomerMessage =>
      //TODO ...this should contain detail from registered user for adding items or any other complaint
      //TODO a good format should be either ADD# or HELP#  or any prompt with a # separator

    val separator = '#'
      val entries = req.msg.split(separator)

      import com.receipty.bantu.core.message.action.MessageAction._
      entries(0).toLowerCase match {
        case "add" =>
          //now add items from entries(1) to end
          val userexists = userDbCache.checkIfUserExists(req.phone)
          userexists match {
            case Some(user) =>
              val userItems = itemDbCache.getUserItems(user.id)
//              if(numItemsLength <= ReceiptyConfig.maxItemsCount) {
//                if (userItems.length + numItemsLength > ReceiptyConfig.maxItemsCount) {
//                  val errorMsg = s"ERROR:\nItem Limit is ${ReceiptyConfig.maxItemsCount}, you already have ${userItems.length} items in store, you can only add ${ReceiptyConfig.maxItemsCount - userItems.length}"
//                  sendMessage(
//                    message = errorMsg,
//                    id      = user.id,
//                    pNumber = user.phoneNumber
//                  )
//                } else {
//                  if (entries.tail.map(_.length).max > 20) {
//                    val errMsg = "Length of An Item to Add is greater than 20"
//                    messageGateway ! SendMessageToClient(
//                      phoneNumber     = req.phone,
//                      msg             = errMsg,
//                      id              = user.id,
//                    )
//                  } else {
//                    val items = entries.foldLeft(List.empty[ItemDbEntry]) {
//                      case (list, entry) =>
//                        if (entry.toLowerCase.contains("add") || entry.toLowerCase.contains("help")) {
//                          list
//                        } else {
//                          val item = ItemDbEntry(
//                            id = 0,
//                            description = entry.trim.toLowerCase,
//                            owner = user.id,
//                            added = ""
//                          )
//                          list :+ item
//                        }
//                    }
              (messageParser ? ParseMessageRequest(req.msg,userItems,user)).mapTo[ParseMessageResponse] onComplete  {
                case Success(res) =>
                  res match {
                    case ParseMessageResponse(Some(Add),items,None)   =>
                      val itemsToSave = items.asInstanceOf[List[ItemDbEntry]]
                      (dbService ? AddItemsRequest(itemsToSave)).mapTo[AddItemsResponse] onComplete {
                        case Success(res) => res match {
                          case AddItemsResponse(true, _) =>
                            //TODO ....send message to user that he/she has successfully added items
                            log.info(s"Successfully added items message to user : phone :{} ", req.phone)
                            val msg = s"Succesfully added ${itemsToSave.length} items\n Items are: \n${showItemList(itemsToSave)._1}"
                            sendMessage(
                              id = user.id,
                              pNumber = user.phoneNumber,
                              message = msg
                            )
                          case AddItemsResponse(false, err) =>
                            val errorMsg = s"ERROR:\nCould not add items, please retry"
                            sendMessage(
                              id = user.id,
                              pNumber = user.phoneNumber,
                              message = errorMsg
                            )
                            log.error(s"Could Not Successfully add items for user : phone :{} error :{}", req.phone, err)
                        }
                        case Failure(ex) =>
                          val errorMsg = s"ERROR:\nCould not add items, please retry"
                          sendMessage(
                            id = user.id,
                            pNumber = user.phoneNumber,
                            message = errorMsg
                          )
                          log.error(s"Could Not Successfully added items for user : phone :{} error  :{}", req.phone, ex.getMessage)

                      }

                    case ParseMessageResponse(Some(Add),_,Some(errMsg))   =>
                      messageGateway ! SendMessageRequest(
                        id          = user.id,
                        msg         = errMsg,
                        recepient   = req.phone
                      )

                    case ParseMessageResponse(Some(Sell),sale,None)       =>

                    case ParseMessageResponse(Some(Sell),_,Some(errMsg))  =>
                      messageGateway ! SendMessageRequest(
                        id          = user.id,
                        msg         = errMsg,
                        recepient   = req.phone
                      )

                    case ParseMessageResponse(Some(List),_,_)             =>

                      val userItemString = s"Your Items in store are\n${showItemList(userItems)._1}"
                      messageGateway ! SendMessageRequest(
                        id          = user.id,
                        msg         = userItemString,
                        recepient   = req.phone
                      )

                    case ParseMessageResponse(None,_,Some(errMsg))  =>
                      messageGateway ! SendMessageRequest(
                        id          = user.id,
                        msg         = errMsg,
                        recepient   = req.phone
                      )
                  }


                case Failure(ex)  =>
              }

//                    (dbService ? AddItemsRequest(items)).mapTo[AddItemsResponse] onComplete {
//                      case Success(res) => res match {
//                        case AddItemsResponse(true, _) =>
//                          //TODO ....send message to user that he/she has successfully added items
//                          log.info(s"Successfully added items message to user : phone :{} ", req.phone)
//                          val msg = s"Succesfully added $numItemsLength items\n Items are: \n${showItemList(items)._1}"
//                          sendMessage(
//                            id = user.id,
//                            pNumber = user.phoneNumber,
//                            message = msg
//                          )
//                        case AddItemsResponse(false, err) =>
//                          val errorMsg = s"ERROR:\nCould not add items, please retry"
//                          sendMessage(
//                            id = user.id,
//                            pNumber = user.phoneNumber,
//                            message = errorMsg
//                          )
//                          log.error(s"Could Not Successfully add items for user : phone :{} error :{}", req.phone, err)
//                      }
//                      case Failure(ex) =>
//                        val errorMsg = s"ERROR:\nCould not add items, please retry"
//                        sendMessage(
//                          id = user.id,
//                          pNumber = user.phoneNumber,
//                          message = errorMsg
//                        )
//                        log.error(s"Could Not Successfully added items for user : phone :{} error  :{}", req.phone, ex.getMessage)
            case None  =>
              val errMsg = s"ERROR:\nHello , You Have not been registered "
              messageGateway ! SendMessageRequest(
                recepient = req.phone,
                msg       = errMsg,
                id        = 0,
              )
          }


        case "help" =>
          //handle help cases

        case _ => //TODO ...send message to user that entry is incorrect and they should follow protocol
          val errorMsg = "Unable to recognise Message, please use  ADD# or HELP# in for add or help cases"
          messageGateway ! SendMessageRequest(
            recepient     = req.phone,
            msg             = errorMsg,
            id              = 0,
          )
      }

  }
}
