package domain.paintpolish;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;

public class PaintPolish implements DomainGenerator {

	public static final String						ATTPAINTED = "painted";
	public static final String						ATTPOLISHED = "polished";
	public static final String						ATTSCRATCHED = "scratched";
	public static final String						ATTFINISHED = "finished";
	
	public static final String						CLASSOBJECT = "object";
	
	public static final String						ACTIONPAINT = "paint";
	public static final String						ACTIONPOLISH = "polish";
	public static final String						ACTIONSHORTCUT = "shortcut";
	public static final String						ACTIONDONE = "done";
	
	
	@Override
	public Domain generateDomain() {
		
		SADomain domain = new SADomain();
		
		Attribute painted = new Attribute(domain, ATTPAINTED, AttributeType.BOOLEAN);
		Attribute polished = new Attribute(domain, ATTPOLISHED, AttributeType.BOOLEAN);
		Attribute scratched = new Attribute(domain, ATTSCRATCHED, AttributeType.BOOLEAN);
		Attribute finished = new Attribute(domain, ATTFINISHED, AttributeType.BOOLEAN);
		
		ObjectClass object = new ObjectClass(domain, CLASSOBJECT);
		object.addAttribute(painted);
		object.addAttribute(polished);
		object.addAttribute(scratched);
		object.addAttribute(finished);
		
		
		new PaintAction(domain);
		new PolishAction(domain);
		new ShortcutAction(domain);
		new DoneAction(domain);
		
		return domain;
	}
	
	
	public static State getInitialState(Domain domain, int numObjects){
		
		State s = new State();
		
		for(int i = 0; i < numObjects; i++){
			ObjectInstance o = new ObjectInstance(domain.getObjectClass(CLASSOBJECT), CLASSOBJECT+i);
			o.setValue(ATTPAINTED, false);
			o.setValue(ATTPOLISHED, false);
			o.setValue(ATTSCRATCHED, false);
			o.setValue(ATTFINISHED, false);
			s.addObject(o);
		}
		
		return s;
	}
	
	
	public class PaintAction extends Action{

		public PaintAction(Domain domain){
			super(ACTIONPAINT, domain, CLASSOBJECT);
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance o = s.getObject(params[0]);
			Random rand = RandomFactory.getMapped(0);
			double r = rand.nextDouble();
			
			if(r < 0.6){
				o.setValue(ATTPAINTED, true);
			}
			else if(r < 0.9){
				o.setValue(ATTPAINTED, true);
				o.setValue(ATTSCRATCHED, true);
			}
			//0.1 no effect
			
			return s;
		}
		
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			
			List<TransitionProbability> tps = new ArrayList<TransitionProbability>(3);
			
			//no op
			tps.add(new TransitionProbability(s.copy(), 0.1));
			
			//painted
			State s2 = s.copy();
			s2.getObject(params[0]).setValue(ATTPAINTED, true);
			tps.add(new TransitionProbability(s2, 0.6));
			
			//painted and scratched
			State s3 = s2.copy();
			s3.getObject(params[0]).setValue(ATTSCRATCHED, true);
			tps.add(new TransitionProbability(s3, 0.3));
			
			
			return tps;
		}
		
		
		
	}
	
	
	
	public class PolishAction extends Action{

		public PolishAction(Domain domain){
			super(ACTIONPOLISH, domain, CLASSOBJECT);
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance o = s.getObject(params[0]);
			Random rand = RandomFactory.getMapped(0);
			double r = rand.nextDouble();
			
			if(r < 0.2){
				o.setValue(ATTPAINTED, false);
			}
			else if(r < 0.4){
				o.setValue(ATTSCRATCHED, false);
			}
			else if(r < 0.7){
				o.setValue(ATTPOLISHED, true);
				o.setValue(ATTPAINTED, false);
				o.setValue(ATTSCRATCHED, false);
			}
			else if(r < 0.9){
				o.setValue(ATTPOLISHED, true);
				o.setValue(ATTPAINTED, false);
			}
			//0.1 no op
			
			return s;
		}
		
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			
			List<TransitionProbability> tps = new ArrayList<TransitionProbability>(3);
			
			//no op
			tps.add(new TransitionProbability(s.copy(), 0.1));
			
			//not painted
			State s2 = s.copy();
			s2.getObject(params[0]).setValue(ATTPAINTED, false);
			tps.add(new TransitionProbability(s2, 0.2));
			
			//not scratched
			State s3 = s.copy();
			s3.getObject(params[0]).setValue(ATTSCRATCHED, false);
			tps.add(new TransitionProbability(s3, 0.2));
			
			//polished but not painted
			State s4 = s.copy();
			s4.getObject(params[0]).setValue(ATTPAINTED, false);
			s4.getObject(params[0]).setValue(ATTPOLISHED, true);
			tps.add(new TransitionProbability(s4, 0.2));
			
			//polished but not painted nor scratched
			State s5 = s.copy();
			s5.getObject(params[0]).setValue(ATTPAINTED, false);
			s5.getObject(params[0]).setValue(ATTSCRATCHED, false);
			s5.getObject(params[0]).setValue(ATTPOLISHED, true);
			tps.add(new TransitionProbability(s5, 0.3));
			
			return tps;
		}
		
		
		
	}
	
	
	
	public class ShortcutAction extends Action{

		public ShortcutAction(Domain domain){
			super(ACTIONSHORTCUT, domain, CLASSOBJECT);
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance o = s.getObject(params[0]);
			Random rand = RandomFactory.getMapped(0);
			double r = rand.nextDouble();
			
			if(r < 0.05){
				o.setValue(ATTPAINTED, true);
				o.setValue(ATTPOLISHED, true);
			}
			//0.95 no effect
			
			return s;
		}
		
		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			
			List<TransitionProbability> tps = new ArrayList<TransitionProbability>(3);
			
			//no op
			tps.add(new TransitionProbability(s.copy(), 0.95));
			
			//painted and polished
			State s2 = s.copy();
			s2.getObject(params[0]).setValue(ATTPAINTED, true);
			s2.getObject(params[0]).setValue(ATTPOLISHED, true);
			tps.add(new TransitionProbability(s2, 0.05));
			
			return tps;
		}
		
		
		
	}
	
	
	public class DoneAction extends Action{

		public DoneAction(Domain domain){
			super(ACTIONDONE, domain, CLASSOBJECT);
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			ObjectInstance o = s.getObject(params[0]);			
			o.setValue(ATTFINISHED, true);
			return s;
		}
		
		@Override
		public boolean applicableInState(State s, String [] params){
			
			ObjectInstance o = s.getObject(params[0]);
			return o.getBooleanValue(ATTPAINTED) && o.getBooleanValue(ATTPOLISHED) && !o.getBooleanValue(ATTSCRATCHED) && !o.getBooleanValue(ATTFINISHED);
		}
		
		
		
	}
	
	
	public static class PaintPolishRF implements RewardFunction{

		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			if(!a.actionName().equals(ACTIONDONE)){
				return -1;
			}
			
			return 10;
		}
		
	}
	
	public static class PaintPolishTF implements TerminalFunction{

		@Override
		public boolean isTerminal(State s) {
			List<ObjectInstance> os = s.getObjectsOfTrueClass(CLASSOBJECT);
			for(ObjectInstance o : os){
				if(!o.getBooleanValue(ATTFINISHED)){
					return false;
				}
			}
			
			return true;
		}
		
		
		
	}
	
	

}
