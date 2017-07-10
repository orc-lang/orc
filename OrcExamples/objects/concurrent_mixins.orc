{- concurrent_mixins.orc -- Using concurrency in mixins.
 -
 - Created by amp
 -}
 
class RequestHandler {
	def handle(r) :: Top
}

class Handler1 extends RequestHandler {
	def handle(r) = Rwait(100) >> Println("Handled " + r) >> 42
}

class Handler2 extends Handler1 {
	def handle(r) = Rwait(300) >> super.handle(r)
}

class HandlerLogger extends RequestHandler {
	def remoteLog(s) = Rwait(600) >> Println("Log: " + s)
	def handle(r) = remoteLog("Entered: " + r) >> super.handle(r) >v> remoteLog("Returned: " + v) >> v
}

class HandlerAsyncLogger extends RequestHandler {
	val logDelay = 600
	def remoteLog(s) = Rwait(logDelay) >> Println("Log: " + s)
	def handle(r) = remoteLog("Entered: " + r) >> stop | ((super.handle(r) >v> (remoteLog("Returned: " + v) >> stop | v)) ; remoteLog("Halted") >> stop)
}

class HandlerTimeout extends RequestHandler {
	val timeout = 350
	def handle(r) = {| Some(super.handle(r)) | Rwait(timeout) >> None |} >Some(v)> v
}

(
(new Handler1 with HandlerTimeout with HandlerAsyncLogger).handle("msg1") >v> Println(v) >> stop
) ; Println("======") >>
(
(new Handler1 with HandlerAsyncLogger with HandlerTimeout).handle("msg2") >v> Println(v) >> stop
) ; Println("======") >>
(
(new Handler2 with HandlerAsyncLogger with HandlerTimeout with { val logDelay = 1 }).handle("msg3") >v> Println(v) >> stop
) ; Println("======") >>
(
(new Handler2 with HandlerTimeout with HandlerAsyncLogger).handle("msg4") >v> Println(v) >> stop
)

{-
OUTPUT:
Handled msg1
42
Log: Entered: msg1
Log: Returned: 42
======
Handled msg2
42
======
Log: Entered: msg3
======
Log: Entered: msg4
Log: Halted
-}
