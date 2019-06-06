package com.receipty.bantu.service.util

import scala.xml.{XML}

object GatewayXMLParser {

  private def toXML(input : String) = {
    XML.loadString(input)
  }

  def getMessageStatusCode(data : String) = {
    (toXML(data)\ "SMSMessageData" \"Recipients" \ "Recipient"\ "statusCode" ).text
  }

}
