include "prelude/op.inc"
site cat = orc.lib.str.Cat
site print = orc.lib.str.Print
site println = orc.lib.str.Println
site parseInt = orc.lib.str.AtoI
site parseBool = orc.lib.str.AtoB

site if = orc.runtime.sites.core.If

type Channel = orc.lib.types.Channel
type Tree a = Node(Tree a, Tree a, a) | Leaf() | orc.lib.types.Single

type T a b = Node(Tree, Tree)

type Pair a b = (a,b)

sig preorder(Tree a) :: [a]
def preorder(Leaf()) = []
def preorder(Node(l,r,n)) = append(preorder(l), append(preorder(r),[n]))

sig Metronome :: () -> Top
def Metronome() = ... 

def f(Number) :: Number
def f(n) = if(n > 1) >> n*f(n-1) | if(n <= 1) >> 1 


def g(Number, Number) :: Number
def g(a,b) = b+a


f(3) | (4,5) >(a,b)> g(b,a)

