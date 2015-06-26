package rosbaxter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author James MacGlashan.
 */
public class JsonTest {

	public static void main(String[] args) {

		String jsonCode = "{\"topic\": \"/ein_right/state\", \"msg\": {\"place_mode\": 0, \"patrol_state\": 0, \"movement_state\": 2, \"objects\": [{\"confidence\": 0.0, \"bounding_mesh\": {\"vertices\": [], \"triangles\": []}, \"pose\": {\"header\": {\"stamp\": {\"secs\": 0, \"nsecs\": 0}, \"frame_id\": \"\", \"seq\": 0}, \"pose\": {\"pose\": {\"position\": {\"y\": -0.5671439537667331, \"x\": 0.6602607612407337, \"z\": 0.020715147482850055}, \"orientation\": {\"y\": 0.0, \"x\": 0.0, \"z\": 0.0, \"w\": 0.0}}, \"covariance\": [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]}}, \"header\": {\"stamp\": {\"secs\": 1434752444, \"nsecs\": 792283307}, \"frame_id\": \"/base\", \"seq\": 0}, \"bounding_contours\": [], \"type\": {\"db\": \"\", \"key\": \"blueBowl\"}, \"point_clouds\": []}, {\"confidence\": 0.0, \"bounding_mesh\": {\"vertices\": [], \"triangles\": []}, \"pose\": {\"header\": {\"stamp\": {\"secs\": 0, \"nsecs\": 0}, \"frame_id\": \"\", \"seq\": 0}, \"pose\": {\"pose\": {\"position\": {\"y\": -0.36179115577491405, \"x\": 0.7232495150115339, \"z\": 0.019804878051232655}, \"orientation\": {\"y\": 0.0, \"x\": 0.0, \"z\": 0.0, \"w\": 0.0}}, \"covariance\": [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]}}, \"header\": {\"stamp\": {\"secs\": 1434752444, \"nsecs\": 792283307}, \"frame_id\": \"/base\", \"seq\": 0}, \"bounding_contours\": [], \"type\": {\"db\": \"\", \"key\": \"whiteSpoon\"}, \"point_clouds\": []}, {\"confidence\": 0.0, \"bounding_mesh\": {\"vertices\": [], \"triangles\": []}, \"pose\": {\"header\": {\"stamp\": {\"secs\": 0, \"nsecs\": 0}, \"frame_id\": \"\", \"seq\": 0}, \"pose\": {\"pose\": {\"position\": {\"y\": -0.6454408828086026, \"x\": 0.42958703205387294, \"z\": 0.020360021434912362}, \"orientation\": {\"y\": 0.0, \"x\": 0.0, \"z\": 0.0, \"w\": 0.0}}, \"covariance\": [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]}}, \"header\": {\"stamp\": {\"secs\": 1434752444, \"nsecs\": 792283307}, \"frame_id\": \"/base\", \"seq\": 0}, \"bounding_contours\": [], \"type\": {\"db\": \"\", \"key\": \"greenSpoon\"}, \"point_clouds\": []}], \"zero_g\": 0, \"patrol_mode\": 0, \"idle_mode\": 3, \"stack\": [\"2\"]}, \"op\": \"publish\"}\n";

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = null;

		try {
			node = mapper.readTree(jsonCode);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		JsonNode msg = node.get("msg");
		Iterator <String> msgFields = msg.fieldNames();
		while(msgFields.hasNext()){
			String field = msgFields.next();
			if(!field.equals("objects")) {
				System.out.println(field + ": " + msg.get(field).toString());
			}
		}
		System.out.println("---");

		JsonNode objects = msg.get("objects");
		System.out.println(objects.size());
		for(int i = 0; i < objects.size(); i++){
			System.out.println(objects.get(i).toString());
			System.out.println();
		}

		System.out.println("--");

		for(int i = 0; i < objects.size(); i++) {
			Iterator<String> fields = objects.get(i).fieldNames();
			while(fields.hasNext()) {
				System.out.print(fields.next() + " ");
			}
			System.out.println();
		}

	}

}
