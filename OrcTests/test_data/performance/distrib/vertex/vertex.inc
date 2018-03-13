{- vertex.inc -- Orc include: Vertex tests common defintions
 -
 - Created by jthywiss on Feb 22, 2018 4:03:37 PM
 -}

include "test-output-util.inc"
include "write-csv-file.inc"

import class JavaSys = "java.lang.System"

{- Number of vertices to generate. -}
val numVertices = Read(JavaSys.getProperty("orc.test.numVertices", "12"))

{- Probability of edge between a given vertex pair. -}
val probEdge = Read(JavaSys.getProperty("orc.test.probEdge", "0.5"))


{--------
 - Graph definition
 --------}

type EdgeWeight = Integer
type VertexName = Integer

type Vertex = {. name :: VertexName, outEdges :: List[Top], pathLen :: Ref[EdgeWeight], pathLenSemaphore :: Semaphore .}

type Edge = {. head :: VertexName, tail :: VertexName, weight :: EdgeWeight .}

val vertices = Array[Vertex](numVertices)

def mkEdge(head :: VertexName, tail :: VertexName, weight :: EdgeWeight) :: Edge = {.
    head = head,
    tail = tail,
    weight = weight
  .}

def mkVertex(name :: VertexName, outEdges :: List[Edge]) :: Vertex = {.
    name = name,
    outEdges = outEdges,
    pathLen = Ref[EdgeWeight](),
    pathLenSemaphore = Semaphore(1)
  .}


{--------
 - Random graph generation 
 --------}

def randomEdges(head :: VertexName, numVertices :: Integer,  probEdge :: Number, randomWeight :: lambda() :: EdgeWeight) :: List[Edge] =
  collect({
    upto(numVertices) >vn> (
      Ift(URandom() <: probEdge) >> mkEdge(head, vn, randomWeight())
    )
  })

def randomGraph(numVertices :: Integer, probEdge :: Number, maxWeight :: EdgeWeight) :: Signal =
  (upto(numVertices) >vn> (
    vertices(vn) := mkVertex(vn, randomEdges(vn, numVertices,  probEdge, { Random(maxWeight) + 1 }))
  )) >> stop ; signal


{--------
 - Output
 --------}

def dumpGraph() :: Signal =
  def dumpVertices(vn) =
    vertices(vn)? >v>
    Println(v.name + ": pathLen=" + v.pathLen? + afold((+), map(lambda(e) = ",  |-"+e.weight+"-> "+e.tail, v.outEdges))) >>
    (if vn + 1 <: numVertices then dumpVertices(vn + 1) else signal)
  dumpVertices(0)


{--------
 - Test Driver
 --------}

val numRepetitions = Read(JavaSys.getProperty("orc.test.numRepetitions", "20"))

def getProcessCumulativeCpuTime() =
  import class ManagementFactory = "java.lang.management.ManagementFactory"
  ManagementFactory.getOperatingSystemMXBean().getProcessCpuTime()

def timeRepetitions(testPayload, numRepetitions) =
  def timeRepetitions'(thisRepetitionNum, remainingRepetitions, testElapsedTimes) =
    Println("Repetition " + thisRepetitionNum + ": start.") >>
    JavaSys.nanoTime() >startElapsed_ns>
    getProcessCumulativeCpuTime()  >startCpuTime_ns>
    (testPayload() >p> Println("Repetition " + thisRepetitionNum + ": published " + p) >> stop; signal) >>
    getProcessCumulativeCpuTime()  >finishCpuTime_ns>
    JavaSys.nanoTime() >finishElapsed_ns>
    (finishElapsed_ns - startElapsed_ns) / 1000  >elapsed_us>
    (finishCpuTime_ns - startCpuTime_ns) / 1000000  >cpuTime_ms>
    Println("Repetition " + thisRepetitionNum + ": finish.  Elapsed time " + elapsed_us + " µs, CPU time " + cpuTime_ms + " ms") >>
    append(testElapsedTimes, [[thisRepetitionNum, elapsed_us, cpuTime_ms]])  >testElapsedTimes'>
    (if remainingRepetitions :> 0 then timeRepetitions'(thisRepetitionNum + 1, remainingRepetitions - 1, testElapsedTimes') else testElapsedTimes')
  timeRepetitions'(1, numRepetitions - 1, [])

import site NumberOfRuntimeEngines = "orc.lib.NumberOfRuntimeEngines"

def runTest() =
  setupOutput()  >>
  writeFactorValuesTable([
    --Factor name, Value, Units, ID, Comments
    ("Program", "shortest-path.orc", "", "", ""),
    ("Number of vertices", numVertices, "", "numVertices", "Number of vertices to generate"),
    ("Probability of edge", probEdge, "", "probEdge", "Probability of edge between a given vertex pair"),
    ("Cluster size", NumberOfRuntimeEngines(), "", "dOrcNumRuntimes", "Number of d-Orc runtime engines running")
  ])  >>
  timeRepetitions(testPayload, numRepetitions)  >repetitionTimes>
  writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
    ["Repetition number", "Elapsed time (µs)", "CPU time (ms)"], repetitionTimes)  >>
  repetitionTimes
