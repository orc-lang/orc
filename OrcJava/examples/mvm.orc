{-
MVM: This function does a matrix vector multiplication.
-}
def mvm([], _) = []
def mvm(vm:mt, v) = 
  def multvv([], []) = 0
  def multvv(a:v1, b:v2) = a*b+multvv(v1, v2)
  multvv(vm, v):mvm(mt, v)


-- A 4x7 matrix
val A = [[3,  5, -9,  7,  98, -78,  2],
         [2,  0,  7, 28, -28,  26,  0], 
         [1,  1,  6,  0, -28,  22, 29],
         [3,  4, -3, -1,  11,  55, 19]]
-- A 7x1 matrix represented as a vector
val V = [-7, 4, -5, 19, 6, 43, 21]

mvm(A, V)

{-
OUTPUT:
[-2547, 1433, 1354, 2821]
-}

