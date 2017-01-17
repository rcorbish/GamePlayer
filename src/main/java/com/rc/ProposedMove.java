package com.rc;

public class ProposedMove {

	final static public int RANGE = 5 ;	// can handle a move of -2, -1, 0 , 1, 2
	
	public final static ProposedMove NULL_MOVE = new ProposedMove(0, 0, false) ;

	private final int x ;
	private final int y ;
	private final boolean fire ;
	public ProposedMove( int x, int y, boolean fire ) {
		this.x = x ;
		this.y = y ;
		this.fire = fire ;
	}
	
	/**
	 * Convert this to the neural network format. We have 25 possible moves 
	 * represented as a grid (column major order) We can only represent a 
	 * max move of 2 with this grid size
	 * 
	 * example X+2 and Y=0 would be 
	 * 
	 * 0 0 0 0 0 
	 * 0 0 0 0 0 
	 * 0 0 0 0 1 
	 * 0 0 0 0 0 
	 * 0 0 0 0 0
	 * 
	 * X-1 Y+2
	 * 
	 * 0 0 0 0 0 
	 * 0 0 0 0 0 
	 * 0 0 0 0 0 
	 * 0 0 0 0 0 
	 * 0 1 0 0 0

	 * The last element ( index 25 ) is whether to fire or not
	 *  
	 * outputs are clamped to +/- 2 
	 * 
	 * @return
	 */
	public int[] toNetworkFormat() {
		int rc[] = new int[ RANGE*RANGE+1 ] ;  
		int limit = RANGE/2 ;
		
		int clampedX = x>limit? limit : ( x<-limit ? -limit : x ) ; 
		int clampedY = y>limit? limit : ( y<-limit ? -limit : y ) ;
		
		int move = clampedY+limit + ( RANGE * (clampedX+limit) ) ;
		rc[move] = 1 ;
		rc[25] = fire ? 1 : 0 ;
		return rc ;
	}
	public String toJson() {
		return "{\"x\":" + x +",\"y\":" + y + ",\"fire\":" + fire + "}" ;
	}
}
