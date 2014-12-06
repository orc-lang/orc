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

type BBTree[A] =
{.
  read :: lambda(List[Integer]) :: A,
  write :: lambda(A, List[Integer]) :: Signal
.}

site BBTree[A](Integer) :: BBTree[A]
def class BBTree(0) =

  def read(List[Integer]) :: A
  def read(is) = stop

  def write(A, List[Integer]) :: Signal
  def write(v,is) = stop

  stop


def class BBTree(n) =
  val (root, left, right) = (Ref[A](), BBTree[A](n-1), BBTree[A](n-1))

  def write(A, List[Integer]) :: Signal
  def write(v,[])   = root := v
  def write(v,0:is) = left.write(v,is)
  def write(v,1:is) = right.write(v,is)

  def read(List[Integer]) :: A
  def read([])   = root?
  def read(0:is) = left.read(is)
  def read(1:is) = right.read(is)

  stop
--
val store = BBTree[Integer](3)

store.write(0,[]) >> store.write(5,[0,1]) -->> store.write(5,[0,1,0])
>> (store.read([0,1]) | store.read([]))

{-
OUTPUT:PERMUTABLE:
0
5
-}
