{- math-div.orc -- Orc program testing the division operator
 - 
 - Created by jthywiss on Dec 16, 2012 8:22:35 PM
 -}

     1 / 1    -- 1
|    1 / 0    -- ArithmeticException: BigInteger divide by zero
|    0 / 1    -- 0
|    0 / 0    -- ArithmeticException: BigInteger divide by zero
|  1.1 / 1    -- 1.1
|  1.1 / 0    -- Infinity
|  0.0 / 1    -- 0.0
|  0.0 / 0    -- NaN
|    1 / 1.1  -- 0.9090909090909091
|    1 / 0.0  -- Infinity
|    0 / 1.1  -- 0E+1
|    0 / 0.0  -- NaN
|  1.1 / 1.1  -- 1
|  1.1 / 0.0  -- Infinity
|  0.0 / 1.1  -- 0
|  0.0 / 0.0  -- NaN
| -1.0 / 0.0  -- -Infinity

{-
OUTPUT:PERMUTABLE
1
Error: orc.error.runtime.JavaException: java.lang.ArithmeticException: BigInteger divide by zero
0
Error: orc.error.runtime.JavaException: java.lang.ArithmeticException: BigInteger divide by zero
1.1
Infinity
0.0
NaN
0.9090909090909091
Infinity
0E+1
NaN
1
Infinity
0
NaN
-Infinity
-}
