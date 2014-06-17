package ethics.experiments.fssimple;

import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.tournament.common.ConstantWorldGenerator;
import domain.stocasticgames.foragesteal.simple.FSSimpleTerminatingJAM;
import ethics.experiments.fssimple.auxiliary.FSSimpleSG;

public class SimpleMatchTermVis extends SimpleMatchVisualizer {

	
	private static final long serialVersionUID = 1L;


	public SimpleMatchTermVis(){
		super();
		
		JointActionModel jam = new FSSimpleTerminatingJAM();
		
		this.worldGenerator = new ConstantWorldGenerator(domain, jam, this.objectiveRF, new FSSimpleTerminatingJAM.FSSimpleTerminatingTF(), new FSSimpleSG(domain));
		this.numMatches = 1000;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleMatchTermVis smv = new SimpleMatchTermVis();
		smv.initGUI();
		smv.pack();
		smv.setVisible(true);

	}

}
