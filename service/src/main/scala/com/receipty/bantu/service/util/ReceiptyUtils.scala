package com.receipty.bantu.service.util

import java.security.MessageDigest

object ReceiptyUtils {

  def hashPassword(password : String) =  MessageDigest.getInstance("SHA-256").digest(password.getBytes).map("%02x".format(_)).mkString

  def comparePasswords(password : Int, hashedPassword : String) = hashedPassword == hashPassword(password.toString)

}
