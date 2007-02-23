/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;
import antlr.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;

import orc.runtime.values.Tuple;


/**
 * Abstract syntax for calls. Includes both site calls and definition calls
 * @author wcook
 */
public class Call extends OrcProcess {
	OrcProcess fun;
	List<OrcProcess> args;
	Token tok = null;

	static int varNum;
	
	public Call(OrcProcess fun, List<OrcProcess> args) {
		this.fun = fun;
		if (args == null)
	  	 	args = new ArrayList<OrcProcess>();
		this.args = args;
	}
	public Call(OrcProcess fun, List<OrcProcess> args, Token t) {
		this.fun = fun;
		if (args == null)
	  	 	args = new ArrayList<OrcProcess>();
		this.args = args;
		this.tok = t;
	}
	
	public Call(String name, List<OrcProcess> args) {
		this.fun = new Name(name);
		if (args == null)
	  	 	args = new ArrayList<OrcProcess>();
		this.args = args;
	}
	
	public Call(OrcProcess fun, OrcProcess p) {
		this.fun = fun;
		this.args = new ArrayList<OrcProcess>();
		this.args.add(p);
	}
	public Call(OrcProcess fun, OrcProcess p, Token t) {
		this.fun = fun;
		this.args = new ArrayList<OrcProcess>();
		this.args.add(p);
		this.tok = t;
	}
	public Call(String name, OrcProcess p) {
		this.fun = new Name(name);
		this.args = new ArrayList<OrcProcess>();
		this.args.add(p);
	}
	public Call(String name, OrcProcess p, Token t) {
		this.fun = new Name(name);
		this.args = new ArrayList<OrcProcess>();
		this.args.add(p);
		this.tok = t;
	}
	public Call(String name) {
		this.fun = new Name(name);
		this.args = new ArrayList<OrcProcess>();
	}

	
	/** 
	 * Resolve a call by resolving its args.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
    	List<OrcProcess> argsRes = new ArrayList<OrcProcess>();
		for (OrcProcess p : args) {
			argsRes.add(p.resolveNames(bound,vals));	
		}
		return new Call(fun.resolveNames(bound,vals),argsRes,tok);
	}
	

	/** 
	 * If "fun" is not a name, then this call fun(args) is really fun >F> F(args) 
	 * beacause fun is an expression that will publish a site or a closure.
	 * Otherwise, if the "fun" is a definition then we use the "deftranslate" to replace complex
	 * args by new local definitions.
	 * Otherwise we assume that the "fun" is a site and we use the "translate" to make sure
	 * that all the args are replaced by Values--variables known to be bound to values.
	 * If "fun" is a definition and it has formals but there are no arguments, then this
	 * "call" should just return the closure for "fun".
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		if (fun.Name() == null){
			String newVar = "V" + varNum++;
			Call newCall = new Call(new Value(newVar), args,tok);
			SequentialComposition seq = 
				new SequentialComposition(fun,newVar, false, newCall);
			return seq.compile(output,defs);
		}
		if (fun.isDef(defs)){
			if (fun.Lookup(defs).formals.size() > 0 && args.size() == 0 && false){
				// compile it as let(fun)
				// this case is deprecated, hence the '&& false'  -dkitchin
				Param p = fun.asParam();
				List<Param> param = new ArrayList<Param>();
				param.add(p);
				return new orc.runtime.nodes.Call(new String("let"),param, output,tok); 
			}
			else for (OrcProcess p : args)
				if (!p.isSimple())
					return deftranslate().compile(output,defs);}
		else {
			for (OrcProcess q : args)
				if (!q.isValue())
					return translate().compile(output,defs);}
			
		List<Param> params = new ArrayList<Param>();
		for (OrcProcess p : args)
			params.add(p.asParam());
		return new orc.runtime.nodes.Call(fun.Name(), params, output,tok); 
	}
	

	/**
	 * Translates nested calls:
	 * <pre>
	 * 	  M(A(), B())
	 * </pre>
	 * is interpreted as
	 * <pre>
	 *    M(a, b) where a = A(); b = B()
	 * </pre>
	 * @return process to be executed
	 */
	public OrcProcess translate() {
		List<OrcProcess> newArgs = new ArrayList<OrcProcess>();
		Call newCall = new Call(fun, newArgs);
		AsymmetricParallelComposition where = 
			new AsymmetricParallelComposition(newCall);

		for (OrcProcess p : args)
			if (p.isValue())
				newArgs.add(p);
			else {
				String newVar = "V" + varNum++;
				newArgs.add(new Value(newVar));
				where.addBinding(newVar, p);
			}
		return where;
	}
	
	/**
	 * Alternate version translates nested calls:
	 * <pre>
	 * 	  M(A(), B())
	 * </pre>
	 * is interpreted as
	 * <pre>
	 *    def a = A()
	 *    def b = B()
	 *    M(a, b) 
	 * </pre>
	 * @return process to be executed
	 */
	public OrcProcess deftranslate() {
		List<OrcProcess> newArgs = new ArrayList<OrcProcess>();
		List<Definition> defs = new ArrayList<Definition>();
		OrcProcess answer = new Call(fun, newArgs,tok);
		List<String> noformals = new ArrayList<String>();
		
		for (OrcProcess p : args)
			if (p.isSimple())
				newArgs.add(p);
			else {
				String newVar = "D" + varNum++;
				newArgs.add(new Def(newVar));
				defs.add(new Definition(newVar,noformals,p));
				}
		answer = new Define(defs,answer);
		return answer;
	}



	public String toString() {
		return fun.Name() + Tuple.format('(', args, ", ", ')');
	}
	
	
}
