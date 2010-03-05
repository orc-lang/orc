def capsule RW() = 
  val read_count = Ref[Integer](0)
  val write_count = Ref[Integer](0)

  val read_count_mtx = Semaphore(1)
  val write_count_mtx = Semaphore(1)
  val entry_lock = Semaphore(1) -- prevents deadlock
  val read_mtx = Semaphore(1)
  val write_mtx = Semaphore(1)

  def read_start() = 
    entry_lock.acquire() >>
    read_mtx.acquire() >>
    read_count_mtx.acquire() >>
    read_count := read_count? + 1 >>
    (if (read_count? = 1) then write_mtx.acquire() else signal) >>
    read_count_mtx.release() >>
    read_mtx.release() >>
    entry_lock.release()
  
  def read_finish() = 
    read_count_mtx.acquire() >>
    read_count := read_count? - 1 >>
    (if (read_count? = 0) then write_mtx.release() else signal) >>
    read_count_mtx.release()
  
  def write_start() = 
    entry_lock.acquire() >>
    write_count_mtx.acquire() >>
    write_count := write_count? + 1 >>
    (if (write_count? = 1) then read_mtx.acquire() else signal) >>
    write_count_mtx.release() >>
    entry_lock.release() >>
    write_mtx.acquire()
  
  def write_finish() =
    write_mtx.release() >> 
    write_count_mtx.acquire() >>
    write_count := write_count? - 1 >>
    (if (write_count? = 0) then read_mtx.release() else signal) >>
    write_count_mtx.release()
  signal

    
val v = Ref[Integer](0)


-- Typechecker will fail here because we do not have a syntax for record types yet
def reader(lock) = 
  Rtimer((random(4)+1)*100) >>
  lock.read_start() >>
--  println(v?) >>
  lock.read_finish()

def writer(lock) = 
  Rtimer((random(4)+1)*100) >>
  lock.write_start() >>
  v := v? + 1 >>
  lock.write_finish()

val rw = RW()


(upto(1000) >> reader(rw) >> stop | upto(1000) >> writer(rw) >> stop); 
println("Final value: "+(v?)) >> stop
{-
OUTPUT:
Final value: 1000
-}


