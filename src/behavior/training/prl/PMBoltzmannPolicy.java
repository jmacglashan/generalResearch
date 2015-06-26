package behavior.training.prl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

public class PMBoltzmannPolicy extends PMDirectPolicy {

	public PMBoltzmannPolicy(Domain domain, StateHashFactory hashingFactory,
			double learningRate) {
		super(domain, hashingFactory, learningRate);
		
	}

	
	
	
	@Override
	public void updateStatePreferences(State s, List<ActionProb> preferences) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode pn = this.preferences.get(sh);
		if(pn == null){
			pn = new PolicyNode(sh);
			this.preferences.put(sh, pn);
		}
		
		
		double curAlignedPreferenceValues [] = new double [preferences.size()];
		List <ActionPreference> alignedNodePrefs = new ArrayList<PMDirectPolicy.ActionPreference>();
		
		for(int i = 0; i < preferences.size(); i++){
			ActionProb aprob = preferences.get(i);
			ActionPreference ap = this.getMatchingPreference(sh, (GroundedAction)aprob.ga, pn);
			alignedNodePrefs.add(ap);
			curAlignedPreferenceValues[i] = ap.preference;
		}
		
		double [] predicted = this.predicated(curAlignedPreferenceValues);
		
		double [] targets = new double[preferences.size()];
		for(int i = 0; i < preferences.size(); i++){
			targets[i] = predicted[i] + this.learningRate * (preferences.get(i).pSelection - predicted[i]);
		}

		this.moveToTarget(curAlignedPreferenceValues, targets, 0.001);
		
		for(int i = 0; i < curAlignedPreferenceValues.length; i++){
			ActionPreference ap = alignedNodePrefs.get(i);
			ap.preference = curAlignedPreferenceValues[i];
		}
		
	}
	
	
	protected void moveToTarget(double [] prefs, double [] target, double minError){
		double [] predicted = this.predicated(prefs);
		double step = 0.5;
		do{
			double grad [] = this.grad(predicted);
			
			for(int i = 0; i < prefs.length; i++){
				double diff = target[i] - prefs[i];
				double change = step * 2 * diff * grad[i];
				prefs[i] += change;
			}
			predicted = this.predicated(prefs);
			
		}while(sse(predicted, target) < minError);
	}
	
	protected double sse(double [] predicted, double [] target){
		double sum = 0.;
		for(int i = 0; i < predicted.length; i++){
			double diff = predicted[i] - target[i];
			sum += diff*diff;
		}
		return sum;
	}
	
	protected double [] predicated(double [] preferences){
		
		double [] energies = new double [preferences.length];
		double [] predicated = new double[preferences.length];
		double sum = 0.;
		for(int i = 0; i < preferences.length; i++){
			double e = Math.exp(preferences[i]);
			energies[i] = e;
			sum += e;
		}
		for(int i = 0; i < energies.length; i++){
			predicated[i] = energies[i] / sum;
		}
		
		return predicated;
	}
	
	protected double [] grad(double [] predicted){
		
		double [] grad = new double[predicted.length];
		
		for(int i = 0; i < predicted.length; i++){
			grad[i] = predicted[i] * (1. - predicted[i]);
		}
		
		return grad;
	}
	
	
	@Override
	public void updateSinglePreferenceInDirection(State s, GroundedAction ga, double preferenceChangeDirection) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode pn = this.preferences.get(sh);
		if(pn == null){
			pn = new PolicyNode(sh);
			this.preferences.put(sh, pn);
		}
		ActionPreference ap = this.getMatchingPreference(sh, ga, pn);
		
		if(preferenceChangeDirection < 0.){
			ap.preference += this.learningRate * preferenceChangeDirection;
		}
		
		
		//not supported for positive or neutral single changes
		
	}
	
	
	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		List <GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.actions, s);
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		PolicyNode node = this.preferences.get(sh);
		if(node == null){
			node = new PolicyNode(sh);
			for(GroundedAction ga : gas){
				node.addPreference(new ActionPreference(ga, 0.0));
			}
			this.preferences.put(sh, node);
		}
		
		List <ActionProb> probs = new ArrayList<ActionProb>(gas.size());
		
		double sumEnergy = 0.;
		List <Double> energies = new ArrayList<Double>(gas.size());
		for(ActionPreference ap : node.preferences){
			double e = Math.exp(ap.preference);
			energies.add(e);
			sumEnergy += e;
			
		}
		for(int i = 0; i < energies.size(); i++){
			double e = energies.get(i);
			ActionPreference ap = node.preferences.get(i);
			double p = e/sumEnergy;
			probs.add(new ActionProb(ap.ga, p));
		}
		
		
		if(this.containsParameterizedActions){
			//then convert back to this states space
			Map <String, String> matching = node.sh.s.getObjectMatchingTo(s, false);
			
			List <ActionProb> translated = new ArrayList<ActionProb>(probs.size());
			for(ActionProb ap : probs){
				if(ap.ga.params.length == 0){
					translated.add(ap);
				}
				else{
					ActionProb tap = new ActionProb(this.translateAction((GroundedAction)ap.ga, matching), ap.pSelection);
					translated.add(tap);
				}
			}
			
			return translated;
			
		}
		
		
		return probs;
		
	}
	
}
