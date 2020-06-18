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
			sender() ! BugOperations.readAll()

		case Read_One_Bug_By_Random() =>
			log.info(s"[Read_One_Bug_By_Random] Selecting BUG by random")
			val bug = BugOperations.readOneByRandom(rarityValue)
			log.info(s"[Read_One_BUG_By_Random] Found BUG ${bug.name}")
			sender() ! bug

		case Read_One_Bug_By_Id(id : Int) =>
			log.info(s"[Read_One_Bug_By_Id] Selecting BUG with id : $id")
			val bugSeq = BugOperations.readOneById(id)
			val bugExists = bugSeq.nonEmpty
			if(bugExists){
				log.info(s"[Read_One_User] BUG with id $id found")
				sender() ! bugSeq.head
			}else {
				log.info(s"[Read_One_User] BUG with id $id does not exist")
				sender() ! Bug()
			}

		case Read_One_Bug_By_Name(name : String) =>
			log.info(s"[Read_One_By_Name] Selecting BUG with name : $name")
			val bugSeq = BugOperations.readOneByName(name)
			val bugExists = bugSeq.nonEmpty
			if(bugExists){
				log.info(s"[Read_One_User] BUG with name $name found")
				sender() ! bugSeq.head
			}else {
				log.info(s"[Read_One_User] BUG with name $name does not exist")
				sender() ! Bug()
			}


		case Read_All_Bug_By_Month(months : List[String]) =>
			log.info(s"[Read_All_Bug_By_Month] Selecting BUG based on month(s) provided")
			sender() ! BugOperations.readAllByMonth(months)

		case Read_All_Rarest_Bug_By_Month(months : List[String]) =>
			log.info(s"[Read_All_Rarest_Bug_By_Month] Selecting BUG based on rarity")
			sender() ! BugOperations.readAllRarestByMonth(months)
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

