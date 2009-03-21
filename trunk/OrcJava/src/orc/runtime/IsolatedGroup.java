package orc.runtime;

public class IsolatedGroup extends Group {
	private Group parent;
	public IsolatedGroup(Group parent) {
		this.parent = parent;
		// NB: do not add to parent group
	}
	public Group getParent() {
		return parent;
	}
}
