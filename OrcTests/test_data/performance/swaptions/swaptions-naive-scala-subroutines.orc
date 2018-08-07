{- swaptions-naive-scala-subroutines

This implementation uses Scala versions of simulation SUBroutines but not for
the simulation control. This allow concurrency between a number of components
of the simulation of a single swaption.

-}

include "benchmark.inc"

import class Processor = "orc.test.item.scalabenchmarks.swaptions.Processor"
import class SwaptionData = "orc.test.item.scalabenchmarks.swaptions.SwaptionData"
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"

def loadData() = SwaptionData.data()
val nTrials = SwaptionData.nTrials()
val nSteps = SwaptionData.nSteps()

def eachArray(a) =
	upto(a.length?) >i> a(i)?

def tabulateArray(n, f) =
	val a = Array(n, "double")
	upto(n) >i> a(i) := f(i) >> stop ;
	a

def sumPubs(f) =
	val sum = DoubleAdder()
	f() >v> sum.add(v) >> stop ;
	sum.sum()

def simulate(processor, swaption, seed) :: Number =
    val timeDelta = swaption.years() / nSteps
    val freqRatio = Ceil(swaption.paymentInterval() / timeDelta)
    val swapPathStart = Ceil(swaption.maturity() / timeDelta)
    val swapTimePoints = Ceil(swaption.tenor() / timeDelta)
    val swapPathLength = nSteps - swapPathStart
    val swapPathYears = swapPathLength * timeDelta
    
    val swapPayoffs = tabulateArray(swapPathLength, lambda(i) =
      if i = 0 then 0
      else if i % freqRatio = 0 then (
        val offset = (if i = swapTimePoints then 0 else -1)
        Exp(swaption.strikeCont() * swaption.paymentInterval()) + offset
      ) else 0
    )
    
    val forwards = processor.yieldToForward(swaption.yields())
    
    val totalDrift = processor.computeDrifts(swaption)

    val path = processor.simPathForward(swaption, forwards, totalDrift, seed)

    val discountingRatePath = tabulateArray(nSteps, { path(_)?(0)? })
    val payoffDiscountFactors = processor.discountFactors(nSteps, swaption.years(), discountingRatePath)
    val swapRatePath = path(swapPathStart)?
    val swapDiscountFactors = processor.discountFactors(swapPathLength, swapPathYears, swapRatePath)
    
    val fixedLegValue = sumPubs({
    	upto(min(swapPayoffs.length?, swapDiscountFactors.length?)) >i>
    	swapPayoffs(i)? * swapDiscountFactors(i)?
	}) 
    
    val swaptionPayoff = max(fixedLegValue - 1, 0)
    val discSwaptionPayoff = swaptionPayoff * payoffDiscountFactors(swapPathStart)?
    
    discSwaptionPayoff

def sim(processor, swaption) =
	val sum = DoubleAdder()
	val sumsq = DoubleAdder()
    upto(nTrials) >i> 
    	simulate(processor, swaption, i) >p>
    	sum.add(p) >>
    	sumsq.add(p*p) >> stop ;
    swaption.setSimSwaptionPriceMean(sum.sum() / nTrials) >>
    swaption.setSimSwaptionPriceStdError(sqrt((sumsq.sum() - sum.sum()*sum.sum()/nTrials) / (nTrials - 1.0)) / sqrt(nTrials))

def simAll(data) =
	val processor = Processor(SwaptionData.nTrials())
	eachArray(data) >swaption> sim(processor, swaption) >> stop ;
	data

benchmarkSized("Swaptions-naive-scala-subroutines-seq", SwaptionData.nSwaptions() * SwaptionData.nTrials(),
	{ loadData() }, simAll, SwaptionData.check)

{-
BENCHMARK
-}

