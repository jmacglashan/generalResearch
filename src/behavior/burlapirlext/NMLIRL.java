package behavior.burlapirlext;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnbydemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.BoltzmannPolicyGradient;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.QGradientPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import org.apache.commons.math3.linear.*;

import java.util.*;

/**
 * @author James MacGlashan.
 */
public class NMLIRL extends MLIRL {

	public NMLIRL(MLIRLRequest request, double learningRate, double maxLikelihoodChange, int maxSteps) {
		super(request, learningRate, maxLikelihoodChange, maxSteps);
	}






	/**
	 * Computes and returns the natural gradient of the log-likelihood of all trajectories
	 * @return the gradient of the log-likelihood of all trajectories
	 */
	@Override
	public double [] logLikelihoodGradient(){


		double [] gradient = new double[this.request.getRf().getParameterDimension()];
		double [] weights = this.request.getEpisodeWeights();
		List<EpisodeAnalysis> exampleTrajectories = this.request.getExpertEpisodes();


		StateHashFactory hashingFactory = this.request.getPlanner().getHashingFactory();
		Set<StateHashTuple> coveredStates = new HashSet<StateHashTuple>();
		List <FisherActionData> fisherData = new ArrayList<FisherActionData>();
		List <GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(this.request.getDomain().getActions(), exampleTrajectories.get(0).getState(0));
		for(GroundedAction ga : gas){
			if(ga.isParameterized() && ga.parametersAreObjects()){
				throw new RuntimeException("Natural MLIRL currently does not support obejct parameterized actions.");
			}
			fisherData.add(new FisherActionData(ga, gradient.length));
		}

		Policy p = new BoltzmannQPolicy((QComputablePlanner)this.request.getPlanner(), 1./this.request.getBoltzmannBeta());



		for(int i = 0; i < exampleTrajectories.size(); i++){
			EpisodeAnalysis ea = exampleTrajectories.get(i);
			double weight = weights[i];
			for(int t = 0; t < ea.numTimeSteps()-1; t++){
				this.request.getPlanner().planFromState(ea.getState(t));
				double [] logPolicyGrad = this.logPolicyGrad(ea.getState(t), ea.getAction(t));
				//weigh it by trajectory strength
				for(int j = 0; j < logPolicyGrad.length; j++){
					logPolicyGrad[j] *= weight;
				}
				this.addToVector(gradient,logPolicyGrad);

				//handle fisher stuff if this is a new state
				StateHashTuple sh = hashingFactory.hashState(ea.getState(t));
				if(!coveredStates.contains(sh)) {
					coveredStates.add(sh);
					for(FisherActionData fad : fisherData) {
						fad.actionProb += p.getProbOfAction(sh.s, fad.a);
						this.addToVector(fad.logActionProbGradient, this.policyGrad(sh.s, fad.a));
					}
				}

			}
		}


		//now create fisher information matrix
		RealMatrix fisher = MatrixUtils.createRealMatrix(gradient.length, gradient.length);


		for(FisherActionData fad : fisherData){

			//first finish normalizing the sub data

			fad.actionProb /= coveredStates.size();
			//gradient is normalized by state prob and then log gradient normalizes by the prob of the action
			for(int i = 0; i < fad.logActionProbGradient.length; i++){
				fad.logActionProbGradient[i] = fad.logActionProbGradient[i] / (coveredStates.size() * fad.actionProb);
			}

			//now turn into real vector
			RealVector logGrad = MatrixUtils.createRealVector(fad.logActionProbGradient);

			//create action matrix
			RealMatrix actionMatrix = logGrad.outerProduct(logGrad);
			//scale by action prob
			actionMatrix.scalarMultiply(fad.actionProb);

			//add it to the fisher matrix
			fisher = fisher.add(actionMatrix);


		}

		//LUDecomposition lu = new LUDecomposition(fisher);
		SingularValueDecomposition svd = new SingularValueDecomposition(fisher);

		/*
		if(!lu.getSolver().isNonSingular()){
			System.out.println("Using vanilla...");
			for(int i = 0; i < fisher.getRowDimension(); i++){
				System.out.println(Arrays.toString(fisher.getRow(i)));
			}
			return gradient;
		}
		*/

		//RealMatrix inverseFisher = lu.getSolver().getInverse();
		RealMatrix inverseFisher = svd.getSolver().getInverse();
		RealMatrix vanillaGrad = MatrixUtils.createColumnRealMatrix(gradient);
		RealMatrix naturalGradientMatrix = inverseFisher.multiply(vanillaGrad);


		double [] naturalGradient = naturalGradientMatrix.getColumn(0);

		return naturalGradient;
	}



	protected double [] policyGrad(State s, GroundedAction ga){
		double [] gradient = BoltzmannPolicyGradient.computeBoltzmannPolicyGradient(s, ga, (QGradientPlanner) this.request.getPlanner(), this.request.getBoltzmannBeta());
		return gradient;
	}


	protected static class FisherActionData{
		public GroundedAction a;
		double actionProb;
		double [] logActionProbGradient;


		public FisherActionData(GroundedAction a, int dim){
			this.a = a;
			this.actionProb = 0.;
			this.logActionProbGradient = new double[dim];
		}
	}

}
