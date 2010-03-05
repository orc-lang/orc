//
// Catch.java -- Java class Catch
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.TokenContinuation;
import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.type.InferredType;
import orc.ast.oil.visitor.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.Token;
import orc.runtime.values.Closure;
import orc.runtime.values.Future;
import orc.type.Type;
import orc.type.TypingContext;

public class Catch extends Expression {

	public Def handler;
	public Expression tryBlock;

	public Catch(final Def handler, final Expression tryBlock) {
		this.handler = handler;

		/* Currently, the argument handler type is assumed to be Bot, as a hack
		 * in the typechecker to allow partial checking of try-catch constructs,
		 * in advance of a more complete solution that accounts for both explicitly
		 * thrown values and Java-level exceptions thrown by sites.
		 */
		handler.argTypes = new LinkedList<orc.ast.oil.type.Type>();
		handler.argTypes.add(orc.ast.oil.type.Type.BOT);

		this.tryBlock = tryBlock;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (handler == null ? 0 : handler.hashCode());
		result = prime * result + (tryBlock == null ? 0 : tryBlock.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Catch other = (Catch) obj;
		if (handler == null) {
			if (other.handler != null) {
				return false;
			}
		} else if (!handler.equals(other.handler)) {
			return false;
		}
		if (tryBlock == null) {
			if (other.tryBlock != null) {
				return false;
			}
		} else if (!tryBlock.equals(other.tryBlock)) {
			return false;
		}
		return true;
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {

		// Find the type of the try block
		final Type blockType = tryBlock.typesynth(ctx);

		/* We ensure that the handler returns the try block type or some subtype.
		 * This is too conservative; the overall try-catch type could instead
		 * be the join of the try block and the handler return. However, in the absence of
		 * reliable type information about the argument to the handler (see comment
		 * in constructor), it is saner to check against a stated type rather than trying to 
		 * synthesize one.
		 */
		handler.resultType = new InferredType(blockType);
		handler.checkDef(ctx);
		handler.resultType = null;

		// The type of a try-catch, as described above, is just the type of the try block.
		return blockType;
	}

	@Override
	public void addIndices(final Set<Integer> indices, final int depth) {
		handler.addIndices(indices, depth);
		tryBlock.addIndices(indices, depth);
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Catch(handler.marshal(), tryBlock.marshal());
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		final TokenContinuation K = new TokenContinuation() {
			public void execute(final Token t) {
				t.popHandler();
				leave(t);
			}
		};
		tryBlock.setPublishContinuation(K);
		tryBlock.populateContinuations();
		handler.populateContinuations();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(final Token t) {
		/* 
		 * Create a closure to do the call.
		 */
		final Set<Variable> free = handler.freeVars();
		final List<Object> freeValues = new LinkedList<Object>();
		for (final Variable v : free) {
			final Object value = v.resolve(t.getEnvironment());
			if (value instanceof Future) {
				freeValues.add(value);
			}
		}

		final Closure closure = new Closure(handler, freeValues);
		final Env<Object> env = new Env<Object>();
		for (final Variable v : handler.freeVars()) {
			env.add(v.resolve(t.getEnvironment()));
		}
		closure.env = env;

		//	pass next so the handler knows where to return.
		t.pushHandler(closure, getPublishContinuation());
		tryBlock.enter(t.move(tryBlock));
	}
}
