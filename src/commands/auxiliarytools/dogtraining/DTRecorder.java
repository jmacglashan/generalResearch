package commands.auxiliarytools.dogtraining;

import commands.auxiliarytools.TrajectoryRecorder;

import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.dogtraining.DTStateParser;
import domain.singleagent.dogtraining.DTVisualizer;
import domain.singleagent.dogtraining.DogTraining;

public class DTRecorder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("Incorrect format; use:\n\tpathToDataDirectory");
			System.exit(0);
		}
		
		DogTraining dt = new DogTraining(5, 5, true);
		Domain domain = dt.generateDomain();
		
		State initialState = DogTraining.getOneDogNLocationNToyState(domain, 4, 1);
		DogTraining.setDog(initialState, 3, 1, 4, 0);
		//DogTraining.setDog(initialState, 3, 1, 3, 0, 1);
		
		DogTraining.setLocation(initialState, 0, 2, 0, DogTraining.LIDHOME);
		DogTraining.setLocation(initialState, 1, 0, 4, DogTraining.LIDRED);
		DogTraining.setLocation(initialState, 2, 2, 4, DogTraining.LIDGREEN);
		DogTraining.setLocation(initialState, 3, 4, 4, DogTraining.LIDBLUE);
		
		//DogTraining.setToy(initialState, 0, 3, 1);
		DogTraining.setToy(initialState, 0, 1, 4);
		
		Visualizer v = DTVisualizer.getVisualizer(domain, dt.getMap());
		
		TrajectoryRecorder rec = new TrajectoryRecorder();
		
		//use w-s-a-d-x
		rec.addKeyAction("w", DogTraining.ACTIONNORTH);
		rec.addKeyAction("s", DogTraining.ACTIONSOUTH);
		rec.addKeyAction("a", DogTraining.ACTIONWEST);
		rec.addKeyAction("d", DogTraining.ACTIONEAST);
		rec.addKeyAction("e", DogTraining.ACTIONPICKUP);
		rec.addKeyAction("q", DogTraining.ACTIONPUTDOWN);
		rec.addKeyAction("f", DogTraining.ACTIONWAIT);
		
		rec.init(v, domain, new DTStateParser(domain), new ConstantStateGenerator(initialState), args[0]);
		
		

	}

}
