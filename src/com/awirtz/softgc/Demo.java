package com.awirtz.softgc;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple command-line application to demonstrate SoftGC class.
 */
public class Demo {
	public static void main(String[] args) {
		System.out.println("SoftGC test...");
		final Runtime rt = Runtime.getRuntime();
		System.out.println("Max: "+rt.maxMemory());
		System.out.println("Mem: "+(rt.totalMemory()-rt.freeMemory()));

		final List<Element> list = new ArrayList<Element>();

		System.out.println("Enabling SoftGC thread");
		new SoftGC(new Free(){
			@Override
			public void free() {
				System.out.print("I need to free some memory... ");
				if(list.isEmpty()) {
					System.out.println("I have nothing left to free!");
					// If we have nothing left to free, better to throw an
					// error rather than getting stuck in an infinite loop.
					throw new OutOfMemoryError();
				} else {
					// This results in a simplistic FIFO eviction strategy.
					// Other more complex strategies are of course possible...
					Element el = list.remove(0);
					System.out.println("Freeing element #"+el.id);
				}
			}
		}, 20*1024*1024);
		System.out.println("Mem: "+(rt.totalMemory()-rt.freeMemory()));
		
		// The default heap size in my JVM is 128Mb, so this is sufficient to exhaust it.
		for(int i = 0; i < 100; i++) {
			list.add(new Element(i));
			// Slow down just enough that we don't outpace the JVM's ability to
			// detect newly freed references.  This might run smoother with
			// one of the alternate JVM GC strategies.
			try { Thread.sleep(10); } catch (InterruptedException e) {}
			System.out.println("Mem: "+(rt.totalMemory()-rt.freeMemory()));
		}
	}

	private static class Element {
		@SuppressWarnings("unused")
		private byte[] payload;
		private int id;
		public Element(int id) {
			this.id = id;
			this.payload = new byte[10*1024*1024];
		}
	}
}
