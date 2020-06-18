package Actors

import java.util.Calendar

import Initializer._
import Actors.MarketActor._
import Dao.{BugOperations, FishOperations}
import system.dispatcher
import scala.language.postfixOps

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Cancellable}

object StartActor {
	case object Create_Creatures_All
	case object Start_Market_Timers
	case object Stop_Market_Timers
}

class StartActor extends Actor with ActorLogging{
	import StartActor._

	var createMovementRecords : Cancellable = _
	var deleteOldMovementRecords : Cancellable = _

	override def receive: Receive = {
		case Create_Creatures_All =>
			log.info("[Create_Creatures_All] Inserting all BUG and FISH in Database")
			BugOperations.createAll()
			FishOperations.createAll()
			sender() ! "Success"

		case Start_Market_Timers =>
			log.info("[StartMarketTimers] Starting Scheduler Jobs")

			val task = new Runnable {
				def run() {
					val hourblock = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
					val quarterblock = Calendar.getInstance().get(Calendar.MINUTE)/15
					marketActor ! Create_New_Movement_Record(hourblock, quarterblock)
				}
			}
			sender() ! "Success"

			createMovementRecords = system.scheduler.scheduleWithFixedDelay(5 seconds, 1 minute){task}
			deleteOldMovementRecords = system.scheduler.scheduleWithFixedDelay(60 days, 10 days, marketActor, Delete_Earliest_Movement_Records)

		case Stop_Market_Timers =>
			log.info("[StartMarketTimers] Stopping Scheduler Jobs")
			//TODO handle null with option
			createMovementRecords.cancel()
			deleteOldMovementRecords.cancel()
			sender() ! "Success"

	}
}
