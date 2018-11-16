{- swaptions-naive-scala-sim-opt

This uses a Scala implementation of the per trial simulation work.

-}

include "benchmark.inc"

import class Processor = "orc.test.item.scalabenchmarks.swaptions.Processor"
import class SwaptionData = "orc.test.item.scalabenchmarks.swaptions.SwaptionData"
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

def loadData() = SwaptionData.data()
val nTrials = SwaptionData.nTrials()

def eachArray(a) =
    upto(a.length?) >i> a(i)?

def signals(n) = Ift(n :> 0) >> (signal | signals(n-1))

def sim(processor, swaption) =
    val sum = DoubleAdder()
    val sumsq = DoubleAdder()
    SinglePublication() >> sum >> sumsq >> processor >> swaption >>
    upto(nTrials) >i> 
        processor.simulate(swaption, i) >p>
        sum.add(p) >>
        sumsq.add(p*p) >> stop ;
    Sequentialize() >> -- Inferable
    swaption.setSimSwaptionPriceMean(sum.sum() / nTrials) >>
    swaption.setSimSwaptionPriceStdError(sqrt((sumsq.sum() - sum.sum()*sum.sum()/nTrials) / (nTrials - 1.0)) / sqrt(nTrials))

def simAll(data) =
    val processor = Processor(SwaptionData.nTrials())
    eachArray(data) >swaption> sim(processor, swaption) >> stop ; 
    data

benchmarkSized("Swaptions-naive-scala-sim-opt", SwaptionData.nSwaptions() * SwaptionData.nTrials(),
    { loadData() }, simAll, SwaptionData.check)

{-
BENCHMARK
-}

