package Model
import Auxillary.Time.todayDateId
import Model.TurnipTime_.TurnipTime

object MovementRecord_ {
	case class MovementRecord(
         id: String = todayDateId(),
         orderNum : Int = 0,
         hourBlockId: Int = 0,
         quarterBlockId: Int = 0,
         todayHigh: Int = 100,
         todayLow: Int = 100,
         stalksPurchased: Int = 0,
         latestTurnip: TurnipTime = TurnipTime(),
         turnipHistory: List[TurnipTime] = List(TurnipTime()),
         hourBlockName: String = "",
         year : Int = 0,
         month: Int = 0,
         day: Int = 0,
	)
	case class toggleArgs(running : Boolean)
	case class daysArgs(days : Int)

}

