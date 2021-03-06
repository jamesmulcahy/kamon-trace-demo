import akka.actor.ActorSystem
import akka.http.server._
import akka.stream.ActorFlowMaterializer
import akka.http.Http
import akka.http.server.Directives._
import kamon.Kamon
import kamon.trace.Tracer

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  Kamon.start()

  implicit val system = ActorSystem("demo")
  implicit lazy val executor : ExecutionContextExecutor = system.dispatcher
  implicit lazy val materializer = ActorFlowMaterializer()
  val log = system.log
  val config = system.settings.config


  def traceName(name: String): Directive0 = mapRequest { req ⇒
    Tracer.withNewContext(name) {
      // Here we manually fetch and dump the current context.
      log.error("Log message dumping trace context" + Tracer.currentContext + ": "+ Tracer.currentContext.token  + " Thread:" + Thread.currentThread().getName)
      req
    }
  }

  def handleRequest() = {
    // This log entry should have a trace context, but instead we see [undefined] in the output
    log.info("Handling Request....")
    "OK"
  }

  val routes = {
    traceName("APIRequest") {
      path("demo") {
        complete {
          handleRequest
        }
      }
    }
  }

  Http().bindAndHandle(routes, interface = config.getString("http.interface"), port = config.getInt("http.port"))
}
