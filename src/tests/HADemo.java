package tests;

import datastructures.HashedAggregator;

import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class HADemo {


	public static void main(String[] args) {
		HashedAggregator<Integer> ha = new HashedAggregator<Integer>(0.);
		ha.add(4, 0.2);
		ha.add(9, 0.05);
		ha.add(4, 0.4);

		for(Map.Entry<Integer, Double> e : ha.entrySet()){
			System.out.println(e.getKey() + " " + e.getValue());
		}
	}
}
