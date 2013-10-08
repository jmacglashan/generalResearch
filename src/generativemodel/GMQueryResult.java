package generativemodel;

public class GMQueryResult extends GMQuery{
	public double							probability;
	
	public GMQueryResult(){
		super();
	}
	
	public GMQueryResult(double p){
		super();
		this.probability = p;
	}
	
	public GMQueryResult(GMQuery superSrc){
		super(superSrc);
	}
	
	public GMQueryResult(GMQuery superSrc, double p){
		super(superSrc);
		this.probability = p;
	}
	
	
	
}
