package domain.singleagent.binarylock;

import java.util.List;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.*;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;

public class BinaryLockDomain implements DomainGenerator {

	public static final String				ATTBITVAL = "val"; //value 0/1 and 2 for unset
	public static final String				ATTBITPOS = "pos";
	public static final String				ATTCURBIT = "cur";
	
	public static final String				CLASSBIT = "bit";
	public static final String				CLASSHEAD = "head";
	
	public static final String				ACTIONSET0 = "set0";
	public static final String				ACTIONSET1 = "set1";
	
	
	
	protected int							maxNumBits;
	
	
	
	public BinaryLockDomain(int maxBitSize){
		this.maxNumBits = maxBitSize;
	}
	
	
	@Override
	public Domain generateDomain() {
		
		SADomain domain = new SADomain();
		
		Attribute attval = new Attribute(domain, ATTBITVAL, AttributeType.DISC);
		attval.setDiscValuesForRange(0, 2, 1);
		
		Attribute attpos = new Attribute(domain, ATTBITPOS, AttributeType.DISC);
		attpos.setDiscValuesForRange(0, maxNumBits-1, 1);
		
		Attribute attcurbit = new Attribute(domain, ATTCURBIT, AttributeType.DISC);
		attcurbit.setDiscValuesForRange(0, maxNumBits, 1);
		
		ObjectClass bit = new ObjectClass(domain, CLASSBIT);
		bit.addAttribute(attval);
		bit.addAttribute(attpos);
		
		ObjectClass head = new ObjectClass(domain, CLASSHEAD);
		head.addAttribute(attcurbit);
		
		Action set0 = new SetBitAction(ACTIONSET0, domain, 0);
		Action set1 = new SetBitAction(ACTIONSET1, domain, 1);
		
		return domain;
	}
	
	
	public static ObjectInstance findObWithBitPos(State s, int bpos){
		List<ObjectInstance> objects = s.getObjectsOfTrueClass(CLASSBIT);
		return findObWithBitPos(objects, bpos);
	}
	
	
	public static ObjectInstance findObWithBitPos(List<ObjectInstance> objects, int bpos){
		
		for(ObjectInstance o : objects){
			int op = o.getDiscValForAttribute(ATTBITPOS);
			if(op == bpos){
				return o;
			}
		}
		
		throw new RuntimeException("Could not find bit at given pos");
	}
	
	
	public static State getStartState(Domain domain, int nBits){
		
		State s = new State();
		
		ObjectInstance head = new ObjectInstance(domain.getObjectClass(CLASSHEAD), "head");
		head.setValue(ATTCURBIT, 0);
		s.addObject(head);
		
		
		for(int i = 0; i < nBits; i++){
			ObjectInstance bit = new ObjectInstance(domain.getObjectClass(CLASSBIT), "bit" + i);
			bit.setValue(ATTBITPOS, i);
			bit.setValue(ATTBITVAL, 2); //unset
			s.addObject(bit);
		}
		
		
		return s;
	}
	
	public static State getState(Domain domain, int headPos, int...bitSequence){
		
		State s = new State();
		
		int nBits = bitSequence.length;
		
		ObjectInstance head = new ObjectInstance(domain.getObjectClass(CLASSHEAD), "head");
		head.setValue(ATTCURBIT, headPos);
		s.addObject(head);
		
		
		for(int i = 0; i < nBits; i++){
			ObjectInstance bit = new ObjectInstance(domain.getObjectClass(CLASSBIT), "bit" + i);
			bit.setValue(ATTBITPOS, i);
			bit.setValue(ATTBITVAL, bitSequence[i]); //unset
			s.addObject(bit);
		}
		
		
		return s;
	}
	
	
	public class SetBitAction extends Action{

		int val;
		
		public SetBitAction(String name, Domain domain, int val){
			super(name, domain, "");
			this.val = val;
		}
		
		
		@Override
		public boolean applicableInState(State s, String [] params){

			ObjectInstance head = s.getFirstObjectOfClass(CLASSHEAD);
			int hpos = head.getDiscValForAttribute(ATTCURBIT);
			
			if(hpos < s.getObjectsOfTrueClass(CLASSBIT).size()){
				return true; 
			}
			return false;
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance head = s.getFirstObjectOfClass(CLASSHEAD);
			int hpos = head.getDiscValForAttribute(ATTCURBIT);
			
			List<ObjectInstance> bits = s.getObjectsOfTrueClass(CLASSBIT);
			ObjectInstance curBit = bits.get(hpos);
			if(curBit.getDiscValForAttribute(ATTBITPOS) != hpos){
				curBit = findObWithBitPos(bits, hpos);
			}
			
			curBit.setValue(ATTBITVAL, this.val);
			
			head.setValue(ATTCURBIT, hpos+1);
			
			return s;
		}


		@Override
		public List<TransitionProbability> getTransitions(State s, String [] params){
			return this.deterministicTransition(s, params);
		}
		
		
		
		
	}
	

}
