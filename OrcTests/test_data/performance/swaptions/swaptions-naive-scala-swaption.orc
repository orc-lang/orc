{- swaptions-naive-scala-swaption.orc

This uses a Scala implementation of the per trial simulation work.

-}

include "benchmark.inc"

import class Processor = "orc.test.item.scalabenchmarks.swaptions.Processor"
import class SwaptionData = "orc.test.item.scalabenchmarks.swaptions.SwaptionData"
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"

val data = SwaptionData.sizedData(SwaptionData.nSwaptions())
val nTrials = SwaptionData.nTrials()

def eachArray(a) =
	upto(a.length?) >i> a(i)?

def simAll(processor) =
	eachArray(data) >swaption> processor(swaption) >> stop ; "Done"

benchmarkSized("Swaptions-naive-scala-swaption", data.length? * SwaptionData.nTrials(),
	{ data >> Processor(SwaptionData.nTrials()) }, simAll)

{-
BENCHMARK
-}

