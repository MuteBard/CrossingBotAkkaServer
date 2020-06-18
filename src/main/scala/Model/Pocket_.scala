package Model
import Bug_.Bug
import Fish_.Fish


object Pocket_ {
	case class Pocket(
		 bug : List[Bug] = List(),
		 fish : List[Fish] = List(),
	)
}
