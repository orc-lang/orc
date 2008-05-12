val c = Confirm("Do you like cheese?")

c() >b> (
	if(b) >> println("You're cool!") >> null
	| if (~b) >> println("You're weird!") >> null
)