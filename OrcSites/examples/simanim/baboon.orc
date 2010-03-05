class canvas = orc.lib.simanim.MonkeyCross
val sideLock = Buffer()
val disp = canvas(10)

-- List switch
def lswitch(n,l) =
	l >h:t>
	( (if(n=0) >> h) |
	  (if(~(n=0)) >> lswitch(n-1,t) )
	)

{--------------------------------------------------------------------
	spurtSource(lMin,lMax,sMin,sMax,oMin,oMax,mnMn,mnMx,mxMn,mxMx))
	-- lMin,lMax -- Lull period
	-- sMin,sMax -- How many per spurt
	-- oMin,oMax -- Range for output
	-- mnMn,mnMx -- Minimum delay during spurts
	-- mxMn,mxMx -- Maximum delay during spurts
	
	* Generate random numbers in periodic bursts:
	  Random intervals of nothing
	  Interspersed with spurts of something 
---------------------------------------------------------------------}
def randRange(mn,mx) =
	random(mx+1-mn)+mn

def nSpurt(n,tMin,tMax,oMin,oMax) =
	( if(n>=1) 
	  >> randRange(tMin,tMax)
	  >d> Ltimer(d)				-- Delay
	  >>	( randRange(oMin,oMax) 	-- Deliver a value
			| nSpurt(n-1,tMin,tMax,oMin,oMax) -- Recurse
			) 
	| if(n<=0)
	  >> false
	)
	
def spurtSource(lMin,lMax,sMin,sMax,oMin,oMax,mnMn,mnMx,mxMn,mxMx) =
	randRange(lMin,lMax) 
	>lull> Ltimer(lull)
	>> randRange(sMin,sMax)
	>spurt>	(nSpurt(spurt,tMin,tMax,oMin,oMax)
		<tMin< randRange(mnMn,mnMx)
		<tMax< randRange(mxMn,mxMx)
		) 
	>x>	( 	if (x=false)
			>> spurtSource(lMin,lMax,sMin,sMax,oMin,oMax,mnMn,mnMx,mxMn,mxMx)
		|	if (~(x=false)) >> x
		) 
----------------------------------------------------------------------	

{-
	Global function for releasing the lock
	that allows the rope to change sides.
-}
def freeSideLock() =
	println("Changing sides") >> sideLock.put(true)

{-
	Sporadically emit a number between 10 and 49
	[i.e., a monkey comes]
-}
def timeSource(tMin,tMax) =
	Ltimer(random(tMax-tMin)+tMin)>> ((random(32)+1) | timeSource(tMin,tMax))

def leftDone() =
	print("Left Done\n")

def rightDone() =
	print("Right Done\n")

{-
	Create a bidirectional linked list of semaphores.
	To cross to each link, you must first own the preceding
	link.  Once you own the next link, you release the first.
-}
def makeRope(len,lb,ls) =
	(if(len=0) >> let((),false,lb,ls)
	|	(  if(1<=len) 
		>> Buffer()
		>b> makeRope(len-1,b,true)
		>(rb,rs,eb,es)> b.put((lb,ls,rb,rs,len))
		>> let(b,true,eb,es)
		)
	)

{-
	Follow a rope along the right, beginning at link b with type s
	When we arrive at the end of the rope (s=false) we execute f().
	Between links, we logically delay for d units. 
-}
def followRight(b,s,f,d) =
	(	( if(~s) >> (let() | f()>>stop)
		)
	|	(  if(s) 
		>> b.get()
		>(lb,ls,rb,rs,len)> ( disp.setLink(len,d) >> false 
							| (Ltimer(d) >> true)
							)
		>c>	(	( if(c)
				>> followRight(rb,rs,f,d)
				>> b.put((lb,ls,rb,rs,len))
				>> disp.setLink(len,0)
				>> stop
				)
			|	if(~c)
			)
		)
	)

{-
	Follow a rope along to the left, beginning at link b with type s
	When we arrive at the end of the rope (s=false) we execute f().
	Between links, we logically delay for d units. 
-}
def followLeft(b,s,f,d) =
	(	( if(~s) >> (let() | f()>>stop)
		)
	|	(  if(s) 
		>> b.get()
		>(lb,ls,rb,rs,len)> ( disp.setLink(len,d) >> false 
							| (Ltimer(d) >> true)
							)
		>c>	(	( if(c)
				>> followLeft(lb,ls,f,d)
				>> disp.setLink(len,0)
				>> b.put((lb,ls,rb,rs,len))
				>> stop
				)
			|	if(~c)
			)
		)
	)	
	
{-
	Get a monkey from 'b'.
	Put it 'on deck'.
	Notify manager.
	Await acknowledgement.
-}
def bGuide(b,deck,sideFlag,sideAck,mainLine) =
	( b.get() 
	>d> deck.put(d)
	>>  sideFlag.get()
	>>  sideFlag.put(true)
	>>  mainLine.put(true)
	>>  sideAck.get()
	>>  bGuide(b,deck,sideFlag,sideAck,mainLine)
	) 
	
def bManager(mainLine,aPack,oPack) =
	oPack >(oFlag,oDeck,oAck,oFollow,oLink,oSign,oMonkeyDone,oPop)>
	aPack >(aFlag,aDeck,aAck,aFollow,aLink,aSign,aMonkeyDone,aPop)>
	mainLine.get() 
	>> oFlag.get() -- Check opposite side
	>rdy> oFlag.put(false) -- Replace notifier
	>>	((	if(rdy) -- A monkey's waiting
			>> oDeck.get() -- Get the waiting monkey
			>d> oAck.put(true) -- Acknowledge the opposite guide
			>> aFollow(aLink,aSign,freeSideLock,0) -- Flush current side
			>> sideLock.get() -- All the monkeys are across
			>> oFollow(oLink,oSign,oMonkeyDone,d) -- Send the new monkey across
			>> oPop()
			>> bManager(mainLine,oPack,aPack)
		)|(
			if(~rdy) -- No opposite monkey to worry about.
			>> aFlag.get() -- Assume this is true (one has to be)
			>> aFlag.put(false) -- Reset
			>> aDeck.get() -- Get the waiting monkey
			>d> aAck.put(true) -- Acknowledge the current guide
			>> aFollow(aLink,aSign,aMonkeyDone,d) -- Send the monkey across
			>> aPop() 
			>> bManager(mainLine,aPack,oPack)
		))

def framerate() =
	disp.redraw() >> Ltimer(5) >> framerate()
	
-- How many miliseconds go by in between logical timer clicks.
def realTime() =
	Ltimer(5) >> Rtimer(15) >> realTime()

	 
disp.open() >> 
( Ltimer(10000)>> (framerate() | realTime())
|( (spurtSource(500,2200,1,5,1,64,10,50,100,400) >lm> disp.leftPush(lm) >> leftQ.put(lm) >> stop)
 | (spurtSource(500,2200,1,5,1,64,10,50,100,400) >rm> disp.rightPush(rm) >> rightQ.put(rm) >> stop) -- The right side is more popular
 | bGuide(leftQ,  leftDeck, leftFlag, leftAck,mainLine)
 | bGuide(rightQ,rightDeck,rightFlag,rightAck,mainLine)
 | bManager(mainLine,aPack,oPack)
 )
< aPack < let( leftFlag, leftDeck, leftAck,followRight,lb,ls, leftDone,disp.leftPop)
< oPack < let(rightFlag,rightDeck,rightAck, followLeft,rb,rs,rightDone,disp.rightPop)
<(lb,ls,rb,rs)< makeRope(10,(),false)
< mainLine < Buffer()
< leftDeck < Buffer()
< rightDeck< Buffer()
< leftFlag < Buffer() >tmp> tmp.put(false) >> tmp
< rightFlag< Buffer() >tmp> tmp.put(false) >> tmp
< leftAck  < Buffer()
< rightAck < Buffer()
< leftQ    < Buffer()
< rightQ   < Buffer()
)


  
