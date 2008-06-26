-- Definition of (recursive) expressions.
-- Expression is called like a site, but may publish many values.

def AskOnce(question) = Email(william,answer)
	<answer< David(question) | Adrian(question)
  
def AskLoop(question, interval) =
	AskOnce(question) >!answer>
	Rtimer(interval*1000) >>
	AskLoop(question, interval) 
  
AskLoop("What time is it?", 10)