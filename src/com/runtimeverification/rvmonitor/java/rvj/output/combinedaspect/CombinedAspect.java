package com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect;

import com.runtimeverification.rvmonitor.util.RVMException;
import com.runtimeverification.rvmonitor.java.rvj.Main;
import com.runtimeverification.rvmonitor.java.rvj.output.EnableSet;
import com.runtimeverification.rvmonitor.java.rvj.output.RVMVariable;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.event.EventManager;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.indexingtree.IndexingDecl;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.indexingtree.IndexingTree;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.indexingtree.IndexingTreeManager;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedaspect.indexingtree.reftree.RefTree;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.BaseMonitor;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.Monitor;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.SuffixMonitor;
import com.runtimeverification.rvmonitor.java.rvj.output.monitorset.MonitorSet;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.RVMSpecFile;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.mopspec.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombinedAspect {
	String name;
	public HashMap<RVMonitorSpec, MonitorSet> monitorSets;
	public HashMap<RVMonitorSpec, SuffixMonitor> monitors;
	public HashMap<RVMonitorSpec, EnableSet> enableSets;
	public HashMap<RVMonitorSpec, HashSet<RVMParameter>> setOfParametersForDisable;

	RVMVariable mapManager;
	boolean versionedStack;

	List<RVMonitorSpec> specs;
	public RVMonitorStatManager statManager;
	public LockManager lockManager;
	public TimestampManager timestampManager;
	public ActivatorManager activatorsManager;
	public IndexingTreeManager indexingTreeManager;
	public EventManager eventManager;
	private final OptionManager optionManager;

	boolean has__ACTIVITY = false;

	public CombinedAspect(String name, RVMSpecFile rvmSpecFile, HashMap<RVMonitorSpec, MonitorSet> monitorSets, HashMap<RVMonitorSpec, SuffixMonitor> monitors,
			HashMap<RVMonitorSpec, EnableSet> enableSets, boolean versionedStack) throws RVMException {
		this.name = name + "RuntimeMonitor";
		this.monitorSets = monitorSets;
		this.monitors = monitors;
		this.enableSets = enableSets;
		this.versionedStack = versionedStack;

		this.specs = rvmSpecFile.getSpecs();
		for (RVMonitorSpec spec : specs) {
			if (spec.has__ACTIVITY()) has__ACTIVITY = true;
		}
		this.statManager = new RVMonitorStatManager(name, this.specs);
		this.lockManager = new LockManager(name, this.specs);
		this.timestampManager = new TimestampManager(name, this.specs);
		this.activatorsManager = new ActivatorManager(name, this.specs);
		this.indexingTreeManager = new IndexingTreeManager(name, this.specs, this.monitorSets, this.monitors, this.enableSets);

		collectDisableParameters(rvmSpecFile.getSpecs());

		this.eventManager = new EventManager(name, this.specs, this);

		this.mapManager = new RVMVariable(name + "MapManager");
		
		this.optionManager = new OptionManager();
	}
	
	public void collectDisableParameters(List<RVMonitorSpec> specs){
		this.setOfParametersForDisable = new HashMap<RVMonitorSpec, HashSet<RVMParameter>>();
		for(RVMonitorSpec spec : specs){
			HashSet<RVMParameter> parametersForDisable = new HashSet<RVMParameter>();
			
			for(EventDefinition event : spec.getEvents()){
				RVMParameters eventParams = event.getRVMParametersOnSpec();
				RVMParameterSet enable = enableSets.get(spec).getEnable(event.getId());
				
				for (RVMParameters enableEntity : enable) {
					if (enableEntity.size() == 0 && !spec.hasNoParamEvent())
						continue;
					if (enableEntity.contains(eventParams))
						continue;
					
					RVMParameters unionOfEnableEntityAndParam = RVMParameters.unionSet(enableEntity, eventParams);
					
					for (RVMParameter p : unionOfEnableEntityAndParam){
						if(!enableEntity.contains(p)){
							parametersForDisable.add(p);
						}
					}
				}
			}
			
			this.setOfParametersForDisable.put(spec, parametersForDisable);
		}
	}

	public String getAspectName() {
		return name;
	}
	
	public String constructor(){
		String ret = "";
		
		HashMap<String, RefTree> refTrees = indexingTreeManager.refTrees;
		
		for(RVMonitorSpec spec : specs){
			IndexingDecl indexDecl = indexingTreeManager.getIndexingDecl(spec);
			
			for(IndexingTree indexTree : indexDecl.getIndexingTrees().values()){
				RVMParameters param = indexTree.queryParam;
				
				if(param.size() == 0)
					continue;
				
				RefTree refTree = refTrees.get(param.get(0).getType().toString());
				
				if(refTree.hostIndexingTree != indexTree)
					ret += refTree.getName() + ".addCleaningChain(" + indexTree.getName() + ");\n";
			}
			
		}
		
		return ret;
	}

	public String initCache(){
		String ret = "";
		
		for(RVMonitorSpec spec : specs){
			IndexingDecl decl = indexingTreeManager.getIndexingDecl(spec);
		
			for(IndexingTree tree : decl.getIndexingTrees().values()){
				if(tree.cache != null){
					ret += tree.cache.init();
				}
			}
		}
		
		
		return ret;
	}


	public String categoryVarsDecl() {
		boolean skipEvent = false;
		Set<RVMVariable> categoryVars = new HashSet<RVMVariable>();
		for (RVMonitorSpec mopSpec : this.specs) {
			if (mopSpec.has__SKIP()) {
				skipEvent = true;
			}
			MonitorSet monitorSet =  monitorSets.get(mopSpec);
			Monitor monitorClass = monitors.get(mopSpec);
			categoryVars.addAll(monitorSet.getCategoryVars());
			categoryVars.addAll(monitorClass.getCategoryVars());
		}
		String ret = "";
		for (RVMVariable variable : categoryVars) {
			ret += "public static boolean " +
					BaseMonitor.getNiceVariable(variable) + " = " +
					"false;\n";
		}
		if (skipEvent) {
			ret += "public static boolean " + BaseMonitor.skipEvent + " = false;" +
					"\n";
		}
		return ret;
	}


	public String toString() {
		String ret = "";

		ret += this.statManager.statClass();
		
		ret += "public class " + this.name + " implements com.runtimeverification.rvmonitor.java.rt.RVMObject {\n";

		ret += categoryVarsDecl();

		ret += "private static com.runtimeverification.rvmonitor.java.rt.map.RVMMapManager " + mapManager + ";\n";

		ret += this.statManager.fieldDecl2();

		// constructor
		ret += "static {\n";
		
		ret += this.optionManager.printCode();

		ret += this.eventManager.printConstructor();
		
		ret += mapManager + " = " + "new com.runtimeverification.rvmonitor.java.rt.map.RVMMapManager();\n";
		ret += mapManager + ".start();\n";

		ret += this.statManager.constructor();
		
		//ret += constructor();
		//ret += initCache();
		
		ret += "}\n";
		ret += "\n";

		ret += this.statManager.fieldDecl();

		ret += this.lockManager.decl();

		ret += this.timestampManager.decl();

		ret += this.activatorsManager.decl();

		ret += this.indexingTreeManager.decl();

		ret += this.eventManager.advices();

		if (this.has__ACTIVITY) {
			ret += "public static void onCreateActivity(Activity a) {\n";
			for (Monitor m : monitors.values()) {
				if (m.has__ACTIVITY()) {
					ret += m.getOutermostName() + "." + m.getActivityName() + " = a;\n";
				}
			}
			ret += "}\n";
			ret += "\n";
		}

		ret += this.statManager.advice();

		if(Main.dacapo2){
			ret += "// after () : (execution(* avrora.Main.main(..)) || call(* dacapo.Benchmark.run(..)) || call(* org.dacapo.harness.Benchmark.run(..))) {\n";

			ret += "public static void resetDaCapo() {\n";

			//ret += "System.err.println(\"reset \" + Thread.currentThread().getName());\n";
			
			ret += this.timestampManager.reset();

			ret += this.activatorsManager.reset();

			ret += this.indexingTreeManager.reset();
			
			ret += "}\n";
		}

		ret += "}\n";

		return ret;
	}
}

class OptionManager {
	public String printCode() {
		String ret = "";
		ret += "com.runtimeverification.rvmonitor.java.rt.RuntimeOption.enableFineGrainedLock(";
		ret += Main.useFineGrainedLock ? "true" : "false";
		ret += ");\n";
		return ret;
	}
}