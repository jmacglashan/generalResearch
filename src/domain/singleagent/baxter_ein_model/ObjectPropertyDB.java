package domain.singleagent.baxter_ein_model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class ObjectPropertyDB {

	public Map<String, ObjectProperties> db = new HashMap<String, ObjectProperties>();

	public ObjectProperties getProperties(String objectType){
		return this.db.get(objectType);
	}

	public void setProperties(String objectType, ObjectProperties properties){
		this.db.put(objectType, properties);
	}

	public ObjectPropertyDB duplicate(){
		ObjectPropertyDB ndb = new ObjectPropertyDB();
		for(Map.Entry<String, ObjectProperties> e : this.db.entrySet()){
			ndb.setProperties(e.getKey(), new ObjectProperties(e.getValue()));
		}
		return ndb;
	}

}
