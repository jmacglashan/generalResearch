package behavior.training.taskinduction.sabl;

import behavior.training.taskinduction.strataware.FeedbackStrategy;

/**
 * @author James MacGlashan.
 */
public class FeedbackStrategyProbPair {

	public FeedbackStrategy strategy;
	public double prior;

	public FeedbackStrategyProbPair(FeedbackStrategy strategy, double prior) {
		this.strategy = strategy;
		this.prior = prior;
	}

	public FeedbackStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(FeedbackStrategy strategy) {
		this.strategy = strategy;
	}

	public double getPrior() {
		return prior;
	}

	public void setPrior(double prior) {
		this.prior = prior;
	}
}
