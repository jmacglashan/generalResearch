package ethics.experiments.tbforagesteal.evaluators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import optimization.OptVariables;
import optimization.VarEvaluaiton;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.stocashticgames.AgentFactory;
import burlap.oomdp.stocashticgames.AgentType;
import burlap.oomdp.stocashticgames.JointReward;
import burlap.oomdp.stocashticgames.SGDomain;
import burlap.oomdp.stocashticgames.WorldGenerator;
import burlap.oomdp.stocashticgames.common.AgentFactoryWithSubjectiveReward;
import burlap.oomdp.stocashticgames.tournament.MatchSelector;
import burlap.oomdp.stocashticgames.tournament.Tournament;
import burlap.oomdp.stocashticgames.tournament.common.AllPairWiseSameTypeMS;
import burlap.oomdp.stocashticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.TBFSAlternatingTurnSG;
import domain.stocasticgames.foragesteal.TBFSStandardMechanics;
import domain.stocasticgames.foragesteal.TBForageSteal;
import domain.stocasticgames.foragesteal.TBForageStealFAbstraction;
import ethics.ParameterizedRFFactory;

public class LazyRoundRobinEval implements VarEvaluaiton {

	protected WorldGenerator			worldGenerator;
	protected JointReward				objectiveReward;
	protected ParameterizedRFFactory	rewardFactory;

	protected AgentFactory				baseFactory;
	
	protected AgentType					fsAgentType;
	
	protected int						nTries;
	protected int						nGames;
	
	Map <String, DoublePair>			cachedPerformance;
	
	
	public LazyRoundRobinEval(int nTries, int nGames, ParameterizedRFFactory rewardFactory, JointReward objectiveReward, double learningRate) {
		
		this.nTries =  nTries;
		this.nGames = nGames;
		this.rewardFactory = rewardFactory;
		this.objectiveReward = objectiveReward;
		
		SGDomain domain = (SGDomain) TBForageSteal.generateDomain();
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(TBForageSteal.CLASSAGENT, domain.getObjectClass(TBForageSteal.CLASSAGENT).attributeList);
		
		double discount = 0.99;
		
		baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory, new TBForageStealFAbstraction());
		
		worldGenerator = new ConstantWorldGenerator(domain, new TBFSStandardMechanics(), objectiveReward, 
				new SinglePFTF(domain.getPropFunction(TBForageSteal.PFGAMEOVER)), new TBFSAlternatingTurnSG());
		
		fsAgentType = new AgentType("default", domain.getObjectClass(TBForageSteal.CLASSAGENT), domain.getSingleActions());
		
		
		
		cachedPerformance = new HashMap<String, DoublePair>();
		
	}

	@Override
	public List<Double> evaluate(List<OptVariables> instances) {
		
		List <Double> sumRR = new ArrayList<Double>(instances.size());
		for(int i = 0; i < instances.size(); i++){
			sumRR.add(0.);
		}
		
		for(int i = 0; i < instances.size(); i++){
			OptVariables v1 = instances.get(i);
			for(int j = i+1; j < instances.size(); j++){
				OptVariables v2 = instances.get(j);
				DoublePair dp = this.getMatchPerformance(v1, v2);
				sumRR.set(i, sumRR.get(i)+dp.a);
				sumRR.set(j, sumRR.get(j)+dp.b);
			}
		}
		
		
		return sumRR;
	}
	
	
	protected DoublePair getMatchPerformance(OptVariables v1, OptVariables v2){
		
		String v1v2 = v1.toString() + v2.toString();
		String v2v1 = v2.toString() + v1.toString();
		
		DoublePair res = cachedPerformance.get(v1v2);
		if(res != null){
			return res;
		}
		
		//otherwise we'll need to run the round robin
		double sumv1 = 0.;
		double sumv2 = 0.;
		for(int i = 0; i < nTries; i++){
			DoublePair dp = this.runRoundRobin(v1, v2);
			sumv1 += dp.a;
			sumv2 += dp.b;
		}
		
		if(!v1v2.equals(v2v1)){
			res = new DoublePair(sumv1/nTries, sumv2/nTries);
		}
		else{
			res = new DoublePair(sumv1/nTries, sumv1/nTries); //if the params are the same, do not penalize one side for noise
		}
		DoublePair resR = res.reverse();
		
		cachedPerformance.put(v1v2, res);
		cachedPerformance.put(v2v1, resR);
		
		
		return res;
	}
	
	
	protected DoublePair runRoundRobin(OptVariables v1, OptVariables v2){
		
		
		MatchSelector selector = new AllPairWiseSameTypeMS(fsAgentType, 2);
		Tournament tourn = new Tournament(1000, nGames, selector, worldGenerator);
		
		JointReward subjectiveRewardV1 = this.rewardFactory.generateRF(v1.vars);
		AgentFactory factV1 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV1);
		
		JointReward subjectiveRewardV2 = this.rewardFactory.generateRF(v2.vars);
		AgentFactory factV2 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV2);
		
		tourn.addAgent(factV1);
		tourn.addAgent(factV2);
		
		tourn.runTournament();
		
		DoublePair res = new DoublePair(tourn.getCumulativeRewardFor(0), tourn.getCumulativeRewardFor(1));
		
		return res;
		
	}
	
	
	
	class DoublePair{
		
		public double a;
		public double b;
		
		public DoublePair(double a, double b){
			this.a = a;
			this.b = b;
		}
		
		public DoublePair reverse(){
			return new DoublePair(this.b, this.a);
		}
		
	}

}
