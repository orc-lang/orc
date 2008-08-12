package orc.ast.extended.declaration;

import orc.error.compiletime.CompilationException;

/**
 * 
 * A unit of syntax that encapsulates some declaration. Declarations affect the environment, 
 * for example by adding new bindings, but do not typically do computation on their own.
 * 
 * A declaration is scoped in the abstract syntax tree by a Declare object.
 * 
 * @author dkitchin
 *
 */
public interface Declaration 
{ 
	public orc.ast.simple.Expression bindto(orc.ast.simple.Expression target) throws CompilationException;
}
