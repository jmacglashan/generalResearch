package collabrl;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;

/**
 * @author James MacGlashan.
 */
public class TaskAndTrain {

	public Domain domain;
	public RewardFunction rf;
	public TerminalFunction tf;
	public double discount;
	public StateGenerator sg;


	public boolean isDiscrete;

	public String [] objectClassForRep;

	public double vMin;
	public double vMax;


	public int numTrainingSteps;


	public String description;



	public TaskAndTrain(Domain domain, RewardFunction rf, TerminalFunction tf, double discount, StateGenerator sg,
						double vMin, double rMax, int numTrainingSteps, boolean isDiscrete, String [] objectClassesForRep, String description){


		this.domain = domain;
		this.rf = rf;
		this.tf = tf;
		this.discount = discount;
		this.sg = sg;
		this.vMin = vMin;
		this.vMax = rMax;
		this.numTrainingSteps = numTrainingSteps;
		this.isDiscrete = isDiscrete;
		this.objectClassForRep = objectClassesForRep.clone();
		this.description = description;



	}

	public TaskAndTrain copy(){
		return new TaskAndTrain(domain, rf, tf, discount, sg, vMin, vMax, numTrainingSteps, isDiscrete, objectClassForRep, description);
	}



}
