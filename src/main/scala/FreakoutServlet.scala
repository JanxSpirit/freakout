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
  val usersColl = MongoConnection("localhost", 27017)("freakout")("users")
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
    val users = usersColl.map(u => {
      u.toString
    })
    """{"users":[""" + users.mkString(",") + "]}"
  }

  put("/users/:name", acceptJson(request)) {
    contentType = jsonType
    val userDbo = JSON.parse(request.body).asInstanceOf[DBObject]
    userDbo.put("fo_count", 0L: java.lang.Long)
    val res = usersColl.update(MongoDBObject("_id" -> params("name")), userDbo, true, false)
    if (res.getLastError().ok) {
      halt(201, "user %s created" format params("name"))
    } else {
      halt(400, "could not create user %s" format params("name"))
    }
    userDbo
  }

  get("/users/:name") {
    contentType = jsonType
    usersColl.findOne(MongoDBObject("_id" -> params("name"))) match {
      case Some(user) => user.toString()
      case None => halt(404, "%s was not found" format params("name"))
    }
    ()
  }

  post("/freakouts/:name") {
    contentType = jsonType
    val now = System.currentTimeMillis
    val idq = ("last" $lt (now - cooloffInMillis)) ++
      ("_id" -> params("name"))
    usersColl.findAndModify(idq,
      ($inc("fo_count" -> 1L) ++
        $set("last" -> now))) match {
        case Some(user) => logFreakout(params("name"), now)
        case _ => {
          usersColl.findOne(MongoDBObject("_id" -> params("name"))) match {
            case Some(user) => {
              user.getAs[Long]("fo_count") match {
                case Some(0L) => {
		  //user has never freaked out so this is legit
                  usersColl.findAndModify(MongoDBObject("_id" -> params("name")),
					  ($inc("fo_count" -> 1L) ++
					   $set("last" -> now)))
                  logFreakout(params("name"), now)
		} 
		case _ => halt(400, "%s has freaked out in the past %s millis"
		   .format(params("name"), cooloffInMillis))
              }
            }
            case _ => halt(400, "Who is %s and why are they freaking out???"
              .format(params("name")))
          }
        }
      }
  }

  def acceptJson(request: HttpServletRequest) = {
    request.getHeader("Content-Type").contains(jsonType) ||
      request.getHeader("Content-Type").contains("text/json")
  }

  def assertParameter(request: HttpServletRequest, params: String*) = {
    params.map(p => {
      request.get(p) match {
        case Some(v) => (p -> v)
        case None => halt(400, "%s is a required parameter" format p)
      }
    })
  }

  def logFreakout(name: String, now: Long) =
    freakoutsColl.insert(MongoDBObject(
      "u" -> name,
      "d" -> now))

  override def destroy = {
    ec.shutdown
  }

}

case class Freakout(d: Long)

case class User(_id: String, fullname: String, fc_total: Long, freakouts: List[Freakout])
