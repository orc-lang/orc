package orc.ast.oil;

import orc.ast.oil.arg.Constant;
import orc.ast.oil.arg.Field;
import orc.ast.oil.arg.Site;
import orc.ast.oil.arg.Var;

/**
 * Visitor for OIL expressions.
 * @author quark
 *
 * @param <E> Return type of the visitor.
 */
public interface Visitor<E> {
	public E visit(Bar expr);
	public E visit(Call expr);
	public E visit(Defs expr);
	public E visit(Silent expr);
	public E visit(Pull expr);
	public E visit(Push expr);
	public E visit(Semi expr);
	public E visit(WithLocation expr);
	public E visit(Constant arg);
	public E visit(Field arg);
	public E visit(Site arg);
	public E visit(Var arg);
	public E visit(Atomic atomic);
	public E visit(Isolated expr);
	public E visit(HasType hasType);
	public E visit(TypeDecl typeDecl);
}
