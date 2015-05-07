package orc.ast.oil;

import orc.ast.oil.arg.Var;

/**
 * Renumber variables in an expression according
 * to some arbitrary mapping (relative to the context
 * of the expression).
 * @author quark
 */
public class RenameVariables extends Walker {
	public interface Renamer {
		public int rename(int var);
	}
	
	public static void rename(Expr expr, Renamer r) {
		expr.accept(new RenameVariables(r));
	}
	
	private int depth = 0;
	private Renamer renamer;
	
	private RenameVariables(Renamer renamer) {
		this.renamer = renamer;
	}
	
	@Override
	public void enterScope(int n) { depth += n; }
	
	@Override
	public void leaveScope(int n) { depth -= n; }
	
	@Override
	public void leave(Var arg) {
		arg.index = depth + renamer.rename(arg.index - depth);
	}
}
