name := "akka-quick-start"

version := "0.1"

scalaVersion := "2.12.12"

//akka版本
lazy val akkaVersion = "2.6.10"

//依赖
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)