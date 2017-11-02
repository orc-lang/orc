{- swaptions-naive-scala-sim.orc

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

def signals(n) = Ift(n :> 0) >> (signal | signals(n-1))

def sim(processor, swaption) =
	val sum = DoubleAdder()
	val sumsq = DoubleAdder()
    signals(nTrials) >> 
    	processor.simulate(swaption) >p>
    	sum.add(p) >>
    	sumsq.add(p*p) >> stop ;
    swaption.setSimSwaptionPriceMean(sum.sum() / nTrials) >>
    swaption.setSimSwaptionPriceStdError(sqrt((sumsq.sum() - sum.sum()*sum.sum()/nTrials) / (nTrials - 1.0)) / sqrt(nTrials))

def simAll(processor) =
	eachArray(data) >swaption> sim(processor, swaption) >> stop ; "Done"

benchmarkSized("Swaptions-naive-scala-sim", data.length? * SwaptionData.nTrials(),
	{ data >> Processor(SwaptionData.nTrials()) }, simAll)

{-
BENCHMARK
-}

