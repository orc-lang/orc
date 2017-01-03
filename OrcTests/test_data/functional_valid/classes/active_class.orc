{- active_class.orc -- An active class object in Orc
 -
 - Created by amp on Jan 4, 2015 7:33:07 PM
 -}

class RW {
	val r = Semaphore(0)
	val w = Semaphore(0)
	val na = Counter(0)
	
	def startread() = this.r.acquire()
	def startwrite() = this.w.acquire()
	def end() = this.na.dec()
	
	def pick() = 
	    def h(true, false) = true
	    def h(false, true) = false
	    def h(true, true) = Random(2) = 0
	    def h(false, false) = {| this.w.snoop() >> false | this.r.snoop() >> true |}
	
	    val rw = this.r.snoopD() >> true ; false
	    val ww = this.w.snoopD() >> true ; false
	    h(rw, ww)
	
	val _ = repeat({  
	    if( this.pick() ) then
	      this.na.inc() >> this.r.release() -- Allow reader in
	    else
	      this.na.onZero() >> this.na.inc() >> this.w.release() >> this.na.onZero() -- Allow writer in after reader 
	      })
}

val v = Ref[Integer](0)

def reader(lock) =
  Rwait((Random(4)+1)*100) >>
  lock.startread() >>
--  Println(v?) >>
  lock.end()

def writer(lock) =
  Rwait((Random(4)+1)*100) >>
  lock.startwrite() >>
  v := v? + 1 >>
  lock.end()

{|
val rw = new RW #
( upto(1000) >> reader(rw) >> stop
| upto(1000) >> writer(rw) >> stop)
; Println("Final value: "+(v?))
|} >> stop

{-
OUTPUT:
Final value: 1000
-}