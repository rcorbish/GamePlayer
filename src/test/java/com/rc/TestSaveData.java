package com.rc;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSaveData {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		for( float y = -20.f ; y<20.f ; y += .5f ) {
			for( float x = -20.f ; x<20.f ; x += .5f ) {
				boolean fire = false ;

				int moveCode = SaveData.DataWriter.convertDataToIndex(x, y, fire) ;
				ProposedMove pm = SaveData.DataWriter.convertIndexToData(moveCode) ;
				System.out.println( x + "," + y + " => " + moveCode ) ;
				assertEquals( x, pm.x, SaveData.SCREEN_X_RANGE / SaveData.NUM_X_BLOCKS + 1.f ) ;
				assertEquals( y, pm.y, SaveData.SCREEN_Y_RANGE / SaveData.NUM_Y_BLOCKS + 1.f ) ;
			}
		}		
	}

}
