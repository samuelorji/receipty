package com.receipty.bantu.service.Messaging

import akka.actor.Props
import akka.http.scaladsl.model.{FormData, HttpRequest, StatusCodes}
import com.receipty.bantu.service.Messaging.MessageGateway.{SendMessageToClient, SendMessageToClientResponse}
import com.receipty.bantu.service.test.TestServiceT
import com.receipty.bantu.service.util.{HttpClient, HttpClientResponse}

import scala.concurrent.Future

class MessageGatewaySpec extends TestServiceT with HttpClient{

  val validPhoneNumber   = "+2348020686607"
  val inValidPhoneNumber = "+23480290909090909090"
  val messageSentData    = "<AfricasTalkingResponse><SMSMessageData><Message>Sent to 1/1 Total Cost: NGN 1.5742</Message><Recipients><Recipient><number>+254706800434</number><cost>KES 0.8000</cost><status>Success</status><statusCode>101</statusCode><messageId>ATXid_40a5404d3601b05f329868e9ae4924c2</messageId></Recipient></Recipients></SMSMessageData></AfricasTalkingResponse>"
  val messageNotSentData = "<AfricasTalkingResponse><SMSMessageData><Message>Sent to 1/1 Total Cost: NGN 1.5742</Message><Recipients><Recipient><number>+254706800434</number><cost>KES 0.8000</cost><status>Success</status><statusCode>301</statusCode><messageId>ATXid_40a5404d3601b05f329868e9ae4924c2</messageId></Recipient></Recipients></SMSMessageData></AfricasTalkingResponse>"

  override def makeHttpRequest(request: HttpRequest): Future[HttpClientResponse] = {
    val req = request.entity.asInstanceOf[FormData]
    val phoneNumber = req.fields(1)
    phoneNumber._2 match {
      case x if x == validPhoneNumber => Future.successful {
        HttpClientResponse(
          status = StatusCodes.OK,
          data = messageSentData
        )
      }
      case x if x == inValidPhoneNumber => Future.successful {
        HttpClientResponse(
          status = StatusCodes.OK,
          data = messageNotSentData
        )
      }

      case _ => Future.failed(new Exception("Error accessing broker to send message"))
    }
  }

  val gateway = system.actorOf(Props[MessageGateway])

  "The Messaging Gateway Actor " must {
    "Send a message with a valid Phone Number " in {
      gateway ! SendMessageToClient(
        id          = 5,
        phoneNumber = validPhoneNumber,
        msg         = "Successful"
      )
      val result = expectMsgClass(classOf[SendMessageToClientResponse])

      result.status shouldBe(true)

    }
    "Fail while sending a message with an invalid Phone Number " in {
        gateway ! SendMessageToClient(
          id          = 5,
          phoneNumber = inValidPhoneNumber,
          msg         = "Failure"
        )
        val result = expectMsgClass(classOf[SendMessageToClientResponse])

        result.status shouldBe(false)

      }

    "Give an error when the phone Number is not regiostered..simulating not contacting the broker" in {
      gateway ! SendMessageToClient(
        id          = 5,
        phoneNumber = "hello",
        msg         = "Failure"
      )
      val result = expectMsgClass(classOf[SendMessageToClientResponse])

      result.status shouldBe(false)
    }
  }

}
