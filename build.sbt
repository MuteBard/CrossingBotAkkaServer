name := "CBAS"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.1"
val akkaHttpVersion = "10.1.11"
val scalaTestVersion = "3.1.0"
lazy val mongodbVersion = "1.1.2"
lazy val mongoDriverVersion = "2.9.0"
lazy val akkaCorsVersion = "0.4.2"

libraryDependencies ++= Seq(

  //MongoDB
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.0-RC2",
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoDriverVersion,
  "ch.megard" %% "akka-http-cors" % akkaCorsVersion,

  //Akka actors
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  //Akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  //Akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

  //GraphQL
  "dev.zio" %% "zio" % "1.0.0-RC18-2",
  "com.github.ghostdogpr" %% "caliban" % "0.7.5",
  "com.github.ghostdogpr" %% "caliban-akka-http" % "0.7.5",
  "de.heikoseeberger"     %% "akka-http-circe" % "1.31.0"



)
