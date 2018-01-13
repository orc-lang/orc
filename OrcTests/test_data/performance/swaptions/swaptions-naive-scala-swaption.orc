{- swaptions-naive-scala-swaption.orc

This uses a Scala implementation of the per trial simulation work.

-}

include "benchmark.inc"

import class Processor = "orc.test.item.scalabenchmarks.swaptions.Processor"
import class SwaptionData = "orc.test.item.scalabenchmarks.swaptions.SwaptionData"
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"

def loadData() = SwaptionData.data()
val nTrials = SwaptionData.nTrials()

def eachArray(a) =
	upto(a.length?) >i> a(i)?

def simAll(data) = 
	val processor =  Processor(SwaptionData.nTrials())
	eachArray(data) >swaption> processor(swaption) >> stop ; "Done"

benchmarkSized("Swaptions-naive-scala-swaption", SwaptionData.nSwaptions() * SwaptionData.nTrials(),
	{ loadData() }, simAll)

{-
BENCHMARK
-}

