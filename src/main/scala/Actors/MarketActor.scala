package Actors

import java.util.Calendar

import Actors.UserActor.Read_All_Stalks_Purchased
import Actors.Initializer._
import Auxillary.Time._
import Dao.MarketOperations
import Model.Day_.Day
import Model.MovementRecord_.MovementRecord
import Model.TurnipTime_.TurnipTime
import akka.actor.{Actor, ActorLogging}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout
import akka.pattern.ask



import scala.language.postfixOps


object MarketActor {
	case class  Create_New_Movement_Record(hourBlock :Int, quarterBlock : Int)
	case object Read_Latest_Movement_Record_Day
	case class  Read_Latest_N_Days_Movement_Record(n : Int)
	case object Request_Turnip_Price
	case class  Update_Stalks_Purchased(quantity : Int, business : String)
	case object Delete_Earliest_Movement_Records
	case object Start_Todays_Market

}

class MarketActor extends Actor with ActorLogging {


	import Actors.MarketActor._

	var todayMarket = Day()
	var dateMarketCreated = ""
	var currentHourBlockId: Int = -1
	var currentQuarterBlockId: Int = -1
	val chill = 10
	implicit val timeout: Timeout = Timeout(chill seconds)

	override def receive: Receive = {

		//AUTOMATED
		case Create_New_Movement_Record(newHourBlockId, newQuarterBlockId) =>

			//log.info(s"[Create_New_Movement_Record] Checking for difference in block ids ($newHourBlockId,$newQuarterBlockId)")
			val twoMRs = MarketOperations.readLastNDaysMovementRecords(2)
			val suspectTodayMarket = twoMRs(0)

			val mr =
				if (suspectTodayMarket.id == todayDateId()) { //if this movement record is within today
					suspectTodayMarket
				} else if (suspectTodayMarket.id == "") { //if this is the very first movement record
					MovementRecord()
				} else { //if this is a new day and a previous movement record has a different date Id
					MovementRecord(
						id = suspectTodayMarket.id,
						latestTurnip = suspectTodayMarket.latestTurnip,
						todayHigh = suspectTodayMarket.latestTurnip.price,
						todayLow = suspectTodayMarket.latestTurnip.price,
						stalksPurchased = suspectTodayMarket.stalksPurchased,
						turnipHistory = List(suspectTodayMarket.turnipHistory.head)
					)
				}

			if ((currentQuarterBlockId != newQuarterBlockId) || (currentHourBlockId == -1 && currentQuarterBlockId == -1)) {

				if (dateMarketCreated != todayDateId()) {
					log.info(s"[Create_New_Movement_Record] A new day has been detected ($newHourBlockId,$newQuarterBlockId)")
					log.info(s"[Create_New_Movement_Record] Generating all block patterns for the day")
					val yesterdayMarket = suspectTodayMarket
					val dayBeforeMarket = twoMRs(1)

					val margin = yesterdayMarket.stalksPurchased * .25

					// if the market grew more than 25% of its size yesterday, then it is a good day otherwise a bad day
					todayMarket = if(yesterdayMarket.stalksPurchased - dayBeforeMarket.stalksPurchased > margin){
						log.info(s"[Create_New_Movement_Record] Generating relatively good day")
						Day().generate("good")
					}else{
						log.info(s"[Create_New_Movement_Record] Generating relatively bad day")
						Day().generate("bad")
					}
					dateMarketCreated = todayDateId()
					log.info(s"[Create_New_Movement_Record] Today's Market: $todayMarket")
				}

				val turnipPriceRaw = mr.latestTurnip.price + todayMarket.getQuarterBlock(newHourBlockId, newQuarterBlockId).change
				val turnipPriceResolved = if(turnipPriceRaw >= 10) turnipPriceRaw else 10
				val newTurnip = TurnipTime(hour,minute, turnipPriceResolved)
				val _id = todayDateId()
				val high = Math.max(newTurnip.price, mr.todayHigh)
				val low = Math.min(newTurnip.price, mr.todayLow)
				val turnipHistory = newTurnip +: mr.turnipHistory
				val stalksPurchased = Await.result((userActor ? UserActor.Read_All_Stalks_Purchased).mapTo[Int], chill seconds)
				val latestHourBlock = todayMarket.getHourBlock(newHourBlockId)
				val latestHourBlockName = todayMarket.getHourBlock(newHourBlockId).name
				val latestQuarterBlock = todayMarket.getQuarterBlock(newHourBlockId, newQuarterBlockId)
				val quarterBlockHistory = todayMarket.getQuarterBlockHistory(newHourBlockId, newQuarterBlockId)
				val yearForMR = year
				val monthForMR = month
				val dayForMR = day

				val newMr = MovementRecord(_id, newHourBlockId, newQuarterBlockId, high, low, stalksPurchased, newTurnip, turnipHistory, latestHourBlockName, latestHourBlock,
					latestQuarterBlock, quarterBlockHistory, yearForMR, monthForMR, dayForMR
				)

				if ((newHourBlockId == 0 && newQuarterBlockId == 0) || mr.id != todayDateId() || ((currentHourBlockId == -1 && currentQuarterBlockId == -1) && mr.id == "")) {
					log.info(s"[Create_New_Movement_Record] Creating new Movement Record ($newHourBlockId,$newQuarterBlockId)")
					MarketOperations.createMovementRecord(newMr)
				} else {
					log.info(s"[Create_New_Movement_Record] Updating Movement Record")
					MarketOperations.massUpdateMovementRecord(newMr)
				}

				currentHourBlockId = newHourBlockId
				currentQuarterBlockId = newQuarterBlockId
			}
		//AUTOMATED
		case Delete_Earliest_Movement_Records =>
			log.info(s"[Delete_Earliest_Movement_Records] Getting earliest Movement Record")
			val dt = Calendar.getInstance()
			val currentMonth = dt.get(Calendar.MONTH) + 1
			val oldMonth = MarketOperations.readEarliestMovementRecord().month
			if (currentMonth - oldMonth > 2) {
				log.info(s"[Delete_Earliest_Movement_Records] Deleting old Movement Records")
				MarketOperations.deleteOldestMovementRecords(oldMonth)
			}

		case Read_Latest_Movement_Record_Day =>
			log.info(s"[Read_Latest_Movement_Record_Day] Getting latest Movement Record")
			sender() ! MarketOperations.readLatestMovementRecord()

		case Read_Latest_N_Days_Movement_Record(n : Int) =>
			log.info(s"[Read_Latest_N_Days_Movement_Record_Day] Getting latest Movement Record")
			sender() ! MarketOperations.readLastNDaysMovementRecords(n)

		case Update_Stalks_Purchased(quantity, business) =>
			log.info(s"[Update_Stalks_Purchased] Updating total live stalks")
			if (business == "sell") {
				val quantitySold = quantity * -1
				MarketOperations.updateStalksPurchased(quantitySold)
			} else {
				MarketOperations.updateStalksPurchased(quantity)
			}

		case Request_Turnip_Price =>
			log.info(s"[Request_Turnip_Price] Getting turnip price")
			sender() ! MarketOperations.readLatestMovementRecord().latestTurnip.price

	}
}