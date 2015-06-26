package ethics.experiments.tbforagesteal.auxiliary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import domain.stocasticgames.foragesteal.TBForageSteal;
import ethics.ParameterizedRF;

public class TBFSSubjectiveRFWS implements ParameterizedRF {

	
	protected JointReward			objectiveRewardFunction;
	
	//0: steal; 1: punch for he started it; 2: punch for I started it
	protected double []				params;
	
	public TBFSSubjectiveRFWS(JointReward objectiveRewardFunction){
		this.objectiveRewardFunction = objectiveRewardFunction;
		this.params = new double[3];
	}
	
	public TBFSSubjectiveRFWS(JointReward objectiveRewardFunction, double [] params){
		this.objectiveRewardFunction = objectiveRewardFunction;
		this.params = params.clone();
	}

	@Override
	public Map<String, Double> reward(State s, JointAction ja, State sp) {
		
		List <String> agents = ja.getAgentNames();
		String a1name = agents.get(0);
		String a2name = null;
		
		if(agents.size() > 1){
			a2name = agents.get(1);
		}
		else{
		
			List <ObjectInstance> agentObs = s.getObjectsOfClass(TBForageSteal.CLASSAGENT);
			for(ObjectInstance aob : agentObs){
				if(!aob.getName().equals(a1name)){
					a2name = aob.getName();
					break;
				}
			}
		}
		
		ObjectInstance a1Ob = s.getObject(a1name);
		ObjectInstance a2Ob = s.getObject(a2name);
		
		Map <String, Double> orewards = objectiveRewardFunction.reward(s, ja, sp);
		
		double a1or = orewards.get(a1name);
		double a2or = 0.;
		if(agents.size() > 1){
			a2or = orewards.get(a2name);
		}
		
		int a1pn = a1Ob.getIntValForAttribute(TBForageSteal.ATTPN);
		int a2pn = a2Ob.getIntValForAttribute(TBForageSteal.ATTPN);
		
		int a1pa = a1Ob.getIntValForAttribute(TBForageSteal.ATTPTA);
		int a2pa = a2Ob.getIntValForAttribute(TBForageSteal.ATTPTA);
		
		double a1sr = a1or + this.subjectiveBias(ja.action(a1name).action.actionName, a1pn, a2pa);
		double a2sr = 0.;
		if(agents.size() > 1){
			a2sr = a2or + this.subjectiveBias(ja.action(a2name).action.actionName, a2pn, a1pa);
		}
		
		
		Map <String, Double> srewards = new HashMap<String, Double>();
		srewards.put(a1name, a1sr);
		srewards.put(a2name, a2sr);
		
		return srewards;
	}

	@Override
	public void setParameters(double[] params) {
		this.params = params.clone();
	}

	@Override
	public int parameterSize() {
		return 3;
	}

	@Override
	public double[] getParameters() {
		return params.clone();
	}

	@Override
	public void printParameters() {
		//System.out.println("Steal bias:           " + params[0]);
		//System.out.println("Punch for steal bias: " + params[1]);
		//System.out.println("Punch for punch bias: " + params[2]);

		System.out.println(this.toString());
		
	}
	
	@Override
	public String toString(){
		StringBuffer sbuf = new StringBuffer(256);
		sbuf.append("Steal bias:              ").append(params[0]).append("\n");
		sbuf.append("Punch for he started it: ").append(params[1]).append("\n");
		sbuf.append("Punch for I started it : ").append(params[2]);
		
		return sbuf.toString();
		
	}

	
	
	protected double subjectiveBias(String actionName, int actingPlayerNum, int previousOpponentAction){
		
		if(actionName.equals(TBForageSteal.ACTIONSTEAL)){
			return params[0];
		}
		else if(actionName.equals(TBForageSteal.ACTIONPUNCH)){
			if(previousOpponentAction == 2 || (actingPlayerNum == 0 && previousOpponentAction == 4) || (actingPlayerNum == 1 && previousOpponentAction == 3)){ //opponent started it
				return params[1];
			}
			else{ //acting player started it
				return params[2];
			}
		}
		
		return 0.;
	}
	
	protected int getPreviousTurnAction(String aname, State s){
		
		ObjectInstance o = s.getObject(aname);
		int pa = o.getIntValForAttribute(TBForageSteal.ATTPTA);
		
		return pa;
	}

}
