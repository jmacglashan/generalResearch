package rosbaxter;

import com.fasterxml.jackson.databind.JsonNode;
import ros.RosBridge;
import ros.RosListenDelegate;

import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class RosBaxterTest {

	public static void main(String[] args) {

		RosBridge rosBridge = RosBridge.createConnection("ws://octopus:9090");
		System.out.println("Waiting for connection.");
		rosBridge.waitForConnection();
		System.out.println("Finished connecting.");


		RosListenDelegate delegate = new RosListenDelegate() {

			@Override
			public void receive(JsonNode data, String stringRep) {

				StringBuilder buf = new StringBuilder();
				JsonNode objects = data.get("msg").get("objects");
				for(int i = 0; i < objects.size(); i++){
					JsonNode ob = objects.get(i);
					JsonNode innerPosition = ob.get("pose").get("pose").get("pose").get("position");
					Pos pos = new Pos(innerPosition.get("x").asDouble(),
							innerPosition.get("y").asDouble(),
							innerPosition.get("z").asDouble());
					buf.append(ob.get("type").get("key").asText() + " " + pos.toString() + "; ");
				}

				System.out.println(buf.toString());

			}

		};
		rosBridge.subscribe("/ein_left/state", "ein/EinState", delegate, 1, 1);

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

}
