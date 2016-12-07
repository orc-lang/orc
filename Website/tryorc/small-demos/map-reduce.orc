{-
A demonstration of the map/reduce idiom expressed in Orc.
This program is not actually allocating the map/reduce
tasks to different machines, but it shows how cleanly the
high-level data flow can be expressed in Orc; all that's
needed to actually distribute the computation is suitable
sites to which the data can be passed at each phase.
-}

import class ConcurrentMap = "java.util.concurrent.ConcurrentHashMap"

-- Publish every element of a Java iterator
def eachIterable(it) = repeat(IterableToStream(it))

{-
The first half of the program is the map/reduce framework,
which is assumed fixed.
-}

-- Build a map of mapper output buffers. One for each key.
val mapOutputBuffers = ConcurrentMap()
def getMapOutputBuffer(k) = 
  mapOutputBuffers.putIfAbsent(k, Channel()) >> mapOutputBuffers.get(k)

-- Given a key, return a function which stores
-- that key.  Here we store all keys in a single
-- buffer, but a real implementation would hash
-- keys to a buffer on the machine where they would
-- be reduced
def partition(k) = 
  val out = getMapOutputBuffer(k)
  lambda (k,v) = out.put(v)

-- To simulate reading data we publish each element
def read(data) = each(data)
-- To simulate writing data we just publish it
def write(datum) = datum

-- Implement a retry policy; if the site f
-- doesn't respond in 1 second, call it again
def retry(f) =
  lambda (a,b) = (
    val (ok, value) = (true, f(a,b)) | (false, Rwait(1000))
    if ok then value else retry(f)(a,b)
  )

-- The map phase reads data, maps it,
-- partitions it, and stores it
def MAP(mapper, data) =
  read(data) >(k1,v1)>
  retry(mapper)(k1,v1) >kvs>
  each(kvs) >(k2,v2)>
  partition(k2) >store>
  store(k2,v2) >>
  stop

-- The reduce phase sorts data,
-- groups it, reduces it, and writes it
def REDUCE(reducer) =
  -- For each mapper output buffer reduce that list.
  eachIterable(mapOutputBuffers.entrySet()) >entry> (
    val k = entry.getKey()
    val vs = entry.getValue().getAll()
    retry(reducer)(k, vs) >v>
    write((k,v))
  )

{-
The second half of the program is the user-provided
mapper and reducer functions. For this example,
our data will be lists of numbers, and we will
count the total number of times each number appears.
-}

def mapper(name, numbers) =
  def makeTuple(v) = (v,1)
  map(makeTuple, numbers)

def reducer(number, counts) =
  foldl((+), 0, counts)

val data = [
  ("primes", [2, 3, 5, 7, 11]),
  ("odd", [1, 3, 5, 7, 9, 11]),
  ("trees", [1, 2, 3, 6, 11]) ]

MAP(mapper, data) ; REDUCE(reducer)
