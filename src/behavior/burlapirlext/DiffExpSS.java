package behavior.burlapirlext;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.DifferentiableRF;
import burlap.behavior.singleagent.learnbydemo.mlirl.support.QGradientTuple;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.datastructures.BoltzmannDistribution;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class DiffExpSS extends DifferentiableSparseSampling{

	protected RewardFunction exploreShaping;
	protected boolean returnUnshapedQs = true;

	public DiffExpSS(Domain domain, DifferentiableRF rf, RewardFunction exploreShaping, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, int h, int c, double boltzBeta) {
		super(domain, rf, tf, gamma, hashingFactory, h, c, boltzBeta);
		this.exploreShaping = exploreShaping;
	}


	protected void setReturnUnshapedQs(boolean returnUnshapedQs){
		this.returnUnshapedQs = returnUnshapedQs;
	}

	public boolean isReturnUnshapedQs() {
		return returnUnshapedQs;
	}

	@Override
	public List<QValue> getQs(State s) {

		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradientExp qvs = (QAndQGradientExp)this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = (QAndQGradientExp)this.rootLevelQValues.get(sh);
		}

		if(this.returnUnshapedQs) {
			return qvs.qs;
		}
		else{
			return qvs.expQs;
		}
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {

		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradientExp qvs = (QAndQGradientExp)this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = (QAndQGradientExp)this.rootLevelQValues.get(sh);
		}

		if(a.params.length > 0 && !this.domain.isObjectIdentifierDependent() && a.parametersAreObjects()){
			StateHashTuple storedSh = this.mapToStateIndex.get(sh);
			a = a.translateParameters(s, storedSh.s);
		}

		List <QValue> qs = qvs.qs;
		if(!this.returnUnshapedQs){
			qs = qvs.expQs;
		}

		for(QValue qv : qs){
			if(qv.a.equals(a)){
				return qv;
			}
		}

		return null;
	}

	@Override
	protected DiffStateNode getStateNode(State s, int height) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		SparseSampling.HashedHeightState hhs = new SparseSampling.HashedHeightState(sh, height);
		DiffStateNode sn = this.nodesByHeight.get(hhs);
		if(sn == null){
			sn = new DiffStateNodeExp(sh, height);
			this.nodesByHeight.put(hhs, sn);
		}

		return sn;
	}

	public class DiffStateNodeExp extends DifferentiableSparseSampling.DiffStateNode {

		double vExp;


		public DiffStateNodeExp(StateHashTuple sh, int height) {
			super(sh, height);
		}


		public QAndQGradient estimateQs() {

			int dim = DiffExpSS.this.rfDim;

			List<GroundedAction> gas = DiffExpSS.this.getAllGroundedActions(this.sh.s);
			QAndQGradientExp qs = new QAndQGradientExp(gas.size());

			int c = DiffExpSS.this.getCAtHeight(this.height);
			for (GroundedAction ga : gas) {
				if (this.height == 0 || c == 0) {
					double vinit = DiffExpSS.this.vinit.value(this.sh.s);
					qs.add(new QValue(this.sh.s, ga, vinit), new QValue(this.sh.s, ga, vinit), new QGradientTuple(this.sh.s, ga, new double[dim]));
				} else {

					if (c > 0) {
						this.sampledBellmanQEstimate(ga, qs);
					} else {
						this.fulldBellmanQEstimate(ga, qs);
					}

				}


			}


			return qs;

		}


		public void sampledBellmanQEstimate(GroundedAction ga, QAndQGradient qs){

			QAndQGradientExp expTyped = (QAndQGradientExp)qs;

			int dim = DiffExpSS.this.rfDim;
			double [] qGradient = new double[dim];

			//generate C samples
			double sum = 0.;
			double expSum = 0.;
			for(int i = 0; i < c; i++){

				//execute
				State ns = ga.executeIn(this.sh.s);
				double r = DiffExpSS.this.rf.reward(this.sh.s, ga, ns);
				double sr = r + DiffExpSS.this.exploreShaping.reward(this.sh.s, ga, ns);
				double [] rGradient = ((DifferentiableRF)DiffExpSS.this.rf).getGradient(this.sh.s, ga, ns);

				DiffStateNode nsn = DiffExpSS.this.getStateNode(ns, this.height-1);

				VAndVGradientExp vVals = (VAndVGradientExp)nsn.estimateV();
				sum += r + DiffExpSS.this.gamma*vVals.v;
				expSum += sr + DiffExpSS.this.gamma*vVals.vExp;
				for(int f = 0; f < rGradient.length; f++){
					qGradient[f] += rGradient[f] + DiffExpSS.this.gamma*vVals.vGrad[f];
				}
			}
			sum /= (double)c;
			expSum /= (double)c;
			for(int f = 0; f < qGradient.length; f++){
				qGradient[f] /= (double)c;
			}

			expTyped.add(new QValue(this.sh.s, ga, sum), new QValue(this.sh.s, ga, expSum), new QGradientTuple(this.sh.s, ga, qGradient));


		}


		public void fulldBellmanQEstimate(GroundedAction ga, QAndQGradient qs){

			QAndQGradientExp expTyped = (QAndQGradientExp)qs;

			int dim = DiffExpSS.this.rfDim;
			double [] qGradient = new double[dim];

			double sum = 0.;
			double expSum = 0.;
			List<TransitionProbability> tps = ga.action.getTransitions(this.sh.s, ga.params);
			for(TransitionProbability tp : tps){

				State ns = tp.s;
				double r = DiffExpSS.this.rf.reward(this.sh.s, ga, ns);
				double sr = r + DiffExpSS.this.exploreShaping.reward(this.sh.s, ga, ns);
				double [] rGradient = ((DifferentiableRF)DiffExpSS.this.rf).getGradient(this.sh.s, ga, ns);

				DiffStateNode nsn = DiffExpSS.this.getStateNode(ns, this.height-1);

				VAndVGradientExp vVals = (VAndVGradientExp)nsn.estimateV();
				sum += tp.p * (r + DiffExpSS.this.gamma*vVals.v);
				expSum += tp.p * (sr + DiffExpSS.this.gamma*vVals.vExp);
				for(int f = 0; f < rGradient.length; f++){
					qGradient[f] += tp.p * (rGradient[f] + DiffExpSS.this.gamma*vVals.vGrad[f]);
				}

			}

			expTyped.add(new QValue(this.sh.s, ga, sum), new QValue(this.sh.s, ga, expSum), new QGradientTuple(this.sh.s, ga, qGradient));

		}


		public VAndVGradient estimateV(){

			if(this.closed){
				return new VAndVGradientExp(this.v, this.vExp, this.vgrad);
			}

			if(DiffExpSS.this.tf.isTerminal(this.sh.s)){
				this.v = 0.;
				this.vExp = 0.;
				this.vgrad = new double[DiffExpSS.this.rfDim];
				this.closed = true;
				return new VAndVGradientExp(this.v, this.vExp, this.vgrad);
			}

			QAndQGradientExp qs = (QAndQGradientExp)this.estimateQs();
			this.setV(qs);
			this.setVGrad(qs);
			this.closed = true;
			DiffExpSS.this.numUpdates++;

			return new VAndVGradientExp(this.v, this.vExp, this.vgrad);
		}


		protected void setV(QAndQGradient qvs){

			QAndQGradientExp expTyped = (QAndQGradientExp)qvs;

			double [] qArray = new double[qvs.qs.size()];
			double [] qExpArray = new double[expTyped.expQs.size()];
			for(int i = 0; i < qvs.qs.size(); i++){
				qArray[i] = qvs.qs.get(i).q;
				qExpArray[i] = expTyped.expQs.get(i).q;
			}
			BoltzmannDistribution bd = new BoltzmannDistribution(qArray, 1./DiffExpSS.this.boltzBeta);
			BoltzmannDistribution bdExp = new BoltzmannDistribution(qExpArray, 1./DiffExpSS.this.boltzBeta);
			double [] probs = bd.getProbabilities();
			double [] expProbs = bdExp.getProbabilities();
			double sum = 0.;
			double expSum = 0.;
			for(int i = 0; i < qArray.length; i++){
				sum += qArray[i] * probs[i];
				expSum += qExpArray[i] * expProbs[i];
			}
			this.v = sum;
			this.vExp = expSum;
		}


	}




	protected static class QAndQGradientExp extends QAndQGradient{

		List <QValue> expQs;

		public QAndQGradientExp(List<QValue> qs, List <QValue> expQs, List<QGradientTuple> qGrads){
			super(qs, qGrads);
			this.expQs = expQs;
		}

		public QAndQGradientExp(int capacity){
			super(capacity);
			this.expQs = new ArrayList<QValue>(capacity);
		}

		public void add(QValue q, QValue qExp, QGradientTuple qGrad){
			super.add(q, qGrad);
			this.expQs.add(qExp);
		}
	}


	protected static class VAndVGradientExp extends VAndVGradient{

		double vExp;

		public VAndVGradientExp(double v, double vExp, double [] vGrad){
			super(v, vGrad);
			this.vExp = vExp;
		}

	}
}
