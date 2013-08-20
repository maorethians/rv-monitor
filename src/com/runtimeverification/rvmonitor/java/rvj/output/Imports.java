package com.runtimeverification.rvmonitor.java.rvj.output;

import com.runtimeverification.rvmonitor.java.rvj.Main;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.ImportDeclaration;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.RVMSpecFile;

import java.util.ArrayList;

public class Imports {
	// Hope this is cleaned up. I think the followings should be put on demand, conditionally.
	private ArrayList<String> imports;
	private final String[] required = {"java.util.concurrent.*", "java.util.concurrent.locks.*", "java.util.*", "java.lang.ref.*",
		"com.runtimeverification.rvmonitor.java.rt.*",
		"com.runtimeverification.rvmonitor.java.rt.ref.*",
		"com.runtimeverification.rvmonitor.java.rt.table.*",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.IBucketNode",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.TableAdopter.Tuple2",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.TableAdopter.Tuple3",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.IDisableHolder",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.IMonitor",
		"com.runtimeverification.rvmonitor.java.rt.tablebase.DisableHolder",
	};
	
	private final String[] observer = {
		"com.runtimeverification.rvmonitor.java.rt.observable.IInternalBehaviorObserver",
		"com.runtimeverification.rvmonitor.java.rt.observable.IInternalBehaviorObserver.LookupPurpose",
		"com.runtimeverification.rvmonitor.java.rt.observable.IObservable",
		"com.runtimeverification.rvmonitor.java.rt.observable.IObservableObject",
		"com.runtimeverification.rvmonitor.java.rt.observable.InternalBehaviorMultiplexer",
	};

	public Imports(RVMSpecFile rvmSpecFile) {
		imports = new ArrayList<String>();

		for (ImportDeclaration imp : rvmSpecFile.getImports()) {
			String n = "";
			if (imp.isStatic())
				n += "static ";
			n += imp.getName().toString().trim(); 
			if (imp.isAsterisk())
				n += ".*";

			if(!imports.contains(n))
				imports.add(n);
		}
		
		this.addImports(this.required);
		
		if (Main.internalBehaviorObserving)
			this.addImports(this.observer);
		
		if (Main.useFineGrainedLock)
			imports.add("java.util.concurrent.atomic.AtomicBoolean");
	}
	
	private void addImports(String[] array) {
		for (String s : array) {
			if (!this.imports.contains(s))
				this.imports.add(s);
		}
	}

	public String toString() {
		String ret = "";

		for (String imp : imports)
			ret += "import " + imp + ";\n";

		return ret;
	}

}
