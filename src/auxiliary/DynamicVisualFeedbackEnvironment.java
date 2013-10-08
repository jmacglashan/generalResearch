package auxiliary;

import behavior.learning.Environment;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class DynamicVisualFeedbackEnvironment extends Environment {

	protected Domain					operatingDomain;
	protected StateVisualizingGUI		gui;
	
	protected double					lastReward;
	protected boolean					isTerminal;
	
	protected long						actionRenderDelay = 1200;
	
	public DynamicVisualFeedbackEnvironment(Domain operatingDomain) {
		this.operatingDomain = operatingDomain;
	}
	
	
	public void setGUI(StateVisualizingGUI gui){
		this.gui = gui;
	}
	
	@Override
	public void setCurStateTo(State s){
		super.setCurStateTo(s);
		this.gui.setRenderState(s);
		this.isTerminal = false;
		this.lastReward = 0.;
	}

	@Override
	public State executeAction(String aname, String[] params) {
		
		//System.out.println("Beginning action execution: " + aname);
		
		this.lastReward = 0.;
		Action a = this.operatingDomain.getAction(aname);
		final State nextState = a.performAction(curState, params);
		
		this.curState = nextState;
		gui.setRenderState(nextState);
		
		
		//System.out.println("Finished Render Call; beginning wait");
		
		Thread waitThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(actionRenderDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		waitThread.start();
		
		try {
			waitThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		//this.curState = nextState;
		
		return nextState;
	}
	
	

	@Override
	public double getLastReward() {
		return this.lastReward;
	}

	@Override
	public boolean curStateIsTerminal() {
		return this.isTerminal;
	}
	
	
	public void setReward(double r){
		this.lastReward = r;
	}
	
	public void setTerminal(){
		this.isTerminal = true;
	}
	
	
	public RewardFunction getEnvRewardFunction(){
		return new RewardFunction() {
			
			@Override
			public double reward(State s, GroundedAction a, State sprime) {
				return getLastReward();
			}
		};
	}
	
	public TerminalFunction getEnvTerminalFunction(){
		return new TerminalFunction() {
			
			@Override
			public boolean isTerminal(State s) {
				return curStateIsTerminal();
			}
		};
	}

}
