include "write-csv-file.inc"
include "test-output-util.inc"

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
    val r = System.getProperty("orc.test.benchmark.problemSize")
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
    val r = System.getProperty("orc.test.benchmark.nRuns")
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
  
def timeIt[A](f :: lambda() :: A) =
  val c = Channel[A]()
  repeat(c.get) |
  (
    Rtime() >s>
    (f() >x> c.put(x) >> stop ; Rtime()) >e> 
    c.close() >>
    Println("Time used: " + (e-s) + "ms") >> stop
  )

def benchmarkSized[A, B](name :: String, size :: Number, setup :: lambda() :: A, benchmark :: lambda(A) :: B) =
	def h(Integer, List[Top]) :: Signal
	def h(i, results) if (i >= nRuns) = results
	def h(i, results) = 
		writeCsvFileOverwrite(buildOutputPathname("benchmark-times", "csv"), "Benchmark times output file",
		  ["Repetition number [rep]", "Elapsed time (s) [elapsedTime]", "Process CPU time (s) [cpuTime]", "Runtime compilation time (s) [rtCompTime]"], 
		  map(lambda(t) = [t.iteration(), t.runTime(), t.cpuTime(), t.compilationTime()], reverse(results))) >>
		setup() >d> timeItNoPub({ benchmark(d) }, i, size) >r> h(i+1, r : results)
	if nRuns >= 1 then
		Println("Benchmarking " + name + " (" + nRuns + " runs, problem size " + problemSize + ", O(" + size + ") work)") >>
		setupOutput() >>
		writeFactorValuesTableWithPropertyFactors([
		  --Factor name, Value, Units, Comments
		  ("Benchmark", name, "", "benchmarkName", ""),
		  ("Problem Size", problemSize, "", "problemSize", "The parameter which controls the amount of work."),
		  ("Work", size, "", "work", "An estimate of the amount of work this benchmark must.")
		])  >>
		h(0, [])
	else
		setup() >d> benchmark(d)
