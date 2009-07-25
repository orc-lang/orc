package orc.ast.oil;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.xml.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

public class Catch extends Expr {
	
	public Def handler;
	public Expr tryBlock;
	
	public Catch(Def handler, Expr tryBlock){
		this.handler = handler;
		
		/* Currently, the argument handler type is assumed to be Bot, as a hack
		 * in the typechecker to allow partial checking of try-catch constructs,
		 * in advance of a more complete solution that accounts for both explicitly
		 * thrown values and Java-level exceptions thrown by sites.
		 */
		handler.argTypes = new LinkedList<Type>(); 
		handler.argTypes.add(Type.BOT);
		
		this.tryBlock = tryBlock;
	}
	
	public Type typesynth(Env<Type> ctx, Env<Type> typectx) throws TypeException {
		
		// Find the type of the try block
		Type blockType = tryBlock.typesynth(ctx, typectx);
	
		/* We ensure that the handler returns the try block type or some subtype.
		 * This is too conservative; the overall try-catch type could instead
		 * be the join of the try block and the handler return. However, in the absence of
		 * reliable type information about the argument to the handler (see comment
		 * in constructor), it is saner to check against a stated type rather than trying to 
		 * synthesize one.
		 */
		handler.resultType = blockType;
		handler.checkDef(ctx, typectx);
		handler.resultType = null;
		
		// The type of a try-catch, as described above, is just the type of the try block.
		return blockType;
	}
	
	public void addIndices(Set<Integer> indices, int depth) {
		handler.addIndices(indices, depth);
		tryBlock.addIndices(indices, depth);
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public Expression marshal() throws CompilationException {
		return new orc.ast.oil.xml.Catch(handler.marshal(), tryBlock.marshal());
	}

}
