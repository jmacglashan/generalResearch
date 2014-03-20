package tests.sync;

public class AgentServer {

	protected MiddleMan mm;
	
	
	public AgentServer(MiddleMan mm){
		this.mm = mm;
	}
	
	public void start(String obsv){
		
		int i = 0;
		while(true){
			String act = "act:" + i;
			this.mm.agentRequest(act);
			i++;
		}
		
	}
	
}
