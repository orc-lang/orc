package orc.ast.extended.expression;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

import orc.ast.extended.Visitor;
import orc.ast.extended.pattern.*;
import orc.ast.extended.type.AssertedType;
import orc.ast.extended.type.Type;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.ast.extended.declaration.def.Clause;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.expression.HasType;
import orc.ast.simple.expression.Throw;
import orc.ast.simple.type.TypeVariable;

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
	
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		
		Variable e = new Variable();
		List<Variable> formals = new ArrayList<Variable>();
		formals.add(e);
		
		/*
		 * Handle the case where none of the handlers match, in which case we need to rethrow.
		 */
		List<Pattern> rethrowFormals = new ArrayList<Pattern>();
		VariablePattern exceptionPattern = new VariablePattern("e");
		rethrowFormals.add(exceptionPattern);
		Expression rethrowBody = new Name("e");
		rethrowBody.setSourceLocation(getSourceLocation());
		orc.ast.extended.expression.Throw rethrow = new orc.ast.extended.expression.Throw(rethrowBody);
		rethrow.setSourceLocation(getSourceLocation());
		Clause rethrowClause = new Clause(rethrowFormals, rethrow);
		
		orc.ast.simple.expression.Expression fail = Pattern.fail();
		orc.ast.simple.expression.Expression body = rethrowClause.simplify(formals, fail);
		
		/*
		 * Simplify the list of exception handlers
		 */
		Collections.reverse(handlers);
		for(CatchHandler c : handlers){
			Clause handlerClause = new Clause(c.catchPattern, c.body);
			body = handlerClause.simplify(formals, body);
		}
		
		orc.ast.simple.expression.Expression simpleTryBlock = tryBlock.simplify();
		Variable unnamedVar = new Variable();
		List<TypeVariable> typeParams = new ArrayList<TypeVariable>();
		SourceLocation sourceLocation = getSourceLocation();
		orc.ast.simple.type.Type resultType = new orc.ast.simple.type.Bot();
		orc.ast.simple.expression.Def def = 
			new orc.ast.simple.expression.Def(unnamedVar, formals, body, 
											  typeParams, null, resultType, sourceLocation);
		return new orc.ast.simple.expression.Catch(def, simpleTryBlock);
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
