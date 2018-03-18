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
val probEdge = Read(JavaSys.getProperty("orc.test.probEdge", "0.33"))


{--------
 - Graph definition
 --------}

import class Vertex = "orc.test.item.distrib.VertexWithPathLen"
import class Edge = "orc.test.item.distrib.EdgeWithIntWeight"
type EdgeWeight = Integer
type VertexName = Integer

val vertices = Array[Vertex](numVertices)

def mkEdge(head :: VertexName, tail :: VertexName, weight :: EdgeWeight) :: Edge = Edge(head, tail, weight)

def mkVertex(name :: VertexName, outEdges :: List[Edge]) :: Vertex = Vertex(name, outEdges, Ref[EdgeWeight](), Semaphore(1))

def resetVertex(vertexRef :: Ref[Vertex]) :: Signal =
  vertexRef?  >oldVertex>
  vertexRef := Vertex(oldVertex.name(), oldVertex.outEdges(), Ref[EdgeWeight](), Semaphore(1))


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

def resetGraph() :: Signal =
  (upto(numVertices) >vn> (
    resetVertex(vertices(vn))
  )) >> stop ; signal

{--------
 - Output
 --------}

def dumpGraph() :: Signal =
  def dumpVertices(vn) =
    vertices(vn)?  >v>
    (v.pathLen().readD(); "(undef)")  >pl>
    Println(v.name() + ": pathLen=" + pl + afold((+), map(lambda(e) = ",  |-"+e.weight()+"-> "+e.tail(), v.outEdges())))  >>
    (if vn + 1 <: numVertices then dumpVertices(vn + 1) else signal)
  dumpVertices(0)


{--------
 - Test Driver
 --------}

val numRepetitions = Read(JavaSys.getProperty("orc.test.numRepetitions", "20"))

def getProcessCumulativeCpuTime() =
  import class ManagementFactory = "java.lang.management.ManagementFactory"
  ManagementFactory.getOperatingSystemMXBean().getProcessCpuTime()

def timeRepetitions(setUpTestRep, runTestRep, tearDownTestRep, numRepetitions) =
  def timeRepetitions'(thisRepetitionNum, remainingRepetitions, testElapsedTimes) =
    Println("Repetition " + thisRepetitionNum + ": setting up.")  >>
    (setUpTestRep() >> stop; signal)  >>
    Println("Repetition " + thisRepetitionNum + ": start run.")  >>
    JavaSys.nanoTime()  >startElapsed_ns>
    getProcessCumulativeCpuTime()  >startCpuTime_ns>
    (runTestRep() >p> Println("Repetition " + thisRepetitionNum + ": published " + p) >> stop; signal)  >>
    getProcessCumulativeCpuTime()  >finishCpuTime_ns>
    JavaSys.nanoTime()  >finishElapsed_ns>
    (finishElapsed_ns - startElapsed_ns) / 1000  >elapsed_us>
    (finishCpuTime_ns - startCpuTime_ns) / 1000000  >cpuTime_ms>
    Println("Repetition " + thisRepetitionNum + ": finish run.  Elapsed time " + elapsed_us + " µs, leader CPU time " + cpuTime_ms + " ms")  >>
    append(testElapsedTimes, [[thisRepetitionNum, elapsed_us, cpuTime_ms]])  >testElapsedTimes'>
    Println("Repetition " + thisRepetitionNum + ": tearing down.")  >>
    (tearDownTestRep() >> stop; signal)  >>
    (if remainingRepetitions :> 0 then timeRepetitions'(thisRepetitionNum + 1, remainingRepetitions - 1, testElapsedTimes') else testElapsedTimes')
  timeRepetitions'(1, numRepetitions - 1, [])

import site NumberOfRuntimeEngines = "orc.lib.NumberOfRuntimeEngines"

def executeTest() =
  setupOutput()  >>
  writeFactorValuesTable([
    --Factor name, Value, Units, ID, Comments
    ("Program", "shortest-path.orc", "", "", ""),
    ("Number of vertices", numVertices, "", "numVertices", "Number of vertices to generate"),
    ("Probability of edge", probEdge, "", "probEdge", "Probability of edge between a given vertex pair"),
    ("Cluster size", NumberOfRuntimeEngines(), "", "dOrcNumRuntimes", "Number of d-Orc runtime engines running")
  ])  >>
  Println("Test start. Setting up test.")  >>
  (setUpTest() >> stop; signal)  >>
  timeRepetitions(setUpTestRep, runTestRep, tearDownTestRep, numRepetitions)  >repetitionTimes>
  writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
    ["Repetition number", "Elapsed time (µs)", "Leader CPU time (ms)"], repetitionTimes)  >>
  Println("Tearing down test.")  >>
  (tearDownTest() >> stop; signal)  >>
  Println("Test finish.")  >>
  repetitionTimes
