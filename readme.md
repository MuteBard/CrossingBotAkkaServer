# Crossing Bot Akka Server (CBAS)

## Akka Actors

Consists of 5 Akka actors that drive the functionality of the server by facilating communication bectween the MongoDB based 
data access objects and the Controller:

- StartActor

- FishActor

- BugActor

- UserActor

- MarketActor

### Start Actor

Manages one time operations such as populating bug and fish collections and starting timers + providing time data for the MarketActor

### Fish Actor

Manages read operations for the mongoDB Fish collection that be needed throughout the application, notably User Actor

### Bug Actor

Also manages read operations but for the mongoDB Bug collection

### User Actor

Manages a series of Create, Read, Update and "Delete" operations for the mongoDB User collection. Operations that manage a Twitch user's state as they utilize the bot on a streamer's channel.
Users are created at the first instance they use !bug or !fish commands. UserActor places caught bugs and fishes in the user's pocket. It facilitate the selling creatures either one at a time or all at once from the user's pocket. And lastly as of now facilitate the buying or selling turnips by the market pricethe user purcased them at with the aid of the MarketActor

### The Market Actor

Also manages a series of Create, Read, Update and genuine Delete operations regarding the mongoDB MovementRecord collection.
This actor calls a method that generates weighted random "stalk" (stock) values for the 24 hours. Each 24 hour interval contains what call an "HourBlock". An hourblock contains four of what is called a "QuarterBlock"

QuarterBlocks can have one of 7 states:
  
- Heavenly
- Awesome
- Good
- Neutral
- Bad
- Awful
- Hellish

Heavenly quarterblocks have a chance of providing +50 to +100 to a value of a turnip for that 15 minute interval of the day
Awesome quarterblocks have a chance of providing +15 to +35 to a value of a turnip
Good quarterblocks have a chance of providing +5 to +15 to a value of a turnip
Neutral quarterblocks have a chance of providing -5 to +5 to a value of a turnip
Bad quarterblocks have a chance of providing -15 to -5 to a value of a turnip
Awful quarterblocks have a chance of providing -35 to -15 to a value of a turnip
Hellish quarterblocks have a chance of providing -100 to -50 to a value of a turnip

4 quarterblocks are combined together to create one hourblock. However there are assortments of quarterblocks within hourblocks that are predetermined and not left to complete randomness. HourBlocks can have one of 5 states and have weighted probablilties of occuring:

- sleepy (Has a 30% chance of occurring)
- normal (Has a 20% chance of occurring)
- good (Has a 20% chance of occurring)
- bad (Has a 10% chance of occurring)
- risky (Has a 10% chance of occurring)

sleepy hourblocks consist of 4 neutral quarterblocks
normal hourblocks consist of a random assortment of 4 quarterblocks that can either be good, neutral or bad
good hourblocks consist of a random assortment of 4 quarterblocks that can be either awesome, good or neutral
bad hourblocks consist of a random assortment of 4 quarterblocks that can be either awful, bad or neutral
risky hourblocks consist of a terrifying random assortment of 4 quarterblocks that can be either heavenly or hellish

A marketDay consists of 24 hourblocks and 96 quarterblocks. Under a timer, the marketActor pulls a quarterblock at every quarter hour (:00, :15, :30 and :45) and stores a quarterblock within the MovementRecord. A MovementRecord contains info of the Hourblock and Quarterblock the market is currently executing, the min and max of turnips price of the day, the number of users who currenly have turnips in the stalkmarket as will as a history list of quarterblocks of that day at each 15 minute interval

When a midnight passes, a new MovementRecord is created and added to the collection for that day and the pocess starts again. The Market Actor in short, manages this collection.

## DAO

Utilizes [Lightbend's Alpakka MongoDB connector](https://doc.akka.io/docs/alpakka/current/mongodb.html "Alpakka Documentation for  MongoDB") to facilitate communication between MongoDB and Akka

Each of the Akka Actors have their own Dao that they manage, should they request data from eachother, the actors may "ask" the other actor for the data.
