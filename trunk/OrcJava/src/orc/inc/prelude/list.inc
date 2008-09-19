def each([]) = stop
def each(h:t) = h | each(t)

def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

def reverse(l) = 
  def tailrev([],x) = x
  def tailrev(h:t,x) = tailrev(t,h:x)
  tailrev(l,[]) 

def index(h:t, 0) = h
def index(h:t, n) = index(t, n-1)

def append([],l) = l
def append(h:t,l) = h:append(t,l)

def foldl(f,[],s) = s
def foldl(f,h:t,s) = foldl(f,t,f(h,s))

def foldr(f,l,s) = foldl(f,reverse(l),s)

def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)

def length([]) = 0
def length(h:t) = 1 + length(t)

def take(0, _) = []
def take(_, []) = []
def take(n, x:xs) = x:take(n-1, xs)

def drop(0, xs) = xs
def drop(_, []) = []
def drop(n, x:xs) = drop(n-1, xs)

def member(item, []) = false
def member(item, h:t) =
  if item = h then true
  else member(item, t)