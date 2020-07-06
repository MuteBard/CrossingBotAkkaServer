package Model
import scala.util.Random
object QuarterBlock_ {
	case class QuarterBlock(name : String, hourBlockId: Int, quarterBlockId: Int, max : Int, min : Int, sign: String, change : Int = 0){
		def create(hbId : Int, qbId: Int) : QuarterBlock = {
			val random = new Random
			val difference = this.max - this.min
			val value = random.nextInt(difference) + this.min
			if(this.sign == "+"){
				QuarterBlock(this.name, hbId, qbId, this.max, this.min, this.sign, value)
			}else if(this.sign == "-"){
				QuarterBlock(this.name, hbId, qbId, this.max, this.min, this.sign, -1 * value)
			}else{
				if (value % 2 == 0){
					QuarterBlock(this.name, hbId, qbId, this.max, this.min, this.sign, value)
				}else{
					QuarterBlock(this.name, hbId, qbId, this.max, this.min, this.sign, -1 * value)
				}
			}
		}
	}
}
