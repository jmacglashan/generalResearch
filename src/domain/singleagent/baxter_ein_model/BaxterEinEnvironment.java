package domain.singleagent.baxter_ein_model;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.states.MutableState;
import burlap.oomdp.singleagent.environment.DomainEnvironmentWrapper;
import burlap.oomdp.singleagent.environment.Environment;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import com.fasterxml.jackson.databind.JsonNode;
import ros.Publisher;
import ros.RosBridge;
import ros.RosListenDelegate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class BaxterEinEnvironment extends Environment implements RosListenDelegate{


	RosBridge rosBridge;
	Publisher actionPublisher;
	Domain domain;

	public BaxterEinEnvironment(String URI){

		BaxterEin bein = new BaxterEin();
		this.domain = bein.generateDomain();
		this.rosBridge = RosBridge.createConnection(URI);
		this.rosBridge.waitForConnection();

		this.actionPublisher = new Publisher("/ein/left/forth_commands", "std_msgs/String", rosBridge);
		this.actionPublisher.advertise();
		this.rosBridge.subscribe("/ein_left/state", "ein/EinState", this, 1, 1);


	}

	public Domain getDomain(){
		return this.domain;
	}

	@Override
	public State getCurState() {


		if(this.curState == null){
			synchronized(this){
				while(this.curState == null){
					try {
						this.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return this.curState;
		//return super.getCurState();
	}

	@Override
	public void setCurStateTo(State s) {

		State oldState = this.curState;
		super.setCurStateTo(s);
		if(oldState == null){
			synchronized(this) {
				this.notifyAll();
			}
		}
	}

	@Override
	public State executeAction(String aname, String[] params) {

		// -45 "table3" "brownMug" "moveObjectToObjectByAmount" ;

		if(aname.equals(BaxterEin.ACTIONSTACK)){
			String strMsg = "-45 \"" + params[1] + "\" \"" + params[0] + "\" moveObjectToObjectByAmount ;";
			this.actionPublisher.publish(getStrMsg(strMsg));
		}

		try {
			Thread.sleep(7000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		return this.curState;
	}

	@Override
	public double getLastReward() {
		return 0;
	}

	@Override
	public boolean curStateIsTerminal() {
		return false;
	}

	@Override
	public void receive(JsonNode data, String stringRep) {

		State s = new MutableState();
		JsonNode objects = data.get("msg").get("objects");
		for(int i = 0; i < objects.size(); i++){
			JsonNode ob = objects.get(i);
			JsonNode innerPosition = ob.get("pose").get("pose").get("pose").get("position");
			Pos pos = new Pos(innerPosition.get("x").asDouble(),
					innerPosition.get("y").asDouble(),
					innerPosition.get("z").asDouble());

			BaxterEin.addObjectToState(this.domain, s, ob.get("type").get("key").asText(), pos.x, pos.y, pos.z);
		}

		boolean printIt = false;
		if(!s.equals(this.curState)){
			//printIt = true;
		}

		this.setCurStateTo(s);

		if(printIt) {
			List<GroundedProp> gps = PropositionalFunction.getAllGroundedPropsFromPFList(domain.getPropFunctions(), s);
			for(GroundedProp gp : gps) {
				if(gp.isTrue(s)) {
					System.out.print(gp.toString() + "; ");
				}
			}
			System.out.println();
			System.out.println(s.toString());
			System.out.println("--");
		}

	}


	protected static Map<String, Object> getStrMsg(String value){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("data", value);
		return map;
	}

	public static class Pos{
		public double x;
		public double y;
		public double z;

		public Pos(double x, double y, double z){
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public String toString(){
			return "(" + x + ", " + y + ", " + z + ")";
		}
	}


	public static void main(String[] args) {

		BaxterEinEnvironment env = new BaxterEinEnvironment("ws://octopus:9090");

		DomainEnvironmentWrapper wrapper = new DomainEnvironmentWrapper(env.getDomain(), env);
		Domain wdomain = wrapper.generateDomain();

		TerminalExplorer exp = new TerminalExplorer(wdomain);

		//System.out.println("Getting cur state");
		State cur = env.getCurState();
		//System.out.println(cur.toString());

		exp.exploreFromState(cur);


	}


}
