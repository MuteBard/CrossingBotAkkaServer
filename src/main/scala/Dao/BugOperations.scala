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
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.model.Filters

import scala.concurrent.duration._
import scala.concurrent.Await
import system.dispatcher

import scala.language.postfixOps
import scala.util.control.Exception._


object BugOperations extends MongoDBOperations{
	val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[Bug]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10
	private val allBugs = db
		.getCollection("bug", classOf[Bug])
		.withCodecRegistry(codecRegistry)


	def safeList(value : () => List[Bug], methodName : String) :  Any = {
		value() match {
			case bugs if bugs.isEmpty =>
				log.warn("BugOperations", methodName, "Failure")
				"empty"
			case bugs => bugs
		}
	}

	def safeListHead(value : () => List[Bug], methodName : String) : Any = {
		allCatch.opt(value().head) match {
			case None =>
				log.warn("BugOperations", methodName, "Failure")
				"empty"
			case Some(bug) => bug

		}
	}

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


	//List[Bug] on success, String on failure
	def readAll(): Any = {
		val source = MongoSource(allBugs.find(classOf[Bug]))
		val bugFuture = source.runWith(Sink.seq)
		lazy val result	= Await.result(bugFuture, chill seconds).toList
		safeList(() => result, "readAll")
	}

	//List[Bug] on success, String on failure
	def readAllByMonth(query : List[String]) : Any = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.availability.intersect(query) == query)
		val bugSeqFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(bugSeqFuture, chill seconds).toList
		safeList(() => result, "readAllByMonth")
	}

	//List[Bug] on success, String on failure
	def readAllRarestByMonth(queryList : List[String]) : Any = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => (bugs.rarity == 5 || bugs.rarity == 4) && bugs.availability.intersect(queryList) == queryList)
		val bugSeqFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(bugSeqFuture, chill seconds).toList
		safeList(() => result, "readAllRarestByMonth")
	}

	//Bug on success, String on failure
	def readOneByRandom(query : Int) : Any = {
		val month = List(threeLetterMonth)
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => (bugs.rarity == query) && bugs.availability.intersect(month) == month)
		val bugFuture = source.runWith(Sink.seq)
		lazy val result = Random.shuffle(Await.result(bugFuture, chill seconds).toList)
		safeListHead(() => result, "readOneByRandom")
	}

	//Bug on success, String on failure
	def readOneById(query : Int) : Any = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.id == query)
		val bugFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(bugFuture, chill seconds).toList
		safeListHead(() => result, "readOneById")
	}

	//Bug on success, String on failure
	def readOneByName(query : String) : Any = {
		val source = MongoSource(allBugs.find(classOf[Bug])).filter(bugs => bugs.name == query)
		val bugFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(bugFuture, chill seconds).toList
		safeListHead(() => result, "readOneByName")
	}

}