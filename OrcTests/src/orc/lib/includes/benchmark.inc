import class System = "java.lang.System"

import site EndBenchmark = "orc.lib.EndBenchmark"
import site StartBenchmark = "orc.lib.StartBenchmark"

val nRuns = (
    val r = System.getProperty("benchmark.nRuns")
    Iff(r = null) >>
    Read(r) 
  ) ; 0

def timeItNoPub[A](f :: lambda() :: A, i) =
  (
    StartBenchmark() >s>
    (f() >v> Println("Publication: " + v) >> stop ; EndBenchmark(s, i)) >r> 
    Println("Time used: " + r + " s; iteration " + (i+1) + " of " + nRuns) >>
    r
  )

def benchmark[A](f :: lambda() :: A) =
	def h(Integer) :: Signal
	def h(i) if (i >= nRuns) = stop
	def h(i) = 
		timeItNoPub(f, i) >r> (r | h(i+1))
	if nRuns >= 1 then
		Println("Benchmark Enabled.") >> h(0)
	else
		f()