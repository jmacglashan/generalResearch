package ethics.experiments.tbforagesteal.matchvisualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import ethics.experiments.tbforagesteal.matchvisualizer.ColoredTableRenderer.ColoredTableCell;
import ethics.experiments.tbforagesteal.matchvisualizer.ColoredTableRenderer.TableLabel;

public class QTableVisController {

	ColoredTableRenderer						renderer;
	MatchAnalizer								ma = null;
	String										agent0Name;
	String										agent1Name;
	
	JointReward									objectiveRF;
	JointReward									subjectiveRFA0;
	JointReward									subjectiveRFA1;
	
	List <StateActionSetTuple> 					agent0QueryStates;
	List <StateActionSetTuple> 					agent1QueryStates;
	
	float										stateGroupHGap = 60f;
	float										cellWidth = 60f;
	int											cellFontPointSize = 16;
	int											colLabelFontPointSize = 14;
	
	
	public QTableVisController(int rw, int rh, JointReward objectiveRF) {
		renderer = new ColoredTableRenderer(3);
		renderer.setPreferredSize(new Dimension(rw, rh));
		this.objectiveRF = objectiveRF;
		
	}
	
	
	public ColoredTableRenderer getRenderer(){
		return this.renderer;
	}
	
	public void setQueryState(List <StateActionSetTuple> agent0Query, List <StateActionSetTuple> agent1Query){
		this.agent0QueryStates = agent0Query;
		this.agent1QueryStates = agent1Query;
	}
	
	public void setMatchAnalyzer(MatchAnalizer ma){
		this.ma = ma;
	}
	
	public void setSubjectiveRFs(JointReward subrf0, JointReward subrf1){
		this.subjectiveRFA0 = subrf0;
		this.subjectiveRFA1 = subrf1;
	}
	
	public void setAgentNames(String agent0Name, String agent1Name){
		this.agent0Name = agent0Name;
		this.agent1Name = agent1Name;
	}
	
	public void setupTable(){
		
		double minValue = -2.;
		double maxValue = 3.;
		
		LandmarkColorBlendInterpolation colorBlend = new LandmarkColorBlendInterpolation();
		colorBlend.addNextLandMark(minValue, Color.red);
		colorBlend.addNextLandMark(0., Color.white);
		colorBlend.addNextLandMark(maxValue, Color.blue);
		
		float lm = 30;
		float tm = 50;
		float tlm = 60;
		float llm = 100;
		
		float startX = lm+llm;
		float startY = tm;
		
		
		TableLabel a0Label = new TableLabel("Agent 0", 24, 60, (int)startY);
		renderer.addLabel(a0Label);
		
		this.setupTableColumn(agent0QueryStates, startX, startY);
		
		startY += tlm;
		
		String baseName = "a0Q";
		TableLabel a0QRowLabel = new TableLabel("Q-values", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0QRowLabel);
		this.setupTableRow(baseName, agent0QueryStates, colorBlend, startX, startY);
		
		startY += cellWidth;
		baseName = "a0SR";
		TableLabel a0SRRowLabel = new TableLabel("Subjective RF", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0SRRowLabel);
		this.setupTableRow(baseName, agent0QueryStates, colorBlend, startX, startY);
		
		startY += cellWidth;
		baseName = "a0OR";
		TableLabel a0ORRowLabel = new TableLabel("Objective RF", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a0ORRowLabel);
		this.setupTableRow(baseName, agent0QueryStates, colorBlend, startX, startY);
		
		
		
		startY += cellWidth*2;
		
		TableLabel a1Label = new TableLabel("Agent 1", 24, 60, (int)startY);
		renderer.addLabel(a1Label);
		
		this.setupTableColumn(agent1QueryStates, startX, startY);
		
		startY += tlm;
		
		baseName = "a1Q";
		TableLabel a1QRowLabel = new TableLabel("Q-values", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1QRowLabel);
		this.setupTableRow(baseName, agent1QueryStates, colorBlend, startX, startY);
		
		startY += cellWidth;
		baseName = "a1SR";
		TableLabel a1SRRowLabel = new TableLabel("Subjective RF", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1SRRowLabel);
		this.setupTableRow(baseName, agent1QueryStates, colorBlend, startX, startY);
		
		startY += cellWidth;
		baseName = "a1OR";
		TableLabel a1ORRowLabel = new TableLabel("Objective RF", colLabelFontPointSize, (int)(lm + (llm/2)), (int)(startY+(cellWidth/2)));
		renderer.addLabel(a1ORRowLabel);
		this.setupTableRow(baseName, agent1QueryStates, colorBlend, startX, startY);
		
		
	}
	
	
	public void changeDisplayedQValues(int stage){
		this.changeDisplayedQValues(0, agent0Name, stage);
		this.changeDisplayedQValues(1, agent1Name, stage);
		
		renderer.repaint();
	}
	
	public void updateRFValues(JointReward a0rf, JointReward a1rf){
		this.setSubjectiveRFs(a0rf, a1rf);
		
		this.setupRFValues(agent0QueryStates, agent0Name, "a0OR", objectiveRF);
		this.setupRFValues(agent1QueryStates, agent1Name, "a1OR", objectiveRF);
		
		this.setupRFValues(agent0QueryStates, agent0Name, "a0SR", subjectiveRFA0);
		this.setupRFValues(agent1QueryStates, agent1Name, "a1SR", subjectiveRFA1);
		
		
	}
	
	
	protected void setupRFValues(List <StateActionSetTuple> sas, String aname, String baseName, JointReward rf){
		for(StateActionSetTuple t : sas){
			for(int i = 0; i < t.gas.size(); i++){
				String al = t.actionLabels.get(i);
				GroundedSingleAction ga = t.gas.get(i);
				JointAction ja = new JointAction();
				ja.addAction(ga);
				Map <String, Double> rs = rf.reward(t.s, ja, null);
				double r = rs.get(aname);
				String cellname = baseName + t.labelName + al;
				renderer.setCellValue(cellname, r);
			}
		}
	}
	
	protected void changeDisplayedQValues(int aid, String aName, int stage){
		
		String baseName = "a"+aid+"Q";
		
		List <StateActionSetTuple> sas = agent0QueryStates;
		if(aid == 1){
			sas = agent1QueryStates;
		}
		
		for(StateActionSetTuple t : sas){
			for(int i = 0; i < t.gas.size(); i++){
				String al = t.actionLabels.get(i);
				GroundedSingleAction ga = t.gas.get(i);
				String cellname = baseName + t.labelName + al;
				
				double q = this.ma.getQFor(aid, stage, t.s, ga);
				renderer.setCellValue(cellname, q);
				
			}
			
		}
		
		
	}
	
	protected void setupTableColumn(List <StateActionSetTuple> sas, float startX, float startY){
		
		float ly = startY + (cellWidth / 2);
		
		for(StateActionSetTuple t : sas){
			
			for(int i = 0; i < t.gas.size(); i++){
				String al = t.actionLabels.get(i);
				
				TableLabel s0n = new TableLabel(t.labelName + "-" + al, colLabelFontPointSize, (int)(startX+(cellWidth/2)), (int)ly);
				renderer.addLabel(s0n);
				startX += cellWidth;
				
			}
			
			startX += stateGroupHGap;
			
		}
		
	}
	
	protected void setupTableRow(String baseName, List <StateActionSetTuple> sas, ColorBlend colorBlend, float startX, float startY){
		
		for(StateActionSetTuple t : sas){
			
			for(int i = 0; i < t.gas.size(); i++){
				String al = t.actionLabels.get(i);
				
				String cellName = baseName + t.labelName + al;
				ColoredTableCell cell = new ColoredTableCell(colorBlend, startX, startY, cellWidth, cellWidth, this.cellFontPointSize);
				renderer.putColorCell(cellName, cell);
				
				startX += cellWidth;
				
			}
			
			startX += stateGroupHGap;
			
		}
		
		
	}
	
	
	
	public static class StateActionSetTuple{
		
		public State s;
		public String labelName;
		public List <GroundedSingleAction> gas;
		public List <String> actionLabels;
		
		public StateActionSetTuple(State s, String slabel){
			this.s = s;
			this.labelName = slabel;
			this.gas = new ArrayList<GroundedSingleAction>();
			this.actionLabels = new ArrayList<String>();
		}
		
		public void addAction(GroundedSingleAction ga, String actionLabel){
			this.gas.add(ga);
			this.actionLabels.add(actionLabel);
		}
		
		
	}

}
