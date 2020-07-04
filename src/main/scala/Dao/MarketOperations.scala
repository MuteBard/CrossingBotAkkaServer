package Dao

import Auxillary.Time._
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.scaladsl.{Sink, Source}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import Actors.Initializer.system
import Dao.MongoDBOperations
import Model.HourBlock_.HourBlock
import Model.MovementRecord_.MovementRecord
import Model.QuarterBlock_.QuarterBlock
import Model.TurnipTime_.TurnipTime
import akka.stream.alpakka.mongodb.DocumentUpdate
import org.mongodb.scala.model.{Filters, Updates}
import system.dispatcher

import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.Exception.allCatch
import scala.util.{Failure, Success}


object  MarketOperations extends MongoDBOperations {
	val codecRegistryStalks = fromRegistries(fromProviders(classOf[MovementRecord],classOf[HourBlock], classOf[QuarterBlock], classOf[TurnipTime]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10

	private val allMR = db
		.getCollection("MovementRecord", classOf[MovementRecord])
		.withCodecRegistry(codecRegistryStalks)


	def safeVector(value : () => Vector[MovementRecord], methodName : String) :  Any = {
		allCatch.opt(value()) match {
			case Some(users) => users
			case None =>
				log.warn("UserOperations", methodName, "Failure")
				"empty"
		}
	}

	def safeVectorHead(value : () => Vector[MovementRecord], methodName : String) : Any = {
		allCatch.opt(value().head) match {
			case Some(user) => user
			case None =>
				log.warn("UserOperations", methodName, "Failure")
				"empty"
		}
	}

	def createMovementRecord(mr : MovementRecord): Unit = {
		val mrList : List[MovementRecord] = List(mr)
		val source = Source(mrList)
		val taskFuture = source.runWith(MongoSink.insertOne[MovementRecord](allMR))
		taskFuture.onComplete{
			case Success(_) => log.info("MarketOperations","createMovementRecord","Success",s"Created 1 MovementRecord")
			case Failure (ex) => log.warn("MarketOperations","createMovementRecord","Failure",s"Failed create 1 MovementRecord: $ex")
		}
	}

	def updateStalksPurchased(amount : Int) : Unit = {
		val source = MongoSource(allMR.find(classOf[MovementRecord]))
			.map(mr => DocumentUpdate(filter = Filters.eq("id", todayDateId()), update = Updates.set("stalksPurchased", mr.stalksPurchased + amount)))
		val taskFuture = source.runWith(MongoSink.updateOne(allMR))
		taskFuture.onComplete{
			case Success(_) => log.info("MarketOperations","updateStalksPurchased","Success",s"Updated MovementRecord ${todayDateId()}'s stalksPurchased")
			case Failure (ex) => log.warn("MarketOperations","updateStalksPurchased","Failure",s"Failed create 1 MovementRecord: $ex")
		}
	}

	def updateMovementRecordField[A](mr : MovementRecord, key :String, value : A) : Unit = {
		val source = MongoSource(allMR.find(classOf[MovementRecord]))
			.map(_ => DocumentUpdate(filter = Filters.eq("id", mr.id), update = Updates.set(key, value)))
		val taskFuture = source.runWith(MongoSink.updateOne(allMR))
		taskFuture.onComplete{
			case Success(_) => ""
			case Failure (ex) => log.warn("MarketOperations","updateMovementRecordField","Failure",s"Failed to update MovementRecord ${mr.id}'s $key: $ex")
		}
	}

	def massUpdateMovementRecord(mr : MovementRecord) : Unit = {
		updateMovementRecordField(mr, "orderNum", mr.orderNum)
		updateMovementRecordField(mr, "hourBlockId", mr.hourBlockId)
		updateMovementRecordField(mr, "quarterBlockId", mr.quarterBlockId)
		updateMovementRecordField(mr, "todayHigh", mr.todayHigh)
		updateMovementRecordField(mr, "todayLow", mr.todayLow)
		updateMovementRecordField(mr, "stalksPurchased", mr.stalksPurchased)
		updateMovementRecordField(mr, "latestTurnip", mr.latestTurnip)
		updateMovementRecordField(mr, "turnipHistory", mr.turnipHistory)
		updateMovementRecordField(mr, "hourBlockName", mr.hourBlockName)
		updateMovementRecordField(mr, "year", mr.year)
		updateMovementRecordField(mr, "month", mr.month)
		updateMovementRecordField(mr, "day", mr.day)

		log.info("MarketOperations","updateMovementRecord","Success",s"Updated ${mr.id}'s MovementRecord")
	}

	def readEarliestMovementRecord(): Any = {
		readMovementRecord() match {
			case "empty" => "empty"
			case mr: Vector[MovementRecord] => mr.head
		}
	}

	def readLatestMovementRecord(): Any = {
		readMovementRecord() match {
			case "empty" => "empty"
			case mr: Vector[MovementRecord] => mr.reverse.head
		}
	}

	def readLastNDaysMovementRecords(n : Int): Any = {
		readMovementRecord() match {
			case "empty" => "empty"
			case mr: Vector[MovementRecord] => mr.reverse.take(n)
		}
	}


	def readMovementRecord(): Any = {
		val source = MongoSource(allMR.find(classOf[MovementRecord]))
		val mrSeqFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(mrSeqFuture, chill seconds).toVector
		safeVector(() => result ,"readMovementRecord")
	}


	def deleteOldestMovementRecords(month : Int) :  Unit = {
		val mrList = readMovementRecord() match {
			case "empty" => List()
			case mr: Vector[MovementRecord] => mr.toList
		}
		val source = Source(mrList).map(_ => Filters.eq("month", month))
		val taskFuture = source.runWith(MongoSink.deleteMany(allMR))
		taskFuture.onComplete{
			case Success(_) => {
				val updatedMrList = readMovementRecord() match {
					case "empty" => List()
					case mr: Vector[MovementRecord] => mr.toList
				}
				val deletedMR = mrList.length - updatedMrList.length
				log.info("MarketOperations","deleteOldestMovementRecords","Success",s" Deleted $deletedMR MovementRecord(s)")
			}
			case Failure (ex) => log.warn("MarketOperations","deleteOldestMovementRecords","Failure",s"Failed to delete MovementRecord(s): $ex")
		}
	}
}