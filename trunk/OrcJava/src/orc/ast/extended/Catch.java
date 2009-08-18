package orc.ast.extended;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import orc.ast.extended.pattern.*;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.ast.extended.declaration.defn.Clause;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.AssertedType;
import orc.ast.simple.type.Type;
import orc.ast.simple.HasType;
import orc.ast.simple.Throw;

public class Catch extends Expression {
	public List<CatchHandler> handlers;
	public Expression tryBlock;
	
	protected List<String> typeParams;
	protected List<Type> argTypes;
	protected Type resultType;
	
	public Catch(Expression tryBlock, List<CatchHandler> handlers){
		this.tryBlock = tryBlock;
		this.handlers = handlers;
	}
	
	public orc.ast.simple.Expression simplify() throws CompilationException {
		
		Var e = new Var();
		List<Var> formals = new ArrayList<Var>();
		formals.add(e);
		
		/*
		 * Handle the case where none of the handlers match, in which case we need to rethrow.
		 */
		List<Pattern> rethrowFormals = new ArrayList<Pattern>();
		VariablePattern exceptionPattern = new VariablePattern("e");
		rethrowFormals.add(exceptionPattern);
		Expression rethrowBody = new Name("e");
		rethrowBody.setSourceLocation(getSourceLocation());
		orc.ast.extended.Throw rethrow = new orc.ast.extended.Throw(rethrowBody);
		rethrow.setSourceLocation(getSourceLocation());
		Clause rethrowClause = new Clause(rethrowFormals, rethrow);
		
		orc.ast.simple.Expression fail = Pattern.fail();
		orc.ast.simple.Expression body = rethrowClause.simplify(formals, fail);
		
		/*
		 * Simplify the list of exception handlers
		 */
		Collections.reverse(handlers);
		for(CatchHandler c : handlers){
			Clause handlerClause = new Clause(c.catchPattern, c.body);
			body = handlerClause.simplify(formals, body);
		}
		
		orc.ast.simple.Expression simpleTryBlock = tryBlock.simplify();
		Var v = new Var();
		List<String> l = new ArrayList<String>();
		SourceLocation sl = getSourceLocation();
		orc.ast.simple.Definition def = new orc.ast.simple.Definition(v, formals, body, l, null, null, sl);
		return new orc.ast.simple.Catch(def, simpleTryBlock);
	}
	
	public String toString() {
		String s = "try (" + tryBlock.toString() + ")";
		for(CatchHandler c : handlers){
			s = s + handlers.toString();
		}
		return s;
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
