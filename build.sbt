name := "CBAS"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(JavaAppPackaging)
mainClass in Compile := Some("Controller.Main")

val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.12"
val scalaTestVersion = "3.1.0"
lazy val mongodbVersion = "1.1.2"
lazy val mongoDriverVersion = "2.9.0"
lazy val akkaCorsVersion = "0.4.2"

libraryDependencies ++= Seq(

  //MongoDB
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.0-RC2",
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoDriverVersion,
  "ch.megard" %% "akka-http-cors" % akkaCorsVersion,

  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

  "com.typesafe.akka" %% "akka-slf4j" % "2.6.1",
  "org.scala-lang" % "scala-library" % "2.13.1",
  "org.slf4j" % "slf4j-simple" % "1.7.25",


//GraphQL
  "dev.zio" %% "zio" % "1.0.0-RC18-2",
  "com.github.ghostdogpr" %% "caliban" % "0.7.5",
  "com.github.ghostdogpr" %% "caliban-akka-http" % "0.7.5",
  "de.heikoseeberger"     %% "akka-http-circe" % "1.31.0"

)



