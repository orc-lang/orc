package orc.test.item.scalabenchmarks.canneal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.util.concurrent.ThreadLocalRandom
import scala.annotation.tailrec
import java.util.concurrent.atomic.AtomicReference
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

final case class Location(x: Int, y: Int) {
  /** Compute the manhattan distance between this and o.
	  */
  def dist(o: Location): Int = (x - o.x).abs + (y - o.y).abs
}

object LocationReference {
  @inline
  val LOCKED_SENTINAL = Location(Int.MinValue, Int.MinValue)
}

final class LocationReference() {
  import LocationReference._
  
  private[this] val ref = new AtomicReference[Location](null)
  
  // The guarantees provided by this implementation are slightly stronger than those of the C++ implementation. This is due to Java 8 not having release/acquire semantics split. With Java 9 we could fix this.
  
  @tailrec
  @inline
  def get(): Location = {
    val l = ref.get() // BARRIER: acquire
    if (l ne LOCKED_SENTINAL) l else get()
  }
  
  @tailrec
  @inline
  private def setInternal(l: Location): Location = {
    val old = get()
    if(ref.compareAndSet(old, l)) old else setInternal(l)
  }
  
  def set(l: Location): Unit = {
    assert(l ne LOCKED_SENTINAL)
    setInternal(l)
  }
  
  def checkout(): Location = {
    setInternal(LOCKED_SENTINAL)
  }
  
  def checkin(l: Location): Unit = {
    ref.lazySet(l)
  }
  
  def swap(o: LocationReference) = {
    val thisHash = System.identityHashCode(this)
    val oHash = System.identityHashCode(o)
    
    val (a, b) = if (thisHash < oHash) (this, o) else (o, this)
    
    val vA = a.checkout()
    val vB = b.setInternal(vA)
    a.checkin(vB)
  }
}

final class Element(val name: String) {
  val location: LocationReference = new LocationReference() 
  val fanin: ArrayBuffer[Element] = ArrayBuffer()
  val fanout: ArrayBuffer[Element] = ArrayBuffer()
  
  @inline
  def costAtLoc(l: Location): Double = {
    fanin.foldLeft(0.0)((acc, o) => acc + (l dist o.location.get())) +
      fanout.foldLeft(0.0)((acc, o) => acc + (l dist o.location.get()))
  }
  
  def swapCost(a: Location, b: Location): Double = {
    costAtLoc(b) - costAtLoc(a)
  }
}

final class NetList(nElements: Int, val maxX: Int, val maxY: Int) {
  val chipSize: Int = maxX * maxY
  val locations = Array.tabulate(chipSize)(i => Location(i % maxX, i / maxX))
  val elements = new ArrayBuffer[Element](chipSize)
  val elementMap = HashMap[String, Element]()
  
  def elementByName(name: String): Element = {
    elementMap.getOrElseUpdate(name, {
      val e = new Element(name)
      e.location.set(locations(elements.size))
      elements += e
      e
    })
  }
  
  def randomElement()(implicit rng: ThreadLocalRandom): Element = {
    elements(rng.nextInt(elements.size))
  }
  
  @tailrec
  def randomElement(differentFrom: Element)(implicit rng: ThreadLocalRandom): Element = {
    val maybe = randomElement()
    if (differentFrom != maybe) maybe else randomElement(differentFrom)
  }
  
  def randomPair()(implicit rng: ThreadLocalRandom): (Element, Element) = {
    val a = randomElement()
    @tailrec
    def b: Element = {
      val maybeB = randomElement()
      if (a != maybeB) maybeB else b
    }
    (a, b)
  }
  
  def shuffle() = {
    for (_ <- (0 until chipSize * 1000).par) {
      val (a, b) = randomPair()(ThreadLocalRandom.current())
      a.location.swap(b.location)
    }
  }
  
  def resetLocations() = {
    for ((e, l) <- (elements zip locations).par) {
      e.location.set(l)
    }
  }
  
  def totalCost(): Double = {
    elementMap.values.par.map(e => e.costAtLoc(e.location.get())).sum / 2
  }
}

object NetList {
  def apply(fn: String): NetList = {
    import java.util.Scanner
    import java.io.File
    
    val inStream = new GZIPInputStream(new FileInputStream(new File(fn)))
    
    val s = new Scanner(inStream)

    val nl = new NetList(s.nextInt(), s.nextInt(), s.nextInt())
    
    while (s.hasNext()) {
      val e = nl.elementByName(s.next())
      val tpe = s.nextInt()
      var wasEnd = false
      while (!wasEnd && s.hasNext()) { // Contains break
        val inName = s.next() 
        if (inName != "END") {
          val inE = nl.elementByName(inName)
          e.fanin += inE
          inE.fanout += e
        } else {
          wasEnd = true;
        }
      }
    }
    
    val realElements = nl.elements.size
    
    while (nl.elements.size < nl.chipSize) {
      val e = new Element("<UNUSED>")
      e.location.set(nl.locations(nl.elements.size))
      nl.elements += e
    }

    println(s"Loaded netlist of ${realElements} elements on a ${nl.maxX}x${nl.maxY} chip (${nl.chipSize}).")
    
    nl
  }
}