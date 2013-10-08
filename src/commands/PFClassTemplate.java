package commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;

public class PFClassTemplate {

	public String		className;
	public String []	parameterClasses;
	public String []	parameterOrderGroups;
	
	
	public PFClassTemplate(String className, String [] parameterClasses, String [] parameterOrderGroups){
		this.className = className;
		this.parameterClasses = parameterClasses;
		this.parameterOrderGroups = parameterOrderGroups;
	}
	
	public PFClassTemplate(PropositionalFunction pf){
		this.className = pf.getClassName();
		this.parameterClasses = pf.getParameterClasses();
		this.parameterOrderGroups = pf.getParameterOrderGroups();
	}
	
	public static Set <PFClassTemplate> getPFClasses(Domain d){
		
		Set <PFClassTemplate> res = new HashSet<PFClassTemplate>();
		List<PropositionalFunction> pfs = d.getPropFunctions();
		
		for(PropositionalFunction pf : pfs){
			PFClassTemplate temp = new PFClassTemplate(pf);
			res.add(temp);
		}
		
		return res;
		
	}
	
	public static PFClassTemplate getPFClassTemplateWithName(Domain d, String name){
		
		PFClassTemplate res = null;
		
		List<PropositionalFunction> pfs = d.getPropFunctions();
		
		for(PropositionalFunction pf : pfs){
			if(pf.getClassName().equals(name)){
				res = new PFClassTemplate(pf);
				break;
			}
		}
		
		return res;
		
	}

	
	
	@Override
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		
		if(!(other instanceof PFClassTemplate)){
			return false;
		}
		
		PFClassTemplate that = (PFClassTemplate)other;
		
		return this.className.equals(that.className);
	}
	
	@Override
	public int hashCode(){
		return this.className.hashCode();
	}
	
}
