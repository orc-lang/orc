val b = Remote("buffer")
def repeat(f) = f() >!x> repeat(f)
repeat(b.get)