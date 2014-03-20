package tests.sync;

public class MiddleMan {

	protected final MyMessage actMsg = new MyMessage();
	protected final MyMessage obsvMsg = new MyMessage();
	
	
	public String start(String obsv){
		
		//starts agent with obsv, no need to wait for obsv at start!		
		final String fobsv = obsv;
		Thread aThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				AgentServer as = new AgentServer(MiddleMan.this);
				as.start(fobsv);
				
			}
		});
		aThread.start();
		
		
		String toRet = null;
		//wait for first aciton though
		synchronized (actMsg) {
			while(actMsg.message.equals("")){
				try{
					System.out.println("start method waiting...");
					actMsg.wait();
				} catch(InterruptedException ex){
					ex.printStackTrace();
				}
			}
			toRet = actMsg.message;
			actMsg.message = "";
		}
		
		return toRet;
	}
	
	public String envRequest(String obsv){
		
		
		synchronized (obsvMsg) {
			obsvMsg.message = obsv;
			obsvMsg.notifyAll();
		}
		
		
		
		String toRet = null;
		//wait for aciton response to observation
		synchronized (actMsg) {
			while(actMsg.message.equals("")){
				try{
					System.out.println("env method waiting...");
					actMsg.wait();
				} catch(InterruptedException ex){
					ex.printStackTrace();
				}
			}
			toRet = actMsg.message;
			actMsg.message = "";
		}
		
		return toRet;
	}
	
	public String agentRequest(String act){
		
		synchronized (actMsg) {
			actMsg.message = act;
			actMsg.notifyAll();
		}
		
		String toRet = null;
		
		//wait for evironment to respond to action
		synchronized (obsvMsg) {
			while(obsvMsg.message.equals("")){
				try{
					System.out.println("agent method waiting...");
					obsvMsg.wait();
				} catch(InterruptedException ex){
					ex.printStackTrace();
				}
			}
			toRet = obsvMsg.message;
			obsvMsg.message = "";
		}
		
		return toRet;
	}
	
	
	
	public class MyMessage{
		
		public String message = "";
		
	}
	
}



