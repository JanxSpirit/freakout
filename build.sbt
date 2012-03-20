import AssemblyKeys._

import com.typesafe.startscript.StartScriptPlugin

name := "freakout"

version := "1.0"

scalaVersion := "2.9.1"

mainClass := Some("JettyLauncher")

seq(webSettings :_*)

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

port in container.Configuration := 8080

seq(assemblySettings: _*)

scalacOptions := Seq("-deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
  "se.scalablesolutions.akka" % "akka-actor" % "1.2" % "compile",
  "cc.spray" % "spray-server" % "0.9.0-RC1" % "compile",
  "cc.spray.can" % "spray-can" % "0.9.2-SNAPSHOT" % "compile",
  "net.liftweb" %% "lift-json" % "2.4",
  "net.liftweb" %% "lift-json-ext" % "2.4",
  "org.specs2" %% "specs2" % "1.6.1"  % "test",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.v20120127" % "container",
  "se.scalablesolutions.akka" % "akka-slf4j" % "1.2",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "ch.qos.logback" % "logback-classic" % "0.9.29"
  )

resolvers ++= Seq(
  "Akka Repository" at "http://akka.io/repository/",
  "Web plugin repo" at "http://siasia.github.com/maven2",
  ScalaToolsSnapshots,
  "Spray Repo" at "http://repo.spray.cc/"
)

