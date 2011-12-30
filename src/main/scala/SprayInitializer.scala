import org.slf4j.LoggerFactory
import akka.actor.Supervisor
import akka.config.Supervision
import Supervision._
import cc.spray.connectors.Initializer
import akka.actor.Actor._
import cc.spray.HttpService._
import com.mongodb.ServerAddress
import com.mongodb.casbah.MongoConnection
import cc.spray.{HttpService, RootService}
import akka.event.slf4j.Logging

class SprayInitializer extends Initializer with Logging {

  val akkaConfig = akka.config.Config.config

  val mongoUrl = akkaConfig.getString("mongodb.url", "localhost")
  val mongoDbName = akkaConfig.getString("mongodb.database", "freakout")

  val projectCollection = akkaConfig.getString("mycotrack.freakout.collection", "freakout")

  val urlList = mongoUrl.split(",").toList.map(new ServerAddress(_))
  val db = urlList match {
    case List(s) => MongoConnection(s)(mongoDbName)
    case s: List[String] => MongoConnection(s)(mongoDbName)
    case _ => MongoConnection("localhost")(mongoDbName)
  }

  // ///////////// INDEXES for collections go here (include all lookup fields)
  // configsCollection.ensureIndex(MongoDBObject("customerId" -> 1), "idx_customerId")

  val foService = actorOf(new HttpService(new FreakoutService{}.service))
  val rootService = actorOf(new RootService(foService))

  // Start all actors that need supervision, including the root service actor.
  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      List(
        Supervise(foService, Permanent),
        Supervise(rootService, Permanent)
      )
    )
  )
}

