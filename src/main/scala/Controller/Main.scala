package Controller

import zio.{Runtime, ZEnv}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import caliban.GraphQL.graphQL
import caliban.RootResolver
import Actors.Initializer.system
import GraphQL.Queries.allQueries
import GraphQL.Mutations.allMutations
import caliban.interop.circe.AkkaHttpCirceAdapter
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

object Main extends App with AkkaHttpCirceAdapter {
	implicit val executionContext: ExecutionContextExecutor = system.dispatcher
	implicit val runtime: Runtime[ZEnv] = Runtime.default
	val api = graphQL(RootResolver(allQueries, allMutations))
	val interpreter = runtime.unsafeRun(api.interpreter)

	val route =
		cors(){
			path("api" / "graphql") {
				adapter.makeHttpService(interpreter)
			} ~ path("graphiql") {
				getFromResource("graphiql.html")
			}
		}

	val bindingFuture = Http().bindAndHandle(route, "localhost", 5000)
	println(s"Server online at http://localhost:5000/graphiql\nPress RETURN to stop...")
	StdIn.readLine()
	bindingFuture
		.flatMap(_.unbind())
		.onComplete(_ => system.terminate())
}

//Thanks to ghostdogpr for Caliban
//https://medium.com/@ghostdogpr/graphql-in-scala-with-caliban-part-1-8ceb6099c3c2
//https://github.com/ghostdogpr/caliban/blob/master/examples/src/main/scala/caliban/akkahttp/ExampleApp.scala