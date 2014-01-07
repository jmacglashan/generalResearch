package behavior.learning.modellearning;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;


/**
 * In model learning, it is not uncommon to have a modeled domain object with its own actions that are distinct from the actual action objects in the world on which
 * planning is performed to generate a policy for the modeled domain which will be followed in the real world. However, since the policy of a modeled domain references
 * modeled actions, instead of the real actions, the model policy needs to be mapped back into the actions of the real world. This class will take the real world domain
 * and the policy of the model domain and map its results into the actions of the real world domain.
 * @author James MacGlashan
 *
 */
public class DomainMappedPolicy extends Policy {

	
	protected Domain			realWorldDomain;
	protected Policy			modelPolicy;
	
	
	/**
	 * Initializes.
	 * @param realWorldDomain the domain to which actions in the source policy should be mapped
	 * @param modelPolicy the source policy that selects actions for a different domain object
	 */
	public DomainMappedPolicy(Domain realWorldDomain, Policy modelPolicy){
		this.realWorldDomain = realWorldDomain;
		this.modelPolicy = modelPolicy;
	}
	
	@Override
	public GroundedAction getAction(State s) {
		return this.mapAction(this.modelPolicy.getAction(s));
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		List <ActionProb> aps = this.modelPolicy.getActionDistributionForState(s);
		List <ActionProb> mapped = new ArrayList<Policy.ActionProb>(aps.size());
		for(ActionProb ap : aps){
			mapped.add(new ActionProb(this.mapAction(ap.ga), ap.pSelection));
		}
		
		return aps;
	}

	@Override
	public boolean isStochastic() {
		return this.modelPolicy.isStochastic();
	}

	@Override
	public boolean isDefinedFor(State s) {
		return this.modelPolicy.isDefinedFor(s);
	}
	
	
	/**
	 * Maps an input GroundedAction to a GroundedAction using an action reference of the action in this object's {@link realWorldDomain} object that has the same name as the action in the input GroundedAction.
	 * @param ga the input GroundedAction to map.
	 * @return a GroundedAction whose action reference belongs to the Action with the same name in this object's {@link realWorldDomain} object 
	 */
	protected GroundedAction mapAction(GroundedAction ga){
		return new GroundedAction(realWorldDomain.getAction(ga.actionName()), ga.params);
	}

}
