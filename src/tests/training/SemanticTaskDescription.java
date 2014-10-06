package tests.training;

import java.util.ArrayList;
import java.util.List;


import behavior.training.taskinduction.TaskDescription;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class SemanticTaskDescription extends TaskDescription{

	public String semeanticString;
	
	public SemanticTaskDescription(GroundedProp gp){
		List <GroundedProp> gps = new ArrayList<GroundedProp>(1);
		gps.add(gp);
		this.rf = new ConjunctiveGPRF(gps);
		this.tf = new ConjunctiveGPTF(gps);
		this.semeanticString = this.rf.toString();
	}

	public SemanticTaskDescription(List<GroundedProp> gps){
		this.rf = new ConjunctiveGPRF(gps);
		this.tf = new ConjunctiveGPTF(gps);
		this.semeanticString = this.rf.toString();
	}
	
	public SemanticTaskDescription(RewardFunction rf, TerminalFunction tf, String semanticString) {
		super(rf, tf);
		this.semeanticString = semanticString;
	}


	
	
	public class ConjunctiveGPRF implements RewardFunction{

		List<GroundedProp> gps = new ArrayList<GroundedProp>();
		
		
		public ConjunctiveGPRF(){
		
		}
		
		public ConjunctiveGPRF(List<GroundedProp> gps){
			this.gps = gps;
		}
		
		public void addGP(GroundedProp gp){
			gps.add(gp);
		}
		
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			
			for(GroundedProp gp : gps){
				if(!gp.isTrue(sprime)){
					return 0.;
				}
			}
			
			return 1;
		}
		
		
		@Override
		public String toString(){
			StringBuffer buf = new StringBuffer();
			
			for(int i = 0; i < this.gps.size(); i++){
				if(i > 0){
					buf.append(" ");
				}
				GroundedProp gp = this.gps.get(i);
				buf.append(gp.pf.getName());
				for(String p : gp.params){
					buf.append(" ").append(p);
				}
			}
			
			return buf.toString();
		}
		
		
	}
	
	
	public class ConjunctiveGPTF implements TerminalFunction{

		List<GroundedProp> gps = new ArrayList<GroundedProp>();
		
		
		public ConjunctiveGPTF(){
			
		}
		
		public ConjunctiveGPTF(List<GroundedProp> gps){
			this.gps = gps;
		}
		
		
		public void addGP(GroundedProp gp){
			gps.add(gp);
		}
		
		
		@Override
		public boolean isTerminal(State s) {
			
			for(GroundedProp gp : gps){
				if(!gp.isTrue(s)){
					return false;
				}
			}
			
			return true;
		}
		
		
		@Override
		public String toString(){
			StringBuffer buf = new StringBuffer();
			
			for(int i = 0; i < this.gps.size(); i++){
				if(i > 0){
					buf.append(" ");
				}
				GroundedProp gp = this.gps.get(i);
				buf.append(gp.pf.getName());
				for(String p : gp.params){
					buf.append(" ").append(p);
				}
			}
			
			return buf.toString();
		}
		
		
		
	}
	
}
