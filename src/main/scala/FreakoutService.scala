import cc.spray._
import directives.Remaining
import http.StatusCodes
//import json._
import typeconversion.SprayJsonSupport
import http.MediaTypes._
import com.mongodb.casbah.Imports._
//import cc.spray.json._

case class Msg(content: String)

//object MsgJsonProtocol extends DefaultJsonProtocol {
//  implicit val msgFormat = jsonFormat(Msg, "content")
//}

trait MongoSupport {
  val akkaConfig = akka.config.Config.config
  val mongoHost = akkaConfig.getString("mongodb.host", "localhost")
  val mongoDb = akkaConfig.getString("mongodb.database", "freakout")
  val freakoutsColl = akkaConfig.getString("freakout.freakouts.collection", "freakouts")
  //val usersColl = akkaConfig.getString("freakout.users.collection", "users")
  val mongoPort = akkaConfig.getInt("mongodb.port", 27017)

  //val mFreakouts = MongoConnection(mongoHost, mongoPort)(mongoDb)(freakoutsColl)
  val mFreakouts = MongoConnection(mongoHost, mongoPort)(mongoDb)(freakoutsColl)

}

trait FreakoutService extends Directives
//with LiftJsonSupport
with MongoSupport {

  val cooloffInMillis = akkaConfig.getLong("freakout.cooloffInMillis", 120000L)

  println(cooloffInMillis)

//  import MsgJsonProtocol.msgFormat
//  import MsgJsonProtocol.listFormat

  val service = {
    path("users") {
      path("") {
        get {
          respondWithMediaType(`text/html`) {
            _.complete {
              <html>
                <head>
                  <title>Freakout Users</title>
                </head>
                <body>
                  <ul>
                    {for (user <- mFreakouts) yield <li>
                    <a href={"/users/%s" format user.get("_id")}>
                      {user.get("fullname")}
                    </a>
                  </li>}
                  </ul>
                  <form method="POST" action="/users">
                    Username:
                      <input type="text" name="username"/>
                      <br/>
                    Full Name:
                      <input type="text" name="fullname"/>
                      <input type="submit"/>
                  </form>
                </body>
              </html>
            }
          }
        } ~
          post {
            formFields('username, 'fullname) {
              (username, fullname) => ctx => {
                mFreakouts.findOne(MongoDBObject("_id" -> username)) match {
                  case Some(u) => {
                    ctx.fail(400, "user %s already exists".format(u.get("_id")))
                  }
                  case None => {
                    mFreakouts += MongoDBObject(
                      "_id" -> username,
                      "fullname" -> fullname,
                      "last" -> 0L,
                      "fc_total" -> 0L
                    )
                    ctx.redirect("users")
                  }
                }
              }
            }
          }
      } ~
        path(Remaining) {
          id =>
            get {
              respondWithMediaType(`text/html`) {
                _.complete {
                  val u = mFreakouts.findOne(MongoDBObject("_id" -> id))
                  <html>
                    <head>
                      <title>Freakouter -
                        {u.get("fullname")}
                      </title>
                    </head>
                    <body>
                      <p>
                        {u.get("fullname")}
                        has freaked out
                        {u.get("fc_total")}
                        times.</p>
                    </body>
                  </html>
                }
              }
            }
        }
    } ~
      path("freakouts") {
        path("") {
          get {
            respondWithMediaType(`text/html`) {
              ctx =>
                ctx.complete {
                  <html>
                    <head>
                      <title>Freakout Users</title>
                    </head>
                    <body>
                      <ul>
                        {for (user <- mFreakouts) yield <li>
                        <a href={"/users/%s" format user.get("_id")}>
                          {user.get("fullname")}
                        </a>
                        -
                        {user.get("fc_total")}
                        -
                        <form method="POST" action="/freakouts">
                            <input type="submit" value="+1 Freakout"/>
                            <input type="hidden" name="username" value={user.get("_id").toString}/>
                        </form>
                      </li>}
                      </ul>
                    </body>
                  </html>
                }
            }/* ~
              respondWithMediaType(`application/json`) {
                produce(instanceOf[List[User]]) {
                  mFreakouts.flatMap(dbo => {
                    for (
                      _id <- dbo.getAs[String]("_id");
                      fullname <- dbo.getAs[String]("fullname");
                      fc_count <- dbo.getAs[Long]("fc_count");
                      freakouts <- dbo.getAs[List[MongoDBObject]]("freakouts")
                    ) yield User(_id, fullname, fc_count, freakouts.flatMap(dbo => {
                      for (
                        d <- dbo.getAs[Long]("d")
                      ) yield Freakout(d)
                    }))
                  })
                }
              } */
          } ~
            post {
              formFields('username) {
                username => ctx =>
                  //val idq = MongoDBObject("_id" -> username)
                  //db.freakouts.find({_id:"toby",freakouts: {$not: {$elemMatch: {d: {$gt: 1325198942688}}}}}).pretty()
                  val now = System.currentTimeMillis
                  //val idq = ("freakouts" $elemMatch {"d" $not {_ $gt (now - cooloffInMillis)}}) ++
                  val idq = ("last" $lt (now - cooloffInMillis)) ++
                    ("_id" -> username)
                  val freak = $push("freakouts" -> MongoDBObject("d" -> now)) ++
                    $inc("fc_total" -> 1) ++
                    $set("last" ->  now)
                  mFreakouts.findAndModify(idq, freak) match {
                    case Some(x) => ctx.redirect("freakouts")
                    case _ => ctx.fail(400, "freakout logged within last %s millis" format cooloffInMillis)
                  }
                  //mFreakouts.update(idq, $push("freakouts" -> freak))
                  //mFreakouts.update(idq, $inc("fc_total" -> 1))

              }
            }

        }
      }
  }

  def msgFormBody =
    <html>
      <head>
        <title>Spray Mongo Example</title>
      </head>
      <body>
        <ul>
          {for (msg <- mFreakouts) yield <li>
          <a href={"/msg/%s" format msg.get("_id")}>
            {msg.get("content")}
          </a>
        </li>}
        </ul>
        <form method="POST" action="/msgs">
            <input type="text" name="content"/>
            <input type="submit"/>
        </form>
      </body>
    </html>
}

case class Freakout(d: Long)

case class User(_id: String, fullname: String, fc_total: Long, freakouts: List[Freakout])