package Model

object Fish_ {

	//Model
	case class Fish(
		id : Int = -1,
		species : String = "fish",
		name : String = "",
		bells : Int = -1,
		availability : List[String] = List(),
		rarity : Int = -1,
		img : String = ""
	)
	//Arguments
	case class fishMonthsArgs(months : List[String])
	case class fishIdArgs(id : Int)
	case class fishNameArgs(name : String)
}
