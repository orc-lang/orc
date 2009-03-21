package orc.runtime;

public class IsolatedGroup extends Group {
	private Group parent;
	public IsolatedGroup(Group parent) {
		this.parent = parent;
		// NB: do not add to parent group; this
		// ensures we will not be killed
	}
	public Group getParent() {
		return parent;
	}
}
