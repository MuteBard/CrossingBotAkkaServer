package Dao

import Data.FishData.Fishes
import Auxillary.Time._
import Model.Fish_.Fish
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
import scala.util.control.Exception.allCatch


object FishOperations extends MongoDBOperations {
	val codecRegistry = fromRegistries(fromProviders(classOf[Fish]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10
	private val allFishes = db
		.getCollection("fish", classOf[Fish])
		.withCodecRegistry(codecRegistry)


	def safeVector(value : () => Vector[Fish], methodName : String) :  Any = {
		allCatch.opt(value()) match {
			case Some(fishes) => fishes
			case None =>
				log.warn("FishOperations", methodName, "Failure")
				"empty"
		}
	}

	def safeVectorHead(value : () => Vector[Fish], methodName : String) : Any = {
		allCatch.opt(value().head) match {
			case Some(fish) => fish
			case None =>
				log.warn("FishOperations", methodName, "Failure")
				"empty"
		}
	}

	def createAll(): Unit = {
		val source = Source(Fishes).map(_ => Filters.eq("species", "fish"))
		val taskFuture = source.runWith(MongoSink.deleteMany(allFishes))
		taskFuture.onComplete {
			case Success(_) =>
				val secondSource = Source(Fishes)
				val secondTaskFuture = secondSource.grouped(2).runWith(MongoSink.insertMany[Fish](allFishes))
				secondTaskFuture.onComplete {
					case Success(_) => log.info("FishOperations", "createAll", "Success", s"Created ${Fishes.length} FISH")
					case Failure(ex) => log.warn("FishOperations", "createAll", "Failure", s"Failed create: $ex")
				}
			case Failure(ex) => log.warn("FishOperations", "createAll", "Failure", s"Failed delete all: $ex")
		}
	}

	//Vector[Fish] on success, String on failure
	def readAll(): Any = {
		val source = MongoSource(allFishes.find(classOf[Fish]))
		val fishFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(fishFuture, chill seconds).toVector
		safeVector(() => result, "readAll")
	}

	//Vector[Fish] on success, String on failure
	def readAllByMonth(query : List[String]) : Any = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.availability.intersect(query) == query)
		val fishSeqFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(fishSeqFuture, chill seconds).toVector
		safeVector(() => result, "readAllByMonth")
	}

	//Vector[Fish] on success, String on failure
	def readAllRarestByMonth(queryList : List[String]) : Any = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => (fishes.rarity == 5 || fishes.rarity == 4) && fishes.availability.intersect(queryList) == queryList)
		val fishSeqFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(fishSeqFuture, chill seconds).toVector
		safeVector(() => result, "readAllRarestByMonth")
	}

	//Fish on success, String on failure
	def readOneByRandom(query : Int) : Any = {
		val month = List(threeLetterMonth)
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => (fishes.rarity == query) && fishes.availability.intersect(month) == month)
		val fishSeqFuture = source.runWith(Sink.seq)
		lazy val result = Random.shuffle(Await.result(fishSeqFuture, chill seconds).toVector)
		safeVectorHead(() => result, "readOneByRandom")
	}

	//Fish on success, String on failure
	def readOneById(query : Int) : Any = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.id == query)
		val fishFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(fishFuture, chill seconds).toVector
		safeVectorHead(() => result, "readOneById")
	}

	//Fish on success, String on failure
	def readOneByName(query : String) : Any = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.name == query)
		val fishFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(fishFuture, chill seconds).toVector
		safeVectorHead(() => result, "readOneByName")
	}

}