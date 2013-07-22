package com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect;

import com.runtimeverification.rvmonitor.java.rvj.Main;
import com.runtimeverification.rvmonitor.java.rvj.output.RVMVariable;

/**
 * Previously, one single global lock was used in a coarse manner; when an event
 * occurs, any other event that occurs at the same time have been blocked. Since
 * this hinders concurrency, this global lock is disabled when fine-grained lock is
 * enabled. In such case, indexing trees and GWRTs need to be synchronized.
 */
public class GlobalLock {
	RVMVariable lock;

	public GlobalLock(RVMVariable lock) {
		this.lock = lock;
	}

	public RVMVariable getName(){
		return lock;
	}
	
	public String toString() {
		String ret = "";
		
		if (!Main.useFineGrainedLock) {
			ret += "static ReentrantLock " + lock + " = new ReentrantLock();\n";
			ret += "static Condition " + lock + "_cond = " + lock + ".newCondition();\n";
		}

		return ret;
	}

	public String getAcquireCode() {
		String ret = "";
		
		if (!Main.useFineGrainedLock) {
			ret += "while (!" + this.getName() + ".tryLock()) {\n";
			ret += "Thread.yield();\n";
			ret += "}\n";
		}
		
		return ret;
	}
	
	public String getReleaseCode() {
		String ret = "";
		
		if (!Main.useFineGrainedLock)
			ret += this.getName() + ".unlock();\n";
		
		return ret;
	}
}
