package tests.sync;

public class EnvServer {

	protected MiddleMan mm;
	
	public EnvServer(){
		this.mm = new MiddleMan();
	}
	
	public void run(){
		
		int i = 0;
		String msg = "env:" + i;
		String act = this.mm.start(msg);
		System.out.println(msg + " " + act);
		i++;
		while(true){
			msg = "env:" + i;
			act = this.mm.envRequest(msg);
			System.out.println(msg + " " + act);
			i++;
		}
		
	}
	
	
	
	public static void main(String [] args){
		EnvServer env = new EnvServer();
		System.out.println("Starting");
		env.run();
	}
	
}
