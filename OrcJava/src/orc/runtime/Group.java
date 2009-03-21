package orc.runtime;

import java.util.HashSet;

/**
 * Every token belongs to a group which decides whether
 * that token is active or not. Groups, unlike regions, can
 * be forcibly killed.
 * 
 * @author quark
 */
public class Group {
	protected boolean alive = true;
	private HashSet<Group> children = new HashSet<Group>();
	
	/** Add a subgroup. */
	public final void add(Group child) {
		if (!alive) return;
		this.children.add(child);
	}
	
	/** Remove a subgroup. */
	public final void remove(Group child) {
		if (!alive) return;
		this.children.remove(child);
	}
	
	/**
	 * Forcibly terminate this group and all of its
	 * children.
	 */
	public void kill() {
		if (!alive) return;
		alive = false;
		for (Group child : children) {
			child.kill();
		}
		children.clear();
	}
	
	public final boolean isAlive() {
		return alive;
	}
}
