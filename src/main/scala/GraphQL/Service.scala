package GraphQL
import Actors.{BugActor, FishActor, MarketActor, StartActor, UserActor}
import Model.User_._
import Model.Bug_._
import Model.Fish_._
import Actors.Initializer._
import Model.MovementRecord_.MovementRecord
import Model.TurnipTransaction_.TurnipTransaction
import zio.{IO, UIO}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout
import akka.pattern.ask

object Service {

	case class NotFound(message: String) extends Throwable
	case class NotValid(message: String) extends Throwable
	trait CrossingBotService {

		//--User--
    	//Queries
		def getUser(username : String):                     IO[NotFound, User]
		def getDoesUserExist(username : String):            UIO[Boolean]
		def getUsersWithCBAdded(dummy: Boolean):            UIO[List[User]]
		def validatePendingTransaction(
			username : String,
			business : String,
			quantity : Int
		):                                                  UIO[TurnipTransaction]

		//--MovementRecord--
		//Queries
		def getDayRecords(dummy : Boolean):                 UIO[MovementRecord]
		def getNDayRecords(days: Int):                      UIO[List[MovementRecord]]
		def getTurnipPrices(dummy : Boolean):               UIO[Int]


		//--Bug--
    	//Queries
		def getAllBugs:                                     UIO[List[Bug]]
		def getAllBugsByMonth(months : List[String]):       IO[NotFound, List[Bug]]
		def getAllRareBugsByMonth(months : List[String]):   IO[NotFound, List[Bug]]
		def getBugById(id : Int):                           IO[NotFound, Bug]
		def getBugByName(name : String):                    IO[NotFound, Bug]
		def getBugByRandom(dummy : Boolean):                IO[NotFound, Bug]

		//--Fish--
		//Queries
		def getAllFishes:                                   UIO[List[Fish]]
		def getAllFishesByMonth(months : List[String]):     IO[NotFound, List[Fish]]
		def getAllRareFishesByMonth(months : List[String]): IO[NotFound, List[Fish]]
		def getFishById(id : Int):                          IO[NotFound, Fish]
		def getFishByName(name : String):                   IO[NotFound, Fish]
		def getFishByRandom(dummy : Boolean):               IO[NotFound, Fish]

		//--Quality of Life--
		//Queries
		def getCreatureSummaryByName(name : String):        IO[NotFound, String]

		//Mutations
		//--Start--
		def createOneUser(
			username : String,
			id : Int,
			avatar : String,
			addedToChannel : Boolean

		):                                                  UIO[String]
		def signUp(
			username : String,
			encryptedPw : String
		):                                                  UIO[String]

		def signIn(
			username : String,
	        encryptedPw : String
	    ):                                                  UIO[Boolean]

		def populate:                                       UIO[String]
		def toggleMarket(running : Boolean):                UIO[String]

		def isCrossingBotAdded(
			username : String,
			added : Boolean
		):                                              UIO[String]

		def catchCreature(
			 username: String,
			 species : String
		):                                                  IO[NotFound, String]
		def finalizeUserCreation(
			username: String,
			id : Int,
			avatar : String
		):                                                  IO[NotFound, String]

		def acknowledgeTransaction(
			username : String,
			business : String,
			quantity : Int,
			marketPrice: Int,
			totalBells: Int
		):                                                  IO[NotFound, String]

		def sellOneCreature(
		    username: String,
		    species : String,
		    creatureName : String
		):                                                  UIO[Int]

		def sellAllBugs(username: String) :                 UIO[Int]

		def sellAllFishes(username: String) :               UIO[Int]

		def sellAllCreatures(username : String):            UIO[Int]

		def deleteUser(username : String):                  UIO[String]


	}

	class CBS extends CrossingBotService{
		var num = 0
		val chill = 10
		implicit val timeout: Timeout = Timeout(chill seconds)

		//--User--
		//Queries
		def getUser(username : String) : IO[NotFound, User] = {
			val user = Await.result((userActor ? UserActor.Read_One_User(username)).mapTo[User], chill seconds)
			if (user.id != -2) IO.succeed(user)
			else IO.fail(NotFound(""))
		}

		def getDoesUserExist(username : String): UIO[Boolean] = {
			val doesExist = Await.result((userActor ? UserActor.Read_Does_User_Exist(username)).mapTo[Boolean], chill seconds)
			IO.succeed(doesExist)
		}

		def getUsersWithCBAdded(dummy : Boolean): UIO[List[User]] = {
			val userList = Await.result((userActor ? UserActor.Read_All_Stream_Added_Users).mapTo[List[User]], chill seconds)
			IO.succeed(userList)
		}

		def validatePendingTransaction(username: String, business : String, quantity : Int) : UIO[TurnipTransaction] = {
			val turnipTransaction = Await.result((userActor ? UserActor.Read_One_User_With_Pending_Turnip_Transaction(username, business, quantity)).mapTo[TurnipTransaction], chill seconds)
			IO.succeed(turnipTransaction)
		}
		//--MovementRecord--

		def getDayRecords(dummy : Boolean): UIO[MovementRecord] = {
			val movementRecord = Await.result((marketActor ? MarketActor.Read_Latest_Movement_Record_Day).mapTo[MovementRecord], chill seconds)
			IO.succeed(movementRecord)
		}

		def getNDayRecords(days : Int): UIO[List[MovementRecord]] = {
			val nMovementRecords = Await.result((marketActor ? MarketActor.Read_Latest_N_Days_Movement_Record(days)).mapTo[Seq[MovementRecord]], chill seconds).toList
			IO.succeed(nMovementRecords)
		}

		def getTurnipPrices(dummy : Boolean): UIO[Int] = {
			val turnips = Await.result((marketActor ? MarketActor.Request_Turnip_Price).mapTo[Int], chill seconds)
			IO.succeed(turnips)
		}

		//--Bug--
		def getAllBugs: UIO[List[Bug]] = {
			val allBugs = Await.result((bugActor ? BugActor.Read_Bug_All).mapTo[List[Bug]], chill seconds)
			IO.succeed(allBugs)
		}
		def getAllBugsByMonth(months : List[String]) : IO[NotFound, List[Bug]] = {
			val allBugs = Await.result((bugActor ? BugActor.Read_All_Bug_By_Month(months)).mapTo[List[Bug]], chill seconds)
			if(allBugs.nonEmpty) IO.succeed(allBugs)
			else IO.fail(NotFound(""))
		}
		def getAllRareBugsByMonth(months : List[String]) : IO[NotFound, List[Bug]] = {
			val allBugs = Await.result((bugActor ? BugActor.Read_All_Rarest_Bug_By_Month(months)).mapTo[List[Bug]], chill seconds)
			if(allBugs.nonEmpty) IO.succeed(allBugs)
			else IO.fail(NotFound(""))
		}
		def getBugById(id: Int): IO[NotFound, Bug] = {
			val bug = Await.result((bugActor ? BugActor.Read_One_Bug_By_Id(id)).mapTo[Bug], chill seconds)
			if(bug.id != -1) IO.succeed(bug)
			else IO.fail(NotFound(""))
		}
		def getBugByName(name : String): IO[NotFound, Bug] = {
			val bug = Await.result((bugActor ? BugActor.Read_One_Bug_By_Name(name)).mapTo[Bug], chill seconds)
			if(bug.id != -1) IO.succeed(bug)
			else IO.fail(NotFound(""))
		}
		def getBugByRandom(dummy : Boolean): IO[NotFound, Bug] = {
			val bug = Await.result((bugActor ? BugActor.Read_One_Bug_By_Random()).mapTo[Bug], chill seconds)
			if(bug.id != -1) IO.succeed(bug)
			else IO.fail(NotFound(""))
		}


		//--Fish--
		def getAllFishes: UIO[List[Fish]] = {
			val allFishes = Await.result((fishActor ? FishActor.Read_Fish_All).mapTo[List[Fish]], chill seconds)
			IO.succeed(allFishes)
		}
		def getAllFishesByMonth(months : List[String]) : IO[NotFound, List[Fish]] = {
			val allFishes = Await.result((fishActor ? FishActor.Read_All_Fish_By_Month(months)).mapTo[List[Fish]], chill seconds)
			if(allFishes.nonEmpty) IO.succeed(allFishes)
			else IO.fail(NotFound(""))
		}
		def getAllRareFishesByMonth(months : List[String]) : IO[NotFound, List[Fish]] = {
			val allFishes = Await.result((fishActor ? FishActor.Read_All_Rarest_Fish_By_Month(months)).mapTo[List[Fish]], chill seconds)
			if(allFishes.nonEmpty) IO.succeed(allFishes)
			else IO.fail(NotFound(""))
		}
		def getFishById(id: Int): IO[NotFound, Fish] = {
			val fish = Await.result((fishActor ? FishActor.Read_One_Fish_By_Id(id)).mapTo[Fish], chill seconds)
			if(fish.id != -1) IO.succeed(fish)
			else IO.fail(NotFound(""))
		}
		def getFishByName(name : String): IO[NotFound, Fish] = {
			val fish = Await.result((fishActor ? FishActor.Read_One_Fish_By_Name(name)).mapTo[Fish], chill seconds)
			if(fish.id != -1) IO.succeed(fish)
			else IO.fail(NotFound(""))
		}
		def getFishByRandom(dummy : Boolean): IO[NotFound, Fish] = {
			val fish = Await.result((fishActor ? FishActor.Read_One_Fish_By_Random()).mapTo[Fish], chill seconds)
			if(fish.id != -1) IO.succeed(fish)
			else IO.fail(NotFound(""))
		}

		def getCreatureSummaryByName(name: String): IO[NotFound, String] = {
			val merge : (String, String) => String = (s1, s2) => s1 + s2+" "
			val bug = Await.result((bugActor ? BugActor.Read_One_Bug_By_Name(name)).mapTo[Bug], chill seconds)
			if(bug.id != -1){
					val str = s"The ${bug.name} is worth ${bug.bells} bells and it has a rarity of lvl ${bug.rarity}. It is available during these following months: ${bug.availability.fold("")(merge)}".trim()
				IO.succeed(str)
			}else{
				val fish = Await.result((fishActor ? FishActor.Read_One_Fish_By_Name(name)).mapTo[Fish], chill seconds)
				if(fish.id != -1) {
					val str = s"The ${fish.name} is worth ${fish.bells} bells and it has a rarity of lvl ${fish.rarity}. It is available during these following months: ${fish.availability.fold("")(merge)}".trim()
					IO.succeed(str)
				}else{
					IO.fail(NotFound(""))
				}
			}
		}

		//Mutations
		def createOneUser(username: String, id: Int, avatar: String, addedToChannel: Boolean): UIO[String] = {
			println("HELLO")
			val status = Await.result((userActor ? UserActor.Create_One_User(username, id, avatar, addedToChannel)).mapTo[String], chill seconds)
			IO.succeed(status)
		}


		def signUp(username: String, encryptedPw: String): UIO[String] = {
			val status = Await.result((userActor ? UserActor.SignUp_One_User(username, encryptedPw)).mapTo[String], chill seconds)
			IO.succeed(status)
		}

		def signIn(username: String, encryptedPw: String): UIO[Boolean] = {
			val authorized = Await.result((userActor ? UserActor.SignIn_One_User(username, encryptedPw)).mapTo[Boolean], chill seconds)
			IO.succeed(authorized)
		}


		def isCrossingBotAdded(username: String, added: Boolean) : UIO[String] = {
			val status = Await.result((userActor ? UserActor.Update_User_Stream_Added(username, added)).mapTo[String], chill seconds)
			IO.succeed(status)
		}

		def catchCreature(username: String, species: String): IO[NotFound, String] = {
			val status = Await.result((userActor ? UserActor.Update_One_User_With_Creature(username, species)).mapTo[String], chill seconds)
			if(status != "Failure") { //there's 4 options for status
				IO.succeed(status)
			}else{
				IO.fail(NotFound(""))
			}
		}

		def finalizeUserCreation(username: String, id: Int, avatar: String): IO[NotFound, String] = {
			val status = Await.result((userActor ? UserActor.FinalizeUserCreation(username, id, avatar)).mapTo[String], chill seconds)
			if(status == "Success"){
				IO.succeed(status)
			}else{
				IO.fail(NotFound(""))
			}

		}

		def acknowledgeTransaction(username : String, business: String, quantity : Int, marketPrice: Int, totalBells: Int) : IO[NotFound, String] = {
			val status = Await.result((userActor ? UserActor.Update_One_User_With_Executing_Turnip_Transaction(username, business, quantity, marketPrice, totalBells)).mapTo[String], chill seconds)
			if(status == "Success"){
				IO.succeed("Success")
			}else{
				IO.fail(NotFound(""))
			}
		}

		def sellOneCreature(username: String, species : String, creatureName : String): UIO[Int] = {
			val bells = Await.result((userActor ? UserActor.Delete_One_Creature_From_Pocket(username, species, creatureName)).mapTo[Int], chill seconds)
			IO.succeed(bells)
		}

		def sellAllBugs(username: String) : UIO[Int] = {
			val bells = Await.result((userActor ? UserActor.Delete_All_Bugs_From_Pocket(username)).mapTo[Int], chill seconds)
			IO.succeed(bells)
		}

		def sellAllFishes(username: String) : UIO[Int] = {
			val bells = Await.result((userActor ? UserActor.Delete_All_Fishes_From_Pocket(username)).mapTo[Int], chill seconds)
			IO.succeed(bells)
		}

		def sellAllCreatures(username : String): UIO[Int] = {
			val bells = Await.result((userActor ? UserActor.Delete_All_Creatures_From_Pocket(username)).mapTo[Int], chill seconds)
			IO.succeed(bells)
		}

		def populate: UIO[String] = {
			val status = Await.result((startActor ? StartActor.Create_Creatures_All).mapTo[String], chill seconds)
			IO.succeed(status)
		}

		def toggleMarket(running : Boolean) : UIO[String] = {
			if(running){
				val status = Await.result((startActor ? StartActor.Start_Market_Timers).mapTo[String], chill seconds)
				IO.succeed(status)
			}else{
				val status = Await.result((startActor ? StartActor.Stop_Market_Timers).mapTo[String], chill seconds)
				IO.succeed(status)
			}
		}

		def deleteUser(username : String): UIO[String] ={
			val status = Await.result((userActor ? UserActor.Delete_User(username)).mapTo[String], chill seconds)
			IO.succeed(status)
		}
	}
}
