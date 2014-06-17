package ethics.experiments.tbforagesteal.auxiliary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import domain.stocasticgames.foragesteal.TBForageSteal;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import ethics.ParameterizedRF;

public class TBFSSRFWS4Param implements ParameterizedRF {

	protected JointReward			objectiveRewardFunction;
	
	//0: steal (alpha); 1: punch for he stole (gamma); 2: punch for he stole and punched (theta); 3: punch for I started it (beta)
	protected double []				params;
	
	
	public TBFSSRFWS4Param(JointReward objectiveRewardFunction){
		this.objectiveRewardFunction = objectiveRewardFunction;
		this.params = new double[4];
	}
	
	public TBFSSRFWS4Param(JointReward objectiveRewardFunction, double [] params){
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
		
			List <ObjectInstance> agentObs = s.getObjectsOfTrueClass(TBForageSteal.CLASSAGENT);
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
		
		double a1sr = a1or + this.getSubjectiveBias(s, a1Ob, ja.action(a1name).action.actionName);
		double a2sr = 0.;
		if(agents.size() > 1){
			a2sr = a2or + this.getSubjectiveBias(s, a2Ob, ja.action(a2name).action.actionName);
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
		return 4;
	}

	@Override
	public double[] getParameters() {
		return this.params.clone();
	}
	
	@Override
	public String toString(){
		StringBuffer sbuf = new StringBuffer(256);
		sbuf.append("Steal bias:                     ").append(params[0]).append("\n");
		sbuf.append("Punch for he stole:             ").append(params[1]).append("\n");
		sbuf.append("Punch for he stole and punched: ").append(params[2]).append("\n");
		sbuf.append("Punch for I started it :        ").append(params[2]);
		
		return sbuf.toString();
	}

	@Override
	public void printParameters() {
		System.out.println(this.toString());
	}

	
	
	protected double getSubjectiveBias(State s, ObjectInstance actingAgent, String actionName){
		
		if(actionName.equals(TBForageSteal.ACTIONSTEAL)){
			return this.params[0];
		}
		else if(actionName.equals(TBForageSteal.ACTIONPUNCH)){
			
			int actPN = actingAgent.getDiscValForAttribute(TBForageSteal.ATTPN);
			int actSA = actingAgent.getDiscValForAttribute(TBForageSteal.ATTPTA);
			if(actPN == 0){
				
				
				if(actSA == 3){
					return this.params[3];
				}
				else if(actSA == 0){
					return this.params[1];
				}
				else{
					return this.params[2];
				}
				
			}
			else{
				
				if(actSA == 4){
					return this.params[3];
				}
				else if(actSA == 0){
					return this.params[1];
				}
				else{
					return this.params[2];
				}
				
			}
			
			
		}
		
		return 0.;
	}
	
	
}
