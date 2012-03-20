import akka.actor._
import akka.dispatch._
import org.scalatra.akka.Akka2Support
import org.scalatra._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletRequest

class FreakoutServlet extends ScalatraServlet with Akka2Support {

  val jsonType = "application/json"

  val cooloffInMillis = 120000L

  def system = ActorSystem("freakout")

  implicit val ec =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  //setup Casbah connection
  val freakoutsColl = MongoConnection("localhost", 27017)("freakout")("freakouts")

  get("/test") {
    <h1>Test resource</h1>
  }

  get("/theFuture") {
    Future {
      Thread.sleep(1000)
      <h1>Welcome to the future</h1>
    }
  }

  get("/users") {
    contentType = jsonType
    val users = freakoutsColl.map(u => {
      u.toString
    })
    """{"users":[""" + users.mkString(",") + "]}"
  }

  put("/users/:name", acceptJson(request)) {
    contentType = jsonType
    freakoutsColl.update(MongoDBObject("_id" -> params("name")), JSON.parse(request.body).asInstanceOf[DBObject], true, false)
    ()
  }

  get("/users/:name") {
    contentType = jsonType
    freakoutsColl.findOne(MongoDBObject("_id" -> params("name"))) match {
      case Some(user) => user.toString()
      case None => halt(404, "$s was not found" format params("name"))
    }
    ()
  }

  post("/freakouts/:name") {
    contentType = jsonType
    val now = System.currentTimeMillis
    val idq = ("last" $lt (now - cooloffInMillis)) ++
      ("_id" -> params("name"))
    val freak = $push("freakouts" -> MongoDBObject("d" -> now)) ++
      $inc("fc_total" -> 1) ++
      $set("last" -> now)
    freakoutsColl.findAndModify(idq, freak) match {
      case Some(x) => {
        val upd = freakoutsColl.findOne(MongoDBObject("_id" -> params("name"))) match {
	  case Some(user) => halt(201, user.toString)
	  case _ => halt(400, "Who is that and why are they freaking out???")
	}
      }
      case _ => halt(400, "freakout logged within last %s millis" format cooloffInMillis)
    }
  }

  def acceptJson(request: HttpServletRequest) = {
    request.getHeader("Accept").contains(jsonType) ||
      request.getHeader("Accept").contains("text/json")
  }

  def assertParameter(request: HttpServletRequest, params: String*) = {
    params.map(p => {
      request.get(p) match {
        case Some(v) => (p -> v)
        case None => halt(400, "%s is a required parameter" format p)
      }
    })
  }

  override def destroy = {
    ec.shutdown
  }

}

case class Freakout(d: Long)

case class User(_id: String, fullname: String, fc_total: Long, freakouts: List[Freakout])
