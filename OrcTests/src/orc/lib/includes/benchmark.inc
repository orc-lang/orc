import class System = "java.lang.System"

import site EndBenchmark = "orc.lib.EndBenchmark"
import site StartBenchmark = "orc.lib.StartBenchmark"

{--
The benchmark should use this number to change the size of the problem.
Ideally the benchmark should increase in runtime roughly linearly with this 
value. However, that may not always be possible or practical.

If a value is not specified the size is 1.
-}
val problemSize = (
    val r = System.getProperty("benchmark.problemSize")
    Iff(r = null) >>
    Read(r) 
  ) ; 1
  
def problemSizeScaledInt(n :: Integer) :: Integer = 
  Floor((n * 1.0) * problemSize)
def problemSizeLogScaledInt(n :: Integer) :: Integer = 
  Floor((n * 1.0) * (Log(problemSize) + 1))
def problemSizeSqrtScaledInt(n :: Integer) :: Integer = 
  Floor((n * 1.0) * sqrt(problemSize))

{--
The number of times the benchmark function will run f or zero to 
disable benchmarking. If benchmarking is disabled then benchmark(f) 
behaves exactly like f(). 

The default value is 0.
-}
val nRuns = (
    val r = System.getProperty("benchmark.nRuns")
    Iff(r = null) >>
    Read(r) 
  ) ; 0

def timeItNoPub[A](f :: lambda() :: A, i, size) =
  (
    StartBenchmark() >s>
    (f() >v> Println("Publication: (value not printed to avoid performance skew)") >> stop ; EndBenchmark(s, i, size)) >r> 
    Println("Time used: " + r + " s; iteration " + (i+1) + " of " + nRuns) >>
    r
  )

def benchmarkSized[A](size :: Number, f :: lambda() :: A) =
	def h(Integer) :: Signal
	def h(i) if (i >= nRuns) = stop
	def h(i) = 
		timeItNoPub(f, i, size) >r> (r | h(i+1))
	if nRuns >= 1 then
		Println("Benchmark Enabled: " + nRuns + " runs, problem size " + problemSize + ", O(" + size + ") work") >> h(0)
	else
		f()
		
def benchmark[A](f :: lambda() :: A) = benchmarkSized(1, f)
