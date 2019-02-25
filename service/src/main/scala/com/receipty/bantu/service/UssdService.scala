package com.receipty.bantu.service

import java.security.MessageDigest

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.Actor

import com.receipty._
import com.receipty.bantu.core.db.mysql.mapper.ReceiptyMapper
import bantu.core.db.mysql.cache.{ItemDbCache, UserDbCache}
import com.receipty.bantu.core.db.mysql.service.MysqlDbService.UserDbEntry

object UssdService {
  case class UssdRequest(
    phoneNumber: String,
    input: String
  )
}

class UssdService extends Actor {
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

  private var response = ""

  private def showCounty(selection: Int): String = {
    def prefix(prov: String) = s"Please Select Your County under ${prov}\n"

    selection match {
      case 1 =>
        val counties = countyMap(provinceMap(1))
        response = prefix(provinceMap(1)) + s"1.) ${counties(0)}"
        response
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

  def receive = {
    case req: UssdRequest =>
      val currentSender = sender()

      val userExist = UserDbCache.checkIfUserExixsts(req.phoneNumber.substring(1))

      userExist match {
        case Some(user) =>
          //User wants to make a sale
          val userItems = ItemDbCache.getUserItems(user.id)
          response = "END You have been registered"
        case None =>
          //New User
          if (req.input.length < 1) {
            //here the app just started so user input is 0
            response = s"CON Welcome to Receipty\nTo continue with registration..\nPlease Select a province...\n ${provinceSelection}"
          } else {

            val entries = req.input.split('*')

            //Add User
            entries.length match {
              case 1 => //the user just entered province number we show him county menu
                val provinceEntry = entries(0)
                try {
                  val provinceNum = provinceEntry.toInt
                  if (provinceNum <= 8 && provinceNum != 0) {
                    response = showCounties(provinceNum)
                  } else {
                    val errorMsg = s"Invalid Entry $provinceEntry"
                    response = s"END $errorMsg "
                  }
                } catch {
                  case _: NumberFormatException =>
                    response  = s"END Invalid Entry. please user numbers "
                }

              case 2 =>
                try {
                  val countyEntry = entries(1).toInt
                  val provinceEntry = entries(0).toInt
                  if (countyEntry > countyMap(provinceMap(provinceEntry)).length || countyEntry == 0) {
                    val errorMsg = s"Invalid Entry $countyEntry"
                    response     = s"END Invalid Entry $errorMsg"
                  } else {
                    val selectionPrefix = "CON Please Enter a 4 Digit Pin"
                    response            = selectionPrefix
                  }
                }
                catch {
                  case _: NumberFormatException =>
                    val errorMsg = s"Invalid Entry"
                    response     = s"END $errorMsg\n Please use Numbers "
                }

              case 3 =>
                try {
                  entries(2).toInt
                  if (entries(2).length == 4) {
                    response = "CON Please Confirm Password"
                  } else {
                    val errorMsg = s"Password should be 4 digits"
                    response     = s"END Invalid Entry \n $errorMsg "
                  }

                } catch {
                  case _: NumberFormatException =>
                    val errorMsg = s"Please use Numbers"
                    response     = s"END Invalid Entry\n $errorMsg "
                }

              case 4 =>
                val password = entries(3)
                if (password == entries(2)) {
                  //here we hash the users password and then input into database
                  val pin = MessageDigest.getInstance("SHA-256").digest(password.getBytes).map("%02x".format(_)).mkString
                  val user = UserDbEntry(
                    phoneNumber = req.phoneNumber,
                    province = entries(0).toInt,
                    county = entries(1).toInt,
                    password = pin
                  )
                  //create Messaging service here
                  val res = Await.result(ReceiptyMapper.insertUserIntoDb(user), 5 seconds)
                  if (res.rowsAffected > 0) {
                    response = "END Registration Successful \nPlease Check your Messages for user Details "
                  } else {
                    response = "END Registration Unsuccessful \n Please try registering again  "
                  }
                } else if (password.length != 4) {
                  response = "END Password should be 4 digits"
                }
                else {
                  response = "END Passwords do not match "
                }
            }
          }

      }

      currentSender ! response
  }

}

