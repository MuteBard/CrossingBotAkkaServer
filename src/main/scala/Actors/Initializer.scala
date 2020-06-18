package Actors

import akka.actor.{ActorRef, ActorSystem, Props}

object Initializer{
	implicit val system: ActorSystem = ActorSystem("CBAS")
	val bugActor: ActorRef = system.actorOf(Props[BugActor], "BugActor")
	val fishActor: ActorRef = system.actorOf(Props[FishActor], "FishActor")
	val userActor: ActorRef = system.actorOf(Props[UserActor], "UserActor")
	val marketActor: ActorRef = system.actorOf(Props[MarketActor], "MarketActor")
	val startActor: ActorRef = system.actorOf(Props[StartActor], "StartActor")
}
