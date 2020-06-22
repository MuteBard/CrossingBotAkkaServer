package Dao

import com.mongodb.reactivestreams.client.{MongoClient, MongoClients, MongoDatabase}

class MongoDBOperations {
	val uri : String = sys.env.getOrElse("CB_MONGODB_URI", "mongodb://localhost:27017")
	val dbname : String = sys.env.getOrElse("DBNAME", "crossingbot")
	val client: MongoClient = MongoClients.create(uri)
	protected val db: MongoDatabase = client.getDatabase(dbname)
	client
}


