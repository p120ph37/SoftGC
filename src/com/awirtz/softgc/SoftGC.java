package com.awirtz.softgc;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
/**
 * SoftGC is an application-level GC thread for the JVM.  It uses a
 * soft-referenced buffer as a trip-wire to detect memory exhaustion, and
 * delegates an event to an application-supplied object to request that memory
 * be freed.  This allows the application to apply its own logic as to how to
 * prioritize the freeing of objects.  The application need not free sufficient
 * memory in a single pass - if the amount freed is insufficient, the event
 * will be fired again almost immediately.  It is, however, more efficient to
 * try to free memory in as few passes as possible.
 *
 */
public class SoftGC {
	private int bufferSize;
	private Free free;
	private Thread thread;
	private boolean run = true;

	/**
	 * Constructs a new SoftGC object and starts a thread to handle out-of-memory events.
	 * This thread will continue to run in the background even if the reference to this object
	 * becomes unreachable.  If you want to fully destroy this object and thread, call the
	 * destroy method.
	 * @param free        An object implementing the Free interface which will receive callbacks
	 *                    when memory needs to be freed.
	 * @param bufferSize  The size of each buffer in bytes.  This should be larger than your largest
	 *                    anticipated single heap object.  Two buffers of this size will be allocated.
	 */
	public SoftGC(Free free, int bufferSize) {
		this.free = free;
		this.bufferSize = bufferSize;
		final SoftGC that = this;
		thread = new Thread() {
			public void run() {
				// Allocate initial hard buffer
				byte[] hardBuffer = new byte[that.bufferSize];
				// Allocate initial soft buffer
				ReferenceQueue<byte[]> queue = new ReferenceQueue<byte[]>();
				@SuppressWarnings("unused") // ref is used only for the GC effects, so quit complaining!
				SoftReference<byte[]> ref = new SoftReference<byte[]>(new byte[that.bufferSize], queue);
				while(run) {
					try {
						// Block until the soft buffer gets garbage collected and shows up on the queue
						queue.remove();
						// Fire the "need more memory" event.
						that.free.free();
						// Soften the hard buffer
						ref = new SoftReference<byte[]>(hardBuffer, queue);
						hardBuffer = null;
						// Allocate a replacement hard buffer
						hardBuffer = new byte[that.bufferSize];
					} catch (InterruptedException e) {}
				}
			}
		};
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

	/**
	 *
	 * @return The running SoftGC thread.
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 *
	 * @return The object currently assigned to handle memory requests.
	 */
	public Free getFree() {
		return free;
	}

	/**
	 *
	 * @param free The new object to assign to handle memory requests.
	 */
	public void setFree(Free free) {
		this.free = free;
	}
	
	/**
	 * Destroy the object and the running thread.
	 */
	public void destroy() {
		run = false;
		this.thread.interrupt();
	}
}
