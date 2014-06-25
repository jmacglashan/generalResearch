package tests;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import domain.singleagent.sokoban2.Sokoban2Domain;

public class JSONTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Sokoban2Domain dgen = new Sokoban2Domain();
		dgen.includeDirectionAttribute(true);
		
		Domain domain = dgen.generateDomain();
		
		State s = Sokoban2Domain.getClassicState(domain);
		StateJSONParser sp = new StateJSONParser(domain);
		
		JsonFactory jsonFactory = new JsonFactory();
		StringWriter writer = new StringWriter();
		JsonGenerator jsonGenerator;
		ObjectMapper objectMapper = new ObjectMapper();
		
		List<Map<String, Object>> data = sp.getJSONPrepared(s);
		
		Map<String, Object> messageShell = new HashMap<String, Object>();
		messageShell.put("STATE", data);
		messageShell.put("message", "Client message");
		
		try {
			jsonGenerator = jsonFactory.createGenerator(writer);
			objectMapper.writeValue(jsonGenerator, messageShell);
		} catch(Exception e){
			System.out.println("Error");
		}
		
		System.out.println(writer.toString());
		
		/*
		String str = sp.stateToString(s);
		
		System.out.println(str);
		*/

	}

}
