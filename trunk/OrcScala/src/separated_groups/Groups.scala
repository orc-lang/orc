// An experiment in modularity: packaging token groups as a separate trait,
// to be mixed in to an Orc implementation.

// At present this accomplishes nothing that just pasting the code into
// the Orc class would not do equally as well. But it's a fun thought experiment
// and separates the functionality into separate files interacting only
// through APIs.

trait Groups {
	self : OrcAPI =>
	
	import Oil.Value
	
	// Groups

// A Group is a structure associated with dynamic instances of an expression,
// tracking all of the executions occurring within that expression.
// Different combinators make use of different Group subclasses.

	trait GroupMember {
		def kill: Unit
	}
	implicit def TokensAreGroupMembers(t: Token): GroupMember = (t : GroupMember)
	

	abstract class Group extends GroupMember {
		
	def publish(t: Token, v: Value): Unit
	def onHalt: Unit

	import scala.collection.mutable.Set	
	var members: Set[GroupMember] = Set()
	
	def halt(t: Token) { remove(t) }
	def kill { for (m <- members) m.kill } 
	/* Note: this is _not_ lazy termination */

	def add(m: GroupMember) { members.add(m) }

	def remove(m: GroupMember) { 
		members.remove(m)
		if (members.isEmpty) { onHalt }
	}
	
	
	}




// A Groupcell is the group associated with expression g in (f <x< g)

// Possible states of a Groupcell
class GroupcellState
case class Unbound(waitlist: List[Token]) extends GroupcellState
case class Bound(v: Value) extends GroupcellState
case object Dead extends GroupcellState

class Groupcell(parent: Group) extends Group {

	var state: GroupcellState = Unbound(Nil) 

	def publish(t: Token, v: Value) {
	state match {
	case Unbound(waitlist) => {
		state = Bound(v)
		schedule(waitlist)
		t.halt
	}
	case _ => t.halt	
	}
}

def onHalt {
	state match {
	case Unbound(waitlist) => {
		for (t <- waitlist) t.halt
		state = Dead
		parent.remove(this)
	}
	case _ => {  }
	}
}

// Specific to Groupcells
def read(reader: Token): Option[Value] = 
	state match {
	case Bound(v) => Some(v)
	case Unbound(waitlist) => {
		state = Unbound(reader :: waitlist)
		None
	}
	case Dead => {
		reader.halt
		None
	}
}
}

object Groupcell {
	def apply(parent: Group): Groupcell = {
		val g = new Groupcell(parent)
		parent.add(g)
		g
	}
}




// A Region is the group associated with expression f in (f ; g)
class Region(parent: Group, r: Token) extends Group {

	// Some(r): No publications have left this region;
	//			if the group halts silently, pending
	//			will be scheduled.
	// None:	A publication has left this region.

	var pending: Option[Token] = Some(r)

	def publish(t: Token, v: Value) {
	pending.foreach(_.halt)
	t.publish(v)
}

def onHalt {
	pending.foreach(schedule(_))
	parent.remove(this)
}

}	

object Region {
	
	def apply(parent: Group, r: Token): Region = {
		val g = new Region(parent, r)
		parent.add(g)
		g
	}
}
	
	
	
}