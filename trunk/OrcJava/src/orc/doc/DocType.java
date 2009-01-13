package orc.doc;

public class DocType extends DocNode {
	public final int depth;
	public final String type;

	public DocType(int depth, String type) {
		this.depth = depth;
		this.type = type;
	}
}
