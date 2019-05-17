package com.receipty.bantu.service
package test

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.receipty.bantu.core.db.mysql.cache.{ItemDbCache, UserDbCache}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


abstract class TestServiceT extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  def initializeCache = {
    system.actorOf(UserDbCache.props)
    system.actorOf(ItemDbCache.props)
  }
  initializeCache
  override def beforeAll {
    Thread.sleep(2000)
  }

  override def afterAll {
    Thread.sleep(2000)
    TestKit.shutdownActorSystem(system)
  }
}
