package Dao

import Auxillary.Time._
import Data.BugData.Bugs
import Model.Bug_.Bug
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.scaladsl.{Sink, Source}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

import scala.util.{Failure, Random, Success}
import Actors.Initializer.system
import org.mongodb.scala.model.Filters

import scala.concurrent.duration._
import scala.concurrent.Await
import system.dispatcher

import scala.language.postfixOps


object BugOperations extends MongoDBOperations{
	val codecRegistry = fromRegistries(fromProviders(classOf[Bug]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10
	private val allBugs = db
		.getCollection("bug", classOf[Bug])
		.withCodecRegistry(codecRegistry)

	def createAll(): Unit = {
		val source = Source(Bugs).map(_ => Filters.eq("species", "bug"))
		val taskFuture = source.runWith(MongoSink.deleteMany(allBugs))
		taskFuture.onComplete {
			case Success(_) =>
				val secondSource = Source(Bugs)
				val secondTaskFuture = secondSource.grouped(2).runWith(MongoSink.insertMany[Bug](allBugs))
				secondTaskFuture.onComplete {
					case Success(_) => log.info("BugOperations", "createAll", "Success", s"Created ${Bugs.length} BUG")
					case Failure(ex) => log.warn("BugOperations", "createAll", "Failure", s"Failed create: $ex")
				}
			case Failure(ex) => log.warn("BugOperations", "createAll", "Failure", s"Failed delete all: $ex")
		}
	}



	def readAll(): List[Bug] = {
		val source = MongoSource(allBugs.find(classOf[Bug]))
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		bugSeq.toList
	}

	def readOneById(query : Int) : Seq[Bug] = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.id == query)
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		bugSeq
	}

	def readOneByName(query : String) : Seq[Bug] = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.name == query)
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		bugSeq
	}

	//	def readOneByRarity(query : Int) : Bug = {
	//		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.rarity == query)
	//		val bugSeqFuture = source.runWith(Sink.seq)
	//		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, 1 seconds)
	//		Random.shuffle(bugSeq.toList).head
	//	}

	def readAllByMonth(query : List[String]) : List[Bug] = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.availability.intersect(query) == query)
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		bugSeq.toList
	}

	def readOneByRandom(queryInt : Int) : Bug = {
		val month = List(threeLetterMonth)
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => (bugs.rarity == queryInt) && bugs.availability.intersect(month) == month)
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		Random.shuffle(bugSeq.toList).head
	}

	def readAllRarestByMonth(queryList : List[String]) : List[Bug] = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => (bugs.rarity == 5 || bugs.rarity == 4 ) && bugs.availability.intersect(queryList) == queryList)
		val bugSeqFuture = source.runWith(Sink.seq)
		val bugSeq : Seq[Bug] = Await.result(bugSeqFuture, chill seconds)
		bugSeq.toList
	}

	//	def updateOne(/*id : String ,data : Bug*/): Unit = {
	//		val id = "B1"
	//		val source = MongoSource(allBugs.find(classOf[Bug]))  //FIND
	//    		.map(bug => DocumentUpdate(filter = Filters.eq("bugId", id), update = Updates.set("bells", 90))) //UPDATE
	//		val taskFuture = source.runWith(MongoSink.updateOne(allBugs)) //REPLACE
	//		taskFuture.onComplete{
	//			case Success(_) => println(s"[BugOperations][createAll][Success] Successfully updated BUG at $id")
	//			case Failure (ex) => println(s"Failed update: $ex")
	//		}
	//	}


}