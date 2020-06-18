
package Dao
import Auxillary.Time.log
import Model.Fish_.Fish
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import akka.stream.alpakka.mongodb.DocumentUpdate
import org.mongodb.scala.model.{Filters, Updates}

import scala.util.{Failure, Success}
import Actors.Initializer.system
import Model.Bug_._
import Model.TurnipTransaction_._
import Model.User_._
import Model.Pocket_._
import scala.language.postfixOps

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import system.dispatcher


object UserOperations extends MongoDBOperations {
	final val BUG = "bug"
	final val FISH = "fish"
	final val chill = 10
	val codecRegistryUser = fromRegistries(fromProviders(classOf[User], classOf[TurnipTransaction],classOf[Pocket], classOf[Bug], classOf[Fish]), DEFAULT_CODEC_REGISTRY)
	val codecRegistryPocket = fromRegistries(fromProviders(classOf[User],classOf[Pocket], classOf[Bug], classOf[Fish]), DEFAULT_CODEC_REGISTRY)

	private val allUsers = db
		.getCollection("user", classOf[User])
		.withCodecRegistry(codecRegistryUser)

	def createOneUser(user : User): Unit = {
		val source = Source(List(user))
		val taskFuture = source.runWith(MongoSink.insertOne(allUsers))
		taskFuture.onComplete{
			case Success(_) => log.info("UserOperations","createOneUser","Success",s"Added USER ${user.username}")
			case Failure (ex) => log.warn("UserOperations","createOneUser","Failure",s"Failed to create USER: $ex")
		}
	}

	def finalizeCreateOneUser(username : String, id : Int, avatar : String): Unit = {
		genericUpdateUser(username, "id", id)
		genericUpdateUser(username, "avatar", avatar)
		}

	def readOneUser(username : String): Seq[User] = {
		val source = MongoSource(allUsers.find(classOf[User])).filter(user => user.username == username)
		val userSeqFuture = source.runWith(Sink.seq)
		val userSeq : Seq[User] = Await.result(userSeqFuture, chill seconds)
		userSeq
	}

	def readAllChannelsWithCrossingBotAdded() : Seq[User] = {
		val source = MongoSource(allUsers.find(classOf[User])).filter(user => user.addedToChannel)
		val userSeqFuture = source.runWith(Sink.seq)
		val userSeq : Seq[User] = Await.result(userSeqFuture, chill seconds)
		userSeq
	}

	def readTotalStalks() : Int = {
		val source = MongoSource(allUsers.find(classOf[User])).filter(user => user.liveTurnips.quantity > 0)
		val userSeqFuture = source.runWith(Sink.seq)
		val userSeq : Seq[User] = Await.result(userSeqFuture, chill seconds)
		val userTurnips = userSeq.map(user => user.liveTurnips.quantity).sum
		userTurnips
	}

	def signUpUser(username : String, encryptedPw : String) : Unit = {
		val source = MongoSource(allUsers.find(classOf[User])).filter(user => user.username == username)
			.map(user => {
				DocumentUpdate(filter = Filters.eq("username", user.username), update = Updates.set("encryptedPw", encryptedPw))
			})
		val taskFuture = source.runWith(MongoSink.updateOne(allUsers))
		taskFuture.onComplete{
			case Success(_) =>
				log.info("UserOperations","signUpUser","Success",s"Updated $username's encrypted password successfully")
			case Failure (ex) =>
				log.warn("UserOperations","signUpUser","Failure",s"Failed update: $ex")
		}
	}


	def signInUser(username : String, encryptedPw : String) : Boolean = {
		val source = MongoSource(allUsers.find(classOf[User])).filter(user => user.username == username && user.encryptedPw == user.encryptedPw)
		val userSeqFuture = source.runWith(Sink.seq)
		val userSeq : Seq[User] = Await.result(userSeqFuture, chill seconds)
		userSeq.length == 1
	}

	def updateUserChannelsWithCrossingBotAdded(username: String, added  : Boolean) : Unit = {
		val source = MongoSource(allUsers.find(classOf[User]))
			.map(user => {
				DocumentUpdate(filter = Filters.eq("username", user.username), update = Updates.set("addedToChannel", added ))
			})
		val taskFuture = source.runWith(MongoSink.updateOne(allUsers))
		taskFuture.onComplete {
			case Success(_) =>
				log.info("UserOperations", "updateUserPocket", "Success", s"Updated CrossingBot presence for $username's channel")
			case Failure(ex) =>
				log.warn("UserOperations", "updateUserPocket", "Failure", s"Failed to update CrossingBot presence for $username's channel: $ex")
		}
	}

	def updateUserPocket(user : User, species: String, pocketedCreature: Pocket ) : Unit = {
		val source = MongoSource(allUsers.find(classOf[User]))
			.map(user => {
				val updatedPocket = newPocket(user.pocket, species, pocketedCreature)
				DocumentUpdate(filter = Filters.eq("username", user.username), update = Updates.set("pocket", updatedPocket))
			})
		val taskFuture = source.runWith(MongoSink.updateOne(allUsers))
		taskFuture.onComplete{
			case Success(_) =>
				log.info("UserOperations","updateUserPocket","Success",s"Updated ${user.username}'s pocket successfully")
			case Failure (ex) =>
				log.warn("UserOperations","updateUserPocket","Failure",s"Failed update: $ex")
		}
	}

	def newPocket(userPocket: Pocket, species : String, pocketedCreature : Pocket): Pocket = {
		if(species == "bug"){
			val newBugList = userPocket.bug :+ pocketedCreature.bug.head
			Pocket(newBugList,userPocket.fish)
		} else {
			val newFishList = userPocket.fish :+ pocketedCreature.fish.head
			Pocket(userPocket.bug, newFishList)
		}
	}

	def genericUpdateUser[A](username : String, key: String, value : A) : Unit = {
		val source = MongoSource(allUsers.find(classOf[User]))
			.map(_ => DocumentUpdate(filter = Filters.eq("username", username), update = Updates.set(key, value)))
		val taskFuture = source.runWith(MongoSink.updateOne(allUsers))
		taskFuture.onComplete{
			case Success(_) => log.info("UserOperations","genericUpdateUser", "Success", s"Updated $key for $username")
			case Failure (ex) => log.warn("UserOperations","genericUpdateUser","Failure",s"Failed update $username: $ex")
		}
	}

	def updateOneUserTransaction(user : User) : User = {
		genericUpdateUser(user.username, "liveTurnips", user.liveTurnips)
		genericUpdateUser(user.username, "turnipTransactionHistory", user.turnipTransactionHistory)
		genericUpdateUser(user.username, "bells", user.bells)
		readOneUser(user.username).head
	}

	def updateTurnipTransactionStatsUponRetrieval(user: User): User = {
		genericUpdateUser(user.username, "liveTurnips", user.liveTurnips)
		genericUpdateUser(user.username, "turnipTransactionHistory", user.turnipTransactionHistory)
		readOneUser(user.username).head
	}

	def deleteOneForUser(user : User, username :String, species : String, creatureName : String, creatureBells: Int): Unit = {
		val updatedPocket = if(species == BUG){
			val	unSoughtBugs = user.pocket.bug.filter(creature => creature.name != creatureName)
			val	soughtBugs = user.pocket.bug.filter(creature => creature.name == creatureName)
			val soughtBugsOneRemoved = if (soughtBugs.map(bug => bug.name).contains(creatureName)) soughtBugs.takeRight(soughtBugs.length - 1 ) else List()
			val bugList = unSoughtBugs.appendedAll(soughtBugsOneRemoved)
			Pocket(bugList, user.pocket.fish)
		} else if(species == FISH){
			val	unSoughtFishes = user.pocket.fish.filter(creature => creature.name != creatureName)
			val	soughtFishes = user.pocket.fish.filter(creature => creature.name == creatureName)
			val soughtFishesOneRemoved = if (soughtFishes.map(fish => fish.name).contains(creatureName)) soughtFishes.takeRight(soughtFishes.length - 1)  else List()
			val fishList = unSoughtFishes.appendedAll(soughtFishesOneRemoved)
			Pocket(user.pocket.bug, fishList)
		}

		val updatedBells = user.bells + creatureBells
		genericUpdateUser(user.username, "pocket", updatedPocket)
		genericUpdateUser(user.username, "bells", updatedBells)
	}

	def deleteAllCreatureForUser(username : String, species : String): Int = {
		val userList: List[User] = readOneUser(username).toList
		val user: User = userList.head
		if(species == BUG){
			val bugBells = Await.result(Source(user.pocket.bug).via(Flow[Bug].fold[Int](0)(_ + _.bells)).runWith(Sink.head), chill second)
			genericUpdateUser(user.username, "bells", user.bells + bugBells)
			genericUpdateUser(user.username, "pocket", Pocket(List(),  user.pocket.fish))
			bugBells
		}else{
			val fishBells = Await.result(Source(user.pocket.fish).via(Flow[Fish].fold[Int](0)(_ + _.bells)).runWith(Sink.head), chill second)
			genericUpdateUser(user.username, "bells", user.bells + fishBells)
			genericUpdateUser(user.username, "pocket", Pocket(user.pocket.bug,  List()))
			fishBells
		}
	}

	def deleteAllForUser(username : String): Int = {
		val userList: List[User] = readOneUser(username).toList
		val user: User = userList.head
		val bugBells = Await.result(Source(user.pocket.bug).via(Flow[Bug].fold[Int](0)(_ + _.bells)).runWith(Sink.head), chill second)
		val fishBells = Await.result(Source(user.pocket.fish).via(Flow[Fish].fold[Int](0)(_ + _.bells)).runWith(Sink.head), chill second)
		genericUpdateUser(user.username, "bells", user.bells + bugBells + fishBells)
		genericUpdateUser(user.username, "pocket", Pocket(List(),  user.pocket.fish))
		bugBells + fishBells
	}



	def deleteUser(username : String): Unit = {
		val source = MongoSource(allUsers.find(classOf[User])).map(_ => Filters.eq("username", username))
		val taskFuture = source.runWith(MongoSink.deleteOne(allUsers))

		taskFuture.onComplete{
			case Success(_) => log.info("UserOperations","deleteUser", "Success", s"Deleteed $username")
			case Failure (ex) => log.warn("UserOperations","deleteUser","Failure",s"Failed delete $username: $ex")
		}
	}
}



