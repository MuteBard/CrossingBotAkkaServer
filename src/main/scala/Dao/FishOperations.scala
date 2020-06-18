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


object FishOperations extends MongoDBOperations {
	val codecRegistry = fromRegistries(fromProviders(classOf[Fish]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10

	private val allFishes = db
		.getCollection("fish", classOf[Fish])
		.withCodecRegistry(codecRegistry)

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

	def readAll(): List[Fish] = {
		val source = MongoSource(allFishes.find(classOf[Fish]))
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		fishSeq.toList
	}

	def readOneById(query : Int) : Seq[Fish] = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.id == query)
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		fishSeq
	}

	def readOneByName(query : String) : Seq[Fish] = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.name == query)
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		fishSeq
	}
	//	def readOneByRarity(query : Int) : Fish = {
	//		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.rarity == query)
	//		val fishSeqFuture = source.runWith(Sink.seq)
	//		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, 1 seconds)
	//		Random.shuffle(fishSeq.toList).head
	//	}
	//want to check if the contents of availability if they intersect with query, only those that have all of query's months with pass
	def readAllByMonth(query : List[String]) : List[Fish] = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => fishes.availability.intersect(query) == query)
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		fishSeq.toList
	}

	def readOneByRandom(queryInt : Int) : Fish = {
		val month = List(threeLetterMonth)
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => (fishes.rarity == queryInt) && fishes.availability.intersect(month) == month)
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		Random.shuffle(fishSeq.toList).head
	}

	def readAllRarestByMonth(queryList : List[String]) : List[Fish] = {
		val source = MongoSource(allFishes.find(classOf[Fish])).filter(fishes => (fishes.rarity == 5 || fishes.rarity == 4) && fishes.availability.intersect(queryList) == queryList)
		val fishSeqFuture = source.runWith(Sink.seq)
		val fishSeq : Seq[Fish] = Await.result(fishSeqFuture, chill seconds)
		fishSeq.toList
	}
}