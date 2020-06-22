package Dao

import com.mongodb.reactivestreams.client.MongoClients

class MongoDBOperations {

	val prod_uri = "mongodb://heroku_lk06xxv0:1s78gsn37rdg05sk60metdsakq@ds155288.mlab.com:55288/heroku_lk06xxv0"
	val local_uri = "mongodb://localhost:27017"
	val client = MongoClients.create(prod_uri)
	protected val db = client.getDatabase("crossingbot")
	client
}


