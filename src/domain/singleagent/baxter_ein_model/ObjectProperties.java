package domain.singleagent.baxter_ein_model;

/**
 * @author James MacGlashan.
 */
public class ObjectProperties {

	public String color;
	public String shape;

	public ObjectProperties(String color, String shape){
		this.color = color;
		this.shape = shape;
	}

	public ObjectProperties(ObjectProperties src){
		this.color = src.color;
		this.shape = src.shape;
	}

}
