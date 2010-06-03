package orc.translation

import orc.oil.named._
import orc.lib.builtin._
import orc.oil._
import orc.sites.Site

object PrimitiveForms {

	def nullaryBuiltinCall(s : Site)() = Call(Constant(s), Nil, None)
	def unaryBuiltinCall(s : Site)(a : Argument) = Call(Constant(s), List(a), None)
	def binaryBuiltinCall(s : Site)(a : Argument, b: Argument) = Call(Constant(s), List(a, b), None)
	
	val callIf = unaryBuiltinCall(If) _
	val callNot = unaryBuiltinCall(Not) _
	val callEq = binaryBuiltinCall(Eq) _
	
	val callCons = binaryBuiltinCall(ConsConstructor) _
	val callIsCons = unaryBuiltinCall(ConsExtractor) _
	val callNil = nullaryBuiltinCall(NilConstructor) _
	val callIsNil = unaryBuiltinCall(NilExtractor) _
	
	val callSome = unaryBuiltinCall(SomeConstructor) _
	val callIsSome = unaryBuiltinCall(SomeExtractor) _
	val callNone = nullaryBuiltinCall(NoneConstructor) _
	val callIsNone = unaryBuiltinCall(NoneExtractor) _
	
	
	def makeUnapply(constructor : Argument, a : Argument) = {
		val extractor = new TempVar()
		val getExtractor = Call(Constant(FindExtractor), List(constructor), None)
		val invokeExtractor = Call(extractor, List(a), None)
		getExtractor > extractor > invokeExtractor
	}
	
	def makeNth(a : Argument, i : Int) = Call(a, List(Constant(Literal(i))), None)
	
	def makeLet(args: List[Argument]): Expression = {
		args match {
			case Nil => Constant(Signal)
			case List(a) => a
			case _ => makeTuple(args)
		}
	}
	
	def makeTuple(elements: List[Argument]) = Call(Constant(TupleConstructor), elements, None)
	
	def makeList(elements: List[Argument]) = {
		val nil : Expression = callNil()
		def cons(h: Argument, t: Expression): Expression = {
			val y = new TempVar()
			t > y > callCons(h, y)
		}
		elements.foldRight(nil)(cons)
	}
	
	def callOperator(opName : String, args : List[Argument]) = 
		Call(NamedVar(opName), args, None)
		
}