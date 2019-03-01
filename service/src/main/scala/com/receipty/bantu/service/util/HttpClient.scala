package com.receipty.bantu.service.util

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCode}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer


case class HttpClientResponse(
 status : StatusCode,
 data : String
 )
trait HttpClient{

  implicit  val system : ActorSystem
  final implicit lazy val materializer = ActorMaterializer()

  def makeHttpRequest(request : HttpRequest) : Future[HttpClientResponse] = {
    for {
      res <- Http(system).singleRequest(request)
      data <- Unmarshal(res).to[String]
    }yield HttpClientResponse(
      status = res.status,
      data   = data)
  }
}

