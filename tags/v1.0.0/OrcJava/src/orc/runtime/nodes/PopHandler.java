package orc.runtime.nodes;

import orc.error.runtime.UncaughtException;
import orc.runtime.Token;

public class PopHandler extends Node {
	
	Node next;
	
	public PopHandler(Node next){
		this.next = next;
	}
	
	public void process(Token t){
		t.popHandler();
		t.move(next).activate();
	}
}
