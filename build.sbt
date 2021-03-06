import AssemblyKeys._
import com.typesafe.startscript.StartScriptPlugin
    
seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "freakout"

version := "1.0"

scalaVersion := "2.9.1"

mainClass := Some("JettyLauncher")

seq(webSettings :_*)

port in container.Configuration := 8080

seq(assemblySettings: _*)

libraryDependencies ++= Seq(
  "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
  "org.scalatra" %% "scalatra" % "2.1.0-SNAPSHOT",
  "org.scalatra" %% "scalatra-akka2" % "2.1.0-SNAPSHOT",
  "org.scalatra" %% "scalatra-specs2" % "2.1.0-SNAPSHOT" % "test",
  "org.mortbay.jetty" % "servlet-api" % "3.0.20100224" % "provided",
  "org.eclipse.jetty" % "jetty-server" % "8.0.0.M3" % "container, compile",
  "org.eclipse.jetty" % "jetty-util" % "8.0.0.M3" % "container, compile",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M3" % "container, compile",
   "com.codahale" %% "jerkson" % "0.5.0"
  )

resolvers ++= Seq(
  "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Akka Repo" at "http://akka.io/repository/",
  "Web plugin repo" at "http://siasia.github.com/maven2",
  "repo.codahale.com" at "http://repo.codahale.com"
)
