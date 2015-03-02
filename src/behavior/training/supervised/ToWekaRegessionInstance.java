package behavior.training.supervised;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.RewardFunction;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public class ToWekaRegessionInstance {


	public static Instance getInstance(State s, StateToFeatureVectorGenerator fvgen, double discountedReturn, Instances dataset){

		double [] fv = fvgen.generateFeatureVectorFrom(s);
		double [] labeled = new double[fv.length+1];
		for(int i = 0; i < fv.length; i++){
			labeled[i] = fv[i];
		}
		labeled[fv.length] = discountedReturn;
		Instance inst = new Instance(1., labeled);

		boolean knownData = dataset != null;

		if(dataset == null){
			dataset = getInstancesShell(s, fvgen, 1);
		}

		dataset.add(inst);
		inst.setDataset(dataset);

		if(knownData){
			//System.out.println(inst.toString());
			//System.out.println("label: " + discountedReturn);
		}


		return inst;

	}

	public static Instances getTrainingInstances(List<EpisodeAnalysis> episodes, StateToFeatureVectorGenerator fvgen, RewardFunction evaluatingRF, double discount){

		int totalDecisions = 0;
		for(EpisodeAnalysis ea : episodes){
			totalDecisions += ea.numTimeSteps()-1;
		}

		Instances dataset = getInstancesShell(episodes.get(0).getState(0), fvgen, totalDecisions);

		for(EpisodeAnalysis ea : episodes){

			double totalReturn = getDiscountedReturn(ea, evaluatingRF, discount);
			for(int i = 0; i < ea.numTimeSteps()-1; i++){

				//System.out.println(i + ": " + totalReturn);

				//create weka instance
				getInstance(ea.getState(i), fvgen, totalReturn, dataset);

				//shift return down for next state
				double r = evaluatingRF.reward(ea.getState(i), ea.getAction(i), ea.getState(i+1));
				totalReturn -= r;
				totalReturn /= discount;



			}

		}

		return dataset;

	}


	protected static Instances getInstancesShell(State s, StateToFeatureVectorGenerator fvgen, int capacity){
		double [] exfv = fvgen.generateFeatureVectorFrom(s);
		FastVector attInfo = new FastVector(exfv.length+1);
		for(int i = 0; i < exfv.length; i++){
			Attribute att = new Attribute("f"+i);
			attInfo.addElement(att);
		}

		Attribute classAtt = new Attribute("discountedReturn");
		attInfo.addElement(classAtt);

		Instances dataset = new Instances("burlap_data", attInfo, capacity);
		dataset.setClassIndex(exfv.length);

		return dataset;
	}


	protected static double getDiscountedReturn(EpisodeAnalysis ea, RewardFunction rf, double discount){

		double totalReward = 0.;
		double nd = 1.;
		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			double r = rf.reward(ea.getState(i), ea.getAction(i), ea.getState(i+1));
			double inc = nd*r;
			totalReward += inc;
			nd *= discount;
		}

		return totalReward;

	}

}
