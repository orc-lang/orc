package orc.ast.oil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnspecifiedReturnTypeException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Unwind;
import orc.type.ArrowType;
import orc.type.Type;

public class Defs extends Expr {

	public List<Def> defs;
	public Expr body;
	
	public Defs(List<Def> defs, Expr body)
	{
		this.defs = defs;
		this.body = body;
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		depth += defs.size();
		for (Def d : defs) d.addIndices(indices, depth);
		body.addIndices(indices, depth);
	}
	
	public String toString() {
		String repn = "(defs  ";
		for (Def d : defs) {
			repn += "\n  " + d.toString();
		}
		repn += "\n)\n" + body.toString();
		return repn;
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {

		/* This is the typing context we will use to check the scoped expression */
		Env<Type> dctx = ctx;
			
		/* Add the types for each definition in the group to the context */
		for (Def d : defs) {
			try {
				dctx = dctx.extend(d.type(typectx));
			}
			/* If the return type is unspecified, this function can't be called recursively,
			 * so its name cannot have a type associated with it.
			 */
			catch (UnspecifiedReturnTypeException e) {
				dctx = dctx.extend(null);
			}
			catch (TypeException e) {
				e.setSourceLocation(d.getSourceLocation());
				throw e;
			}
		}
		
		/* 
		 * Use this context, with all definition names bound,
		 * to verify each definition individually.
		 */ 
		for (Def d : defs) {
			d.checkDef(dctx, typectx);
		}
		
		/* Now, repeat the process, but require each definition to provide a type.
		 * Any missing return type ascriptions should now be filled in.
		 */
		Env<Type> bodyctx = ctx;
		for (Def d : defs) {
			bodyctx = bodyctx.extend(d.type(typectx));
		}
		
		/*
		 * The synthesized type of the body in this context is
		 * the synthesized type for the whole expression.
		 */ 
		return body.typesynth(bodyctx, typectx); 
	}
	
	
	/* There is a special case when checking translated lambdas, so we override this method */
	public void typecheck(Type T, Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		/* Check whether this definition group is a translated lambda,
		 * and make sure that the type being checked is an arrow type.
		 */
		if (defs.size() == 1
				&& body.getClass().equals(Var.class) 
				&& ((Var)body).index == 0
				&& T instanceof ArrowType) {
			/* Add an empty mapping for the type of the function itself;
			 * since it is anonymous, recursion can never occur.
			 */
			ctx = ctx.extend(null);
			
			defs.get(0).checkLambda((ArrowType)T, ctx, typectx);
		}
		else {
			/* Otherwise, perform checking as usual */
			super.typecheck(T, ctx, typectx);
		}
	}
	
	
}
