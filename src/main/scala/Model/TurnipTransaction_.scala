package Model

object TurnipTransaction_ {

	case class TurnipTransaction(
		business: String = "",
		quantity: Int = 0,
		marketPrice : Int = 0,
		totalBells: Int = 0,
		status : String = "",
		netGainLossAsBells : Int = 0,
		netGainLossAsPercentage: Int = 0

	)

	case class pendingTransactionArgs(username : String, business: String, quantity : Int)
	case class authorizedTransactionArgs(username : String, business: String, quantity : Int, marketPrice: Int, totalBells: Int)

}
