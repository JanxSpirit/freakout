import akka.config.Supervision._
import akka.actor.{Supervisor, Actor}
import akka.actor.Actor._
import cc.spray._
import can.{HttpServer, ServerConfig}

class Boot extends App {
  
  val mainModule = new FreakoutService {
    // bake your module cake here
  }

  val host = "0.0.0.0"
  val port = Option(System.getenv("PORT")).getOrElse("8080").toInt

  val httpService = actorOf(new HttpService(mainModule.service))
  val rootService = actorOf(new RootService(httpService))
  val sprayCanServer = actorOf(new HttpServer(new ServerConfig(host = host, port = port)))

  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      List(
        Supervise(httpService, Permanent),
        Supervise(rootService, Permanent),
	Supervise(sprayCanServer, Permanent)
      )
    )
  )
}
