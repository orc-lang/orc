class canvas = "orc.lib.simanim.BarberShop"
{----------------------------------------------------------------
	Non-blocking channel.  Sets up a 'try' functionality that returns
	a pair.  Second value of the pair indicates success or failure of
	the fetch.  Technically, this is not _completely_ non-blocking since
	it does force single thread access.  However, it should always return
	quickly.  
-----------------------------------------------------------------}
def NBChannel() =
	Channel() >q> Channel() >cnt> cnt.put(0) >> (cnt,q)
	
def nbPut(mb,v) =
	mb >(cnt,q)> cnt.get() >size> q.put(v) >> cnt.put(size+1)
	
def nbTry(mb) =
	mb 	>(cnt,q)> cnt.get() 
		>size> 	( (Ift(size>=1) >> q.get() >v> cnt.put(size-1) >> Let(v,true))
				| (Ift(size<=0) >> cnt.put(size) >> Let(signal,false))
				)

def nbGet(mb) =
	mb 	>(cnt,q)> cnt.get() 
		>size> 	( q.get()
				| cnt.put(size-1) >> stop
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
	Random(mx+1-mn)+mn

def nSpurt(n,tMin,tMax,oMin,oMax) =
	( Ift(n>=1) 
	  >> randRange(tMin,tMax)
	  >d> Rwait(d)				-- Delay
	  >>	( randRange(oMin,oMax) 	-- Deliver a value
			| nSpurt(n-1,tMin,tMax,oMin,oMax) -- Recurse
			) 
	| Ift(n<=0)
	  >> false
	)
	
def spurtSource(lMin,lMax,sMin,sMax,oMin,oMax,mnMn,mnMx,mxMn,mxMx) =
	randRange(lMin,lMax) 
	>lull> Rwait(lull)
	>> randRange(sMin,sMax)
	>spurt>	(nSpurt(spurt,tMin,tMax,oMin,oMax)
		<tMin< randRange(mnMn,mnMx)
		<tMax< randRange(mxMn,mxMx)
		) 
	>x>	( 	Ift(x=false)
			>> spurtSource(lMin,lMax,sMin,sMax,oMin,oMax,mnMn,mnMx,mxMn,mxMx)
		|	Ift(~(x=false)) >> x
		) 
------------------------------------------------------------------
-- Globals
------------------------------------------------------------------
val shop = NBChannel() -- No one waits outside to get their hair cut
val bChair = NBChannel() -- If I can sit down immediately, I will.
val wChair = Channel() -- Waiting-area chairs
val cashReg = Channel() -- The cash register
val job1 = Channel()
val job2 = Channel()
val job3 = Channel()
val ch1 = Channel()
val ch2 = Channel()
val ch3 = Channel()
val disp = canvas(8,3)

-----------------
-- List switch --
-----------------
def lswitch(n,l) =
	l >h:t>
	( (Ift(n=0) >> h) |
	  (Ift(~(n=0)) >> lswitch(n-1,t) )
	)

def animateTransition(tt,n,d) =
	(	Ift(n<=19)
		>> disp.setTransition(tt,n)
		>> Rwait(d/8+1)
		>> animateTransition(tt,n+1,d)
	|   Ift(n>=20)
	)

-------------------------------------------------------------------
--  Customer Functions
-------------------------------------------------------------------
-- If the shop is full, we can't enter
def tryToEnterShop(cID) =
	nbTry(shop)
	>(x,ok)> 	(	Ift(ok)
					>>Print("Cst ",cID," enters\n")
					>>disp.pushFloor(cID) 
					>>stop
				| Rwait(5) >> ok
				)
	
-- Put the customer's place back in the channel
def leaveShop(cID) =
	Print("Cst ",cID," leaves happy\n")
	>> disp.popRegister()
	>> Rwait(25)
	>> nbPut(shop,true)

-- Nothing to do here, unless we want to animate it
def leaveAngry(cID) =
	Print("Cst ",cID," can't fit\n")
	>> cID 

def tryToGetBarberChair(cID) =
	nbTry(bChair) 
	>(ch,ok)> 	(	Ift(ok) -- There's no wait for the barber
					>> ch 
					>(chID,chDone,bJob)> disp.popFloor() --Leave the floor
					>> disp.lineToBChair(chID,cID)
					>tt> animateTransition(tt,1,cID)
					>> disp.endTransition(tt)
					>> disp.setBChairState(chID,cID) -- Take a seat at the barber's
				|	Ift(~ok)
				)
	>>(ch,ok)
	
def getBarberChair(cID) =
	nbGet(bChair) 
	>ch> ch 
	
	
def freeBarberChair(cID,ch) =
	ch 
	>(chID,chDone,bJob)> disp.setBChairState(chID,0) -- Dust off all the hair
	>> nbPut(bChair,ch)
	
def getWaitChair(cID) =
	wChair.get() 
	>wt> disp.popFloor() -- Get out of line
	>> disp.lineToChair(wt,cID)
	>tt> animateTransition(tt,1,cID)
	>> disp.endTransition(tt)
	>> disp.setChairState(wt,cID) -- Have a seat to wait
	>> Rwait(20) -- Read the Highlights magazine
	>> wt
	
def freeWaitChair(cID,chair,wt) =
	disp.setChairState(wt,0) -- Put the magazine down
	>> wChair.put(wt)
	>> chair 
	>(chID,chDone,bJob)> disp.chairToBChair(wt,chID,cID)
	>tt> animateTransition(tt,1,cID)
	>> disp.endTransition(tt)
	>> disp.setBChairState(chID,cID)
	

-- ch has the chairID and the associated barber.
def signalBarber(cID,ch) =
	Print("Cst ",cID," wants a haircut\n")
	>> Rwait(10)
	>> ch >(chID,chDone,bJob)> bJob.put((true,cID))
	
def waitForHairCut(cID,ch) =
	ch >(chID,chDone,bJob)> chDone.get()

def waitForCashRegister(cID,chair) =
	Print("Cst ",cID," at cashReg\n")
	>> chair 
	>(chID,chDone,bJob)> disp.bChairToLine(chID,cID)
	>tt> animateTransition(tt,1,cID)
	>> disp.endTransition(tt)
	>> disp.pushRegister(cID)
	>> waitForBarber(cID)

-- Ring the bell so a barber will come accept your payment
def waitForBarber(cID) =
	Channel()  -- Create a place to receive the barber
	>ding> NBChannel() -- Alert all three barbers of your need
	>cust> nbPut(cust,ding)
	>> 	(job1.put((false,cust))>>stop  
		|job2.put((false,cust))>>stop
		|job3.put((false,cust))
		)
	>> ding.get() -- Wait for a barber to respond
	
-- Give a hand full of cash, expect a receipt in return
def presentPayment(cID,brbr) =
	Channel() 
	>hand> brbr.put(hand)
	>> hand
	
-- Once the barber gives you the receipt, get out of the way
def waitForReceipt(cID,rcpt) =
	rcpt.get() >> cashReg.put(true)
	
	
{-----------------------------------------------------------------
	Customer wants to enter the store and take a barber chair if
	one is available.  Otherwise, he tries to sit on the waiting
	chairs.  When a barber chair is available, the customer takes
	it and waits for his hair to be cut.  After the haircut, the
	customer waits in line at the cash register, presents payment
	takes his receipt, and leaves.  
-----------------------------------------------------------------}
def customer(cID) =
	tryToEnterShop(cID) >gotIn>  
	( 	( Ift(gotIn) 
		>>tryToGetBarberChair(cID) 
		>(chair,ok)> 	( Ift(~ok)>> getWaitChair(cID)
								 >wait> getBarberChair(cID) 
								 >chair> freeWaitChair(cID,chair,wait)
								 >> chair
						| Ift(ok) >> chair  -- We got the chair without waiting
						)
		>chair> signalBarber(cID,chair)
		>> waitForHairCut(cID,chair)
		>> freeBarberChair(cID,chair)
		>> waitForCashRegister(cID,chair) 
		>brbr> presentPayment(cID,brbr)
		>rcpt> waitForReceipt(cID,rcpt) -- This also frees the cashReg
		>> leaveShop(cID) 
		)
	| 	( Ift(~gotIn) >> leaveAngry(cID) )
	) >> stop	  

------------------------------------------------------------------
-- Barber Functions
------------------------------------------------------------------
-- Sleep until something needs to be done
def waitForJob(bID,jobQ) =
	Print("Barber ",bID," waits\n") 
	>> disp.setBarberState(bID,0)
	>> jobQ.get() 
	>jb> Print("Barber ",bID," got job\n")
	>> Rwait(lswitch(Random(4),[10,20,40,50]))
	>> jb
	
-- The chair holds the hair semaphore
def cutHair(bID,cID,chDone) =
	disp.setBarberState(bID,1)
	>> Rwait(Random(300)+50) 
	>> Print("Barber ",bID," cut hair",cID,"\n") 
	>> chDone.put(true)
	
-- Answer the ringing bell of a customer at the cash register
-- Extend a hand to receive cash
def takePayment(bID,ding) =
	cashReg.get()
	>> disp.setBarberState(bID,4) -- Don't draw
	>> disp.barberToReg(bID)
	>tt> animateTransition(tt,1,5)
	>> disp.endTransition(tt)
	>> disp.setBarberState(bID,2)
	>> Channel() 
	>hand> ding.put(hand)
	>> disp.setReg(true) 
	>> hand.get()
	>h> Rwait(50)
	>> Print("Barber ",bID," got cash\n") 
	>> h

-- Once we get the cash deliver a receipt and always say 'thank-you'
def giveReceipt(bID,cash) =
	disp.setReg(false)
	>> Rwait(15)
	>> cash.put(bID)
	>> disp.setBarberState(bID,4) -- Don't draw
	>> disp.barberFromReg(bID)
	>tt> animateTransition(tt,1,5)
	>> disp.endTransition(tt)
	>> Print("Barber ",bID," gave receipt\n") 

{----------------------------------------------------------------
	Barber keeps a job queue.  When work needs to be done, he
	looks at the kind of work.  If it's a customer in his barber
	chair, he cuts the customer's hair.  
-----------------------------------------------------------------}
def barber(bID,jobQ,chDone) =
	waitForJob(bID,jobQ) -- True for a haircut, false for a cashier job
	>(job,cust)> (	Ift(job) 
					>> cutHair(bID,cust,chDone)
				 |	Ift(~job)
				 	>> nbTry(cust)
				 	>(cust,ok)> ( 	Ift(ok) 
				 					>> takePayment(bID,cust)
				 					>cash> giveReceipt(bID,cash)
				 				| 	Ift(~ok) -- The customer's been taken care of
				 				) 
				 )
	>> barber(bID,jobQ,chDone) 
-----------------------------------------------------------------
def forLoop(ii,f) =
	  Ift(ii>=1) >> f(ii) >> forLoop(ii-1,f)
	| Ift(ii<=0) >> signal

def fillShop(ii) =
	nbPut(shop,ii)

def framerate() =
	disp.redraw() >> Rwait(1) >> framerate()
	
-- How many miliseconds go by in between logical timer clicks.
def realTime() =
	Rwait(1) >> Rwait(5) >> realTime()
	

-----------------------------------------------------------------
-- Main
-----------------------------------------------------------------	
-- Initialize Variables	
  forLoop(8,wChair.put) >> stop		-- Eight waiting chairs
| cashReg.put(true) >> stop			-- One cash register
| nbPut(bChair,(1,ch1,job1)) >> stop
| nbPut(bChair,(2,ch2,job2)) >> stop
| nbPut(bChair,(3,ch3,job3)) >> stop
-- Each barber has an ID, a job queue, and "clippers"
| barber(1,job1,ch1)
| barber(2,job2,ch2)
| barber(3,job3,ch3)
-- Open the shop for business
| disp.open() 
  >> (	forLoop(20,fillShop) 
  		>> spurtSource(300,800,1,12,1,64,10,25,50,75) >cst> customer(cst)
  	 | Rwait(500)>> (framerate() | realTime())
  	 )

--(10|20|30|40|50|60|15|25|35)
 --spurtSource(500,2200,1,6,1,64,1,5,100,400)
 