package Actors

import Dao.FishOperations
import Model.Fish_.Fish
import akka.actor.{Actor, ActorLogging}

import scala.util.Random

object FishActor {
	case object Read_Fish_All
	case class Read_All_Fish_By_Month(month : List[String])
	case class Read_All_Rarest_Fish_By_Month(month : List[String])
	case class Read_One_Fish_By_Id(id : Int)
	case class Read_One_Fish_By_Name(name : String)
	case class Read_One_Fish_By_Random()

}

class FishActor extends Actor with ActorLogging{
	import FishActor._

	override def receive: Receive = {
		case Read_Fish_All =>
			log.info("[Read_Fish_All] Selecting all FISH")
			sender() ! FishOperations.readAll()

		case Read_One_Fish_By_Random() =>
			log.info(s"[Read_One_Fish_By_Random] Selecting FISH by random")
			val fish = FishOperations.readOneByRandom(rarityValue)
			log.info(s"[Read_One_Fish_By_Random] Found FISH ${fish.name}")
			sender() ! fish

		case Read_One_Fish_By_Id(id : Int) =>
			log.info(s"[Read_One_Fish_By_Id] Selecting FISH with id : $id")
			val fishSeq = FishOperations.readOneById(id)
			val fishExists = fishSeq.nonEmpty
			if(fishExists){
				log.info(s"[Read_One_Fish_By_Id] FISH with id $id found")
				sender() ! fishSeq.head
			}else {
				log.info(s"[Read_One_Fish_By_Id] FISH with id $id does not exist")
				sender() ! Fish()
			}

		case Read_One_Fish_By_Name(name : String) =>
			log.info(s"[Read_One_Fish_By_Name] Selecting FISH with name : $name")
			val fishSeq = FishOperations.readOneByName(name)
			val fishExists = fishSeq.nonEmpty
			if(fishExists){
				log.info(s"[Read_One_Fish_By_Name] FISH with name $name found")
				sender() ! fishSeq.head
			}else {
				log.info(s"[Read_One_Fish_By_Name] FISH with name $name does not exist")
				sender() ! Fish()
			}

		case Read_All_Fish_By_Month(month : List[String]) =>
			log.info(s"[Read_All_Fish_By_Month] Selecting FISH based on month(s) provided")
			sender() ! FishOperations.readAllByMonth(month)

		case Read_All_Rarest_Fish_By_Month(month : List[String]) =>
			log.info(s"[Read_All_Rarest_Fish_By_Month] Selecting FISH based on rarity")
			sender() ! FishOperations.readAllRarestByMonth(month)

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
