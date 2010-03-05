package net.chayden.eliza;

import java.util.Vector;

/**
 *  Eliza reassembly list.
 */
public final class ReasembList {
	private final Vector<String> list = new Vector<String>();

    /**
     *  Add an element to the reassembly list.
     */
    public void add(String reasmb) {
    	list.add(reasmb);
    }
    
    public int size() {
    	return list.size();
    }
    
    public String get(int index) {
    	return list.get(index);
    }

    /**
     *  Print the reassembly list.
     */
    public void print(int indent) {
    	for (String s : list) {
            for (int j = 0; j < indent; j++) System.out.print(" ");
            System.out.println("reasemb: " + s);
        }
    }
}

