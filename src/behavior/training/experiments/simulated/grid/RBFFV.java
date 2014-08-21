package behavior.training.experiments.simulated.grid;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.behavior.singleagent.vfa.rbf.DistanceMetric;
import burlap.behavior.singleagent.vfa.rbf.RBF;
import burlap.behavior.singleagent.vfa.rbf.functions.GaussianRBF;
import burlap.oomdp.core.State;

public class RBFFV implements StateToFeatureVectorGenerator {

	List<RBF> rbfs = new ArrayList<RBF>();
	DistanceMetric stateMetric;
	
	public RBFFV(DistanceMetric metric){
		this.stateMetric = metric;
	}
	
	public void addRBF(State centeredState, double epsilon){
		this.rbfs.add(new GaussianRBF(centeredState, this.stateMetric, epsilon));
	}
	
	@Override
	public double[] generateFeatureVectorFrom(State s) {
		
		double [] fv = new double[this.rbfs.size()];
		for(int i = 0; i < this.rbfs.size(); i++){
			fv[i] = this.rbfs.get(i).responseFor(s);
		}
		
		return fv;
	}

}
