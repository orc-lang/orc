package orc.ast.oil.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.oil.type.InferredType;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

public class Catch extends Expression {
	
	public Def handler;
	public Expression tryBlock;
	
	public Catch(Def handler, Expression tryBlock){
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
	
	public Type typesynth(TypingContext ctx) throws TypeException {
		
		// Find the type of the try block
		Type blockType = tryBlock.typesynth(ctx);
	
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
	
	public void addIndices(Set<Integer> indices, int depth) {
		handler.addIndices(indices, depth);
		tryBlock.addIndices(indices, depth);
	}
	
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
	
	public <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext) {
		return cvisitor.visit(this, initialContext);
	}
	
	public orc.ast.xml.expression.Expression marshal() throws CompilationException {
		return new orc.ast.xml.expression.Catch(handler.marshal(), tryBlock.marshal());
	}

}
