package ethics.experiments.fssimple.aux;

import java.util.HashMap;
import java.util.Map;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import ethics.ParameterizedRF;
import ethics.ParameterizedRFFactory;

public class FSSubjectiveRF implements ParameterizedRF{

	protected JointReward		objectiveRewardFunction;
	protected double [] 		params; //0 is steal bias 1 is punish bias
	
	
	public FSSubjectiveRF(JointReward objectiveRewardFunction){
		this.objectiveRewardFunction = objectiveRewardFunction;
	}
	
	@Override
	public Map<String, Double> reward(State s, JointAction ja, State sp) {
		
		Map<String, Double> r = new HashMap<String, Double>();
		Map<String, Double> or = this.objectiveRewardFunction.reward(s, ja, sp);
		
		for(GroundedSingleAction gsa : ja){
			double nr = or.get(gsa.actingAgent);
			if(gsa.action.actionName.equals(FSSimple.ACTIONSTEAL)){
				nr += this.params[0];
			}
			else if(gsa.action.actionName.equals(FSSimple.ACTIONPUNISH)){
				nr += this.params[1];
			}
			r.put(gsa.actingAgent, nr);
		}
		
		return r;
	}

	@Override
	public void setParameters(double[] params) {
		this.params = params;
	}

	@Override
	public int parameterSize() {
		return 2;
	}

	@Override
	public double[] getParameters() {
		return this.params;
	}

	@Override
	public void printParameters() {
		System.out.println(this.toString());
	}
	
	@Override
	public String toString(){
		return "Steal bias:  " + params[0] + "\nPunish bias: " + params[1];
	}
	
	
	
	public static class FSSubjectiveRFFactory implements ParameterizedRFFactory{

		public JointReward objectiveRF;
		
		
		public FSSubjectiveRFFactory(JointReward objectiveRF){
			this.objectiveRF = objectiveRF;
		}
		
		@Override
		public ParameterizedRF generateRF(double[] params) {
		
			FSSubjectiveRF rf = new FSSubjectiveRF(objectiveRF);
			rf.setParameters(params);
			
			return rf;
		}

		@Override
		public int parameterSize() {
			return 2;
		}
		
		
		
	}

}
