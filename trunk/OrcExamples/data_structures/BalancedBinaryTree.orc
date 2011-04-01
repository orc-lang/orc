{- 
   A BBtree is a balanced binary tree of depth n. An index is a list
   of 0s and 1s whose length is less than n. It identifies a node in
   the tree as follows: a 0 identifies a left branch and a 1 a right
   branch. 

   Two operations are supported: read(is) where is an index list, 
   reads the value at the specified node (given by the index) and 
   write(v,is) writes value v at the given node.
-}

def class BBTree(0) =
 def read(is) = stop
 def write(v,is) = stop
stop

def class BBTree(n :: Integer) =
 val (root, left, right) = (Ref(), BBTree(n-1), BBTree(n-1))

 def write(v,[])   = root := v
 def write(v,0:is) = left.write(v,is)
 def write(v,1:is) = right.write(v,is)

 def read([])   = root?
 def read(0:is) = left.read(is)
 def read(1:is) = right.read(is)

stop
--
val store = BBTree(3)

store.write(0,[]) >> store.write(5,[0,1]) -->> store.write(5,[0,1,0])
>> (store.read([0,1]) | store.read([]))

{-
OUTPUT:
0
5
-}
