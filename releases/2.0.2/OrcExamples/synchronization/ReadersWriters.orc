{- ReadersWriters.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 30, 2010 2:25:16 PM
 -}
 
{-A RelaxedChannel implements the explicit channel in a more compact
fashion. It supports the put and get operations as before. But, unlike
a FIFO channel it only guarantees that every entry is eventually
removed. 

We can not claim that the readers will get service in the order in
which they were put in the channel. When a semaphore, r or w, is
released, it may be acquired by any waiting process. However, we can
assert the following: every process will acquire the corresponding
semaphore eventually. This is because Orc implements strong semaphore;
any waiting process for a semaphore receives the semaphore provided it
is released arbitrarily often. Keeping a channel corresponding to the
waiting processes ensures that both semaphores will be released
arbitrarily often. That is, we will not be releasing w only while
there are waiting readers.

We don't need a FIFO channel to ensure this property. A RelaxedChannel
ensures that while there are waiting readers, some reader is
eventually removed (and similarly for the writers). Two mutable
entries nwr and nww count the number of waiting readers and waiting
writers. semaphore count is the sum of waiting entries (nwr? +
nww?). It is used to block access of get until there are waiting
entries.

A call to get removes a reader or a writer according to the
following policy:

1. If either nwr or nww is 0, then an entry of the other kind is
removed.

2. If neither nwr nor nww is 0, then a coin flip decides which one to
remove. Since coin flip is fair, it will not favor of one kind forever.
-}

def class RelaxedChannel() =
 val (nwr, nww) = (Ref(0),Ref(0)) -- # waiting readers, writers
 val mutex = Semaphore(1) -- to gain access to (nwr, nww)
 val count = Semaphore(0) -- nwr? + nww? 

 def put(b) = 
     mutex.acquire() >> 
     (if b then nwr := nwr? + 1 else nww := nww? + 1) >>
     mutex.release() >> count.release()

 def get() = 
    count.acquire() >>
    mutex.acquire() >> chooseone(nwr?,nww?) >b> mutex.release() >> b

  def removereader() = nwr := nwr? - 1 >> true
  def removewriter() = nww := nww? - 1 >> false
  
  def chooseone(0,_) = removewriter()
  def chooseone(_,0) = removereader()
  def chooseone(_,_) =      
      if (Random(2) = 0) then removereader() else removewriter()

 stop

def class ReadersWriters() =
  val buff = RelaxedChannel()
  val cb  = Counter()
  val (r,w) = (Semaphore(0),Semaphore(0))

def start(b) = buff.put(b) >> 
               (if b then r.acquire()                        
                     else w.acquire())

def end() = cb.dec()

def main() =
   buff.get() >b> 
   (if  b 
      then  (cb.inc() >> r.release()  >> main())
      else  (cb.onZero() >> cb.inc() >> w.release() >> cb.onZero() >> main())
   )

main()

val rw = ReadersWriters()

  rw.start(true) >> Println("1 read") >> Rwait(75) >> rw.end()
| Rwait(50) >> rw.start(true)  >> Println("2 read") >> rw.end()
| Rwait(60) >>  rw.start(false) >> Println("3 write") >> rw.end()


----------------------------------------------------
{-
Readers-Writers written with semaphore pool

def class ReadersWriters() =
val req      = Channel()
val sempool  = SemaphorePool()

def start(b) = -- read coded as "true", write as "false"
 val s = sempool.allocate(0)
 req.put((b,s)) >> s.acquire() >> sempool.deallocate(s)

def end() = cb.dec()

def main() =
   req.get() >(b,s)>
   (if  b then  cb.inc() >> s.release()  >> main()
    else cb.onZero() >> cb.inc() >> s.release() >> cb.onZero() >> main()
   )

main()

-}