package Model

import Model.HourBlock_.HourBlock
import Model.QuarterBlock_.QuarterBlock
import Model.TurnipTime_.TurnipTime

//import Data.Market.MarketHourBlock.{HourBlock, HourBlockJsonProtocol}
//import Data.Market.MarketQuarterBlock.{QuarterBlock, QuarterBlockJsonProtocol}

object MovementRecord_ {
	case class MovementRecord(
         id: String = "",
         hourBlockId: Int = 0,
         quarterBlockId: Int = 0,
         todayHigh: Int = 100,
         todayLow: Int = 100,
         stalksPurchased: Int = 0,
         latestTurnip: TurnipTime = TurnipTime(),
         turnipHistory: List[TurnipTime] = List(TurnipTime()),
         hourBlockName: String = "",
         latestHourBlock: HourBlock = null,
         latestQuarterBlock: QuarterBlock = null,
         quarterBlockHistory: List[QuarterBlock] = List(),
         year : Int = 0,
         month: Int = 0,
         day: Int = 0,
	)
	case class toggleArgs(running : Boolean)
	case class daysArgs(days : Int)

}

