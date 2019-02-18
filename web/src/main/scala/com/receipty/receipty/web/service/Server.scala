package com.receipty.receipty.web
package service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import com.receipty._

import receipty.core.config.ReceiptyConfig
import receipty.core.db.mysql.cache.{ItemDbCache, UserDbCache}

object Server extends App {

  implicit val system       = ActorSystem("Receipty")
  implicit val materializer = ActorMaterializer()

  initializeCache
  Http().bindAndHandle(
    new ReceiptyWebServiceT {
      override def actorRefFactory: ActorSystem = system
    }.routes,
    ReceiptyConfig.host, ReceiptyConfig.port)

  def initializeCache = {
    system.actorOf(UserDbCache.props)
    system.actorOf(ItemDbCache.props)
  }


}
