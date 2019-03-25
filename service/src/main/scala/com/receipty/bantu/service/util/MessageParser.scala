package com.receipty.bantu.service.util

import scala.xml.{XML}

object MessageParser {

  private def toXML(input : String) = {
    XML.loadString(input)
  }

  def getMessageStatusCode(data : String) = {
    (toXML(data)\ "SMSMessageData" \"Recipients" \ "Recipient"\ "statusCode" ).text
  }

}
