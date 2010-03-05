//
// Walker.java -- Java class Walker
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

package orc.ast.oil.visitor;

import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;

/**
 * Abstract base class tree walker for OIL expressions.
 * 
 * @author quark
 */
public abstract class Walker implements Visitor<Void> {
	public void enterScope(final int n) {
	}

	public void leaveScope(final int n) {
	}

	public Void visit(final Parallel expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final Parallel expr) {
	};

	public void leave(final Parallel expr) {
	};

	public Void visit(final Call expr) {
		this.enter(expr);
		expr.callee.accept(this);
		for (final Argument a : expr.args) {
			a.accept(this);
		}
		this.leave(expr);
		return null;
	}

	public void enter(final Call expr) {
	};

	public void leave(final Call expr) {
	};

	public Void visit(final DeclareDefs expr) {
		this.enter(expr);
		this.enterScope(expr.defs.size());
		for (final Def def : expr.defs) {
			this.enter(def);
			this.enterScope(def.arity);
			def.body.accept(this);
			this.leaveScope(def.arity);
			this.leave(def);
		}
		expr.body.accept(this);
		this.leaveScope(expr.defs.size());
		this.leave(expr);
		return null;
	}

	public void enter(final Def def) {
	};

	public void leave(final Def def) {
	};

	public void enter(final DeclareDefs expr) {
	};

	public void leave(final DeclareDefs expr) {
	};

	public Void visit(final Stop arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}

	public void enter(final Stop arg) {
	};

	public void leave(final Stop arg) {
	};

	public Void visit(final Pruning expr) {
		this.enter(expr);
		this.enterScope(1);
		expr.left.accept(this);
		this.leaveScope(1);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final Pruning expr) {
	};

	public void leave(final Pruning expr) {
	};

	public Void visit(final Sequential expr) {
		this.enter(expr);
		expr.left.accept(this);
		this.enterScope(1);
		expr.right.accept(this);
		this.leaveScope(1);
		this.leave(expr);
		return null;
	}

	public void enter(final Sequential expr) {
	};

	public void leave(final Sequential expr) {
	};

	public Void visit(final Otherwise expr) {
		this.enter(expr);
		expr.left.accept(this);
		expr.right.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final Otherwise expr) {
	};

	public void leave(final Otherwise expr) {
	};

	public Void visit(final WithLocation expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final WithLocation expr) {
	};

	public void leave(final WithLocation expr) {
	};

	public Void visit(final Constant arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}

	public void enter(final Constant arg) {
	};

	public void leave(final Constant arg) {
	};

	public Void visit(final Field arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}

	public void enter(final Field arg) {
	};

	public void leave(final Field arg) {
	};

	public Void visit(final Site arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}

	public void enter(final Site arg) {
	};

	public void leave(final Site arg) {
	};

	public Void visit(final Variable arg) {
		this.enter(arg);
		this.leave(arg);
		return null;
	}

	public void enter(final Variable arg) {
	};

	public void leave(final Variable arg) {
	};

	public Void visit(final HasType expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final HasType expr) {
	};

	public void leave(final HasType expr) {
	};

	public Void visit(final DeclareType expr) {
		this.enter(expr);
		expr.body.accept(this);
		this.leave(expr);
		return null;
	}

	public void enter(final DeclareType expr) {
	};

	public void leave(final DeclareType expr) {
	};

	//TODO:
	public Void visit(final Catch catchExpr) {
		return null;
	}

	public Void visit(final Throw throwExpr) {
		return null;
	}

}
