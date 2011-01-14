def repeat[A](lambda () :: A) :: A
def repeat(f) = f() >x> (x | repeat(f))

signal