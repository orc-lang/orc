{- swaptions-naive-opt

This implementation uses Scala versions of simulation SUBroutines but not for
the simulation control. This allow concurrency between a number of components
of the simulation of a single swaption.

This version uses Sequentialize() to seqientialize the simulation operation for a given trial.

-}

include "benchmark.inc"

import class Swaption = "orc.test.item.scalabenchmarks.swaptions.Swaption"

import class CumNormalInv = "orc.test.item.scalabenchmarks.swaptions.CumNormalInv"
import class SwaptionData = "orc.test.item.scalabenchmarks.swaptions.SwaptionData"
import class DoubleAdder = "java.util.concurrent.atomic.DoubleAdder"
import class JURandom = "java.util.Random"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

type Double = Number

-- These functions are not counted as they should be part of the standard library.

def sfor(low, high, f) = Sequentialize() >> ( -- Inferable (recursion)
  def h(i) if (i >= high) = signal
  def h(i) = f(i) >> h(i + 1)
  h(low)
  )

def loadData() = SwaptionData.data()
val nTrials = SwaptionData.nTrials()
val nSteps = SwaptionData.nSteps()

def eachArray(a) = Sequentialize() >> (
	upto(a.length?) >i> a(i)?
	)

def tabulateArrayDouble(n, f) = Sequentialize() >> (
    val a = Array(n, "double")
    upto(n) >i> a(i) := f(i) >> stop ;
    a
    )

def tabulateArrayDoubleSeq(n, f) = Sequentialize() >> (
    val a = Array(n, "double")
    sfor(0, n, lambda(i) = a(i) := f(i)) >>
    a
    )

def tabulateArrayObject(n, f) = Sequentialize() >> (
	val a = Array(n)
	upto(n) >i> a(i) := f(i) >> stop ;
	a
	)

def mapArrayDouble(a, f) = Sequentialize() >> (
	val n = a.length?
	tabulateArrayDouble(n, { f(a(_)?) })
	)

-- Lines: 4
def sumPubs(f) = Sequentialize() >> (
	val sum = DoubleAdder()
	f() >v> sum.add(v) >> stop ;
	sum.sum()
	)

-- Lines: 8
def yieldToForward(yields :: Array[Double]) :: Array[Double] = Sequentialize() >>
    tabulateArrayDouble(yields.length?, lambda (i) =
      val now = yields(i)?
      if i = 0 then
      	now
	  else (
	    val last = yields(i-1)? #
    	(i-2)*now - (i-1)*last
	  )
    )

-- Lines: 16
def computeDrifts(swaption :: Swaption) :: Array[Double] = Sequentialize() >> (
    val timeDelta = swaption.years() / nSteps
    val drifts = Array(swaption.factors().length?)
    sfor(0, swaption.factors().length?,
        lambda(i) =
            drifts(i) := Array(nSteps - 1, "double") >>
            drifts(i)?(0) := 0.5 * timeDelta * swaption.factors()(i)?(0)? * swaption.factors()(i)?(0)?) >>
            
    sfor(0, swaption.factors().length?,
        lambda(i) =
            sfor(1, nSteps - 1,
                lambda (j) = (
                drifts(i)?(j) := (
                    val prevDrift = sumPubs({ upto(j) >l> drifts(i)?(l)? })
                    val prevFactor = sumPubs({ upto(j+1) >l> swaption.factors()(i)?(l)? }) #
                  
                    (-prevDrift) + 0.5 * timeDelta * prevFactor * prevFactor
                )
            ))) >>
   
    tabulateArrayDouble(nSteps - 2, lambda (i) = sumPubs({ upto(swaption.factors().length?) >j> drifts(j)?(i+1)? }))
    )

-- Lines: 11
def simPathForward(swaption :: Swaption, forwards :: Array[Double], totalDrift :: Array[Double], seed :: Integer) :: Array[Array[Double]] = Sequentialize() >> (
    val rng = JURandom(seed + swaption.id())
    
    val timeDelta = swaption.years() / nSteps
    val path = tabulateArrayObject(nSteps, lambda(i) = Array(nSteps, "double"))
    path(0) := forwards >>
    sfor(1, nSteps, lambda(j) = 
      tabulateArrayDoubleSeq(swaption.factors().length?, { _ >> CumNormalInv(rng.nextDouble()) }) >shocks> 
	  sfor(0, nSteps - (j + 1), lambda(l) = 
        val totalShock = sumPubs({ upto(swaption.factors().length?) >i> swaption.factors()(i)?(l)? * shocks(i)? })
        path(j)?(l) := path(j-1)?(l+1)? + totalDrift(l)? + sqrt(timeDelta) * totalShock
      )
    ) >>
    path
    )

-- Lines: 7
def discountFactors(nSteps :: Integer, years :: Double, path :: Array[Double]) :: Array[Double] = Sequentialize() >> (
    val timeDelta = years / nSteps
    val discountFactors = tabulateArrayDouble(nSteps, lambda(_) = 1.0)
    
    sfor(1, nSteps, lambda(i) = 
    	sfor(0, i, lambda(j) = 
	      discountFactors(i) := discountFactors(i)? * Exp(-(path(j)?) * timeDelta)
	      )
      ) >>

    discountFactors
    )

-- Lines: 27
def simulate(swaption, seed) :: Number = Sequentialize() >> ( -- Inferable (if tabulate array is sequential)
    val timeDelta = swaption.years() / nSteps
    val freqRatio = Ceil(swaption.paymentInterval() / timeDelta)
    val swapPathStart = Ceil(swaption.maturity() / timeDelta)
    val swapTimePoints = Ceil(swaption.tenor() / timeDelta)
    val swapPathLength = nSteps - swapPathStart
    val swapPathYears = swapPathLength * timeDelta
    
    val swapPayoffs = tabulateArrayDouble(swapPathLength, lambda(i) =
      if i = 0 then 0
      else if (i % freqRatio) = 0 then (
        val offset = (if i = swapTimePoints then 0 else -1)
        Exp(swaption.strikeCont() * swaption.paymentInterval()) + offset
      ) else 0
    )
    
    val forwards = yieldToForward(swaption.yields())
    
    val totalDrift = computeDrifts(swaption)

    val path = simPathForward(swaption, forwards, totalDrift, seed)

    val discountingRatePath = tabulateArrayDouble(nSteps, { path(_)?(0)? })
    val payoffDiscountFactors = discountFactors(nSteps, swaption.years(), discountingRatePath)
    val swapRatePath = path(swapPathStart)?
    val swapDiscountFactors = discountFactors(swapPathLength, swapPathYears, swapRatePath)
    
    val fixedLegValue = sumPubs({
    	upto(min(swapPayoffs.length?, swapDiscountFactors.length?)) >i>
    	swapPayoffs(i)? * swapDiscountFactors(i)?
	}) 
    
    val swaptionPayoff = max(fixedLegValue - 1, 0)
    val discSwaptionPayoff = swaptionPayoff * payoffDiscountFactors(swapPathStart)?
    
    discSwaptionPayoff
    )

-- Lines: 11
def sim(swaption) =
	val sum = DoubleAdder()
	val sumsq = DoubleAdder()
	sum >> sumsq >>
    upto(nTrials) >i> Sequentialize() >>
    	simulate(swaption, i) >p>
    	sum.add(p) >>
    	sumsq.add(p*p) >> stop ;
    Sequentialize() >> -- Inferable
    swaption.setSimSwaptionPriceMean(sum.sum() / nTrials) >>
    swaption.setSimSwaptionPriceStdError(sqrt((sumsq.sum() - sum.sum()*sum.sum()/nTrials) / (nTrials - 1.0)) / sqrt(nTrials))

-- Lines: 3
def simAll(data) =
	eachArray(data) >swaption> sim(swaption) >> stop ;
	data

benchmarkSized("Swaptions-naive-scala-subroutines-opt", SwaptionData.nSwaptions() * SwaptionData.nTrials(),
	{ loadData() }, simAll, SwaptionData.check)

{-
BENCHMARK
-}

