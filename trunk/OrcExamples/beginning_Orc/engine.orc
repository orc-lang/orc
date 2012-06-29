{- engine.orc -- Orc program demonstrating some simple UI services provided by the Orc engine
 -}

  1 | signal | [1,2,3] -- publications
| Print("Hi ") >> Println("there") >> -- Printing
  Error("Error message!") -- error message
| Prompt("Enter your name") -- prompts
