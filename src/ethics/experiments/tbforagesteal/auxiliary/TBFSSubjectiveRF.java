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

public class TBFSSubjectiveRF implements ParameterizedRF {

	
	protected JointReward			objectiveRewardFunction;
	
	//0: steal; 1: punch for steal; 2: punch for punch
	protected double []				params;
	
	public TBFSSubjectiveRF(JointReward objectiveRewardFunction){
		this.objectiveRewardFunction = objectiveRewardFunction;
		this.params = new double[3];
	}
	
	public TBFSSubjectiveRF(JointReward objectiveRewardFunction, double [] params){
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
		
	
		
		Map <String, Double> orewards = objectiveRewardFunction.reward(s, ja, sp);
		
		double a1or = orewards.get(a1name);
		double a2or = 0.;
		if(agents.size() > 1){
			a2or = orewards.get(a2name);
		}
		
		int a1pa = this.getPreviousTurnAction(a1name, s);
		int a2pa = this.getPreviousTurnAction(a2name, s);
		
		double a1sr = a1or + this.subjectiveBias(ja.action(a1name).action.actionName, a2pa);
		double a2sr = 0.;
		if(agents.size() > 1){
			a2sr = a2or + this.subjectiveBias(ja.action(a2name).action.actionName, a1pa);
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
		sbuf.append("Steal bias:           ").append(params[0]).append("\n");
		sbuf.append("Punch for steal bias: ").append(params[1]).append("\n");
		sbuf.append("Punch for punch bias: ").append(params[2]);
		
		return sbuf.toString();
		
	}

	
	
	protected double subjectiveBias(String actionName, int previousOpponentAction){
		
		if(actionName.equals(TBForageSteal.ACTIONSTEAL)){
			return params[0];
		}
		else if(actionName.equals(TBForageSteal.ACTIONPUNCH)){
			if(previousOpponentAction == 2){ //opponent stole in previous turn
				return params[1];
			}
			else if(previousOpponentAction == 3){ //opponent punched in previous turn
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
