package Actors

import Actors.Initializer._
import Auxillary.Time._
import Dao.MarketOperations
import Model.Day_.Day
import Model.MarketRecord_.MarketRecord
import Model.TurnipTime_.TurnipTime
import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object MarketActor {
	case class  Create_New_Market_Record(hourBlock :Int, quarterBlock : Int)
	case object Read_Latest_Market_Record_Day
	case class  Read_Latest_N_Days_Market_Record(n : Int)
	case object Request_Turnip_Price
	case class  Update_Stalks_Purchased(quantity : Int, business : String)
	case object Delete_Earliest_Market_Records
	case object Start_Todays_Market

}

class   MarketActor extends Actor with ActorLogging {


	import Actors.MarketActor._

	var todayMarket = Day()
	var dateMarketCreated = ""
	var currentHourBlockId: Int = -1
	var currentQuarterBlockId: Int = -1
	val chill = 10
	implicit val timeout: Timeout = Timeout(chill seconds)

	override def receive: Receive = {

		//AUTOMATED
		case Create_New_Market_Record(newHourBlockId, newQuarterBlockId) =>

			//log.info(s"[Create_New_Market_Record] Checking for difference in block ids ($newHourBlockId,$newQuarterBlockId)")

			MarketOperations.readLatestNMarketRecords(3) match {
				case "empty" =>
					todayMarket = Day().generate("good")
					currentHourBlockId = newHourBlockId
					currentQuarterBlockId = newQuarterBlockId
					val mr = MarketRecord()
					val id = mr.id
					val orderNum = mr.orderNum
					val turnipPriceRaw = mr.latestTurnip.price + todayMarket.getQuarterBlock(newHourBlockId, newQuarterBlockId).change
					val turnipPriceResolved = if(turnipPriceRaw >= 10) turnipPriceRaw else 10
					val newTurnip = TurnipTime(hour,minute, turnipPriceResolved)
					val high = Math.max(newTurnip.price, mr.todayHigh)
					val low = Math.min(newTurnip.price, mr.todayLow)
					val turnipHistory =  List(newTurnip)
					val stalksPurchased = Await.result((userActor ? UserActor.Read_All_Stalks_Purchased).mapTo[Int], chill seconds)
					val latestHourBlockName = todayMarket.getHourBlock(newHourBlockId).name
					val yearForMR = year
					val monthForMR = month
					val dayForMR = day
					MarketOperations.createMarketRecord(MarketRecord(id, orderNum,newHourBlockId, newQuarterBlockId, high, low, stalksPurchased, newTurnip, turnipHistory, latestHourBlockName, yearForMR, monthForMR, dayForMR))
				case marketRecords : List[MarketRecord] =>
					val latestMarketRecord =
						if(marketRecords(0).id != todayDateId()) { //carry over remnants of the last record if the previous day into the next day
							MarketRecord(
								id = todayDateId(),
								orderNum = marketRecords(0).orderNum + 1,
								latestTurnip = marketRecords(0).latestTurnip,
								todayHigh = marketRecords(0).latestTurnip.price,
								todayLow = marketRecords(0).latestTurnip.price,
								stalksPurchased = marketRecords(0).stalksPurchased,
								turnipHistory = List(marketRecords(0).turnipHistory.head)
							)

						}else{ //otherwise just pass along the data for every 15 minutes
							marketRecords(0)
						}

					if ((currentQuarterBlockId != newQuarterBlockId) || (currentHourBlockId == -1 && currentQuarterBlockId == -1)){
						//determine whether todayMarket should generate a good or bad day based on user turnip purchases
						todayMarket = if (dateMarketCreated != todayDateId()) {
							dateMarketCreated = todayDateId()
							log.info(s"[Create_New_Market_Record] A new day has been detected ($newHourBlockId,$newQuarterBlockId)")
							log.info(s"[Create_New_Market_Record] Generating all block patterns for the day")

							if(marketRecords(0).orderNum >= 2) {
								val yesterdayMarket = marketRecords(1)
								val dayBeforeMarket = marketRecords(2)
								val margin = yesterdayMarket.stalksPurchased * .25
								// if the market grew more than 25% of its size yesterday, then it is a good day otherwise a bad day
								if (yesterdayMarket.stalksPurchased - dayBeforeMarket.stalksPurchased > margin) {
									log.info(s"[Create_New_Market_Record] Generating relatively good day")
									Day().generate("good")
								} else {
									log.info(s"[Create_New_Market_Record] Generating relatively bad day")
									Day().generate("bad")
								}
							}else{
								log.info(s"[Create_New_Market_Record] Generating relatively good day")
								Day().generate("good")
							}
						} else todayMarket

						val id = todayDateId()
						val newOrderNum = latestMarketRecord.orderNum
						val turnipPriceRaw = latestMarketRecord.latestTurnip.price + todayMarket.getQuarterBlock(newHourBlockId, newQuarterBlockId).change
						val turnipPriceResolved = if(turnipPriceRaw >= 10) turnipPriceRaw else 10
						val newTurnip = TurnipTime(hour,minute, turnipPriceResolved)
						val high = Math.max(newTurnip.price, latestMarketRecord.todayHigh)
						val low = Math.min(newTurnip.price, latestMarketRecord.todayLow)
						val turnipHistory =  newTurnip +: latestMarketRecord.turnipHistory
						val stalksPurchased = Await.result((userActor ? UserActor.Read_All_Stalks_Purchased).mapTo[Int], chill seconds)
						val latestHourBlockName = todayMarket.getHourBlock(newHourBlockId).name
						val yearForMR = year
						val monthForMR = month
						val dayForMR = day
						val newMr = MarketRecord(id, newOrderNum,newHourBlockId, newQuarterBlockId, high, low, stalksPurchased, newTurnip, turnipHistory, latestHourBlockName, yearForMR, monthForMR, dayForMR)

						if ((newHourBlockId == 0 && newQuarterBlockId == 0) || newMr.id != todayDateId()) {
							log.info(s"[Create_New_Market_Record] Creating new Market Record ($newHourBlockId,$newQuarterBlockId)")
							MarketOperations.createMarketRecord(newMr)
						} else {
							log.info(s"[Create_New_Market_Record] Updating Market Record")
							MarketOperations.massUpdateMarketRecord(newMr)
						}

						currentHourBlockId = newHourBlockId
						currentQuarterBlockId = newQuarterBlockId
					}
			}



		//AUTOMATED
		case Delete_Earliest_Market_Records =>
			log.info(s"[Delete_Earliest_Market_Records] Getting earliest Market Record")
			val currentMonth = month
			MarketOperations.readEarliestMarketRecord match {
				case "empty" => log.info(s"[Delete_Earliest_Market_Records] Nothing to Delete")
				case mr : MarketRecord =>
					if (currentMonth - mr.month > 2) {
						log.info(s"[Delete_Earliest_Market_Records] Deleting old Market Records")
						MarketOperations.deleteOldestMarketRecords(mr.month)
					}
			}


		case Read_Latest_Market_Record_Day =>
			log.info(s"[Read_Latest_Market_Record_Day] Getting latest Market Record")
			MarketOperations.readLatestMarketRecord match {
				case "empty" =>
					log.info(s"[Read_Latest_Market_Record_Day] Nothing to retrieve")
					sender() ! MarketRecord()
				case mr : MarketRecord =>
					sender() ! mr
			}

		case Read_Latest_N_Days_Market_Record(n : Int) =>
			log.info(s"[Read_Latest_N_Days_Market_Record_Day] Getting latest Market Record")
			MarketOperations.readLatestNMarketRecords(n) match {
				case "empty" =>
					sender() ! List()
				case marketRecords : List[MarketRecord] =>
					sender() ! marketRecords
			}

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
			MarketOperations.readLatestMarketRecord match {
				case "empty" =>
					sender() ! 0
				case mr : MarketRecord =>
					sender() ! mr.latestTurnip.price
			}

	}
}