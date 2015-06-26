package tests;

import java.util.Arrays;

/**
 * @author James MacGlashan.
 */
public class ArrayCopy {

	public static void main(String[] args) {
		double [] a1 = new double[]{1., 2.};
		System.out.println(Arrays.toString(a1));
		double [] a2 = a1.clone();
		a1[0] = 2.;

		System.out.println("--");
		System.out.println(Arrays.toString(a1));
		System.out.println(Arrays.toString(a2));
	}

}
