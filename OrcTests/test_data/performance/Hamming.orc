-- Hamming sequence

include "benchmark.inc"

def listUpto(Integer) :: List[Integer]
def listUpto(0) = []
def listUpto(n) if (n <: 0) = stop
def listUpto(n) = n-1 : listUpto(n-1)

type Putter[A] = lambda(A) :: Signal
type Getter[A] = lambda() :: A

def Null[A]() = null :!: A 

def Fanout[A](c :: Getter[A], cs :: List[Putter[A]]) =
  repeat({ c() >x> 
    collect({ each(cs) >c'> c'(x) }) }) >> stop

def Fanin[A](cs :: List[Getter[A]], c :: Putter[A]) =
  each(cs) >c'> repeat({ c'() >x> c(x) }) >> stop 

def Trans[A, B](f :: lambda(A) :: B, in :: Getter[A], out :: Putter[B]) =
  repeat({ out(f(in())) }) >> stop

-- TODO: Use actual type parameters when they work here.
class UniqueMerge { 
  val is :: List[Getter[Top]]
  val o :: Putter[Top]

  val tops = Array[Top](length(is))
  
  def fillTops() =
    joinMap({ _ >i> (if( tops(i)? = null ) then 
    		  tops(i) := index(is, i)()
    		else 
    		  signal)}, listUpto(length(is)))
  def getMin() =
    fillTops() >> (
    def f((mi, mv, i) :: (Integer, Top, Integer), v) = 
      if (if mv /= null then v >= mv else false) then (mi, mv, i+1) else (i, v, i+1)
    val (mi, mv, _) = foldl(f, (-1,Null(),0), arrayToList(tops))
    --Println("Filled " + arrayToList(tops)) >> 
    joinMap({ _ >i> (if( tops(i)? = mv ) then 
    		  tops(i) := Null()
    		else
    		  signal) }, listUpto(length(is))) >>
    mv)
    
  val _ = repeat({
    --Println(arrayToList(tops)) >> 
    getMin() >x> o(x)
  })
}

def UniqueMerge[A](is_ :: List[Getter[Top]], o_ :: Putter[Top]) :: UniqueMerge = 
  new UniqueMerge { val is = is_ # val o = o_ }

def takeN[A](Integer, Channel[A]) :: List[A]
def takeN(n, chan) = 
  val cnt = Counter(n)
  val c = Channel[A]()
  val l = (
    cnt.onZero() >> c.getAll() |
   	repeat({ chan.get() >x> c.put(x) >> cnt.dec() }) >> stop
  ) #
  (l  >> c.closeD() >> l)

def makeChannels[A](n :: Integer) = collect({ upto(n) >> Channel[A]() })

val N = problemSizeScaledInt(400)

benchmarkSized("Hamming", N, { signal }, lambda(_) =
val [out, out', x2, x3, x5, x2', x3', x5'] as chans = makeChannels[Integer](8)

Fanout(out.get, [x2.put, x3.put, x5.put, out'.put]) >> stop |
UniqueMerge([x2'.get, x3'.get, x5'.get], out.put) >> stop |
Trans({ _*2 }, x2.get, x2'.put) >> stop | 
Trans({ _*3 }, x3.get, x3'.put) >> stop | 
Trans({ _*5 }, x5.get, x5'.put) >> stop |

Println(takeN(N, out')) >>
each(chans) >c> c.closeD() >> stop

|
out.put(1) >> stop
)

{-
OUTPUT:
[1, 2, 3, 4, 5, 6, 8, 9, 10, 12, 15, 16, 18, 20, 24, 25, 27, 30, 32, 36, 40, 45, 48, 50, 54, 60, 64, 72, 75, 80, 81, 90, 96, 100, 108, 120, 125, 128, 135, 144, 150, 160, 162, 180, 192, 200, 216, 225, 240, 243, 250, 256, 270, 288, 300, 320, 324, 360, 375, 384, 400, 405, 432, 450, 480, 486, 500, 512, 540, 576, 600, 625, 640, 648, 675, 720, 729, 750, 768, 800, 810, 864, 900, 960, 972, 1000, 1024, 1080, 1125, 1152, 1200, 1215, 1250, 1280, 1296, 1350, 1440, 1458, 1500, 1536, 1600, 1620, 1728, 1800, 1875, 1920, 1944, 2000, 2025, 2048, 2160, 2187, 2250, 2304, 2400, 2430, 2500, 2560, 2592, 2700, 2880, 2916, 3000, 3072, 3125, 3200, 3240, 3375, 3456, 3600, 3645, 3750, 3840, 3888, 4000, 4050, 4096, 4320, 4374, 4500, 4608, 4800, 4860, 5000, 5120, 5184, 5400, 5625, 5760, 5832, 6000, 6075, 6144, 6250, 6400, 6480, 6561, 6750, 6912, 7200, 7290, 7500, 7680, 7776, 8000, 8100, 8192, 8640, 8748, 9000, 9216, 9375, 9600, 9720, 10000, 10125, 10240, 10368, 10800, 10935, 11250, 11520, 11664, 12000, 12150, 12288, 12500, 12800, 12960, 13122, 13500, 13824, 14400, 14580, 15000, 15360, 15552, 15625, 16000, 16200, 16384, 16875, 17280, 17496, 18000, 18225, 18432, 18750, 19200, 19440, 19683, 20000, 20250, 20480, 20736, 21600, 21870, 22500, 23040, 23328, 24000, 24300, 24576, 25000, 25600, 25920, 26244, 27000, 27648, 28125, 28800, 29160, 30000, 30375, 30720, 31104, 31250, 32000, 32400, 32768, 32805, 33750, 34560, 34992, 36000, 36450, 36864, 37500, 38400, 38880, 39366, 40000, 40500, 40960, 41472, 43200, 43740, 45000, 46080, 46656, 46875, 48000, 48600, 49152, 50000, 50625, 51200, 51840, 52488, 54000, 54675, 55296, 56250, 57600, 58320, 59049, 60000, 60750, 61440, 62208, 62500, 64000, 64800, 65536, 65610, 67500, 69120, 69984, 72000, 72900, 73728, 75000, 76800, 77760, 78125, 78732, 80000, 81000, 81920, 82944, 84375, 86400, 87480, 90000, 91125, 92160, 93312, 93750, 96000, 97200, 98304, 98415, 100000, 101250, 102400, 103680, 104976, 108000, 109350, 110592, 112500, 115200, 116640, 118098, 120000, 121500, 122880, 124416, 125000, 128000, 129600, 131072, 131220, 135000, 138240, 139968, 140625, 144000, 145800, 147456, 150000, 151875, 153600, 155520, 156250, 157464, 160000, 162000, 163840, 164025, 165888, 168750, 172800, 174960, 177147, 180000, 182250, 184320, 186624, 187500, 192000, 194400, 196608, 196830, 200000, 202500, 204800, 207360, 209952, 216000, 218700, 221184, 225000, 230400, 233280, 234375, 236196, 240000, 243000, 245760, 248832, 250000, 253125, 256000, 259200, 262144, 262440, 270000, 273375, 276480, 279936, 281250, 288000, 291600, 294912, 295245, 300000, 303750, 307200, 311040]
-}
{-
BENCHMARK
-}
