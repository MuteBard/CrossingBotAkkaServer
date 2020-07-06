package Model
import Pocket_.Pocket
import TurnipTransaction_.TurnipTransaction

object User_ {
	case class User(
		id : Int = -1,
		username : String = "NULL/USER",
		fishingPoleLvl : Int = 1,
		bugNetLvl : Int = 1,
		bells : Int = 0,
		pocket : Pocket = Pocket(),
		liveTurnips : TurnipTransaction = TurnipTransaction(),
		turnipTransactionHistory : List[TurnipTransaction] = List(),
		avatar : String = "",
		encryptedPw : String = "",
		addedToChannel : Boolean = false,
	)

	//Arguments
	case class usernameArgs(username : String)
	case class catchCreatureArgs(username: String, species: String)
	case class finalizeUserArgs(username : String, id : Int, avatar : String)
	case class sellCreatureArgs(username : String, species : String, creatureName : String)
	case class sellCreatureByNameArgs(username : String, creatureName : String)
	case class authenticationArgs(username : String, encryptedPw : String)
	case class addToChannelArgs(username: String, added : Boolean)
	case class createUserArgs(username: String, id: Int, avatar: String, addedToChannel: Boolean)
	case class authArgs(username: String, encryptedPw: String)

}
