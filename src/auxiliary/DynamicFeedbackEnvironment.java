package auxiliary;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.environment.Environment;

public class DynamicFeedbackEnvironment extends Environment {

	protected Domain					operatingDomain;
	
	
	protected double					lastReward = 0.;
	protected boolean					isTerminal = false;
	
	protected long						actionUpdateDelay = 1200;
	
	public DynamicFeedbackEnvironment(Domain operatingDomain){
		this.operatingDomain = operatingDomain;
	}
	
	
	/**
	 * Sets the amount of delay between the completion of executing an given action in the operating domain, and when the 
	 * {@link #executeAction(String, String[])} returns. This delay is useful becuse it may be that the results of 
	 * action in the operating domain are conveyed to the user, but the agent response is delayed thereby giving
	 * the user time to provide a feedback signal.
	 * @param actionUpdateDelay the amount of delay between an action execution and the return of the {@link #executeAction(String, String[])} method.
	 */
	public void setActionUpdateDelay(long actionUpdateDelay){
		this.actionUpdateDelay = actionUpdateDelay;
	}
	
	
	/**
	 * Tells the environment to recieve a human feedback signal to update the reward it returns. The default behavior
	 * is to set to the last reward to this new feedback.
	 * @param feedback the feedback provided by the human
	 */
	public void receiveHumanFeedback(double feedback){
		this.lastReward = feedback;
	}
	
	/**
	 * Tells the environment to receive a termination signal from the human. Sets the current state observed by the human to be a terminal state.
	 * @param isTerminal whether the current state observed by the human is to be set as a terminal state or not.
	 */
	public void receiveIsTerminalSignal(boolean isTerminal){
		this.isTerminal = isTerminal;
	}
	
	@Override
	public State executeAction(String aname, String[] params) {
		
		this.decayHumanFeedback();
		Action a = this.operatingDomain.getAction(aname);
		final State nextState = a.performAction(curState, params);
		
		this.curState = nextState;
		
		this.waitForUpdateDelay();
		
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
	
	/**
	 * Method is called at the start of each {@link #executeAction(String, String[])} method, which is used to decay the last feedback signals
	 * from the previous actions. The default behavior is to completely decay the feedback signal to 0
	 */
	protected void decayHumanFeedback(){
		this.lastReward = 0.;
	}
	
	
	protected void waitForUpdateDelay(){
		Thread waitThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(DynamicFeedbackEnvironment.this.actionUpdateDelay);
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
	}

}
