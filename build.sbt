scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"

Compile / resourceDirectory := baseDirectory.value / "resources"

fork := true
//javaOptions += "-Djavax.net.debug=all"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.10"
) ++ Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
).map(_.cross(CrossVersion.for3Use2_13))
