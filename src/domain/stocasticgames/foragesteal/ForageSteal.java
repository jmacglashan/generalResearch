package domain.stocasticgames.foragesteal;

import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stocashticgames.SGDomain;
import burlap.oomdp.stocashticgames.SingleAction;
import burlap.oomdp.stocashticgames.common.UniversalSingleAction;

public class ForageSteal {

	public static final String				ATTPN = "playerNum";
	public static final String				ATTFA = "forageAlternative";
	
	public static final String				CLASSAGENT = "agent";
	public static final String				CLASSFALT = "forageAlternative";
	
	public static final String				ACTIONNOP = "noop";
	public static final String				ACTIONSTEAL = "steal";
	public static final String				ACTIONPUNISH = "punish";
	public static final String				ACTIONFORAGE = "forage";
	
	
	public static final int					NALTS = 3;
	
	
	private static SGDomain					DOMAIN = null;
	
	
	
	public static Domain generateDomain(){
		if(DOMAIN != null){
			return DOMAIN;
		}
		
		DOMAIN = new SGDomain();
		
		Attribute pnAtt = new Attribute(DOMAIN, ATTPN, Attribute.AttributeType.DISC);
		pnAtt.setDiscValuesForRange(0, 1, 1);
		
		Attribute faAtt = new Attribute(DOMAIN, ATTFA, Attribute.AttributeType.DISC);
		faAtt.setDiscValuesForRange(0, NALTS, 1);
		
		ObjectClass agentClass = new ObjectClass(DOMAIN, CLASSAGENT);
		agentClass.addAttribute(pnAtt);
		
		ObjectClass forageClass = new ObjectClass(DOMAIN, CLASSFALT);
		forageClass.addAttribute(faAtt);
		
		
		SingleAction actNOP = new UniversalSingleAction(DOMAIN, ACTIONNOP);
		SingleAction actSteal = new UniversalSingleAction(DOMAIN, ACTIONSTEAL);
		SingleAction actPunish = new UniversalSingleAction(DOMAIN, ACTIONPUNISH);
		SingleAction actForage = new UniversalSingleAction(DOMAIN, ACTIONFORAGE, new String []{CLASSFALT});
		
		
		return DOMAIN;
		
	}
	
	
	
	public static State getCleanState(int nf){
		
		generateDomain();
		
		State s = new State();
		ObjectInstance a1 = new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), "player0");
		ObjectInstance a2 = new ObjectInstance(DOMAIN.getObjectClass(CLASSAGENT), "player1");
		
		a1.setValue(ATTPN, 0);
		a2.setValue(ATTPN, 1);
		
		s.addObject(a1);
		s.addObject(a2);
		
		for(int i = 0; i < nf; i++){
			ObjectInstance fa = new ObjectInstance(DOMAIN.getObjectClass(CLASSFALT), "FA"+i);
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
	
	
	
}
