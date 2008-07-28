-- By analogy to a traditional short-
-- circuiting sequential "or" operation, we
-- return true as soon as any expression
-- evaluates to true, and false if all
-- expressions evaluate to false.

z <z< (   if(x) >> true
        | if(y) >> true
        | (x || y)

            <x< Prompt("True or false?") >x>
                parseBool(x)
            <y< Prompt("True or false?") >y>
                parseBool(y) )
