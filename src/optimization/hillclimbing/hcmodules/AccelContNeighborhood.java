package optimization.hillclimbing.hcmodules;

import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import optimization.hillclimbing.NeighborhoodGenerator;

public class AccelContNeighborhood implements NeighborhoodGenerator {

	protected double			baseVelocity;
	protected double			maxVelocity;
	protected double			acceleration;
	
	protected double			curVelocity;
	protected int				lastVariable;
	
	
	public AccelContNeighborhood(double bv, double mxv, double a){
		this.baseVelocity = bv;
		this.maxVelocity = mxv;
		this.acceleration = a;
		
		this.curVelocity = this.baseVelocity;
		this.lastVariable = -1;
	}
	
	
	@Override
	public List<OptVariables> neighborhood(OptVariables startPoint) {
		
		List <OptVariables> neighbors = new ArrayList<OptVariables>(startPoint.size());
		
		for(int i = 0; i < startPoint.size(); i++){
			OptVariables neighborPos = new OptVariables(startPoint.vars);
			OptVariables neighborNeg = new OptVariables(startPoint.vars);
			if(i != lastVariable){
				neighborPos.vars[i] += baseVelocity;
				neighborNeg.vars[i] -= baseVelocity;
			}
			else{
				if(curVelocity > 0){
					neighborPos.vars[i] += curVelocity;
					neighborNeg.vars[i] -= baseVelocity;
				}
				else{
					neighborPos.vars[i] += baseVelocity;
					neighborNeg.vars[i] += curVelocity; //use += because current velocity is signed
				}
			}
			
			neighbors.add(neighborPos);
			neighbors.add(neighborNeg);
		}
		
		
		return neighbors;
	}

	@Override
	public void selectedNeighbor(int i) {
		
		if(i/2 == lastVariable && ((i % 2 == 0 && curVelocity > 0) || (i % 2 == 1 && curVelocity < 0))){
			if(i % 2 == 0){
				curVelocity += acceleration;
			}
			else{
				curVelocity -= acceleration;
			}
		}
		else if(i != -1){
			if(i % 2 == 0){
				curVelocity = baseVelocity + acceleration;
			}
			else{
				curVelocity = -(baseVelocity + acceleration);
			}
			
			lastVariable = i/2;
		}
		else{
			lastVariable = -1;
		}
		
		
		//clamp velocity
		if(curVelocity > maxVelocity){
			curVelocity = maxVelocity;
		}
		
		if(curVelocity < -maxVelocity){
			curVelocity = -maxVelocity;
		}
		

	}

	@Override
	public NeighborhoodGenerator copy() {
		
		AccelContNeighborhood acn = new AccelContNeighborhood(baseVelocity, maxVelocity, acceleration);
		acn.acceleration = this.acceleration;
		acn.lastVariable = this.lastVariable;
		return acn;
	}

}
