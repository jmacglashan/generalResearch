package behavior.training.experiments.simulated.grid;

public class IntMap {

	public int[][] map;
	
	
	public IntMap(int width, int height){
		this.map = new int[width][height];
	}
	
	public void set(int x, int y, int v){
		this.map[x][y] = v;
	}
	
	public void horizontal(int xi, int xf, int y, int v){
		for(int x = xi; x <= xf; x++){
			this.map[x][y] = v;
		}
	}
	
	public void vertical(int yi, int yf, int x, int v){
		for(int y = yi; y <= yf; y++){
			this.map[x][y] = v;
		}
	}
	

}
