package behavior.vfa.heterogenousafd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.FeatureDatabase;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class HeterogenousActionFeatureDatabase implements FeatureDatabase {

	Map<String, FeatureDatabase>			featureGenerators;
	
	Map<String, Map<Integer, Integer>>		featureMapping;
	protected int							nextActionId = 0;
	
	public HeterogenousActionFeatureDatabase() {
		featureGenerators = new HashMap<String, FeatureDatabase>();
		featureMapping = new HashMap<String, Map<Integer,Integer>>();
	}
	
	public void addFDForAction(String actionName, FeatureDatabase fd){
		this.featureGenerators.put(actionName, fd);
		this.featureMapping.put(actionName, new HashMap<Integer, Integer>());
	}

	@Override
	public List<StateFeature> getStateFeatures(State s) {
		throw new UnsupportedOperationException();
	}
	
	public ValueFunctionApproximation getLinearVFA(double defaultWeight){
		return new LinearVFA(this, defaultWeight);
	}

	@Override
	public List<ActionFeaturesQuery> getActionFeaturesSets(State s, List<GroundedAction> actions) {
		List <ActionFeaturesQuery> result = new ArrayList<ActionFeaturesQuery>(actions.size());
		
		Map <String, List<GroundedAction>> groundedActionsByAction = this.groundedActionsByAction(actions);
		
		for(Map.Entry<String, List<GroundedAction>> e : groundedActionsByAction.entrySet()){
			
			String actionName = e.getKey();
			
			Map<Integer, Integer> fmap = this.featureMapping.get(actionName);
			List<ActionFeaturesQuery> lowerQueries = featureGenerators.get(actionName).getActionFeaturesSets(s, e.getValue());
			for(ActionFeaturesQuery lowerAFQ : lowerQueries){
				
				ActionFeaturesQuery translatedAFQ = new ActionFeaturesQuery(lowerAFQ.queryAction);
				for(StateFeature sf : lowerAFQ.features){
					Integer mf = fmap.get(sf.id);
					if(mf == null){
						mf = nextActionId;
						fmap.put(sf.id, mf);
						nextActionId++;
					}
					translatedAFQ.addFeature(new StateFeature(mf, sf.value));
					
					
				}
				
				
				result.add(translatedAFQ);
				
			}
			
		}
		
		
		
		return result;
	}

	@Override
	public void freezeDatabaseState(boolean toggle) {
		for(FeatureDatabase fd : featureGenerators.values()){
			fd.freezeDatabaseState(toggle);
		}
	}

	
	
	
	
	protected Map<String, List<GroundedAction>> groundedActionsByAction(List <GroundedAction> gas){
		
		Map<String, List<GroundedAction>> result = new HashMap<String, List<GroundedAction>>();
		
		for(GroundedAction ga : gas){
			List<GroundedAction> set = result.get(ga.action.getName());
			if(set == null){
				set = new ArrayList<GroundedAction>();
				result.put(ga.action.getName(), set);
			}
			set.add(ga);
		}
		
		return result;
		
	}
	
	
	
	
}
