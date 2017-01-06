{- balanced-binary-tree.orc -- Orc program that maintains balanced binary trees
 - 
 - A BBtree is a balanced binary tree of depth n. An index is a list
 - of 0s and 1s whose length is less than n. It identifies a node in
 - the tree as follows: a 0 identifies a left branch and a 1 a right
 - branch.
 - 
 - Two operations are supported: read(is) where is an index list,
 - reads the value at the specified node (given by the index) and
 - write(v,is) writes value v at the given node.
-}

-- TODO: Add [A] for all classes in this file. Use A as needed in the members.
class BBTree {
  def read(List[Integer]) :: Top
  def write(Top, List[Integer]) :: Signal
}

class BBTreeLeaf extends BBTree {
  def read(List[Integer]) :: BBTree
  def read(is) = stop

  def write(Top, List[Integer]) :: Signal
  def write(v,is) = stop
}

class BBTreeBranch extends BBTree {
  val n :: Integer
  val BBTree :: lambda(Integer) :: BBTree

  val (root, left, right) = (Ref(), BBTree(n-1), BBTree(n-1))

  def write(Top, List[Integer]) :: Signal
  def write(v,[])   = root := v
  def write(v,0:is) = left.write(v,is)
  def write(v,1:is) = right.write(v,is)

  def read(List[Integer]) :: Top
  def read([])   = root?
  def read(0:is) = left.read(is)
  def read(1:is) = right.read(is)
}
def BBTree(0) = new BBTreeLeaf
def BBTree(n' :: Integer) = 
  val rec = BBTree 
  new BBTreeBranch with { val n = n' # val BBTree = rec }

-- TODO: Convert this to use constructor syntactic sugar when it is available.


--
val store = BBTree[Integer](3)

store.write(0,[]) >> store.write(5,[0,1]) -->> store.write(5,[0,1,0])
>> (store.read([0,1]) | store.read([]))

{-
OUTPUT:PERMUTABLE:
0
5
-}
