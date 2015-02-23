package behavior.burlapirlext;

/**
 * @author James MacGlashan.
 */
public interface VisitationRewardBonus {
	public double bonus(double visitation);

	public static class ThresholdVisitationBonus implements VisitationRewardBonus{

		protected double bonus;
		protected double threshold;

		public ThresholdVisitationBonus(double bonus, double threshold){
			this.bonus = bonus;
			this.threshold = threshold;
		}

		@Override
		public double bonus(double visitation) {
			return visitation < this.threshold ? this.bonus : 0.;
		}
	}

	public static class InverseRatioThresholdBondus implements  VisitationRewardBonus{

		protected double baseBonus;
		protected double inverseConstant;

		public InverseRatioThresholdBondus(double baseBonus, double inverseConstant) {
			this.baseBonus = baseBonus;
			this.inverseConstant = inverseConstant;
		}

		@Override
		public double bonus(double visitation) {
			double ratio = this.inverseConstant / (this.inverseConstant + visitation);
			return ratio * baseBonus;
		}
	}
}
