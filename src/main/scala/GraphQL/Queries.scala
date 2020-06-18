package GraphQL
import Model.Bug_._
import Model.Fish_._
import Model.MovementRecord_._
import Model.TurnipTransaction_._
import Model.User_._
import Service._
import zio.{IO, UIO}

object Queries {

	//Type Definitions not associated with any class
	case class creatureNameArgs(name : String)
	case class dummyArgs(dummy : Boolean)

	case class Queries(
		//User
		getUser:                        usernameArgs => IO[NotFound, User],
		getDoesUserExist:               usernameArgs => UIO[Boolean],
		getUsersWithCBAdded:            dummyArgs => UIO[List[User]],

	    //TurnipTransaction
		validatePendingTransaction:     pendingTransactionArgs => UIO[TurnipTransaction],

	    //MovementRecord
		getDayRecords:                  dummyArgs => UIO[MovementRecord],
		getNDayRecords:                 daysArgs => UIO[List[MovementRecord]],
		getTurnipPrices:                dummyArgs => UIO[Int],

		//Bug
		getAllBugs:                     UIO[List[Bug]],
		getAllBugsByMonth:              bugMonthsArgs => IO[NotFound, List[Bug]],
		getAllRareBugsByMonth:          bugMonthsArgs => IO[NotFound, List[Bug]],
		getBugById:                     bugIdArgs => IO[NotFound, Bug],
		getBugByName:                   bugNameArgs => IO[NotFound, Bug],
		getBugByRandom:                 dummyArgs => IO[NotFound, Bug],
		//Fish
		getAllFishes:                   UIO[List[Fish]],
		getAllFishesByMonth:            fishMonthsArgs => IO[NotFound, List[Fish]],
		getAllRareFishesByMonth:        fishMonthsArgs => IO[NotFound, List[Fish]],
		getFishById:                    fishIdArgs => IO[NotFound, Fish],
		getFishByName:                  fishNameArgs => IO[NotFound, Fish],
		getFishByRandom:                dummyArgs => IO[NotFound, Fish],

		//Quality of life
		getCreatureSummaryByName:       creatureNameArgs => IO[NotFound, String]
		)
		val cbs : CrossingBotService = new CBS()


	val allQueries = Queries(
		args => cbs.getUser(args.username),
		args => cbs.getDoesUserExist(args.username),
		args => cbs.getUsersWithCBAdded(args.dummy),
		args => cbs.validatePendingTransaction(args.username, args.business, args.quantity),
		args => cbs.getDayRecords(args.dummy),
		args => cbs.getNDayRecords(args.days),
		args => cbs.getTurnipPrices(args.dummy),
		cbs.getAllBugs,
		args => cbs.getAllBugsByMonth(args.months),
		args => cbs.getAllRareBugsByMonth(args.months),
		args => cbs.getBugById(args.id),
		args => cbs.getBugByName(args.name),
		args => cbs.getBugByRandom(args.dummy),
		cbs.getAllFishes,
		args => cbs.getAllFishesByMonth(args.months),
		args => cbs.getAllRareFishesByMonth(args.months),
		args => cbs.getFishById(args.id),
		args => cbs.getFishByName(args.name),
		args => cbs.getFishByRandom(args.dummy),
		args => cbs.getCreatureSummaryByName(args.name)
	)
}
