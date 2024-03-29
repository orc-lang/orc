{- general-rendezvous.orc
 - 
 - Created by misra on Mar 15, 2010 5:19:55 PM
 -}

{- n party rendezvous with data transfer.
   The instance of a Rendezvous specifies
     n: the number of processes in the rendezvous
     f: a distribution function that maps a list of n data items to
         a list of n data items.
The distribution function could be:
  - a 2-party rendezvous in which the first data item is placed as
  the second item in the result; the classical sender-receiver.

  - a 2-party rendezvous in which the two data items are swapped;
    it can simulate the classical sender-receiver.

  - a n-party rendezvous in which the first data item is given to all
    other parties. This is the broadcast paradigm.

  - a n-party rendezvous in which the value returned by the function
    is the rank of the value among all values. This can be used to
    radix-sorting in which each value is input on a separate channel
    one digit at a time; the ith process is the one that has the
    current rank i. As it inputs the next digit, its rank may change.

  - a n-party rendezvous for secret sharing.

Access protocol:
A party calls the rendezvous object with its own identity and data.
It receives the result value as the effect of the call.

Implementation strategy:
b is an array of n channels. The data submitted by process i is
added to b(i). Additionally, a callback cell is is also put in b(i)
where the result to i will be delivered. The data and cell are put
as a pair in b(i).

A manager sweeps through all the channels from 0 through n-1
removing one item from each, and storing them in a list.
After one sweep, it applies the distribution function to all the
data, and computes the result list. Next, it distributes the results
among the callback cells.

-}

-- TODO: Add [A] to List
class Rendezvous {
  val n :: Integer
  val f :: lambda(List) :: List

  -- TODO: Add [(A, Cell[A])] to Channel
  val b = Table(n, ignore({ Channel() }))

  -- TODO: Add A type to v and Cell 
  def apply(i :: Integer, v) =
    val c = Cell()
    b(i).put((v,c)) >> c.read()

  -- TODO: Add [A] to List and Cell types 
  def collect((List, List[Cell]), Integer) :: (List, List[Cell])
  def collect(vcl,0) = vcl
  def collect((vl,cl),i) =  -- collect i more items
    b(n-i).get() >(v,c)> collect((v:vl,c:cl),i-1)

  -- TODO: Add [A] to List and Cell types 
  def distribute(List, List[Cell]) :: Signal
  def distribute([],[]) = signal
{-
distribute(vl,cl)
 where vl is a list of results,
 cl is a list of cells,
 vl and cl are of the same length,
 stores each result in the corresponding cells.
-}
  def distribute(v:vl,c:cl) = c.write(v) >> distribute(vl,cl)

  def manager() :: Bot =
   collect(([],[]),n) >(vl,cl)> distribute(f(vl),cl) >> manager()

  val _ = manager()
}

{-
The following implementation does not use callback mechanism. But, it
uses two arrays of Channels, b and c. The ith process participating in
the rendezvous calls it as before, with its identity i and a value v
that would be used in the rendezvous. The call returns a value after
rendezvous is accomplished.

The implementation strategy is to:
deposits i's value in b(i). Then the manager proceeds as before and
deposits the corresponding result in c(i). Access to b,c is locked
using a semaphore array sem. Locking is required because of the following scenario: process p
deposits a value into b(i), later the manager removes this value,
process q can then deposit a value into b(i); now, both po and q can
wait to receive values from c(i), though p should be the only process
entitled to receive this value. So, we prevent q from depositing a value
into b(i) until p has completed its cycle, i.e., removed the result
from c(i).
-}

-- TODO: Add [A] to List
class Rendezvous2 {
  val n :: Integer
  val f :: lambda(List) :: List

  -- TODO: Add [A] to channels 
  val b = Table(n, ignore({ Channel() }))
  val c = Table(n, ignore({ Channel() }))
  val sem = Table(n, ignore({ Semaphore(1) }))

  -- TODO: Add :: A to v 
  def apply(i :: Integer, v) =
    sem(i).acquire() >>
    b(i).put(v) >> c(i).get() >w>
    sem(i).release() >>
    w

  -- TODO: Add [A] to List types 
  def collect(List, Integer) :: List
  def collect(vl,0) = vl
  def collect(vl,i) = b(n-i).get() >v> collect(v:vl,i-1)

  -- TODO: Add [A] to List types 
  def distribute(List, Integer) :: Signal
  def distribute(_,0) = signal
  def distribute(v:vl,i) = c(n-i).put(v) >> distribute(vl,i-1)

  def manager() :: Bot =
    collect([],n) >vl> distribute(f(vl),n) >> manager()

  val _ = manager()
}

{- Test -}

{-
def exch([a,b]) = [b,a]
val rg = new Rendezvous { 
  val n = 2
  val f = exch
}

  rg(0,0) >x> ("0 gets " + x) |  rg(0,3) >x> ("0 gets " + x)
| rg(1,2) >y> ("1 gets " + y)
| rg(1,5) >y> ("1 gets " + y)
-}

def avg([a,b,c] :: List[Integer])  =
  val av = (a+b+c)/3
  [av,av,av]

val rg3 = new Rendezvous2 { 
  val n = 3
  val f = avg
}

  rg3(0,0) >x> ("0 gets " + x)
| rg3(1,1) >x> ("1 gets " + x)
| rg3(2,5) >x> ("2 gets " + x)
| rg3(2,2) >x> ("2 gets " + x)
