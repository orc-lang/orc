-- Parallel or

def askBool(M) = M("True or false?") >s> parseBool(s)

z <z< ( if(x) >> true 
      | if(y) >> true 
      | (x || y)
      <x< askBool(David)
      <y< askBool(Adrian) )
