include "mail.inc"
{- EXAMPLE -}
-- 3 + 4 >x> x + 5  -- = 3 + 4 + 5

-- wait for up to 3 seconds, randomly
-- random(3) >x> x * 1000 >y> Rtimer(y)

-- (random(1000) | random(1000) ) >x> Rtimer(x) >> println(x)
-- (random(1000) | random(1000) ) >x> Rtimer(x) >> println(x) >> stop

-- random(10) | Rtimer(1000) >> (random(10) | Rtimer(1000) >> random(10))

-- signals(5) >> random(10)

-- signals(4) >> SendMail("misra@cs.utexas.edu", "Orc Demo", "Orc is great!")
