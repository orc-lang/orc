--
-- unity.inc -- Orc include for the UNITY execution scheduler
-- Project OrcJava
--
-- $Id$
--
-- Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{- UNITY execution scheduler -}

def unity(List[lambda() :: Signal]) :: Bot
def unity(fs) =
  {- First, transfer all the functions from the argument list fs to array fnarray for easy indexing -}
  val arlen = length(fs)
  val fnarray = Array[lambda() :: Signal](arlen)

  {- function populate() transfers from fs to fnarray -}
  def populate(Integer, List[lambda() :: Signal]) :: Signal
  def populate(_,[]) = signal
  def populate(i,g:gs) = fnarray(i) := g >> populate(i+1,gs)

  {- Execute a random statement and loop. Randomness guarantees fairness. -}
  def exec() :: Bot
  def exec() = random(arlen) >j> fnarray(j)?() >> stop ; exec()

  {- Initiate the work -}
  populate(0,fs) >> exec()
