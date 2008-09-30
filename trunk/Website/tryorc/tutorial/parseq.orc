-- Ask two questions and use both answers
(   Prompt("Pick a movie you like:")
  | Prompt("Pick a movie you like:") )

  >movie>
 
"I heard that " + movie + " is a good movie."
