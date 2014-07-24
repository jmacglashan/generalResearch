package behavior.vfa.heterogenousafd.stateenumerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.FeatureDatabase;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

public class EnumeratedFeatureDatabase implements FeatureDatabase{

	protected StateEnumerator						enumerator;
	Map <Integer, StoredActions>					actionFeatures;
	protected int									nextActionFeatureId = 0;
	

	public EnumeratedFeatureDatabase(StateEnumerator enumerator){
		this.enumerator = enumerator;
		this.actionFeatures = new HashMap<Integer, EnumeratedFeatureDatabase.StoredActions>();
	}
	
	
	@Override
	public List<StateFeature> getStateFeatures(State s) {
		List <StateFeature> result = new ArrayList<StateFeature>();
		result.add(new StateFeature(enumerator.getEnumeratedStateValue(s).enumeratedValue, 1.0));
		return result;
	}

	@Override
	public List<ActionFeaturesQuery> getActionFeaturesSets(State s, List<GroundedAction> actions) {
		
		EnumeratedState enumerated = enumerator.getEnumeratedStateValue(s);
		
		List <ActionFeaturesQuery> result = new ArrayList<ActionFeaturesQuery>(actions.size());
		for(GroundedAction ga : actions){
			ActionFeaturesQuery afq = new ActionFeaturesQuery(ga);
			result.add(afq);
		}
		
		StoredActions sa = actionFeatures.get(enumerated.enumeratedValue);
		if(sa == null){
			sa = new StoredActions(enumerated);
			for(ActionFeaturesQuery afq : result){
				sa.addFeature(new StoredActionFeature(afq.queryAction, nextActionFeatureId));
				afq.addFeature(new StateFeature(nextActionFeatureId, 1.0));
				nextActionFeatureId++;
			}
			actionFeatures.put(enumerated.enumeratedValue, sa);
			
		}
		else{
			for(ActionFeaturesQuery afq : result){
				StoredActionFeature saf = sa.getStoredActionFeatureFor(enumerated, afq.queryAction);
				if(saf == null){
					sa.addFeatureForQuery(enumerated, afq.queryAction, nextActionFeatureId);
					afq.addFeature(new StateFeature(nextActionFeatureId, 1.0));
					nextActionFeatureId++;
				}
				else{
					afq.addFeature(new StateFeature(saf.id, 1.0));
				}
			}
		}
		
		return result;
	}

	@Override
	public void freezeDatabaseState(boolean toggle) {
		//don't do anything
	}
	
	
	
	class StoredActions{
		EnumeratedState eState;
		List <StoredActionFeature> features;
		
		public StoredActions(EnumeratedState eState){
			this.eState = eState;
			this.features = new ArrayList<EnumeratedFeatureDatabase.StoredActionFeature>();
		}
		
		public void addFeature(StoredActionFeature saf){
			this.features.add(saf);
		}
		
		public void addFeatureForQuery(EnumeratedState es, GroundedAction queryAction, int featureId){
			if(queryAction.params.length > 0){
				throw new RuntimeErrorException(new Error("EnumeratedFeatureDatabase currently does not supported paramaterized Actions. Support will be added in a later version"));
			}
			this.features.add(new StoredActionFeature(queryAction, featureId));
		}
		
		public StoredActionFeature getStoredActionFeatureFor(EnumeratedState es, GroundedAction queryAction){
			if(queryAction.params.length > 0){
				throw new RuntimeErrorException(new Error("EnumeratedFeatureDatabase currently does not supported paramaterized Actions. Support will be added in a later version"));
			}
			for(StoredActionFeature saf : this.features){
				if(saf.srcGA.action.getName().equals(queryAction.action.getName())){
					return saf;
				}
			}
			return null;
		}
		
		
	}
	
	class StoredActionFeature{
		
		public GroundedAction			srcGA;
		public int						id;
		
		public StoredActionFeature(GroundedAction ga, int id){
			this.srcGA = ga;
			this.id = id;
		}
		
	}

	@Override
	public int numberOfFeatures() {
		return this.nextActionFeatureId;
	}
	

}
