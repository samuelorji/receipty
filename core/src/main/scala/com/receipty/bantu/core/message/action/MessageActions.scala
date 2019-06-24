package com.receipty.bantu.core.message.action

object MessageAction extends Enumeration {
  val List,Sell,Add = Value

  def getAction(action : String) : Option[Value] = {
    action.toString.toLowerCase match {
      case "add"  => Some(Add)
      case "list" => Some(List)
      case "sell" => Some(Sell)
      case _      => None
    }

  }
}
