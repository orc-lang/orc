package test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import orc.Orc;
import orc.OrcInstance;
import orc.runtime.values.Value;

/**
 * 
 * Simple test harness for testing Orc programs.
 * 
 * TODO: Expand this into a full regression test suite.
 * TODO: Develop a test result comparison based on partial orders to capture causality precisely.
 * 
 * @author dkitchin
 *
 */
public class Test {

	public static void main(String[] args) {
		
		test("examples/time/countdown.orc");
		
		System.exit(0);
	}
	
	public static void test(String src) {
		OrcInstance inst = Orc.runEmbedded(src);
		
		System.out.println("Executing source file " + src);
		
		System.out.println("---");
		while (inst.isRunning()) {
			try {
				Value v = inst.pubs().poll(1, TimeUnit.MILLISECONDS);
				if (v != null) {
					System.out.println("! " + v.toString());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
		System.out.println("---");
		
		System.out.println("Execution complete.");
	}
	
	
	public static List<Value> eval(String src) {
		
		List<Value> pubs = new LinkedList<Value>();
		OrcInstance inst = Orc.runEmbedded(src);
		
		while (inst.isRunning()) {
			try {
				Value v = inst.pubs().poll(1, TimeUnit.MILLISECONDS);
				if (v != null) {
					pubs.add(v);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}
		}
		
		return pubs;
	}

}
