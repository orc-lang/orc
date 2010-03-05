{--
You are given a data type for binary trees with the
constructors <code>Tree(left, value, right)</code> and
<code>Leaf()</code>. Write a function which, given a tree,
returns a list of values in the tree ordered by
depth. I.e. the first element should be the root
value, followed by the root's children, followed by
its grandchildren, and so on.
--}

type Tree[A] = Node(Tree[A], A, Tree[A]) | Leaf()

def levels[A](Tree[A]) :: List[A]
def levels(tree) =
  def helper(List[Tree[A]]) :: List[A]
  def helper([]) = []
  def helper(Leaf():rest) = helper(rest)
  def helper(Node(l,v,r):rest) =
    v:helper(append(rest, [l, r]))
  helper([tree])

levels(Node(
  Node(
    Node(Leaf(), 3, Leaf()),
    2,
    Node(Leaf(), 3, Leaf())),
  1,
  Node(
    Node(Leaf(), 3, Leaf()),
    2,
    Node(Leaf(), 3, Leaf()))))

{-
OUTPUT:
[1, 2, 2, 3, 3, 3, 3]
-}