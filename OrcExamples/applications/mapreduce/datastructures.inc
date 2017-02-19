{- A collection of data structures for use in MapReduce
 -
 - Created by amp on Feb 17, 2017
 -}

import class ConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
import class ConcurrentLinkedQueue = "java.util.concurrent.ConcurrentLinkedQueue"

{-
A concurrent map.

This does not support null values.
-}
class Map {
  val underlying = ConcurrentHashMap()
  
  val put = underlying.put
    
  def getOrUpdate(k, f :: lambda() :: Top) = 
    get(k) ; underlying.putIfAbsent(k, f()) >> underlying.get(k)
    
  def get(k) = 
    val v = underlying.get(k)
    Iff(v = null) >> v
    
  def each() = 
    val stream = IterableToStream(underlying.entrySet())
    repeat(stream) >e> (e.getKey(), e.getValue())
}

{-
A concurrent bag.

This implementation provides FIFO ordering, but this is not guarenteed. An 
unordered implementation is allowed and will probably perform better.

This does not support null values.
-}
class Bag {
  val underlying = ConcurrentLinkedQueue()
  
  def add(v) = 
    underlying.add(v)
  
  def takeD() = 
    val v = underlying.poll()
    Iff(v = null) >> v
  
  val size = underlying.size
  
  val iterator = underlying.iterator
}
