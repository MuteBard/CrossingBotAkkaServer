package Dao

import Actors.Initializer.system
import Actors.Initializer.system.dispatcher
import Auxillary.Time._
import Model.HourBlock_.HourBlock
import Model.MarketRecord_.MarketRecord
import Model.QuarterBlock_.QuarterBlock
import Model.TurnipTime_.TurnipTime
import akka.stream.alpakka.mongodb.DocumentUpdate
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.scaladsl.{Sink, Source}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{Filters, Updates}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.Exception.allCatch
import scala.util.{Failure, Success}


object  MarketOperations extends MongoDBOperations {
	val codecRegistryStalks: CodecRegistry = fromRegistries(fromProviders(classOf[MarketRecord],classOf[HourBlock], classOf[QuarterBlock], classOf[TurnipTime]), DEFAULT_CODEC_REGISTRY)
	final val chill = 10

	private val allMR = db
		.getCollection("MarketRecord", classOf[MarketRecord])
		.withCodecRegistry(codecRegistryStalks)


	def safeList(value : () => List[MarketRecord], methodName : String) :  Any = {
		value() match {
			case mrs if mrs.isEmpty =>
				log.warn("MarketOperations", methodName, "Initialization or Failure")
				"empty"
			case mrs =>
				mrs
		}
	}

	def safeListHead(value : () => List[MarketRecord], methodName : String) : Any = {
		allCatch.opt(value().head) match {
			case None =>
				log.warn("MarketOperations", methodName, "Failure")
				"empty"
			case Some(mr) => mr
		}
	}

	def createMarketRecord(mr : MarketRecord): Unit = {
		val mrList : List[MarketRecord] = List(mr)
		val source = Source(mrList)
		val taskFuture = source.runWith(MongoSink.insertOne[MarketRecord](allMR))
		taskFuture.onComplete{
			case Success(_) => log.info("MarketOperations","createMarketRecord","Success",s"Created 1 MarketRecord")
			case Failure (ex) => log.warn("MarketOperations","createMarketRecord","Failure",s"Failed create 1 MarketRecord: $ex")
		}
	}

	def updateStalksPurchased(amount : Int) : Unit = {
		val source = MongoSource(allMR.find(classOf[MarketRecord]))
			.map(mr => DocumentUpdate(filter = Filters.eq("id", todayDateId()), update = Updates.set("stalksPurchased", mr.stalksPurchased + amount)))
		val taskFuture = source.runWith(MongoSink.updateOne(allMR))
		taskFuture.onComplete{
			case Success(_) => log.info("MarketOperations","updateStalksPurchased","Success",s"Updated MarketRecord ${todayDateId()}'s stalksPurchased")
			case Failure (ex) => log.warn("MarketOperations","updateStalksPurchased","Failure",s"Failed create 1 MarketRecord: $ex")
		}
	}

	def updateMarketRecordField[A](mr : MarketRecord, key :String, value : A) : Unit = {
		val source = MongoSource(allMR.find(classOf[MarketRecord]))
			.map(_ => DocumentUpdate(filter = Filters.eq("id", mr.id), update = Updates.set(key, value)))
		val taskFuture = source.runWith(MongoSink.updateOne(allMR))
		taskFuture.onComplete{
			case Success(_) => ""
			case Failure (ex) => log.warn("MarketOperations","updateMarketRecordField","Failure",s"Failed to update MarketRecord ${mr.id}'s $key: $ex")
		}
	}

	def massUpdateMarketRecord(mr : MarketRecord) : Unit = {
		updateMarketRecordField(mr, "orderNum", mr.orderNum)
		updateMarketRecordField(mr, "hourBlockId", mr.hourBlockId)
		updateMarketRecordField(mr, "quarterBlockId", mr.quarterBlockId)
		updateMarketRecordField(mr, "todayHigh", mr.todayHigh)
		updateMarketRecordField(mr, "todayLow", mr.todayLow)
		updateMarketRecordField(mr, "stalksPurchased", mr.stalksPurchased)
		updateMarketRecordField(mr, "latestTurnip", mr.latestTurnip)
		updateMarketRecordField(mr, "turnipHistory", mr.turnipHistory)
		updateMarketRecordField(mr, "hourBlockName", mr.hourBlockName)
		updateMarketRecordField(mr, "year", mr.year)
		updateMarketRecordField(mr, "month", mr.month)
		updateMarketRecordField(mr, "day", mr.day)

		log.info("MarketOperations","updateMarketRecord","Success",s"Updated ${mr.id}'s MarketRecord")
	}

	def readLatestNMarketRecords(n : Int) : Any = {
		readMarketRecords() match {
			case "empty" => "empty"
			case mrs : List[MarketRecord] => generateLatestNMarketRecords(mrs, n)
		}
	}

	def readEarliestMarketRecord : Any = {
		readMarketRecords() match {
			case "empty" => "empty"
			case mrs : List[MarketRecord] => mrs.filter(mr => mr.orderNum == mrs.map(mr => mr.orderNum).min).head
		}
	}

	def readLatestMarketRecord : Any = {
		readMarketRecords() match {
			case "empty" => "empty"
			case mrs : List[MarketRecord] => mrs.filter(mr => mr.orderNum == mrs.map(mr => mr.orderNum).max).head
		}
	}


	def generateLatestNMarketRecords(mrList : List[MarketRecord], n : Int) : List[MarketRecord] = {
		def findNextMax(mrList : List[MarketRecord]) : (MarketRecord, List[MarketRecord]) = {
			val max = mrList.filter(mr => mr.orderNum == mrList.map(mr => mr.orderNum).max).head
			val tail = mrList.filter(mr => mr.orderNum != mrList.map(mr => mr.orderNum).max)
			Tuple2(max, tail)
		}

		@scala.annotation.tailrec
		def innerGeneration(n : Int, size: Int,  list : ListBuffer[(MarketRecord, List[MarketRecord])]) : List[MarketRecord] = {
			if(n == size){
				list.toList.map(tuples => tuples._1)
			}else{
				if(n == 0){
					list  += findNextMax(mrList)
					innerGeneration(n + 1, size, list)
				}
				else{
					list += findNextMax(list.reverse.head._2)
					innerGeneration(n + 1, size, list)
				}
			}
		}

		val size = if( n <= mrList.length ) n else mrList.length
		val list = new ListBuffer[(MarketRecord, List[MarketRecord])]()
		innerGeneration(0, size, list)
	}

	def readMarketRecords(): Any = {
		val source = MongoSource(allMR.find(classOf[MarketRecord]))
		val mrFuture = source.runWith(Sink.seq)
		lazy val result = Await.result(mrFuture, chill seconds).toList
		safeList(() => result , "readMarketRecord")
	}

	def deleteOldestMarketRecords(month : Int) :  Unit = {
		val mrList = readMarketRecords() match {
			case "empty" => List()
			case mr: List[MarketRecord] => mr
		}
		val source = Source(mrList).map(_ => Filters.eq("month", month))
		val taskFuture = source.runWith(MongoSink.deleteMany(allMR))
		taskFuture.onComplete{
			case Success(_) =>
				val updatedMrList = readMarketRecords() match {
					case "empty" => List()
					case mr: List[MarketRecord] => mr
				}
				val deletedMR = mrList.length - updatedMrList.length
				log.info("MarketOperations","deleteOldestMarketRecords","Success",s" Deleted $deletedMR MarketRecord(s)")
			case Failure (ex) => log.warn("MarketOperations","deleteOldestMarketRecords","Failure",s"Failed to delete MarketRecord(s): $ex")
		}
	}
}