package tests;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.StateYAMLParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.VisualActionObserver;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.taxi.TaxiDomain;
import domain.singleagent.taxi.TaxiVisualizer;

public class TaxiLearn {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TaxiDomain dg = new TaxiDomain();
		dg.includeFuel = false;
		Domain d = dg.generateDomain();
		State s = TaxiDomain.getClassicState(d);
		
		Visualizer v = TaxiVisualizer.getVisualizer(5, 5);
		
		TerminalFunction tf = new TerminalFunction() {
			
			@Override
			public boolean isTerminal(State s) {
				
				ObjectInstance p = s.getFirstObjectOfClass(TaxiDomain.PASSENGERCLASS);
				int px = p.getDiscValForAttribute(TaxiDomain.XATT);
				int py = p.getDiscValForAttribute(TaxiDomain.YATT);
				int intaxi = p.getDiscValForAttribute(TaxiDomain.INTAXIATT);
				
				if(intaxi == 0 && px == 0 && py == 4){
					return true;
				}
				
				return false;
			}
		};
		
		RewardFunction rf = new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				
				ObjectInstance p = s.getFirstObjectOfClass(TaxiDomain.PASSENGERCLASS);
				int px = p.getDiscValForAttribute(TaxiDomain.XATT);
				int py = p.getDiscValForAttribute(TaxiDomain.YATT);
				int intaxi = p.getDiscValForAttribute(TaxiDomain.INTAXIATT);
				
				if(intaxi == 0 && px == 0 && py == 4){
					return 20.;
				}
				
				return -1.;
			}
		};
		
		StateParser sp = new StateYAMLParser(d);
		
		//VisualActionObserver obs = new VisualActionObserver(d, v);
		//obs.initGUI();
		//((SADomain)d).addActionObserverForAllAction(obs);
		
		QLearning ql = new QLearning(d, rf, tf, 0.99, new DiscreteStateHashFactory(), 0., 1.);
		
		for(int i = 0; i < 300; i++){
			EpisodeAnalysis ea = ql.runLearningEpisodeFrom(s);
			ea.writeToFile(String.format("taxiLearn/%04d", i), sp);
			System.out.println("" + i + " " + ea.numTimeSteps());
		}
		
		
		new EpisodeSequenceVisualizer(v, d, sp, "taxiLearn");

	}

}
