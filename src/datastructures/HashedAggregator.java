package datastructures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashedAggregator <K>{

	protected HashMap<K, Double>		storage;
	protected double					initialValue = 0.;
	
	public HashedAggregator(){
		this.storage = new HashMap<K, Double>();
	}
	
	public HashedAggregator(double initialValue){
		this.storage = new HashMap<K, Double>();
		this.initialValue = initialValue;
	}
	
	public void add(K ind, double v){
		Double cur = storage.get(ind);
		double c = cur != null ? cur : initialValue;
		this.storage.put(ind, c+v);
	}
	
	public double v(K ind){
		Double cur = storage.get(ind);
		double c = cur != null ? cur : initialValue;
		return c;
	}
	
	public int size(){
		return storage.size();
	}
	
	public Set<K> keySet(){
		return this.storage.keySet();
	}
	
	public Collection<Double> valueSet(){
		return this.storage.values();
	}
	
	public Set<Map.Entry<K, Double>> entrySet(){
		return this.storage.entrySet();
	}
	
	public Map<K, Double> getHashMap(){
		return this.storage;
	}
	
}
