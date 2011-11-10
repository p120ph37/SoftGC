package com.awirtz.softgc;

/**
 * This interface is used by SoftGC to request more free memory when needed.
 * @author awirtz
 *
 */
public interface Free {
	/**
	 * Should free some memory and then return.
	 * 
	 */
	public abstract void free();
}
