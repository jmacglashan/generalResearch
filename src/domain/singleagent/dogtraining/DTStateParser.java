package domain.singleagent.dogtraining;

import java.util.List;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.objects.MutableObjectInstance;
import burlap.oomdp.core.states.MutableState;

public class DTStateParser implements StateParser {

	protected Domain		domain;
	
	public DTStateParser(Domain domain){
		this.domain = domain;
	}
	
	
	@Override
	public String stateToString(State s) {
		
		StringBuffer buf = new StringBuffer();
		

		List<ObjectInstance> locs = s.getObjectsOfClass(DogTraining.CLASSLOCATION);
		for(ObjectInstance loc : locs){
			buf.append("<loc>,").append(loc.getIntValForAttribute(DogTraining.ATTX)).append(",").append(loc.getIntValForAttribute(DogTraining.ATTY)).append(",");
			buf.append(loc.getStringValForAttribute(DogTraining.ATTLOCID)).append(" ");
		}
		
		List<ObjectInstance> toys = s.getObjectsOfClass(DogTraining.CLASSTOY);
		for(ObjectInstance toy : toys){
			buf.append("<toy>,").append(toy.getIntValForAttribute(DogTraining.ATTX)).append(",").append(toy.getIntValForAttribute(DogTraining.ATTY)).append(" ");
		}
		
		
		//do dog last since there must always be one and can end on with without a ' '
		ObjectInstance dog = s.getObjectsOfClass(DogTraining.CLASSDOG).get(0);
		buf.append("<dog>,").append(dog.getIntValForAttribute(DogTraining.ATTX)).append(",").append(dog.getIntValForAttribute(DogTraining.ATTY)).append(",");
		buf.append(dog.getIntValForAttribute(DogTraining.ATTLOOKING)).append(",").append(dog.getIntValForAttribute(DogTraining.ATTHOLDING)).append(",");
		buf.append(dog.getIntValForAttribute(DogTraining.ATTWAITING));
		
		
		return buf.toString();
	}

	@Override
	public State stringToState(String str) {
		State s = new MutableState();
		
		String [] objects = str.split(" ");
		int nl = 0;
		int nt = 0;
		
		for(String ostr : objects){
			
			String [] comps = ostr.split(",");
			if(comps[0].equals("<loc>")){
				s.addObject(this.locationObject(comps, nl));
				nl++;
			}
			else if(comps[0].equals("<toy>")){
				s.addObject(this.toyObject(comps, nt));
				nt++;
			}
			else if(comps[0].equals("<dog>")){
				s.addObject(this.dogObject(comps));
			}
			
		}
		
		return s;
	}
	
	protected ObjectInstance locationObject(String [] comps, int i){
		
		ObjectInstance o = new MutableObjectInstance(domain.getObjectClass(DogTraining.CLASSLOCATION), DogTraining.CLASSLOCATION+i);
		o.setValue(DogTraining.ATTX, Integer.parseInt(comps[1]));
		o.setValue(DogTraining.ATTY, Integer.parseInt(comps[2]));
		o.setValue(DogTraining.ATTLOCID, comps[3]);
		
		return o;
	}
	
	protected ObjectInstance toyObject(String [] comps, int i){
		
		ObjectInstance o = new MutableObjectInstance(domain.getObjectClass(DogTraining.CLASSTOY), DogTraining.CLASSTOY+i);
		o.setValue(DogTraining.ATTX, Integer.parseInt(comps[1]));
		o.setValue(DogTraining.ATTY, Integer.parseInt(comps[2]));
		
		return o;
	}
	
	protected ObjectInstance dogObject(String [] comps){
		
		if(comps.length != 6){
			for(String s : comps){
				System.out.print(s + " ");
			}
			System.out.println("");
		}
		
		ObjectInstance o = new MutableObjectInstance(domain.getObjectClass(DogTraining.CLASSDOG), DogTraining.CLASSDOG+0);
		o.setValue(DogTraining.ATTX, Integer.parseInt(comps[1]));
		o.setValue(DogTraining.ATTY, Integer.parseInt(comps[2]));
		o.setValue(DogTraining.ATTLOOKING, Integer.parseInt(comps[3]));
		o.setValue(DogTraining.ATTHOLDING, Integer.parseInt(comps[4]));
		o.setValue(DogTraining.ATTWAITING, Integer.parseInt(comps[5]));
		
		return o;
	}

}
