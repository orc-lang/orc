-- Adrian examples
--
-- Examples from Adrian Quark's Orc Secure Information Flow
-- presentation of 30 Jan 2009
--

val l = Ref[Boolean]()
val h = Ref[Boolean{6}]()

-- The following 3 lines are a workaround for type var inference
type StoreType = Boolean
def (:=)(ref::Ref[StoreType], val::StoreType) = ref.write(val)
def (?)(ref::Ref[StoreType]) = ref.read()


--h.write(true) >>

{-
--------
-- Memory
-}

l := h.read()

{-
--------
-- Control flow

if h.read() then l := true else l := false

--------
-- Dynamic security failure

l := false >> if h.read() then l := true else signal

--------
-- Non-determinism

l := true | l := false | l := h.read()

--------
-- Compositionality

  ( h.write(true) >> 
    Rtimer(10) >> if h.read() then l := true
                              else l := false
  )
|
  ( Rtimer(5) >> h.write(false)
  )

--------
-- Internal Timing

( Rtimer(50) >> l := true 
| (if h.read() then Rtimer(100)
               else signal) >>
   l := false
)

--------
-- External Timing

l := true >>
(if h.read() then Rtimer(100)
             else signal) >>
l := false

--------
-- Synchronization

Semaphore(0) >s>
l := false >>
( s.acquire() >> l := true
| if(h.read()) >> s.release()
)

--------
-- Non-termination

def loop(x::Boolean)::Signal = if x then loop(x)
                                    else signal
h.write(true) >>

( Rtimer(50) >> l := true
| loop(h.read()) >> l := false
)

--------
-}
