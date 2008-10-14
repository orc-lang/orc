package orc.doc;

public class DocNode {
	public int depth;
	public String type;
	public String description;

	public DocNode(int depth, String type, String description) {
		this.depth = depth;
		this.type = type;
		this.description = description;
	}
}
