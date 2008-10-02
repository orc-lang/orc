package orc.runtime.nodes;

public interface Visitor<E> {
	public E visit(Node node);
	public E visit(Assign node);
	public E visit(Call node);
	public E visit(Defs node);
	public E visit(Fork node);
	public E visit(Leave node);
	public E visit(Let node);
	public E visit(Pub node);
	public E visit(Return node);
	public E visit(Semi node);
	public E visit(Silent node);
	public E visit(Store node);
	public E visit(Subgoal node);
	public E visit(Unwind node);
	public E visit(WithLocation node);
}
