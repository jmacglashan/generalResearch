package collabrl.lagenerators;

import behavior.statehashing.DiscretizingStateHashFactory;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Attribute;
import collabrl.TaskAndTrain;

/**
 * @author James MacGlashan.
 */
public class TabularHashFactoryGen {

	public static StateHashFactory getHashingFactory(TaskAndTrain task){
		StateHashFactory hashingFactory = null;
		if(task.isDiscrete){
			if(task.domain.isObjectIdentifierDependent()){
				NameDependentStateHashFactory nameDepnd = new NameDependentStateHashFactory();
				hashingFactory = nameDepnd;
			}
			else {
				DiscreteMaskHashingFactory dh = new DiscreteMaskHashingFactory();
				for (String className : task.objectClassForRep) {
					dh.setAttributesForClass(className, task.domain.getObjectClass(className).attributeList);
				}
				hashingFactory = dh;
			}
		}
		else{

			DiscretizingStateHashFactory dh = new DiscretizingStateHashFactory();
			for(Attribute att : task.domain.getAttributes()){
				if(att.type == Attribute.AttributeType.REAL){
					double r = att.upperLim - att.lowerLim;
					double interval = r / 100;
					dh.addFloorDiscretizingMultipleFor(att.name, interval);
				}
			}

		}

		return hashingFactory;
	}

}
