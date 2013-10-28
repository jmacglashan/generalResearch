package domain.stocasticgames.foragesteal;

import java.util.List;

import ethics.experiments.tbforagesteal.aux.TBFSSubjectiveRFWS;

import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.stocashticgames.JointActionModel;
import burlap.oomdp.stocashticgames.JointReward;
import burlap.oomdp.stocashticgames.SGDomain;
import burlap.oomdp.stocashticgames.SingleAction;
import burlap.oomdp.stocashticgames.common.UniversalSingleAction;
import burlap.oomdp.stocashticgames.explorers.SGTerminalExplorer;

public class TBForageSteal {

	public static final String				ATTPN = "playerNum";
	public static final String				ATTFA = "forageAlternative";
	public static final String				ATTPTA = "previousTurnSocialAction";
	public static final String				ATTISTURN = "isTurn";
	
	public static final String				CLASSAGENT = "agent";
	public static final String				CLASSFALT = "forageAlternative";
	
	public static final String				ACTIONNOP = "noop";
	public static final String				ACTIONSTEAL = "steal";
	public static final String				ACTIONPUNCH = "punch";
	public static final String				ACTIONFORAGE = "forage";
	
	public static final String				PFGAMEOVER = "gameover";
	
	
	public static final int					NALTS = 5;
	
	
	private static SGDomain					DOMAIN = null;
	
	
	
	
	public static void main(String [] args){
		
		SGDomain d = (SGDomain) generateDomain();
		//int nf = NALTS;
		int nf = 0;
		int [] allAlts = new int[nf];
		for(int i = 0; i < nf; i++){
			allAlts[i] = i;
		}
		
		State s = getGameStartState(allAlts, 0);
		
		//JointActionModel jam = new TBFSStandardMechanics();
		JointActionModel jam = new TBFSWhoStartedMechanics();
		
		JointReward objectiveRF = new TBFSStandardReward(1, -1, -.1, -2, new double[]{-1, -.5, .5, 1., 1.5});
		JointReward subjectiveRF = new TBFSSubjectiveRFWS(objectiveRF, new double[]{-1.0, 2.0, -0.4});
		
		SGTerminalExplorer exp = new SGTerminalExplorer(d, jam);
		exp.setTrackingRF(subjectiveRF);
		exp.addActionShortHand("n", ACTIONNOP);
		exp.addActionShortHand("s", ACTIONSTEAL);
		exp.addActionShortHand("p", ACTIONPUNCH);
		for(int i = 0; i < NALTS; i++){
			exp.addActionShortHand("f"+i, ACTIONFORAGE+i);
		}
		
		exp.exploreFromState(s);
		
	}
	
	
	
	public static Domain generateDomain(){
		
		if(DOMAIN != null){
			return DOMAIN;
		}
		
		DOMAIN = new SGDomain();
		
		Attribute pnAtt = new Attribute(DOMAIN, ATTPN, Attribute.AttributeType.DISC);
		pnAtt.setDiscValuesForRange(0, 1, 1);
		
		
		/*
		 * default mode: game start, non-aggressive, steal, punch
		 * who started it: game start, non-aggressive, steal, punch-p1-start, punch-p2-start
		 */
		Attribute ptAtt = new Attribute(DOMAIN, ATTPTA, Attribute.AttributeType.DISC);
		ptAtt.setDiscValuesForRange(0, 4, 1); 
		
		Attribute pitAtt = new Attribute(DOMAIN, ATTISTURN, Attribute.AttributeType.DISC);
		pitAtt.setDiscValuesForRange(0, 1, 1);
		
		Attribute faAtt = new Attribute(DOMAIN, ATTFA, Attribute.AttributeType.DISC);
		faAtt.setDiscValuesForRange(0, NALTS, 1);
		
		ObjectClass agentClass = new ObjectClass(DOMAIN, CLASSAGENT);
		agentClass.addAttribute(pnAtt);
		agentClass.addAttribute(ptAtt);
		agentClass.addAttribute(pitAtt);
		
		ObjectClass forageClass = new ObjectClass(DOMAIN, CLASSFALT);
		forageClass.addAttribute(faAtt);
		
		
		SingleAction actNOP = new UniversalSingleAction(DOMAIN, ACTIONNOP);
		SingleAction actSteal = new RootNodeSingleAction(DOMAIN, ACTIONSTEAL);
		SingleAction actPunch = new PunchSingleAction(DOMAIN, ACTIONPUNCH);
		
		//instead of using a parameterized action to objects, create an action for each forage type
		//this will be more robust to state abstractions and is permissible since there can only be 
		//one forage alt for any given type in any given state
		for(int i = 0; i < NALTS; i++){
			SingleAction actForage = new ForageSingleAction(DOMAIN, ACTIONFORAGE+i, i);
		}
		
		PropositionalFunction gopf = new GameOverPF(PFGAMEOVER, DOMAIN, "");
		
		
		return DOMAIN;
		

	}
	
	
	public static State getGameStartState(int [] falts, int playerTurn){
		
		generateDomain();
		
		State s = new State();
		ObjectInstance a1 = new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), "player0");
		ObjectInstance a2 = new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), "player1");
		
		a1.setValue(ATTPN, 0);
		a2.setValue(ATTPN, 1);
		
		a1.setValue(ATTPTA, 0);
		a2.setValue(ATTPTA, 0);
		
		if(playerTurn == 0){
			a1.setValue(ATTISTURN, 1);
			a2.setValue(ATTISTURN, 0);
		}
		else{
			a1.setValue(ATTISTURN, 0);
			a2.setValue(ATTISTURN, 1);
		}
		
		
		s.addObject(a1);
		s.addObject(a2);
		
		for(int i = 0; i < falts.length; i++){
			ObjectInstance fa = new ObjectInstance(DOMAIN.getObjectClass(CLASSFALT), "FA"+i);
			fa.setValue(ATTFA, falts[i]);
			s.addObject(fa);
		}
		
		
		return s;
	}
	
	
	public static void setForageAlternaive(State s, int fa, int t){
		ObjectInstance fao = s.getObjectsOfTrueClass(CLASSFALT).get(fa);
		setForageAlternative(fao, t);
	}
	
	public static void setForageAlternative(ObjectInstance fa, int t){
		fa.setValue(ATTFA, t);
	}
	
	public static boolean isRootNode(State s){
		
		List <ObjectInstance> agents = s.getObjectsOfTrueClass(CLASSAGENT);
		for(ObjectInstance a : agents){
			int ptv = a.getDiscValForAttribute(ATTPTA);
			if(ptv != 0){
				return false;
			}
		}
		
		return true;
	}
	
	
	public static class ForageSingleAction extends SingleAction{

		protected int forageAlt;
		
		public ForageSingleAction(SGDomain d, String name, int forageAlt){
			super(d, name);
			this.forageAlt = forageAlt;
		}
		
		
		
		@Override
		public boolean isApplicableInState(State s, String actingAgent, String [] params) {
			
			ObjectInstance agent = s.getObject(actingAgent);
			
			if(!isRootNode(s)){
				return false;
			}
			
			int pt = agent.getDiscValForAttribute(ATTISTURN);
			if(pt == 0){
				return false; //must be player's turn
			}
			
			//must have forage alt
			List<ObjectInstance> forageAlts = s.getObjectsOfTrueClass(CLASSFALT);
			for(ObjectInstance f : forageAlts){
				int fa = f.getDiscValForAttribute(ATTFA);
				if(fa == forageAlt){
					return true;
				}
			}
			
			return false;
		}

		
	}
	
	public static class RootNodeSingleAction extends SingleAction{

		public RootNodeSingleAction(SGDomain d, String name) {
			super(d, name);
		}

		@Override
		public boolean isApplicableInState(State s, String actingAgent, String [] params) {
			
			ObjectInstance agent = s.getObject(actingAgent);
			
			if(!isRootNode(s)){
				return false;
			}
			
			int pt = agent.getDiscValForAttribute(ATTISTURN);
			if(pt == 0){
				return false; //must be player's turn
			}
			
			return true;
		}
		
		
	}
	
	public static class PunchSingleAction extends SingleAction{

		public PunchSingleAction(SGDomain d, String name) {
			super(d, name);
		}

		@Override
		public boolean isApplicableInState(State s, String actingAgent, String [] params) {
			
			ObjectInstance agent = s.getObject(actingAgent);
			
			if(isRootNode(s)){
				return false; //must NOT be root node
			}
			
			int pt = agent.getDiscValForAttribute(ATTISTURN);
			if(pt == 0){
				return false; //must be player's turn
			}
			
			return true;
			
		}
		

	}

	
	
	public static class GameOverPF extends PropositionalFunction{

		public GameOverPF(String name, Domain domain, String parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			List <ObjectInstance> agents = s.getObjectsOfTrueClass(CLASSAGENT);
			for(ObjectInstance a : agents){
				int v = a.getDiscValForAttribute(ATTISTURN);
				if(v == 1){
					return false;
				}
			}
			
			return true;
		}
		
		
		
	}
	
	
}
