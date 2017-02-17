-- Tying the knot using lenient objects
-- "Tying the knot" in the same sense as cyclical values in haskell

class LList {
  val head
  val tail
  
  def toString() :: String
}

class LCons extends LList {
  def toString() :: String = head + " :L: " + tail.toString()
}
val LCons = new {
  def apply(h, t :: LList) = new LCons {
    val head = h
    val tail = t
  }
  def unapply(v) = 
    (v.head, v.tail) ; stop
}

val LNil = new LList {
  val head = stop
  val tail = stop

  def toString() :: String = "LNil"
  
  def unapply(v) = if v = this then signal else stop 
}


-- TODO: Take cannot be a method on LList (like it should be) because it would require LList to be recursive with other defs and vals outside the class group
def take(LList, Integer) :: LList
def take(_, 0) = LNil
def take(LNil(), _) = LNil
def take(l, n) = LCons(l.head, take(l.tail, n-1))
  
val o = new {
  val x = LCons(1, LCons(2, x))
}

Println(LCons(1, LCons(2, LNil)).toString()) >>
Println(take(LCons(1, LCons(2, LNil)), 1).toString()) >>
Println(take(o.x, 10).toString()) >> 
stop

{-
OUTPUT:
1 :L: 2 :L: LNil
1 :L: LNil
1 :L: 2 :L: 1 :L: 2 :L: 1 :L: 2 :L: 1 :L: 2 :L: 1 :L: 2 :L: LNil
-}