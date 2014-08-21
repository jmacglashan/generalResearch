package behavior.training;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import auxiliary.DynamicVisualFeedbackEnvironment;
import auxiliary.StateVisualizingGUI;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.Visualizer;

public class DynamicFeedbackGUI  extends JFrame implements StateVisualizingGUI{

	private static final long serialVersionUID = 1L;
	
	protected Visualizer									painter;
	protected DynamicVisualFeedbackEnvironment				env;
	
	protected int											cWidth = 800;
	protected int											cHeight = 800;
	
	
	protected char punishKey = 'z';
	protected char rewardKey = '/';
	protected char temrinateKey = 's';
	
	public DynamicFeedbackGUI(Visualizer v, DynamicVisualFeedbackEnvironment env) {
		this.painter = v;
		this.env = env;
	}
	
	public void initGUI(){
		
		
		painter.setPreferredSize(new Dimension(cWidth, cHeight));
		getContentPane().add(painter, BorderLayout.CENTER);
		
		Container controlContainer = new Container();
		//controlContainer.setPreferredSize(new Dimension(cWidth, controlHeight));
		getContentPane().add(controlContainer, BorderLayout.SOUTH);
		GridBagLayout layout = new GridBagLayout();
		controlContainer.setLayout(layout);
		
		GridBagConstraints c = new GridBagConstraints();
		
		JButton punishB = new JButton("Punish");
		Action punishAct = new AbstractAction("punish") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("punish");
				env.setReward(-1.);
			}
		};
		punishB.addActionListener(punishAct);
		punishB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.punishKey), "punish");
		punishB.getActionMap().put("punish", punishAct);

		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(20, 0, 0, 0);
		controlContainer.add(punishB, c);
		
		
		JButton rewardB = new JButton("Reward");
		Action rewardAct = new AbstractAction("reward") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("reward");
				env.setReward(1.);
			}
		};
		rewardB.addActionListener(rewardAct);
		rewardB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.rewardKey), "reward");
		rewardB.getActionMap().put("reward", rewardAct);
		c.gridx = 2;
		c.gridy = 0;
		controlContainer.add(rewardB, c);
		
		
		
		JButton terminateB = new JButton("Terminate");
		Action terminateAct = new AbstractAction("terminate") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("terminate");
				env.setTerminal();
			}
		};
		terminateB.addActionListener(terminateAct);
		terminateB.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(this.temrinateKey), "terminate");
		terminateB.getActionMap().put("terminate", terminateAct);
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 0, 0);
		controlContainer.add(terminateB, c);
		
		

		
	}
	
	public void launch(){
		pack();
		setVisible(true);
	}


	@Override
	public void setRenderState(State s){
		painter.updateState(s);
		painter.repaint();
	}
	
}
