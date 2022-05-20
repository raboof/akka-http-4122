scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"
val ScalaTestVersion = "3.2.10"

lazy val globalResources  = file("resources")
Compile / resourceDirectory := globalResources

fork := true
//javaOptions += "-Djavax.net.debug=all"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.10"
) ++ Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
).map(_.cross(CrossVersion.for3Use2_13))

lazy val `integration-tests` = project
  .in(file("./integration-tests"))
  .disablePlugins(RevolverPlugin)
  .settings(
    ThisBuild / parallelExecution := false,
    Compile / unmanagedResourceDirectories += globalResources,
    useCoursier := false
  )
  .settings(
    libraryDependencies ++= Seq(
      // Bouncy Castle cryptographic library
     // "org.bouncycastle" % "bcprov-jdk15on" % BouncyCastleVersion % Test,
     // "org.bouncycastle" % "bcpkix-jdk15on" % BouncyCastleVersion % Test,
      "org.scalatest"    %% "scalatest" %  ScalaTestVersion % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Test
    )
  )
