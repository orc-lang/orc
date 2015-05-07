package orc.runtime;

/**
 * A token in an isolated group is protected from forced termination.
 * @author quark
 */
public class IsolatedGroup extends Group {
	private Group parent;
	public IsolatedGroup(Group parent) {
		this.parent = parent;
		// NB: do not add ourselves to parent group; when
		// the parent is killed and recursively kills
		// its children, it won't include us so we
		// won't be killed.
	}
	public Group getParent() {
		return parent;
	}
}
