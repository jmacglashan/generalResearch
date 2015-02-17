package commands.tests;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;
import domain.singleagent.sokoban2.Sokoban2Domain;
import domain.singleagent.sokoban2.Sokoban2Visualizer;

/**
 * @author James MacGlashan.
 */
public class SokoStateTester {

	public static void main(String[] args) {

		Sokoban2Domain dg = new Sokoban2Domain();
		Domain domain = dg.generateDomain();

		Visualizer v = Sokoban2Visualizer.getVisualizer("oomdpResearch/robotImages");

		State s = star2Teal_3(domain);

		VisualExplorer exp = new VisualExplorer(domain, v, s);
		exp.addKeyAction("w", Sokoban2Domain.ACTIONNORTH);
		exp.addKeyAction("s", Sokoban2Domain.ACTIONSOUTH);
		exp.addKeyAction("d", Sokoban2Domain.ACTIONEAST);
		exp.addKeyAction("a", Sokoban2Domain.ACTIONWEST);
		exp.initGUI();

	}


	/*
	orange -> red
	tan -> green
	teal -> blue
	 */

	/**
	 * Goal is agentInRoom(agent0, room1)
	 * @param domain
	 * @return
	 */
	public static State agent2Organge_1(Domain domain){

		//goal is agentInRoom(agent0, room1)

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);
		setRoomLayout1(s);

		Sokoban2Domain.setAgent(s, 13, 9);

		return s;
	}


	/**
	 * Goal is agentInRoom(agent0, room0)
	 * @param domain
	 * @return
	 */
	public static State agent2Organge_2(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 12, 4, "chair", "yellow");

		Sokoban2Domain.setAgent(s, 13, 9);

		return s;
	}


	/**
	 * Goal is agentInRoom(agent0, room2)
	 * @param domain
	 * @return
	 */
	public static State agent2Organge_3(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);

		setRoomLayout3(s);

		Sokoban2Domain.setAgent(s, 14, 5);

		return s;

	}


	/**
	 * Goal is agentInRoom(agent0, room2)
	 * @param domain
	 * @return
	 */
	public static State agent2Tan_1(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);
		setRoomLayout1(s);

		Sokoban2Domain.setAgent(s, 4, 3);

		return s;
	}


	/**
	 * Goal is agentInRoom(agent0, room1)
	 * @param domain
	 * @return
	 */
	public static State agent2Tan_2(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);
		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 12, 4, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 4, 3);


		return s;
	}



	/**
	 * Goal is agentInRoom(agent0, room0)
	 * @param domain
	 * @return
	 */
	public static State agent2Tan_3(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);
		setRoomLayout3(s);

		Sokoban2Domain.setAgent(s, 2, 3);

		return s;
	}




	/**
	 * Goal is agentInRoom(agent0, room0)
	 * @param domain
	 * @return
	 */
	public static State agent2Teal_1(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);
		setRoomLayout1(s);

		Sokoban2Domain.setAgent(s, 4, 3);

		return s;
	}


	/**
	 * Goal is agentInRoom(agent0, room2)
	 * @param domain
	 * @return
	 */
	public static State agent2Teal_2(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);
		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 12, 9, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 4, 3);


		return s;
	}



	/**
	 * Goal is agentInRoom(agent0, room1)
	 * @param domain
	 * @return
	 */
	public static State agent2Teal_3(Domain domain){

		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 0);
		setRoomLayout3(s);

		Sokoban2Domain.setAgent(s, 3, 3);

		return s;
	}




	/**
	 * Goal is blockInRoom(block0, room1)
	 * @param domain
	 * @return
	 */
	public static State star2Organge_1(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout1(s);

		Sokoban2Domain.setBlock(s, 0, 12, 9, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 10, 9);

		return s;
	}










	/**
	 * Goal is blockInRoom(block0, room0)
	 * @param domain
	 * @return
	 */
	public static State star2Organge_2(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 12, 4, "chair", "yellow");

		Sokoban2Domain.setAgent(s, 13, 9);

		return s;
	}

	/**
	 * Goal is blockInRoom(block0, room2)
	 * @param domain
	 * @return
	 */
	public static State star2Organge_3(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout3(s);

		Sokoban2Domain.setBlock(s, 0, 8, 4, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 13, 2);

		return s;
	}




	/**
	 * Goal is blockInRoom(block0, room2)
	 * @param domain
	 * @return
	 */
	public static State star2Tan_1(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout1(s);

		Sokoban2Domain.setBlock(s, 0, 3, 3, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 9, 4);

		return s;
	}




	/**
	 * Goal is blockInRoom(block0, room1)
	 * @param domain
	 * @return
	 */
	public static State star2Tan_2(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 11, 4, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 3, 3);

		return s;
	}

	/**
	 * Goal is blockInRoom(block0, room0)
	 * @param domain
	 * @return
	 */
	public static State star2Tan_3(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout3(s);

		Sokoban2Domain.setBlock(s, 0, 3, 4, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 13, 2);

		return s;
	}



	/**
	 * Goal is blockInRoom(block0, room0)
	 * @param domain
	 * @return
	 */
	public static State star2Teal_1(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout1(s);

		Sokoban2Domain.setBlock(s, 0, 3, 3, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 9, 4);

		return s;
	}




	/**
	 * Goal is blockInRoom(block0, room2)
	 * @param domain
	 * @return
	 */
	public static State star2Teal_2(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout2(s);

		Sokoban2Domain.setBlock(s, 0, 11, 9, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 3, 3);

		return s;
	}

	/**
	 * Goal is blockInRoom(block0, room1)
	 * @param domain
	 * @return
	 */
	public static State star2Teal_3(Domain domain){
		State s = Sokoban2Domain.getCleanState(domain, 3, 2, 1);

		setRoomLayout3(s);

		Sokoban2Domain.setBlock(s, 0, 3, 4, "chair", "yellow");
		Sokoban2Domain.setAgent(s, 13, 2);

		return s;
	}




	/**
	 * room0: blue
	 * room1: red
	 * room2: green
	 * @param s
	 */
	public static void setRoomLayout1(State s){

		Sokoban2Domain.setRoom(s, 0, 6, 8, 0, 15, "blue");
		Sokoban2Domain.setRoom(s, 1, 6, 0, 0, 8, "red");
		Sokoban2Domain.setRoom(s, 2, 11, 8, 6, 15, "green");


		Sokoban2Domain.setSingleCellDoor(s, 0, 8 ,2);
		Sokoban2Domain.setSingleCellDoor(s, 1, 10 ,6);

	}


	/**
	 * room0: red
	 * room1: green
	 * room2: blue
	 * @param s
	 */
	public static void setRoomLayout2(State s){

		Sokoban2Domain.setRoom(s, 2, 6, 8, 0, 15, "blue");
		Sokoban2Domain.setRoom(s, 0, 6, 0, 0, 8, "red");
		Sokoban2Domain.setRoom(s, 1, 11, 8, 6, 15, "green");


		Sokoban2Domain.setSingleCellDoor(s, 0, 8 ,2);
		Sokoban2Domain.setSingleCellDoor(s, 1, 10 ,6);

	}


	/**
	 * room0: green
	 * room1: blue
	 * room2: red
	 * @param s
	 */
	public static void setRoomLayout3(State s){
		Sokoban2Domain.setRoom(s, 2, 6, 0, 0, 6, "red");
		Sokoban2Domain.setRoom(s, 0, 6, 6, 0, 12, "green");
		Sokoban2Domain.setRoom(s, 1, 6, 12, 0, 18, "blue");

		Sokoban2Domain.setSingleCellDoor(s, 0 , 6, 2);
		Sokoban2Domain.setSingleCellDoor(s, 1 , 12, 4);
	}




}
