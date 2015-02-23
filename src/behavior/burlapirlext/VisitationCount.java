package behavior.burlapirlext;

import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.State;
import datastructures.HashedAggregator;

import java.util.Map;

/**
 * @author James MacGlashan.
 */
public interface VisitationCount {
	public double visitation(State s);
	public void addVisits(State s, double v);


	public static class TabularVisitationCount implements VisitationCount{

		StateHashFactory hashingFactory;
		HashedAggregator<StateHashTuple> visitations;

		public TabularVisitationCount(StateHashFactory hashingFactory){
			this.hashingFactory = hashingFactory;
			this.visitations = new HashedAggregator<StateHashTuple>(0.);
		}

		@Override
		public double visitation(State s) {
			return this.visitations.v(this.hashingFactory.hashState(s));
		}

		@Override
		public void addVisits(State s, double v) {
			this.visitations.add(this.hashingFactory.hashState(s), v);
		}
	}

}
