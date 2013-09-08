{- ls.orc

EXERCISE:

You are given the site <code>ls(x)</code> which returns
a list of file names found in the directory named
by the path <code>x</code>, or an empty list if <code>x</code> is
not a directory. Paths are written as strings
using POSIX conventions, e.g. "/foo/bar/baz".
So for example <code>ls("/usr/")</code> might return
<code>["local/", "bin/", "lib/]</code>. Unlike POSIX, you
can assume that directory paths always end in "/".

Write a function <code>find(x)</code> which returns a list
of all paths in the directory tree starting at the
given path. E.g. <code>find("/")</code> might return
<code>["/", "/usr/", "/usr/bin/", "/usr/bin/ls"]</code>.
-}

{--
Example ls function which lists
files in directories: results are
hard-coded.
--}

type Path = String

def ls(Path) :: List[Path]
def ls("/") = ["usr/", "bin/", "lib/"]
def ls("/usr/") = ["local/", "bin/", "lib/"]
def ls("/usr/bin/") = ["ls", "sh", "find"]
def ls(_) = []

{-
SOLUTION:
-}
-------- main program ------

{-
This implementation makes use of map
and concat to achieve a much more concise
and readable solution.
-}


def find(root :: Path) :: List[Path] =
  def resolve(file :: Path) = root + file
  root:concat(map(find, map(resolve, ls(root))))

find("/")

{-
OUTPUT:
["/", "/usr/", "/usr/local/", "/usr/bin/", "/usr/bin/ls", "/usr/bin/sh", "/usr/bin/find", "/usr/lib/", "/bin/", "/lib/"]
-}
