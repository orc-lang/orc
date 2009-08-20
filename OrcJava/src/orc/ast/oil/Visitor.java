package orc.ast.oil;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.Defs;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Isolated;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.TypeDecl;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Var;

/**
 * Visitor for OIL expressions.
 * @author quark
 *
 * @param <E> Return type of the visitor.
 */
public interface Visitor<E> {
	public E visit(Parallel expr);
	public E visit(Call expr);
	public E visit(Defs expr);
	public E visit(Stop expr);
	public E visit(Pruning expr);
	public E visit(Sequential expr);
	public E visit(Otherwise expr);
	public E visit(WithLocation expr);
	public E visit(Constant arg);
	public E visit(Field arg);
	public E visit(Site arg);
	public E visit(Var arg);
	public E visit(Atomic atomic);
	public E visit(Isolated expr);
	public E visit(HasType hasType);
	public E visit(TypeDecl typeDecl);
	public E visit(Throw expr);
	public E visit(Catch catchExpr);
}
