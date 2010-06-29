{- Parser27.orc
 - 
 - Torture test for the Orc parser
 -
 - MALFORMED COMBINATOR
 - 
 - Created by brian on Jun 29, 2010 11:18:53 AM
 -}

1 >>> 2 >> signal

-- 1 || 2 >> signal

-- 1 <<x< signal

-- 1 :>> signal

-- 1 <<: signal

-- 1 ;; signal

