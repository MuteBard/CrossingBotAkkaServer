package GraphQL
import Model.MovementRecord_.toggleArgs
import Model.TurnipTransaction_._
import Model.User_._
import Service._
import zio.{IO, UIO}

object Mutations {

	case class dummyArgs(dummy : Boolean)
	case class Mutations(
	    //User
	    createOneUser :           createUserArgs => UIO[String],
	    signUp:                   authArgs => UIO[String],
	    signIn:                   authArgs => UIO[Boolean],
	    catchCreature :           catchCreatureArgs => IO[NotFound, String],
	    isCrossingBotAdded:       addToChannelArgs => UIO[String],
	    finalizeUserCreation :     finalizeUserArgs => IO[NotFound, String],
	    sellOneCreature :         sellCreatureArgs => UIO[Int],
	    sellAllBugs :             usernameArgs => UIO[Int],
	    sellAllFishes :           usernameArgs => UIO[Int],
	    sellAllCreatures :        usernameArgs => UIO[Int],
	    acknowledgeTransaction:   authorizedTransactionArgs => IO[NotFound, String],
	    populate:                 UIO[String],
	    toggleMarket:             toggleArgs => UIO[String],
	    deleteUser:               usernameArgs => UIO[String]

	)
	val cbs : CrossingBotService = new CBS()

	val allMutations = Mutations(
		args => cbs.createOneUser(args.username, args.id, args.avatar, args.addedToChannel),
		args => cbs.signUp(args.username, args.encryptedPw),
		args => cbs.signIn(args.username, args.encryptedPw),
		args => cbs.catchCreature(args.username, args.species),
		args => cbs.isCrossingBotAdded(args.username, args.added),
		args => cbs.finalizeUserCreation(args.username, args.id, args.avatar),
		args => cbs.sellOneCreature(args.username, args.species, args.creatureName),
		args => cbs.sellAllBugs(args.username),
		args => cbs.sellAllFishes(args.username),
		args => cbs.sellAllCreatures(args.username),
		args => cbs.acknowledgeTransaction(args.username, args.business, args.quantity, args.marketPrice, args.totalBells),
		cbs.populate,
		args => cbs.toggleMarket(args.running),
		args => cbs.deleteUser(args.username)
	)
}
