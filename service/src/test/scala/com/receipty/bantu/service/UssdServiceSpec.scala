//
////package com.receipty.bantu.service
////
////import akka.actor.Props
////
////import com.receipty._
////import com.receipty.bantu.core.db.mysql.cache.UserDbCache
////import com.receipty.bantu.service.test.TestServiceT
////import bantu.service.UssdService.UssdRequest
////
////class UssdServiceSpec extends TestServiceT{
////
////  system.actorOf(UserDbCache.props)
////
////  val ussdService = system.actorOf(Props[UssdService])
////  val unregisteredNumber = "+2349090900909"
////
////  "the ussd service " should {
////    "Register a new user not stored in cache " in {
////      ussdService ! UssdRequest(
////        phoneNumber = unregisteredNumber,
////        input       = ""
////      )
////      val response = expectMsgType[String]
////      assert(response.contains("CON Welcome to Receipty\nTo continue with registration..\nPlease Select a province"))
////    }
////  }
////
////  "End the USSD session when a user enters a a non digit when selecting a province " in {
////    ussdService ! UssdRequest(
////      phoneNumber = unregisteredNumber,
////      input       = "q"
////    )
////    expectMsg("END Invalid Entry\n please use Numbers")
////  }
////  "End the USSD session when a user enters a digit not shown when selecting a province " in {
////    ussdService ! UssdRequest(
////      phoneNumber = unregisteredNumber,
////      input       = "100"
////    )
////    expectMsg("END Invalid Entry 100")
////  }
////  "End the USSD session when a user enters a non digit when selecting a county " in {
////    ussdService ! UssdRequest(
////      phoneNumber = unregisteredNumber,
////      input       = "1*q"
////    )
////    expectMsg("END Invalid Entry\n Please use Numbers")
////  }
////
////  "End the USSD Session if the passwords do not match " in {
////    ussdService ! UssdRequest(
////      phoneNumber = unregisteredNumber,
////      input       = "1*1*1234*2345"
////    )
////    expectMsg("END Passwords do not match")
////  }
////  "End the USSD Session if the password is more than 4 digits " in {
////    ussdService ! UssdRequest(
////      phoneNumber = unregisteredNumber,
////      input       = "1*1*12345"
////    )
////    expectMsg("END Invalid Entry \n Password should be 4 digits")
////  }
////
////}
//=======
//package com.receipty.bantu.service
//
//import akka.actor.Props
//
//import com.receipty._
//import com.receipty.bantu.core.db.mysql.cache.UserDbCache
//import com.receipty.bantu.service.test.TestServiceT
//import bantu.service.UssdService.UssdRequest
//import com.receipty.bantu.service.Ussd.UssdService
//
//class UssdServiceSpec extends TestServiceT{
//
//  system.actorOf(UserDbCache.props)
//
//  val ussdService = system.actorOf(Props[UssdService])
//  val unregisteredNumber = "+23490909009"
//
//  "the ussd service " should {
//    "Register a new user not stored in cache " in {
//      ussdService ! UssdRequest(
//        phoneNumber = unregisteredNumber,
//        input       = ""
//      )
//      val response = expectMsgType[String]
//      assert(response.contains("CON Welcome to Receipty\nTo continue with registration..\nPlease Select your province...\n"))
//    }
//  }
//
//  "End the USSD session when a user enters a a non digit when selecting a province " in {
//    ussdService ! UssdRequest(
//      phoneNumber = unregisteredNumber,
//      input       = "q"
//    )
//    expectMsg("END Invalid Entry, Please use Numbers")
//  }
//  "End the USSD session when a user enters a digit not shown when selecting a province " in {
//    ussdService ! UssdRequest(
//      phoneNumber = unregisteredNumber,
//      input       = "100"
//    )
//    expectMsg("END Invalid Entry 100")
//  }
//  "End the USSD session when a user enters a non digit when selecting a county " in {
//    ussdService ! UssdRequest(
//      phoneNumber = unregisteredNumber,
//      input       = "1*q"
//    )
//    expectMsg("END Invalid Entry, Please use Numbers")
//  }
//
//  "End the USSD Session if the passwords do not match " in {
//    ussdService ! UssdRequest(
//      phoneNumber = unregisteredNumber,
//      input       = "1*1*1234*2345"
//    )
//    expectMsg("END Passwords do not match ")
//  }
//  "End the USSD Session if the password is more than 4 digits " in {
//    ussdService ! UssdRequest(
//      phoneNumber = unregisteredNumber,
//      input       = "1*1*12345"
//    )
//    expectMsg("END Password should be 4 digits")
//  }
//
//}
//>>>>>>> test
