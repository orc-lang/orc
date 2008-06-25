-- Definition of (recursive) expressions

def AskOnce(question) =
  Email(william,answer) <answer< David(question) | Adrian(question)
  
def AskLoop(question, interval) =
  AskOnce(question) >!answer> Rtimer(interval*1000) >> AskLoop(question) 
  
AskLoop("What time is it?", 10) 