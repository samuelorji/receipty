package com.receipty.bantu.service.Ussd

import java.security.MessageDigest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorLogging, Props}
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.mauricio.async.db.mysql.message.client.SendLongDataMessage
import com.receipty._
import com.receipty.bantu.core.config.ReceiptyConfig
import com.receipty.bantu.core.db.mysql.cache.{ItemDbCache, UserDbCache}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.{ItemDbEntry, UserDbEntry}
import com.receipty.bantu.service.Db.DbService
import com.receipty.bantu.service.Db.DbService.{AddUser, AddUserResponse}
import com.receipty.bantu.service.Messaging.MessagingService
import com.receipty.bantu.service.Messaging.MessagingService.{SendCustomMessage, SendRegistrationMessage}
import com.receipty.bantu.service.util.ReceiptyUtils


object UssdService {
  case class UssdRequest(
    sessionID: String,
    phoneNumber: String,
    input: String
  )
}

class UssdService extends Actor with ActorLogging {
  // this is the service that will check if the user exist or not and handle user registration

  private val countyMap =
    Map[String, List[String]](
      "Nairobi" -> List("Nairobi"), "Central" -> List("Kiambu", "Nyeri", "Muranga", "Muranga", "Nyandarua"),
      "Eastern" -> List("Marsabit", "Isiolo", "Meru", "Tharaka Nithi", "Embu", "Kitui", "Machakos", "Makueni"),
      "Rift Valley" -> List("West Pokot", "Samburu", "Transzoia", "Uasin Gishu", "Elgeyo", "Nandi", "Baringo", "Laikipia", "Nakuru", "Narok", "Kajiado", "Kericho", "Bomet"),
      "Nyanza" -> List("Siaya", "Kisumu", "Homabay", "Migori", "Kisii", "Nyamira"),
      "Western" -> List("Kakamega", "Vihiga", "Busia", "Bungoma"),
      "Coast" -> List("Mombasa", "Kwale", "Kilifi", "Tana River", "Taita Taveta", "Lamu"),
      "North Eastern" -> List("Garissa", "Wajir", "Mandera"))

  private val provinceMap =
    Map[Int, String](1 -> "Nairobi", 2 -> "Central", 3 -> "Eastern", 4 -> "Rift Valley", 5 -> "Nyanza", 6 -> "Western", 7 -> "Coast", 8 -> "North Eastern")


  private def showCounty(selection: Int): String = {
    def prefix(prov: String) = s"Please Select Your County under ${prov}\n"

    selection match {
      case 1 =>
        val counties = countyMap(provinceMap(1))
        prefix(provinceMap(1)) + s"1.) ${counties(0)}"

      case 2 =>
        val counties = countyMap(provinceMap(2))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(2)) + string

      case 3 =>
        val counties = countyMap(provinceMap(3))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(3)) + string

      case 4 =>
        val counties = countyMap(provinceMap(4))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(4)) + string


      case 5 =>
        val counties = countyMap(provinceMap(5))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(5)) + string


      case 6 =>
        val counties = countyMap(provinceMap(6))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(6)) + string

      case 7 =>
        val counties = countyMap(provinceMap(7))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(7)) + string

      case 8 =>
        val counties = countyMap(provinceMap(8))
        val string = createCountyMenu(counties)._1
        prefix(provinceMap(8)) + string
    }
  }

  private def createCountyMenu(counties: List[String]) = {

    counties.foldLeft(("", 1)) {
      case ((str, ind), county) =>
        if (ind < counties.length) {
          (str + s"${ind}.) ${county} \n", ind + 1)
        } else {
          (str + s"${ind}.) ${county} ", ind)
        }

    }
  }

  import UssdService._

  def showCounties(provinceNum: Int): String = {
    provinceNum match {
      case 1 =>
        //selected Nairobi
        "CON " + showCounty(1)
      case 2 =>
        "CON " + showCounty(2)
      case 3 =>
        "CON " + showCounty(3)
      case 4 =>
        "CON " + showCounty(4)
      case 5 =>
        "CON " + showCounty(5)
      case 6 =>
        "CON " + showCounty(6)
      case 7 =>
        "CON " + showCounty(7)
      case 8 =>
        "CON " + showCounty(8)
    }
  }

  val provinceSelection =
    s"1.) ${provinceMap(1)}\n" +
      s"2.) ${provinceMap(2)}\n" +
      s"3.) ${provinceMap(3)}\n" +
      s"4.) ${provinceMap(4)}\n" +
      s"5.) ${provinceMap(5)}\n" +
      s"6.) ${provinceMap(6)}\n" +
      s"7.) ${provinceMap(7)}\n" +
      s"8.) ${provinceMap(8)}"

  val dbService        = createDbService
  def createDbService  = context.actorOf(Props[DbService])

  val messagingService        = createMessagingService
  def createMessagingService  = context.actorOf(Props[MessagingService])

  implicit val timeout = Timeout(5 seconds)

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
    case req: UssdRequest =>
      val currentSender = sender()
      val userExist     = UserDbCache.checkIfUserExixsts(req.phoneNumber)

      userExist match {
        case Some(user) =>
          //User wants to make a sale
          val userItems = ItemDbCache.getUserItems(user.id)
          println(userItems)
          if(req.input.length < 1){
            val response = s"CON 1) Send Receipt\n2) View All Items\n3) Add Item\n 4) Account  "
            currentSender ! response
          }else{
            val entries = req.input.split('*')
            entries.length match {
              case 1 =>
                val firstEntry = entries(0)
                try{
                  firstEntry.toInt match {
                    case 1 =>
                      if(userItems.isEmpty){
                       currentSender ! "END No items Added, Please check your phone for Information regarding adding Items"
                        val msg      = s"Hi there, Your user Id is ${user.id}, To add items, Life sucks "
                        messagingService ! SendCustomMessage(
                          msg   = msg,
                          phone = user.phoneNumber
                        )
                      }else {
                        val prefix = s"CON Please Select the Items Sold separated by a a comma ','\n"
                        currentSender ! (prefix + showItemList(userItems)._1)
                      }
                    case 2 =>
                        currentSender ! s"END Items : \n" +  showItemList(userItems)._1
                    case 3 =>
                      //user wants to add items
                      if(userItems.length >= ReceiptyConfig.maxItemsCount){
                        currentSender ! s"END Limit for Number of items to Add reached (${ReceiptyConfig.maxItemsCount})"
                      }else{
                        val msg = s"Hello user ${user.id}, to Add items please send so and so to 3340"
                        messagingService ! SendCustomMessage(
                          msg   = msg ,
                          phone = user.phoneNumber
                        )
                        currentSender ! s"END Please check your inbox for a detailed message on how to add items"
                      }

                  }
                }catch {
                  case ex : NumberFormatException =>
                    val response = s"END Invalid Entry. please use numbers "
                    currentSender ! response

                }
              case 2 =>
                //user probably entered 1* or 2*
                val secondEntryString = entries(1)
                try{
                  val userItems = ItemDbCache.getUserItems(user.id)
                  val firstEntry = entries(0)
                  firstEntry.toInt match {
                    case 1 =>
                      //user has typed in sale separated by a comma
                      if(!secondEntryString.contains(',')){
                        val errorMessage = "END Item entries entered are not separated by commas ','"
                        currentSender ! errorMessage
                      }else {
                        val itemsNumList = secondEntryString.split(",").map(_.toInt)
                        if (itemsNumList.max > ReceiptyConfig.maxItemsCount) {
                          val msg = "END Entry out of bounds, Maximum entry is 10"
                          currentSender ! msg
                        } else {
                          currentSender ! s"CON Please enter the total amount of all products sold "
                        }
                      }
                    case 4 =>
                      currentSender ! "END Accounts stuff"

                    case _ =>
                      currentSender ! "END Invalid Entry "
                  }
                }catch {
                  case ex : NumberFormatException =>
                    val errorMessage = s"END, Invalid Entry \nPlease enter the item Numbers separated by a comma ','"
                    currentSender ! errorMessage

                }

              case 3 =>
                //here 3rd entry is total amount

                try{
                  entries(2).toFloat
                  currentSender ! s"CON Please enter phone Number of Customer "
                }catch {
                  case ex : NumberFormatException =>
                    val response = s"END Invalid Entry. please use numbers "
                    currentSender ! response

                }

              case 4 =>

                try{
                  val phoneNumberString = entries(3)
                  if(phoneNumberString.startsWith("+")){
                    //international format
                    phoneNumberString.substring(1).toLong
                  }else{
                    //regular format
                    phoneNumberString .toLong
                  }
                  currentSender ! s"CON Please enter your password to authenticate this transaction"
                }catch {
                  case ex : NumberFormatException =>
                    val response = s"END Invalid Entry. please use numbers "
                    currentSender ! response

                }

              case 5 =>

                try{
                  val password = entries(4).toInt
                  val phoneNumberString = entries(3)
                  val totalAmount = entries(2).toDouble
                  var phoneNumber = ""
                  if(phoneNumberString.startsWith("+")){
                    //international format
                    phoneNumberString.substring(1).toLong
                    phoneNumber = phoneNumberString
                  }else{
                    //regular format
                    phoneNumberString .toLong
                    phoneNumber = phoneNumberString.replaceFirst("0","+254")
                  }
                  val secondEntryString = entries(1)
                  val itemsNumList = secondEntryString.split(",").map(_.toInt)
                  val itemsToSell = userItems.foldLeft((List[ItemDbEntry](), 1)) {
                    case ((itms, ind), entry) =>
                      if (itemsNumList.contains(ind)) {
                        (itms :+ entry, ind + 1)
                      } else {
                        (itms, ind + 1)
                      }
                  }._1
                  if(ReceiptyUtils.comparePasswords(
                    password       = password,
                    hashedPassword = user.password
                  )){
                    val msg  = s"Summary of items: Item number ${itemsToSell.length}\n ${showItemList(itemsToSell)._1}"+
                    s"\ntotal price is ${totalAmount}\n Customer Number is ${phoneNumber}"
                    currentSender ! s"END ${msg}"

                  }else{
                    currentSender ! "END Invalid password "

                  }

                }catch {
                  case ex : NumberFormatException =>
                    val response = s"END Invalid Entry\nPhone Number is invalid "
                    currentSender ! response

                }




            }
          }



        case None =>
          //  New User
          if (req.input.length < 1) {
            //here the app just started so user input is 0
            val response = s"CON Welcome to Receipty\nTo continue with registration..\nPlease Select a province...\n ${provinceSelection}"
            currentSender ! response
          } else {
            //Add User
            val entries = req.input.split('*')
            entries.length match {
              case 1 =>
                val provinceEntry = entries(0)
                try {
                  val provinceNum = provinceEntry.toInt
                  if (provinceNum <= 8 && provinceNum != 0) {
                    val response = showCounties(provinceNum)
                    currentSender ! response
                  } else {
                    val errorMsg = s"Invalid Entry $provinceEntry"
                    val response = s"END $errorMsg "
                    currentSender ! response
                  }
                } catch {
                  case _: NumberFormatException =>
                    val response = s"END Invalid Entry. please use numbers "
                    currentSender ! response

                }

              case 2 =>
                try {
                  val countyEntry = entries(1).toInt
                  val provinceEntry = entries(0).toInt
                  if (countyEntry > countyMap(provinceMap(provinceEntry)).length || countyEntry == 0) {
                    val errorMsg = s"Invalid Entry $countyEntry"
                    val response = s"END Invalid Entry $errorMsg"
                    currentSender ! response
                  } else {
                    val selectionPrefix = "CON Please Enter a 4 Digit Pin"
                    val response = selectionPrefix
                    currentSender ! response
                  }
                }
                catch {
                  case _: NumberFormatException =>
                    val errorMsg = s"Invalid Entry"
                    val response = s"END $errorMsg\n Please use Numbers "
                    currentSender ! response

                }

              case 3 =>
                try {
                  entries(2).toInt
                  if (entries(2).length == 4) {
                    val response = "CON Please Confirm Password"
                    currentSender ! response
                  } else {
                    val errorMsg = s"Password should be 4 digits"

                    val response = s"END Invalid Entry \n $errorMsg "
                    currentSender ! response

                  }

                } catch {
                  case _: NumberFormatException =>
                    val errorMsg = s"Please use Numbers"
                    val response = s"END Invalid Entry\n $errorMsg "
                    currentSender ! response

                }

              case 4 =>
                val password = entries(3)
                if (password == entries(2)) {
                  //here we hash the users password and then input into database
                  val pin = ReceiptyUtils.hashPassword(password)
                  val user = UserDbEntry(
                    phoneNumber = req.phoneNumber,
                    province    = entries(0).toInt,
                    county      = entries(1).toInt,
                    password    = pin
                  )

                  (dbService ? AddUser(user)).mapTo[AddUserResponse] onComplete {
                    case Success(res) => res match {
                      case AddUserResponse(true, _) =>
                        log.info("Successful registration for sessionId:{}, phoneNumber:{}, input:{}", req.sessionID, req.phoneNumber, req.input)
                        val response = "END Registration Successful \nPlease Check your Messages for user Details "
                        messagingService ! SendRegistrationMessage(
                          phoneNumber = req.phoneNumber,
                          sessionId   = req.sessionID
                        )
                        currentSender ! response
                      case AddUserResponse(false, msg) =>
                        println(msg)
                        log.error("UnSuccessful registration for sessionId:{}, phoneNumber:{}, input:{}, Error : {} ", req.sessionID, req.phoneNumber, req.input, msg)
                        val response = "END Registration Unsuccessful \n Please try registering again  "
                        currentSender ! response
                    }
                    case Failure(ex) =>
                      println(ex)
                      log.error("UnSuccessful registration for sessionId:{}, phoneNumber:{}, input:{}, Error : {} ", req.sessionID, req.phoneNumber, req.input, ex.getMessage)
                      val response = "END Registration Unsuccessful \n Please try registering again  "
                      currentSender ! response

                  }
                }
                else if (password.length != 4) {
                  val response = "END Password should be 4 digits"
                  currentSender ! response
                }
                else {
                  val response = "END Passwords do not match "
                  currentSender ! response

                }
            }
          }
      }
  }
}


