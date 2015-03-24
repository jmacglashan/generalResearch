package tests.ros;

import burlap.behavior.singleagent.Policy;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Domain;
import burlap.oomdp.singleagent.common.NullRewardFunction;
import burlap.oomdp.singleagent.environment.DomainEnvironmentWrapper;
import burlap.ros.AsynchronousRosEnvironment;

/**
 * @author James MacGlashan.
 */
public class ROSGridWorld {

	public static void main(String[] args) {

		//define the grid world
		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		gwd.makeEmptyMap();
		final Domain domain = gwd.generateDomain();

		//setup ROS information
		String uri = "ws://chelone:9090";
		String stateTopic = "/burlap_state";
		String actionTopic = "/burlap_action";

		//create environment with 2000ms (2s) action execution time
		AsynchronousRosEnvironment env = new AsynchronousRosEnvironment(domain, uri, stateTopic, actionTopic, 2000);
		env.blockUntilStateReceived();

		//optionally, uncomment the below so that you can see the received state printed to the terminal
		env.setPrintStateAsReceived(true);

		//create a domain wrapper of the environment so that wrapped domain's actions go to
		//to the environment, rather than the normal GridWorld action simulator code.
		DomainEnvironmentWrapper envDomainWrapper = new DomainEnvironmentWrapper(domain, env);
		final Domain envDomain = envDomainWrapper.generateDomain();

		//create a random policy for control that connects to the environment wrapped domain
		Policy randPolicy = new Policy.RandomPolicy(envDomain);

		//begin behavior for 100 steps (200 seconds)
		randPolicy.evaluateBehavior(env.getCurState(), new NullRewardFunction(), 100);

	}

}
