include "write-csv-file.inc"
include "test-output-util.inc"

import class System = "java.lang.System"
import class JString = "java.lang.String"

import class BenchmarkConfig = "orc.test.item.scalabenchmarks.BenchmarkConfig"
import class Benchmark = "orc.lib.Benchmark"
import site EndBenchmark = "orc.lib.EndBenchmark"
import site StartBenchmark = "orc.lib.StartBenchmark"
import class OrcTracer = "orc.util.Tracer"
import class DumperRegistry = "orc.util.DumperRegistry"


val nPartitions = BenchmarkConfig.nPartitions()

val softTimeLimit = BenchmarkConfig.softTimeLimit()
val hardTimeLimit = BenchmarkConfig.hardTimeLimit()

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
def problemSizeLogScaledInt(n :: Integer, base :: Integer) :: Integer = 
  Floor(Log(n * problemSize) / Log(base) + 1)
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
  
def timeItSinglePub[A](f :: lambda() :: A, i, size) =
  (
  	val pub = Cell()
    StartBenchmark() >s>
    (f() >v> pub.write(v) >> stop ; EndBenchmark(s, i, size)) >r> 
    Println("Time used: " + r + " s; iteration " + (i+1) + " of " + nRuns) >>
    (r, pub.readD() ; "stop") 
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

val repStartTime = Ref(Rtime())

def setRepStartTime() = repStartTime := Rtime()
def getRepTime() = Rtime() - repStartTime?

def benchmarkSized[A, B](name :: String, size :: Number, setup :: lambda() :: A, benchmark :: lambda(A) :: B, check :: lambda(B) :: Signal) =
	val clock = Rclock()
	def h(Integer, List[Top]) :: Signal
	def h(i, results) if (i >= nRuns) = 
	    val t = take(min(10, length(results)), results)
	    val tavg = sum(map({ _.runTime() }, t)) / length(t)
        Println("Minimum run time: " + minimum(map({ _.runTime() }, results))) >>
        Println("Minimum run time of last " + length(t) + ": " + minimum(map({ _.runTime() }, t))) >>
        Println("Average run time of last " + length(t) + ": " + tavg) >>
        Println("\nRun times: " + afold({ _ + ", " + _ }, map({ JString.format("%.3f", _.runTime()) }, results)) + "\n") >>
	    results
	def h(i, results) if (softTimeLimit :> 0 && clock.time() :> (softTimeLimit * 1000)) = h(nRuns, results)
	def h(i, results) = 
		setup() >d>
		Benchmark.waitForCompilation() >>
        DumperRegistry.clear() >> -- Clear dumpers that support that.
        Println("Start.") >>
        setRepStartTime() >>
		timeItSinglePub({ benchmark(d) }, i, size) >(r, pub)>
        DumperRegistry.dump("rep" + i) >> -- Run Dumpers as soon as possible after the end of the benchmark to avoid including extra stuff.
		(if check(pub) then Println(name + " results are correct up to hash collision.") else Error(name + " results are WRONG.")) >> (
			val results' = r : results
			writeCsvFileOverwrite(buildOutputPathname("benchmark-times", "csv"), "Benchmark times output file",
			  ["Repetition number [rep]", "Elapsed time (s) [elapsedTime]", "Process CPU time (s) [cpuTime]", 
			   "Runtime compilation time (s) [rtCompTime]", "GC time (s) [gcTime]"], 
			  map(lambda(t) = [t.iteration(), t.runTime(), t.cpuTime(), t.compilationTime(), t.gcTime()], reverse(results'))) >>
			h(i+1, results')
		)
	if nRuns >= 1 then
		Println("Benchmarking " + name + " (" + nRuns + " runs, problem size " + problemSize + ", O(" + size + ") work, soft time limit " + softTimeLimit + ", hard time limit " + hardTimeLimit + ")") >>
		setupOutput() >>
		writeFactorValuesTableWithPropertyFactors([
		  --Factor name, Value, Units, Comments
		  ("Benchmark", name, "", "benchmarkName", ""),
  		  ("Number of Partitions", nPartitions, "", "nPartitions", "Number of parallel partitions or threads."),
		  ("Problem Size", problemSize, "", "problemSize", "The parameter which controls the amount of work."),
		  ("Work", size, "", "work", "An estimate of the amount of work this benchmark must."),
  		  ("Language", "Orc", "", "language", "")
		])  >>
		BenchmarkConfig.startHardTimeLimit() >>
		h(0, [])
	else
		setup() >d> benchmark(d)



def printLogLine(s :: String) :: Signal = Println(getRepTime() + ": " + s)
