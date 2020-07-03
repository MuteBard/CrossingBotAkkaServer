package Actors

import Dao.BugOperations
import Model.Bug_.Bug
import akka.actor.{Actor, ActorLogging}

import scala.util.Random

object BugActor {
	case object Read_Bug_All
	case class Read_All_Bug_By_Month(months : List[String])
	case class Read_All_Rarest_Bug_By_Month(month : List[String])
	case class Read_One_Bug_By_Id(id : Int)
	case class Read_One_Bug_By_Name(name : String)
	case class Read_One_Bug_By_Random()

}

class BugActor extends Actor with ActorLogging{
	import BugActor._

	override def receive: Receive = {

		case Read_Bug_All =>
			log.info("[Read_Bug_All] Selecting all BUG")
			BugOperations.readAll() match {
				case "empty" => sender() ! Vector()
				case bugs => sender() ! bugs
			}

		case Read_All_Bug_By_Month(months : List[String]) =>
			log.info(s"[Read_All_Bug_By_Month] Selecting BUG based on month(s) provided")
			BugOperations.readAllByMonth(months) match {
				case "empty" => sender() ! Vector()
				case bugs => sender() ! bugs
			}

		case Read_All_Rarest_Bug_By_Month(months : List[String]) =>
			log.info(s"[Read_All_Rarest_Bug_By_Month] Selecting BUG based on rarity")
			BugOperations.readAllRarestByMonth(months) match {
				case "empty" => sender() ! Vector()
				case bugs => sender() ! bugs
			}

		case Read_One_Bug_By_Random() =>
			log.info(s"[Read_One_Bug_By_Random] Selecting BUG by random")
			BugOperations.readOneByRandom(rarityValue) match {
				case "empty" => sender() ! Bug()
				case bug => sender() ! bug
			}

		case Read_One_Bug_By_Id(id : Int) =>
			log.info(s"[Read_One_Bug_By_Id] Selecting BUG with id : $id")
			BugOperations.readOneById(id) match {
				case "empty" => sender() ! Bug()
				case bug => sender() ! bug
			}

		case Read_One_Bug_By_Name(name : String) =>
			log.info(s"[Read_One_By_Name] Selecting BUG with name : $name")
			BugOperations.readOneByName(name) match {
				case "empty" => sender() ! Bug()
				case bug => sender() ! bug
			}
	}

	def rarityValue : Int = {
		val random = new Random()
		val chance = random.nextInt(400)+1
		if(chance % 40 == 0) 5
		else if (chance % 20 == 0) 4
		else if(chance % 15 == 0) 3
		else if(chance % 5 == 0) 2
		else 1
	}
}

