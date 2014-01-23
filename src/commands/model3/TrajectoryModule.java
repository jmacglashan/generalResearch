package commands.model3;

import generativemodel.GMModule;
import generativemodel.GMQuery;
import generativemodel.GMQueryResult;
import generativemodel.ModelTrackedVarIterator;
import generativemodel.RVariable;
import generativemodel.RVariableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import behavior.irl.TabularIRL;
import behavior.irl.TabularIRL.TaskCondition;
import behavior.irl.TabularIRLPlannerFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;

import commands.data.Trajectory;
import commands.model3.TaskModule.ConjunctiveGroundedPropRF;
import commands.model3.TaskModule.RFConVariableValue;

public class TrajectoryModule extends GMModule{

	public static final String								TNAME = "trajectory";
	
	protected RVariable										stateRV;
	protected RVariable										rewardRV;
	protected RVariable										behaviorRV;
	
	protected Domain										oomdpDomain;
	protected TabularIRL									irl;
	
	protected boolean										useIRLCache;
	protected Map<GMQuery, Double>							cachedResults;
	protected Map<GMQuery, Double>							cachedLogResults;
	
	
	public TrajectoryModule(String name, RVariable stateRV, RVariable rewardRV, Domain oomdpDomain, TabularIRLPlannerFactory irlPlanFactory, boolean addTerminateAction, boolean useCache){
		
		super(name);
		
		this.stateRV = stateRV;
		this.rewardRV = rewardRV;
		
		this.externalDependencyList.add(stateRV);
		this.externalDependencyList.add(rewardRV);
		
		
		behaviorRV = new RVariable(TNAME, this);
		behaviorRV.addDependency(rewardRV);
		behaviorRV.addDependency(stateRV);
		
		
		this.oomdpDomain = oomdpDomain;
		this.useIRLCache = useCache;
		
		this.irl = new TabularIRL(oomdpDomain, irlPlanFactory, addTerminateAction);
		this.irl.setTemperature(0.005);
		
		
		cachedResults = new HashMap<GMQuery, Double>();
		cachedLogResults = new HashMap<GMQuery, Double>();
		
	}


	@Override
	public GMQueryResult computeProb(GMQuery query) {
		
		TrajectoryValue bval = (TrajectoryValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);
		
		double p;
		
		Double cachedP = this.cachedResults.get(query);
		if(cachedP != null){
			p = cachedP;
		}
		else{
			ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rval.rf);
			TaskCondition tc = new TaskCondition(rval.rf, tf);
			
			
			p = irl.getBehaviorProbability(bval.t.convertToZeroRewardEpisodeAnalysis(), tc);
			
			if(this.useIRLCache){
				this.cachedResults.put(query, p);
			}
			
			//System.out.println("t_" + rval.toString() + ": " + p);
			
			
		}
		
		
		
		return new GMQueryResult(query, p);
	}
	
	@Override
	public GMQueryResult getLogProb(GMQuery query){
		
		GMQueryResult cachedResult = owner.getCachedLoggedResultForQuery(query);
		if(cachedResult != null){
			return cachedResult;
		}
		
		return this.computeLogProb(query);
	}
	

	public GMQueryResult computeLogProb(GMQuery query){
		
		TrajectoryValue bval = (TrajectoryValue)query.getSingleQueryVar();
		
		Set <RVariableValue> conditions = query.getConditionValues();
		RFConVariableValue rval = (RFConVariableValue)this.extractValueForVariableFromConditions(rewardRV, conditions);
		
		double p;
		
		Double cachedP = this.cachedLogResults.get(query);
		if(cachedP != null){
			p = cachedP;
		}
		else{
			ConjunctiveGroundedPropTF tf = new ConjunctiveGroundedPropTF(rval.rf);
			TaskCondition tc = new TaskCondition(rval.rf, tf);
			
			p = irl.getBehaviorLogProbability(bval.t.convertToZeroRewardEpisodeAnalysis(), tc);
			
			if(this.useIRLCache){
				this.cachedLogResults.put(query, p);
			}
			
			//System.out.println(rval.toString() + ": " + p);
			
			
		}
		
		
		
		return new GMQueryResult(query, p);
		
	}


	@Override
	public ModelTrackedVarIterator getNonZeroProbIterator(RVariable queryVar,
			List<RVariableValue> conditions) {
		
		//there are a ton of different possible behaviors; so we're not implementing a method
		//to iterate them.
						
		throw new UnsupportedOperationException();
		
	}


	@Override
	public Iterator<RVariableValue> getRVariableValuesFor(RVariable queryVar) {
		//there are a ton of different possible behaviors; so we're not implementing a method
		//to iterate them.
						
		throw new UnsupportedOperationException();
	}
	
	
	
	
	
	
	
	
	
	
	
	public static class TrajectoryValue extends RVariableValue{

		public Trajectory		t;
		
		public TrajectoryValue(Trajectory t, RVariable owner){
			
			this.t = t;
			
			this.setOwner(owner);
		}
		
		@Override
		public boolean valueEquals(RVariableValue other) {
			
			if(this == other){
				return true;
			}
			
			if(!(other instanceof TrajectoryValue)){
				return false;
			}
			
			TrajectoryValue that = (TrajectoryValue)other;
			
			if(this.t.numStates() != that.t.numStates()){
				return false;
			}
			
			for(int i = 0; i < this.t.numStates(); i++){
				State s = this.t.getState(i);
				State ts = that.t.getState(i);
				if(!s.equals(ts)){
					return false;
				}
				
				if(i < this.t.numStates()-1){
					GroundedAction ga = this.t.getAction(i);
					GroundedAction tga = that.t.getAction(i);
					
					if(!ga.equals(tga)){
						return false;
					}
					
				}
				
			}
			
			
			return true;
		}

		@Override
		public String stringRep() {
			
			StringBuffer buf = new StringBuffer();
			
			for(int i = 0; i < this.t.numStates(); i++){
				buf.append(t.getState(i).getCompleteStateDescription()).append("\n");
				if(i < this.t.numStates()-1){
					buf.append(t.getAction(i).toString()).append("\n");
				}
			}
			
			
			return buf.toString();
		}
		
		
		
		
	}
	
	
	
	
	public static class ConjunctiveGroundedPropTF implements TerminalFunction{
		
		public List <GroundedProp>			gps;

		public ConjunctiveGroundedPropTF(List <GroundedProp> gps){
			this.gps = gps;
		}
		
		public ConjunctiveGroundedPropTF(ConjunctiveGroundedPropRF rf){
			this.gps = new ArrayList<GroundedProp>(rf.gps);
		}
		
		
		@Override
		public boolean isTerminal(State s) {
			for(GroundedProp gp : gps){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			
			return true;
		}
		
	}
	
	

}
