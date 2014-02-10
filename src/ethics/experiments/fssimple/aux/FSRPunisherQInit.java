package ethics.experiments.fssimple.aux;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.DPrint;
import burlap.domain.singleagent.graphdefined.GraphDefinedDomain;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;

public class FSRPunisherQInit implements ValueFunctionInitialization {

	protected FSSubjectiveRF		subjectiveRF;
	protected FSSimpleJR			objectiveRF;
	protected QComputablePlanner 	planner;
	protected Domain				domain;
	
	
	public FSRPunisherQInit(FSSubjectiveRF subjectiveRF, FSSimpleJR objectiveRF) {
		
		this.subjectiveRF = subjectiveRF;
		this.objectiveRF = objectiveRF;
		
		this.domain = getDomainForSAPunisherPlayingAgainstAContingentStealer(0.1);
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				int spn = GraphDefinedDomain.getNodeId(sprime);
				if(spn == 2){
					return FSRPunisherQInit.this.objectiveRF.getStealeeReward();
				}
				
				int sn = GraphDefinedDomain.getNodeId(s);
				if(sn == 2 && spn == 1){
					return FSRPunisherQInit.this.subjectiveRF.params[1] + FSRPunisherQInit.this.objectiveRF.getPuncherReward(); //punish cost incurred when transitioning from my decision state to the punish state
				}
				
				return 0;
			}
		};
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		double discount = 0.95;
		planner = new ValueIteration(domain, rf, new NullTermination(), discount, hashingFactory, 0.0001, 1000);
		State s = GraphDefinedDomain.getState(domain, 0);
		
		DPrint.toggleCode(((ValueIteration)planner).getDebugCode(), false);
		
		((OOMDPPlanner)planner).planFromState(s);
		
	}
	
	@Override
	public double value(State s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double qValue(State s, AbstractGroundedAction a) {
		
		GroundedSingleAction gsa = (GroundedSingleAction)a;
		
		State singleAgentState = GraphDefinedDomain.getState(this.domain, s.getFirstObjectOfClass(FSSimple.CLASSSTATENODE).getDiscValForAttribute(FSSimple.ATTSTATENODE));
		GroundedAction ga = this.getGroundedActionMapping(gsa);
		return this.planner.getQ(singleAgentState, ga).q;
	}
	
	
	protected GroundedAction getGroundedActionMapping(GroundedSingleAction gsa){
		
		if(gsa.action.actionName.equals(FSSimple.ACTIONDONOTHING)){
			return new GroundedAction(this.domain.getAction("action0"), "");
		}
		else if(gsa.action.actionName.equals(FSSimple.ACTIONPUNISH)){
			return new GroundedAction(this.domain.getAction("action1"), "");
		}
		
		throw new RuntimeException("Could not find mapped action for: " + gsa.action.actionName);
	}
	
	public static Domain getDomainForSAPunisherPlayingAgainstAContingentStealer(double opponentError){
		GraphDefinedDomain gen = new GraphDefinedDomain(3);
		gen.setTransition(0, 0, 2, 1.-opponentError);
		gen.setTransition(0, 0, 0, opponentError);
		
		gen.setTransition(2, 0, 0, 1.); //action 0 is do nothing
		gen.setTransition(2, 1, 1, 1.); //action 1 is punish
		
		gen.setTransition(1, 0, 1, 1.-opponentError);
		gen.setTransition(1, 0, 2, opponentError);
		
		Domain domain = gen.generateDomain();
		
		return domain;
	}

	

}
