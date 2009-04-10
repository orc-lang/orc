package orc.ast.oil.xml;

import java.util.LinkedList;

import orc.ast.oil.HasType;
import orc.ast.oil.TypeDecl;
import orc.ast.oil.Visitor;

/**
 * Convert an Orc OIL expression into a representation which
 * can be directly serialized into XML by JAXB.
 * 
 * FIXME: currently discards all type information
 * 
 * @author quark
 */
public class Marshaller implements Visitor<Expression> {
	
	public Expression visit(orc.ast.oil.Bar expr) {
		return new Bar(expr.left.accept(this), expr.right.accept(this));
	}
	
	public Expression visit(orc.ast.oil.Call expr) {
		LinkedList<Argument> arguments
			= new LinkedList<Argument>();
		for (orc.ast.oil.arg.Arg a : expr.args) {
			arguments.add((Argument)a.accept(this));
		}
		return new Call(
				(Argument)expr.callee.accept(this),
				arguments.toArray(new Argument[]{}));
	}
	
	public Expression visit(orc.ast.oil.Defs expr) {
		LinkedList<Definition> definitions
			= new LinkedList<Definition>();
		for (orc.ast.oil.Def d : expr.defs) {
			definitions.add(new Definition(d.arity, d.body.accept(this), d.location, d.name));
		}
		return new Definitions(definitions.toArray(new Definition[]{}),
				expr.body.accept(this));
	}
	
	public Expression visit(orc.ast.oil.Silent expr) {
		return new Silent();
	}
	
	public Expression visit(orc.ast.oil.Pull expr) {
		return new Pull(expr.left.accept(this), expr.right.accept(this), expr.name);
	}
	
	public Expression visit(orc.ast.oil.Push expr) {
		return new Push(expr.left.accept(this), expr.right.accept(this), expr.name);
	}
	
	public Expression visit(orc.ast.oil.Semi expr) {
		return new Semicolon(expr.left.accept(this), expr.right.accept(this));
	}
	
	public Expression visit(orc.ast.oil.WithLocation expr) {
		return new WithLocation(expr.expr.accept(this), expr.location);
	}
	
	public Expression visit(orc.ast.oil.Atomic atomic) {
		return new Atomic(atomic.body.accept(this));
	}
	
	public Expression visit(orc.ast.oil.Isolated atomic) {
		return new Isolated(atomic.body.accept(this));
	}

	public Expression visit(orc.ast.oil.arg.Constant arg) {
		return new Constant(arg.v);
	}
	
	public Expression visit(orc.ast.oil.arg.Field arg) {
		return new Field(arg.key);
	}
	
	public Expression visit(orc.ast.oil.arg.Site arg) {
		return new Site(arg.site.getProtocol(), arg.site.getLocation());
	}

	public Expression visit(orc.ast.oil.arg.Var arg) {
		return new Variable(arg.index);
	}

	public Expression visit(HasType hasType) {
		// FIXME: need to encode this
		return hasType.body.accept(this);
	}

	public Expression visit(TypeDecl typeDecl) {
		// FIXME: need to encode this
		return typeDecl.body.accept(this);
	}
}