package Dao

import com.mongodb.reactivestreams.client.MongoClients

class MongoDBOperations {
	val client = MongoClients.create("mongodb://localhost:27017")
	protected val db = client.getDatabase("crossingbot")
	client
}