package com.receipty.receipty.core.db
package mysql.cache.InnerWorkings

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable }
import akka.util.Timeout
import akka.pattern.ask

trait MySqlDbCacheEntry

case object UpdateCacheRequestImpl

trait MySqlDbCacheManagerT[DbEntry <: MySqlDbCacheEntry]{

  private var entries: Option[List[DbEntry]] = None
  def setEntries(x : List[DbEntry]): Unit ={
    entries = Some(x)
  }
}

trait MySqlDbCache[DbEntry <: MySqlDbCacheEntry] extends Actor
  with ActorLogging {

  private case object UpdateCacheRequest

  implicit val timeout = Timeout(1 minute)

  val mysqlDbService = createMySqlDbService
  def createMySqlDbService : ActorRef

  protected val updateFrequency: FiniteDuration

  override def preStart(): Unit = {
    self ! UpdateCacheRequest
  }

  protected val manager : MySqlDbCacheManagerT[DbEntry]
  private var currentScheduler : Cancellable = null

  protected def specificReceive : Receive

  protected def genericReceive : Receive = {
    case UpdateCacheRequest =>
      scheduleUpdate
      val updateFut = (self ? UpdateCacheRequestImpl).mapTo[List[DbEntry]]
      updateFut onComplete{
        case Success(entries) => manager.setEntries(entries)
        case Failure(ex) => log.error(s"Some error occurred fetching the data , exception : $ex")

      }

  }

  override def postStop(): Unit = {
    if(currentScheduler != null && !currentScheduler.isCancelled) currentScheduler.cancel()
  }

  override def receive: Receive = specificReceive orElse genericReceive

  private def scheduleUpdate {
    currentScheduler = context.system.scheduler.scheduleOnce(
      updateFrequency,
      self,
      UpdateCacheRequest
    )
  }


}