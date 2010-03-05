package net.chayden.eliza;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *  Eliza decomp list.
 *  This stores all the decompositions of a single key.
 */
public class DecompList implements Iterable<Decomp> {
	private List<Decomp> list = new LinkedList<Decomp>();

    /**
     *  Add another decomp rule to the list.
     */
    public void add(String word, boolean mem, ReasembList reasmb) {
    	list.add(new Decomp(word, mem, reasmb));
    }

    /**
     *  Print the whole decomp list.
     */
    public void print(int indent) {
    	for (Decomp d : list) {
            d.print(indent);
        }
    }
    
    public Iterator<Decomp> iterator() {
    	return list.iterator();
    }
}

