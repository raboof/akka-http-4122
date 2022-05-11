scalaVersion := "3.2.0-RC1-bin-20220511-7c446ce-NIGHTLY"

val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.10"
) ++ Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
).map(_.cross(CrossVersion.for3Use2_13))
