{- 
This program is an exercise in imperative sequential programming
which uses pointers and mutable storage. It nicely separates the aspects of
programming that are imperative -- updating values in place in the
storage -- from aspects that are functional -- algorithms on trees
described recursively. 

The program implements a binary search tree which stores a set of
values. See Wikipedia entry
http://en.wikipedia.org/wiki/Binary_search_tree
for a desription of binary search tree. 

The following interface is supported:

insert(key): inserts key into the set. Returns true if key was not in
the set prior to this operation, and false otherwise.

delete(key): deletes key from the set. Returns true if key was in
the set prior to this operation, and false otherwise. 

search(key): Returns true if key is in the set, and false otherwise.   

sort():      Returns a list of the set items in increasing order.

Each node of a binary search tree (BST) includes value, a pointer to
its left child (if it exists), and a pointer to its right child (if it
exists). We simplify the algorithm by adding sentinels as
follows. Below, tsent (for "top sentinel") is a node whose left
pointer points to the root of the BST; its remaining fields are
irrelevant. And, bsent stands for "bottom sentinel". Every node in the
BST that should point to a null child points to bsent instead (thus,
our structure is not really a tree). Initially, the tree is empty and
the left child of tsent is bsent. Henceforth, the left child of bsent
is called "root"; initially, the root is bsent.

Search for key starts by storing key in the value part of bsent, and
applying the traditional algorithm starting at the root. There is no
need to test for null pointers because the search is guaranteed to
succeed, either by encountering a genuine value, or the stored value in
bsent. The search result is then computed by testing if the final node
in the search is bsent. A similar algorithm is used to insert a value
into the set.

In the following definitions, we often use a triple (p,d,q) to
transmit a pair of nodes; here, q is p's child, it is the left child
if d is false and right child if d is true. Since the tree has at
least two nodes at all times (bsent and tsent) such a scheme is always
feasible. Further, this interface allows us to insert and delete nodes
more easily.

-} 

(
   
val bsent = Ref()
val tsent = Ref() >r> r.write((0,bsent,0)) >> r

-- direction of pointer: false for left and true for right.
def update (p,d,q) = -- redirect d pointer of p to q.
    val (v,l,r) = p.read()
    if d then p.write((v,l,q)) else p.write((v,q,r))

def searchstart(key) = -- return (p,d,q) where p.d = q and q.val = key
    def searchloop(p,d,q,key) =
    {- given q is p's d-child.
       Start search from q. Return (s,d,t) where s.d = t and t.val = key
    -}
        val (v,l,r) = q.read()
        if(key < v) >> searchloop(q,false,l,key)
      | if(key = v) >> (p,d,q)
      | if(key > v) >> searchloop(q,true,r,key)

  {- Goal for searchstart -}
  val (_,root,_) = tsent.read()

  bsent.write((key,0,0)) >>
  searchloop(tsent,false,root,key)

def search(key) = -- return true or false
    searchstart(key) >(_,_,q)> (q /= bsent)

def insert(key) = -- return true if value was inserted, false if it was there
    searchstart(key) >(p,d,q)>
    if q = bsent
       then Ref() >r> r.write((key,bsent,bsent)) >> update(p,d,r) >> true
       else false

def delete(key) =
   def isucc(p) =
       {- in-order successor of p. p has genuine left and right sons.
          Returns (s,d,t) where t is the d-child of s.
          t is the in-order sucessor of s, t.left = bsent
          t is the leftmost genuine (non-sentinel) node in the right subtree of p.
       -}

       def leftmost(p,d,r) =
       -- given r is the d-child of p and r /= bsent.
       -- Return (p',d,r') where r' is the d-child of p' and r'.left = bsent
       -- Either (p,r) = (p',r') or (p',r') is in the leftmost path in 
       -- the subtree rooted at r.

       val (_,l,_) = r.read()
       if l = bsent then (p,d,r) else leftmost(r,false,l)

   {- Goal of isucc: -}
   val (_,_,r) = p.read()
   leftmost(p,true,r)

  {- Goal of delete: -}
  val (p,d,q) = searchstart(key)
  val (_,l,r) = q.read()
  -- Below, nc is the number of children of q.
  val nc      = (if l = bsent then 0 else 1) + (if r = bsent then 0 else 1)

  if(q = bsent) then {- key is not in -} false
  else
   ( if (nc /= 2) then -- q has zero or one genuine child
(if l /= bsent then l else r) >t> update(p,d,t)  >> true
     else  -- q has two genuine children
   isucc(q) >(s,d,t)>
   t.read() >(v,_,r')>
   q.write((v,l,r)) >>
   update(s,d,r') >> true
   )

def sort() = -- do an in order traversal of the BST
    
    {- An explicit append operation on lists -}
    def append([],ys) = ys
    def append(xs,[]) = xs
    def append(x:xs, ys) = x:append(xs,ys)

    def traverse(p) =
     val (v,l,r) = p.read()
     if(p = bsent) then []
     else append(traverse(l),v:traverse(r))

  val (_,root,_) = tsent.read()
  traverse(root)

-- Test

insert(30)  >>
insert(20)  >>
insert(24)  >>
insert(35)  >>
insert(33)  >>
insert(38) >>
delete(35)>>
  sort()
  
) :!: Top  {- As currently written, this program cannot pass the typechecker -} 
  
{-
OUTPUT:
[20, 24, 30, 33, 38]
-}