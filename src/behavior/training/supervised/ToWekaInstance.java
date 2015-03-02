package behavior.training.supervised;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.vfa.StateToFeatureVectorGenerator;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class ToWekaInstance {

	public static Instance genInstance(State s, StateToFeatureVectorGenerator fvgen, List <Action> actions){
		double [] fv = fvgen.generateFeatureVectorFrom(s);
		double [] labeled = new double[fv.length+1];
		for(int i = 0; i < fv.length; i++){
			labeled[i] = fv[i];
		}
		Instance inst = new Instance(1., labeled);

		Instances dataset = getInstancesShell(s, fvgen, actions, 1);
		dataset.add(inst);
		inst.setDataset(dataset);

		return inst;
	}

	public static Instance genInstance(State s, StateToFeatureVectorGenerator fvgen, GroundedAction actionLabel){
		List<Action> actions = actionLabel.action.getDomain().getActions();
		return genInstance(s, fvgen, actionLabel, actions);

	}

	public static Instance genInstance(State s, StateToFeatureVectorGenerator fvgen, GroundedAction actionLabel, List <Action> actions){
		double [] fv = fvgen.generateFeatureVectorFrom(s);
		double [] labeled = new double[fv.length+1];
		for(int i = 0; i < fv.length; i++){
			labeled[i] = fv[i];
		}

		//get class label
		int ind = 0;
		for(int i = 0; i < actions.size(); i++){
			if(actionLabel.actionName().equals(actions.get(i).getName())){
				ind = i;
				break;
			}
		}
		labeled[fv.length] = (double)ind;

		Instance inst = new Instance(1., labeled);

		return inst;
	}


	public static Instances getTrainingDataset(List<EpisodeAnalysis> episodes, StateToFeatureVectorGenerator fvgen){

		//setup class attribute
		List <Action> actions = episodes.get(0).getAction(0).action.getDomain().getActions();
		return getTrainingDataset(episodes, fvgen, actions);


	}

	protected static Instances getInstancesShell(State s, StateToFeatureVectorGenerator fvgen, List<Action> actions, int capacity){
		double [] exfv = fvgen.generateFeatureVectorFrom(s);
		FastVector attInfo = new FastVector(exfv.length+1);
		for(int i = 0; i < exfv.length; i++){
			Attribute att = new Attribute("f"+i);
			attInfo.addElement(att);
		}

		//setup class attribute
		FastVector actionLabels = new FastVector(actions.size());
		for(Action a : actions){
			actionLabels.addElement(a.getName());
		}
		Attribute classAtt = new Attribute("actionLabel", actionLabels);
		attInfo.addElement(classAtt);
		Instances dataset = new Instances("burlap_data", attInfo, capacity);
		dataset.setClassIndex(exfv.length);

		return dataset;
	}

	public static Instances getTrainingDataset(List<EpisodeAnalysis> episodes, StateToFeatureVectorGenerator fvgen, List<Action> actions){


		int totalDecisions = 0;
		for(EpisodeAnalysis ea : episodes){
			totalDecisions += ea.numTimeSteps()-1;
		}

		Instances dataset = getInstancesShell(episodes.get(0).getState(0), fvgen, actions, totalDecisions);

		//instantiate the values for the dataset
		for(EpisodeAnalysis ea : episodes){
			for(int i = 0; i < ea.numTimeSteps()-1; i++){
				Instance inst = genInstance(ea.getState(i),fvgen, ea.getAction(i));
				dataset.add(inst);
			}
		}

		return dataset;

	}

	public static Instances getTrainingDataset(EpisodeAnalysis episode, StateToFeatureVectorGenerator fvgen){
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>(1);
		episodes.add(episode);
		return getTrainingDataset(episodes, fvgen);
	}

	public static Instances getTrainingDataset(EpisodeAnalysis episode, StateToFeatureVectorGenerator fvgen, List<Action> actions){
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>(1);
		episodes.add(episode);
		return getTrainingDataset(episodes, fvgen, actions);
	}

}
