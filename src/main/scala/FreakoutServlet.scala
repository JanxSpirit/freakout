import akka.actor._
import akka.dispatch._
import org.scalatra.akka.Akka2Support
import org.scalatra._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import java.util.concurrent.Executors
import javax.servlet.http.HttpServletRequest
import FreakoutFields._

class FreakoutServlet extends ScalatraServlet with Akka2Support {

  val jsonType = "application/json"

  val cooloffInMillis = 120000L

  def system = ActorSystem("freakout")

  implicit val ec =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  //setup Casbah connection
  val usersColl = MongoConnection("localhost", 27017)("freakout")("users")
  val freakoutsColl = MongoConnection("localhost", 27017)("freakout")("freakouts")
  val dodgesColl = MongoConnection("localhost", 27017)("freakout")("dodges")

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
    userDbo.put(FO_COUNT, 0L: java.lang.Long)
    userDbo.put(D_COUNT, 0L: java.lang.Long)
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
    val idq = ("last_fo" $lt (now - cooloffInMillis)) ++
      ("_id" -> params("name"))
    usersColl.findAndModify(idq,
      ($inc("fo_count" -> 1L) ++
        $set("last_fo" -> now))) match {
        case Some(user) => logFreakout(user, now)
        case _ => {
          usersColl.findOne(MongoDBObject("_id" -> params("name"))) match {
            case Some(user) => {
              user.getAs[Long]("fo_count") match {
                case Some(0L) => {
		  //user has never freaked out so this is legit
                  usersColl.findAndModify(MongoDBObject("_id" -> params("name")),
					  ($inc("fo_count" -> 1L) ++
					   $set("last_fo" -> now)))
                  logFreakout(user, now)
		} 
		case _ => halt(409, "%s has freaked out in the past %s millis"
		   .format(params("name"), cooloffInMillis))
              }
            }
            case _ => halt(404, "Who is %s and why are they freaking out???"
              .format(params("name")))
          }
        }
      }
  }

  get("/freakouts/mostRecent") {
    contentType = jsonType
    val fo_user = usersColl.find(MongoDBObject()).sort(MongoDBObject("last_fo" -> -1)).limit(1)
    if (fo_user.size == 1) {
      halt(200, fo_user.next().toString)
    } else {
      halt(404, "no one has ever freaked out")
    }
    ()
  } 

  post("/dodges/:name") {
    contentType = jsonType
    val now = System.currentTimeMillis
    val idq = (D_LAST $lt (now - cooloffInMillis)) ++
      ("_id" -> params("name"))
    usersColl.findAndModify(idq,
      ($inc(D_COUNT -> 1L) ++
        $set(D_LAST -> now))) match {
        case Some(user) => logDodge(user, now)
        case _ => {
          usersColl.findOne(MongoDBObject("_id" -> params("name"))) match {
            case Some(user) => {
              user.getAs[Long](D_COUNT) match {
                case Some(0L) => {
		  //user has never dodged so this is legit
                  usersColl.findAndModify(MongoDBObject("_id" -> params("name")),
					  ($inc(D_COUNT -> 1L) ++
					   $set(D_LAST -> now)))
                  logDodge(user, now)
		} 
		case _ => halt(409, "%s has dodged in the past %s millis"
		   .format(params("name"), cooloffInMillis))
              }
            }
            case _ => halt(404, "Who is %s and what are they dodging???"
              .format(params("name")))
          }
        }
      }
  }


  /**
   *   
   */

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

  def logFreakout(user: DBObject, now: Long) = {
    freakoutsColl.insert(MongoDBObject(
      "u" -> user.getAs[String]("_id"),
      "d" -> now))
    val c = user.getAs[Long](FO_COUNT).getOrElse(0L) + 1L
    user.put(FO_COUNT, c)
    halt(201, user.toString)
  }

  def logDodge(user: DBObject, now: Long) = {
    dodgesColl.insert(MongoDBObject(
      "u" -> user.getAs[String]("_id"),
      "d" -> now))
    val c = user.getAs[Long](D_COUNT).getOrElse(0L) + 1L
    user.put(D_COUNT, c)
    halt(201, user.toString)
  }

  override def destroy = {
    ec.shutdown
  }
}

case class Freakout(d: Long)

case class User(_id: String, fullname: String, fc_total: Long, freakouts: List[Freakout])

object FreakoutFields {
  val FO_COUNT = "fo_count"
  val D_COUNT = "d_count"
  val FO_LAST = "fo_last"
  val D_LAST = "d_last"
  
}
