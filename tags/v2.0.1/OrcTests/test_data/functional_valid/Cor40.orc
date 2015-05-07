{- Cor40.orc
 -
 - Test for Cor record patterns
 -
 - Created by dkitchin on Oct 29, 2010
 -}

val a = {. x = 0 .}
val b = {. y = 1 .}
val c = {. z = 2 .}
val d = {. x = 3, y = 4, z = 5 .} 
val e = {. x = {. y = 6, z = 7 .}, y = 8, z = 10 .} 

  a >{..}> 9
| b >{. y = n .}> n 
| d >{. z = m, x = n .}> ( m | n )
| e >{. x = {. z = m .}, y = _, z = _ .}> m

| c >{. z = 10 .}> 10

| ( []          :!: Bot ) >{..}> 12
| ( ("x", 14)   :!: Bot ) >{. x = n .}> n
| ( [("x", 16)] :!: Bot ) >{. x = n .}> n
| ( ["x", 18]   :!: Bot ) >{. x = n .}> n

{-
OUTPUT:PERMUTABLE:
1
3
5
7
9
-}