package orc.orchard.test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.CompilerService;
import orc.orchard.error.*;
import orc.orchard.Oil;
import orc.orchard.CompilerService;
import orc.orchard.Publication;
import orc.orchard.rmi.RemoteJobService;
import orc.orchard.rmi.RemoteExecutorService;
import orc.orchard.rmi.ExecutorService;

public class RmiTest {
	public static void main(String[] args) {
		URI executorURI;
		try {
			executorURI = new URI("rmi://localhost/orchard");
		} catch (URISyntaxException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
		if (args.length > 0) {
			try {
				executorURI = new URI(args[0]);
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI '" + executorURI + "'");
				return;
			}
		}
		CompilerService compiler = new CompilerService();
		RemoteExecutorService executor;
		try {
			executor = (RemoteExecutorService)Naming.lookup(executorURI.toString());
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + executorURI + "'");
			return;
		} catch (NotBoundException e) {
			System.err.println("URI not bound '" + executorURI);
			return;
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		}
		Oil oil;
		try {
			oil = compiler.compile("def M(x) = x | Rtimer(1000) >> M(x+1) M(1)");
		} catch (InvalidProgramException e) {
			// this is impossible by construction
			throw new AssertionError(e);			
		}
		URI jobURI;
		try {
			jobURI = executor.submit(oil);
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (QuotaException e) {
			System.err.println("Quota error: " + e.toString());
			return;
		} catch (InvalidOilException e) {
			System.err.println("OIL error: " + e.toString());
			return;
		}
		System.out.println("Job URI: " + jobURI);
		RemoteJobService job;
		try {
			job = (RemoteJobService)Naming.lookup(jobURI.toString());
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + jobURI + "'");
			return;
		} catch (NotBoundException e) {
			System.err.println("URI not bound '" + jobURI);
			return;
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		}
		System.out.println("Bound job");
		try {
			System.out.println(job.state());
			job.start();
			System.out.println(job.state());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// ignore
			}
			System.out.println("Done waiting");
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			List<Publication> pubs = job.publications();
			System.out.println(pubs.toString());
			job.abort();
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (InvalidJobStateException e) {
			// impossible by construction
			throw new AssertionError(e);
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
}
