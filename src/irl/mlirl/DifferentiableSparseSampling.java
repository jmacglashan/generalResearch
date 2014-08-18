package irl.mlirl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.QValue;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling;
import burlap.behavior.singleagent.planning.stochastic.sparsesampling.SparseSampling.HashedHeightState;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.datastructures.BoltzmannDistribution;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;

public class DifferentiableSparseSampling extends OOMDPPlanner implements
		QGradientPlanner {

	/**
	 * The height of the tree
	 */
	protected int h;
	
	/**
	 * The number of transition dynamics samples (for the root if depth-variable C is used)
	 */
	protected int c;
	
	/**
	 * Whether the number of transition dyanmic samples should scale with the depth of the node. Default is false.
	 */
	protected boolean useVariableC = false;
	
	/**
	 * Whether previous planning results should be forgetten or reused; default is reused (false).
	 */
	protected boolean forgetPreviousPlanResults = false;
	
	/**
	 * The state value used for leaf nodes; default is zero.
	 */
	protected ValueFunctionInitialization vinit = new ValueFunctionInitialization.ConstantValueFunctionInitialization();
	
	
	/**
	 * The tree nodes indexed by state and height.
	 */
	protected Map<HashedHeightState, DiffStateNode> nodesByHeight;
	
	/**
	 * The root state node Q-values that have been estimated by previous planning calls.
	 */
	protected Map<StateHashTuple, QAndQGradient> rootLevelQValues;
	
	
	
	protected double										boltzBeta;
	
	protected int rfDim;
	
	
	/**
	 * The total number of pseudo-Bellman updates
	 */
	protected int numUpdates = 0;
	
	
	
	
	public DifferentiableSparseSampling(Domain domain, DifferentiableRF rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, int h, int c, double boltzBeta){
		this.plannerInit(domain, rf, tf, gamma, hashingFactory);
		this.h = h;
		this.c = c;
		this.boltzBeta = boltzBeta;
		this.nodesByHeight = new HashMap<SparseSampling.HashedHeightState, DifferentiableSparseSampling.DiffStateNode>();
		this.rootLevelQValues = new HashMap<StateHashTuple, DifferentiableSparseSampling.QAndQGradient>();
		this.rfDim = rf.getParameterDimension();
		
		this.debugCode = 6368290;
	}
	
	
	/**
	 * Sets whether the number of state transition samples (C) should be variable with respect to the depth of the node. If set
	 * to true, then the samples will be defined using C_i = C_0 * gamma^(2i), where i is the depth of the node from the root, gamma is the discount factor
	 * and C_0 is the normal C value set for this object.
	 * @param useVariableC if true, then depth-variable C will be used; if false, all state nodes use the same number of samples.
	 */
	public void setUseVariableCSize(boolean useVariableC){
		this.useVariableC = useVariableC;
	}
	
	
	/**
	 * Sets the number of state transition samples used.
	 * @param c the number of state transition samples used.
	 */
	public void setC(int c){
		this.c = c;
	}
	
	/**
	 * Sets the height of the tree.
	 * @param h the height of the tree.
	 */
	public void setH(int h){
		this.h = h;
	}
	
	/**
	 * Returns the number of state transition samples
	 * @return teh number of state transition samples
	 */
	public int getC(){
		return this.c;
	}
	
	/**
	 * Returns the height of the tree
	 * @return the height of the tree
	 */
	public int getH(){
		return this.h;
	}
	
	
	/**
	 * Sets whether previous planning results should be forgetten or resued in subsequent planning. Forgetting results is more memory efficient, but less
	 * CPU efficient.
	 * @param forgetPreviousPlanResults if true, then previous planning results will be forgotten; if true, they will be remembered and reused in susbequent planning.
	 */
	public void setForgetPreviousPlanResults(boolean forgetPreviousPlanResults){
		this.forgetPreviousPlanResults = forgetPreviousPlanResults;
		if(this.forgetPreviousPlanResults){
			this.nodesByHeight.clear();
		}
	}
	
	/**
	 * Sets the {@link ValueFunctionInitialization} object to use for settting the value of leaf nodes.
	 * @param vinit the {@link ValueFunctionInitialization} object to use for settting the value of leaf nodes.
	 */
	public void setValueForLeafNodes(ValueFunctionInitialization vinit){
		this.vinit = vinit;
	}
	
	/**
	 * Returns the debug code used for logging plan results with {@link DPrint}.
	 * @return the debug code used for logging plan results with {@link DPrint}.
	 */
	public int getDebugCode(){
		return this.debugCode;
	}
	

	/**
	 * Sets the debug code used for logging plan results with {@link DPrint}.
	 * @param debugCode the debugCode to use.
	 */
	public void setDebugCode(int debugCode){
		this.debugCode = debugCode;
	}
	
	/**
	 * Returns the total number of state value estimates performed since the {@link #resetPlannerResults()} call.
	 * @return the total number of state value estimates performed since the {@link #resetPlannerResults()} call.
	 */
	public int getNumberOfValueEsitmates(){
		return this.numUpdates;
	}
	
	
	@Override
	public List<QValue> getQs(State s) {
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradient qvs = this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = this.rootLevelQValues.get(sh);
		}
		
		return qvs.qs;
	}

	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradient qvs = this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = this.rootLevelQValues.get(sh);
		}
		
		if(a.params.length > 0 && !this.domain.isObjectIdentifierDependent() && a.parametersAreObjects()){
			StateHashTuple storedSh = this.mapToStateIndex.get(sh);
			a = a.translateParameters(s, storedSh.s);
		}
		
		for(QValue qv : qvs.qs){
			if(qv.a.equals(a)){
				return qv;
			}
		}
		
		return null;
	}

	@Override
	public List<QGradientTuple> getAllQGradients(State s) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradient qvs = this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = this.rootLevelQValues.get(sh);
		}
		return qvs.qGrads;
	}

	@Override
	public QGradientTuple getQGradient(State s, GroundedAction a) {
		StateHashTuple sh = this.hashingFactory.hashState(s);
		QAndQGradient qvs = this.rootLevelQValues.get(sh);
		if(qvs == null){
			this.planFromState(s);
			qvs = this.rootLevelQValues.get(sh);
		}
		
		if(a.params.length > 0 && !this.domain.isObjectIdentifierDependent() && a.parametersAreObjects()){
			StateHashTuple storedSh = this.mapToStateIndex.get(sh);
			a = (GroundedAction)a.translateParameters(s, storedSh.s);
		}
		
		for(QGradientTuple qg : qvs.qGrads){
			if(qg.a.equals(a)){
				return qg;
			}
		}
		
		return null;
	}

	@Override
	public void planFromState(State initialState) {
		
		if(this.forgetPreviousPlanResults){
			this.rootLevelQValues.clear();
		}
		
		StateHashTuple sh = this.hashingFactory.hashState(initialState);
		if(this.rootLevelQValues.containsKey(sh)){
			return; //already planned for this state
		}
		
		DPrint.cl(this.debugCode, "Beginning Planning.");
		int oldUpdates = this.numUpdates;
		
		DiffStateNode sn = this.getStateNode(initialState, this.h);
		rootLevelQValues.put(sh, sn.estimateQs());
		
		DPrint.cl(this.debugCode, "Finished Planning with " + (this.numUpdates - oldUpdates) + " value esitmates; for a cumulative total of: " + this.numUpdates);
		
		if(this.forgetPreviousPlanResults){
			this.nodesByHeight.clear();
		}
		
		this.mapToStateIndex.put(sh, sh);

	}

	@Override
	public void resetPlannerResults() {
		this.nodesByHeight.clear();
		this.rootLevelQValues.clear();
		this.numUpdates = 0;
	}
	
	
	
	/**
	 * Returns the value of C for a node at the given height (height from a leaf node).
	 * @param height the height from a leaf node.
	 * @return the value of C to use.
	 */
	protected int getCAtHeight(int height){
		if(!this.useVariableC){
			return c;
		}
		
		//convert height from bottom to depth from root
		int d = this.h = height;
		int vc = (int) (c * Math.pow(this.gamma, 2*d));
		if(vc == 0){
			vc = 1;
		}
		return vc;
	}
	
	/**
	 * Either returns, or creates, indexes, and returns, the state node for the given state at the given height in the tree
	 * @param s the state
	 * @param height the height (distance from leaf node) of the node.
	 * @return the state node for the given state at the given height in the tree
	 */
	protected DiffStateNode getStateNode(State s, int height){
		StateHashTuple sh = this.hashingFactory.hashState(s);
		HashedHeightState hhs = new HashedHeightState(sh, height);
		DiffStateNode sn = this.nodesByHeight.get(hhs);
		if(sn == null){
			sn = new DiffStateNode(sh, height);
			this.nodesByHeight.put(hhs, sn);
		}
		
		return sn;
	}
	
	
	
	
	/**
	 * A class for value differentiable state nodes. Includes the state, a value estimate, whether the node has been closed and methods for estimating the Q and V values.
	 * @author James MacGlashan
	 *
	 */
	public class DiffStateNode{
		
		/**
		 * The hashed state this node represents
		 */
		StateHashTuple sh;
		
		/**
		 * The height of the node (distance from a leaf)
		 */
		int height;
		
		/**
		 * The estimated value of the state at this height
		 */
		double v;
		
		/**
		 * The gradient of the value function
		 */
		double [] vgrad;
		
		
		/**
		 * Whether this node has been closed.
		 */
		boolean closed = false;
		
		
		public DiffStateNode(StateHashTuple sh, int height){
			this.sh = sh;
			this.height = height;
		}
		
		
		public QAndQGradient estimateQs(){
			
			int dim = DifferentiableSparseSampling.this.rfDim;
			
			List<GroundedAction> gas = DifferentiableSparseSampling.this.getAllGroundedActions(this.sh.s);
			QAndQGradient qs = new QAndQGradient(gas.size());
			
			int c = DifferentiableSparseSampling.this.getCAtHeight(this.height);
			for(GroundedAction ga : gas){
				if(this.height == 0 || c < 1){
					qs.add(new QValue(this.sh.s, ga, DifferentiableSparseSampling.this.vinit.value(this.sh.s)), new QGradientTuple(this.sh.s, ga, new double[dim]));
				}
				else{
					
					double [] qGradient = new double[dim];
					
					//generate C samples
					double sum = 0.;
					for(int i = 0; i < c; i++){
						
						//execute
						State ns = ga.executeIn(this.sh.s);
						double r = DifferentiableSparseSampling.this.rf.reward(this.sh.s, ga, ns);
						double [] rGradient = ((DifferentiableRF)DifferentiableSparseSampling.this.rf).getGradient(this.sh.s, ga, ns);
						
						DiffStateNode nsn = DifferentiableSparseSampling.this.getStateNode(ns, this.height-1);
						
						VAndVGradient vVals = nsn.estimateV();
						sum += r + DifferentiableSparseSampling.this.gamma*vVals.v;
						for(int f = 0; f < rGradient.length; f++){
							qGradient[f] += rGradient[f] + DifferentiableSparseSampling.this.gamma*vVals.vGrad[f];
						}
					}
					sum /= (double)c;
					for(int f = 0; f < qGradient.length; f++){
						qGradient[f] /= (double)c;
					}
					
					qs.add(new QValue(this.sh.s, ga, sum), new QGradientTuple(this.sh.s, ga, qGradient));
					
				}
				
				
			}
			
			
			return qs;
			
		}
		
		public VAndVGradient estimateV(){
			
			if(this.closed){
				return new VAndVGradient(this.v, this.vgrad);
			}
			
			if(DifferentiableSparseSampling.this.tf.isTerminal(this.sh.s)){
				this.v = 0.;
				this.vgrad = new double[DifferentiableSparseSampling.this.rfDim];
				this.closed = true;
				return new VAndVGradient(this.v, this.vgrad);
			}
			
			QAndQGradient qs = this.estimateQs();
			this.setV(qs);
			this.setVGrad(qs);
			this.closed = true;
			DifferentiableSparseSampling.this.numUpdates++;
			
			return new VAndVGradient(this.v, this.vgrad);
		}
		
		
		protected void setV(QAndQGradient qvs){
			double [] qArray = new double[qvs.qs.size()];
			for(int i = 0; i < qvs.qs.size(); i++){
				qArray[i] = qvs.qs.get(i).q;
			}
			BoltzmannDistribution bd = new BoltzmannDistribution(qArray, 1./DifferentiableSparseSampling.this.boltzBeta);
			double [] probs = bd.getProbabilities();
			double sum = 0.;
			for(int i = 0; i < qArray.length; i++){
				sum += qArray[i] * probs[i];
			}
			this.v = sum;
		}
		
		protected void setVGrad(QAndQGradient qvs){
			
			this.vgrad = new double[DifferentiableSparseSampling.this.rfDim];
			int d = vgrad.length;
			
			//pack qs into double array
			double [] qs = new double[qvs.qs.size()];
			for(int i = 0; i < qs.length; i++){
				qs[i] = qvs.qs.get(i).q;
			}
			
			//get all q gradients
			double [][] gqs = new double[qs.length][d];
			for(int i = 0; i < qs.length; i++){
				double [] gq = qvs.qGrads.get(i).gradient;
				for(int j = 0; j < d; j++){
					gqs[i][j] = gq[j];
				}
			}
			
			double maxBetaScaled = BoltzmannPolicyGradient.maxBetaScaled(qs, DifferentiableSparseSampling.this.boltzBeta);
			double logSum = BoltzmannPolicyGradient.logSum(qs, maxBetaScaled, DifferentiableSparseSampling.this.boltzBeta);
			
			for(int i = 0; i < qs.length; i++){
				
				double probA = Math.exp(DifferentiableSparseSampling.this.boltzBeta * qs[i] - logSum);
				double [] policyGradient = BoltzmannPolicyGradient.computePolicyGradient((DifferentiableRF)DifferentiableSparseSampling.this.rf, 
						DifferentiableSparseSampling.this.boltzBeta, qs, maxBetaScaled, logSum, gqs, i);
				
				for(int j = 0; j < d; j++){
					this.vgrad[j] += (probA * gqs[i][j]) + qs[i] * policyGradient[j];
				}
				
			}
			
		}
		
		
	}
	
	
	
	
	
	protected static class QAndQGradient{
		List<QValue> qs;
		List<QGradientTuple> qGrads;
		
		public QAndQGradient(List<QValue> qs, List<QGradientTuple> qGrads){
			this.qs = qs;
			this.qGrads = qGrads;
		}
		
		public QAndQGradient(int capacity){
			this.qs = new ArrayList<QValue>(capacity);
			this.qGrads = new ArrayList<QGradientTuple>(capacity);
		}
		
		public void add(QValue q, QGradientTuple qGrad){
			this.qs.add(q);
			this.qGrads.add(qGrad);
		}
	}
	
	
	protected static class VAndVGradient{
		
		double v;
		double [] vGrad;
		
		public VAndVGradient(double v, double [] vGrad){
			this.v = v;
			this.vGrad = vGrad;
		}
		
	}
	
	

}
