package tests;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;




public class ConsumerSim implements DomainGenerator{

	public enum Distribution {
		Normal, Exponential, Uniform
	}

	//wtp = willingness-to-pay
	public static final String ATTWTP = "wtp";
	public static final String ATTDECISIONHISTORY = "decisionhistory";
	public static final String ATTPRICEHISTORY = "pricehistory";

	public static final String CLASSSELLER = "seller";
	public static final String CLASSCONSUMER = "consumer";

	public static final int PMIN = 0;
	public static final int PMAX = 100;

	public static final Distribution DIST = Distribution.Normal;
	public static final int MEAN = 20;

	@Override
	public Domain generateDomain() {

		SADomain domain = new SADomain();

		Attribute wtpAtt = new Attribute(domain, ATTWTP, Attribute.AttributeType.INT);
		wtpAtt.setLims(PMIN, PMAX);
		Attribute decisionHistoryAtt = new Attribute(domain, ATTDECISIONHISTORY, Attribute.AttributeType.INTARRAY);
		Attribute priceHistoryAtt = new Attribute(domain, ATTPRICEHISTORY, Attribute.AttributeType.INTARRAY);

		ObjectClass sellerClass = new ObjectClass(domain, CLASSSELLER);
		sellerClass.addAttribute(decisionHistoryAtt);
		sellerClass.addAttribute(priceHistoryAtt);

		ObjectClass consumerClass = new ObjectClass(domain, CLASSCONSUMER);
		consumerClass.addAttribute(wtpAtt);

		for(int p = PMIN; p <= PMAX; p++){
			new SetPrice("price" + p, domain, p);
		}

		return domain;

	}

	public static State getExampleState(Domain domain){
		State s = new State();
		ObjectInstance seller = new ObjectInstance(domain.getObjectClass(CLASSSELLER), "seller0");
		int[] decisionHistoryInit = new int[1];
		decisionHistoryInit[0] = 1;
		int[] priceHistoryInit = new int[1];
		priceHistoryInit[0] = 0;
		seller.setValue(ATTDECISIONHISTORY, decisionHistoryInit);
		seller.setValue(ATTPRICEHISTORY, priceHistoryInit);

		ObjectInstance consumer = new ObjectInstance(domain.getObjectClass(CLASSCONSUMER), "consumer0");
		consumer.setValue(ATTWTP, ConsumerSim.getSampleWTP());

		s.addObject(seller);
		s.addObject(consumer);

		return s;
	}

	private static int getSampleWTP(){
		Random gen = new Random();
		int wtp = 0;
		switch (DIST) {
			case Normal:
				wtp = (int) gen.nextGaussian()*MEAN;
				break;
			case Exponential:
				wtp =  (int) (-1)*((int) (((double) MEAN)*Math.log(gen.nextDouble())));
				break;
			case Uniform:
				wtp = PMIN + gen.nextInt(PMAX-PMIN);
			default:
				break;
		}
		wtp = Math.min(wtp, PMAX);
		wtp = Math.max(wtp, PMIN);
		return wtp;
	}

	protected class SetPrice extends Action {
		private int price;

		public SetPrice(String actionName, Domain domain, int price){
			super(actionName, domain, "");
			this.price = price;
		}

		@Override
		protected State performActionHelper(State s, String[] params) {

			ObjectInstance consumer = s.getFirstObjectOfClass(CLASSCONSUMER);
			ObjectInstance seller = s.getFirstObjectOfClass(CLASSSELLER);

			int curWTP = consumer.getDiscValForAttribute(ATTWTP);
			int[] curDecisionHistory = seller.getIntArrayValue(ATTDECISIONHISTORY);
			int[] curPriceHistory = seller.getIntArrayValue(ATTPRICEHISTORY);

			int decision = this.getDecision(price, curWTP);

			int[] newDecisionHistory;
			int[] newPriceHistory;
			if(curDecisionHistory == null){
				newDecisionHistory = new int[1];
				newDecisionHistory[0] = decision;
				newPriceHistory = new int[1];
				newPriceHistory[0] = this.price;
			}
			else{
				newPriceHistory = Arrays.copyOf(curPriceHistory, curPriceHistory.length);
				newPriceHistory[newPriceHistory.length-1] = this.price;

				newDecisionHistory = Arrays.copyOf(curDecisionHistory, curDecisionHistory.length);
				newDecisionHistory[newDecisionHistory.length-1] = decision;
			}

			seller.setValue(ATTDECISIONHISTORY, newDecisionHistory);
			seller.setValue(ATTPRICEHISTORY, newPriceHistory);

			//return the state we just modified
			return s;
		}


		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){

			List<TransitionProbability> tps = new ArrayList<TransitionProbability>(3);

			ObjectInstance consumer = s.getFirstObjectOfClass(CLASSCONSUMER);
			ObjectInstance seller = s.getFirstObjectOfClass(CLASSSELLER);
			int curWTP = consumer.getDiscValForAttribute(ATTWTP);
			int d = getDecision(this.price, curWTP);

			for(int i = 0; i < 3; i++){
				State ns = s.copy();
				int[] curDecisionHistory = seller.getIntArrayValue(ATTDECISIONHISTORY);
				int[] curPriceHistory = seller.getIntArrayValue(ATTPRICEHISTORY);

				int[] newDecisionHistory;
				int[] newPriceHistory;
				if(curDecisionHistory == null){
					newDecisionHistory = new int[1];
					newDecisionHistory[0] = d;
					newPriceHistory = new int[1];
					newPriceHistory[0] = this.price;
				}
				else{
					newPriceHistory = Arrays.copyOf(curPriceHistory, curPriceHistory.length);
					newPriceHistory[newPriceHistory.length-1] = this.price;

					newDecisionHistory = Arrays.copyOf(curDecisionHistory, curDecisionHistory.length);
					newDecisionHistory[newDecisionHistory.length-1] = d;
				}

				seller.setValue(ATTDECISIONHISTORY, newDecisionHistory);
				seller.setValue(ATTPRICEHISTORY, newPriceHistory);

				//probability is 1 if it's the decision and 0 otherwise
				if(i == d){
					tps.add(new TransitionProbability(ns, 1));
				}
				else{
					tps.add(new TransitionProbability(ns, 0));
				}
			}

			return tps;
		}

		//0: Buy, 1: Pass, 2: Leave
		protected int getDecision(double price, double wtp){
			if(price < wtp){
				return 0;
			}
			else if (price > 2*wtp){
				return 2;
			}
			else{
				return 1;
			}
		}

	}


	public static class SellerRF implements RewardFunction {

		public SellerRF(){
		}

		@Override
		public double reward(State s, GroundedAction a, State sprime) {

			ObjectInstance seller = sprime.getFirstObjectOfClass(CLASSSELLER);
			int[] curDecisionHistory = seller.getIntArrayValue(ATTDECISIONHISTORY);
			int[] curPriceHistory = seller.getIntArrayValue(ATTPRICEHISTORY);

			//Did the player buy it?
			switch (curDecisionHistory[curDecisionHistory.length-1]){
				case 0:
					return curPriceHistory[curPriceHistory.length-1];
				case 1:
					return -1;
				case 2:
					return (-1)*PMAX;
				default:
					return -9999999;
			}
		}


	}

	public static class SellerTF implements TerminalFunction {


		public SellerTF(int goalX, int goalY){
		}

		@Override
		public boolean isTerminal(State s) {

			//get location of agent in next state
			ObjectInstance seller = s.getFirstObjectOfClass(CLASSSELLER);
			int[] curDecisionHistory = seller.getIntArrayValue(ATTDECISIONHISTORY);

			if(curDecisionHistory[curDecisionHistory.length-1] == 1 ){
				return false;
			}

			return true;
		}
	}



	public static void main(String [] args){

		ConsumerSim gen = new ConsumerSim();
		Domain domain = gen.generateDomain();

		State initialState = ConsumerSim.getExampleState(domain);

		TerminalExplorer exp = new TerminalExplorer(domain);
		exp.exploreFromState(initialState);

	}

}
