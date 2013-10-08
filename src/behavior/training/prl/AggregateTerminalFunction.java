package behavior.training.prl;

import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;

public class AggregateTerminalFunction implements TerminalFunction {

	List<TerminalFunction> tfs;
	
	public AggregateTerminalFunction() {
		this.tfs = new ArrayList<TerminalFunction>();
	}
	
	public void addTerminalFunction(TerminalFunction tf){
		this.tfs.add(tf);
	}

	@Override
	public boolean isTerminal(State s) {
		
		for(TerminalFunction tf : tfs){
			if(tf.isTerminal(s)){
				return true;
			}
		}
		
		return false;
	}

}
