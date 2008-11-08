package orc.doc;

public class DocType extends DocNode {
	public int depth;
	public String type;

	public DocType(int depth, String type) {
		this.depth = depth;
		this.type = type;
	}
}
