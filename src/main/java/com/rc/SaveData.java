package com.rc;

import java.awt.image.DataBuffer;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveData {

	private static Logger logger = LoggerFactory.getLogger(SaveData.class);

	public static int BATCH_SIZE = 10 ;

	public static int NUM_X_BLOCKS  = 10 ;
	public static int NUM_Y_BLOCKS  = 10 ;
	public static int NUM_OUTPUTS = NUM_X_BLOCKS * NUM_Y_BLOCKS * 2 ; // x2 for fire or not
	public static int SCREEN_X_MIN = -20 ; 
	public static int SCREEN_X_MAX = 20 ; 
	public static int SCREEN_X_RANGE = SCREEN_X_MAX - SCREEN_X_MIN ;
	public static int SCREEN_Y_MIN = -20 ; 
	public static int SCREEN_Y_MAX = 20 ; 
	public static int SCREEN_Y_RANGE = SCREEN_Y_MAX - SCREEN_Y_MIN ;
	public static int X_BLOCK = ( SCREEN_X_RANGE / NUM_X_BLOCKS  ) ;
	public static int Y_BLOCK = ( SCREEN_Y_RANGE / NUM_Y_BLOCKS  ) ;

	private File parentDir ;
	private volatile boolean processingAFrame ;

	private final Map<String,DataWriter> dataWriters ;

	public SaveData( int imageWidth, int imageHeight, int imageChannels) {

		processingAFrame = false ;

		parentDir = new File( "./src/main/resources/data/" ) ;
		dataWriters = new HashMap<>() ;

		final Thread t = new Thread( new Runnable() {			
			@Override
			public void run() {
				try {
					logger.info( "Cleanup thread started." );
					while( !Thread.interrupted() ) {	
						Iterator<Entry<String, DataWriter>> i = dataWriters.entrySet().iterator() ;
						while( i.hasNext() ) {
							Entry<String, DataWriter> entry = i.next() ;
							if( entry.getValue().secondsSinceUpdate()>5 ) {
								logger.info( "Closing down data writer {}", entry.getKey() );
								entry.getValue().close();
								i.remove() ;
							}
						}
						Thread.sleep( 10000 ) ;
					}
				} catch (InterruptedException e) {						
				} catch (IOException ioe) {
					logger.error( "IO error closing data writer", ioe );
				}
				logger.info( "Cleanup thread finished." );
			}
		}) ;
		t.setName( "DW-Cleanup" ) ;
		t.start();
	}


	public ProposedMove processData( DataBuffer image, String gameInstance, int score, float x, float y, boolean fire ) throws IOException {
		if( !processingAFrame ) {
			processingAFrame = true ;

			DataWriter dw = dataWriters.get( gameInstance ) ;
			if( dw == null ) {
				dw = new DataWriter(gameInstance, parentDir) ;
				dataWriters.put( gameInstance, dw ) ;
				logger.info( "Created new data writer {}", gameInstance );
			}
			dw.write( image, score, x, y, fire ) ;

			processingAFrame = false ;			
		}				
		return ProposedMove.NULL_MOVE ;
	}



	public static class DataWriter implements Closeable {

		private static Logger logger = LoggerFactory.getLogger(DataWriter.class);

		private final File featuresFile ;
		private final File labelsFile ;
		private final BufferedWriter featureWriter ;
		private final BufferedWriter labelWriter ;
		private long lastUpdated ;

		public DataWriter( String gameInstance, File parentDir ) throws IOException {

			parentDir.mkdirs() ;

			featuresFile = new File( parentDir, gameInstance + ".features" );
			labelsFile = new File( parentDir, gameInstance + ".labels" ); 

			featureWriter = new BufferedWriter( new FileWriter(featuresFile,true) ) ;
			labelWriter = new BufferedWriter( new FileWriter(labelsFile,true) ) ;
		}

		public int secondsSinceUpdate() {
			return (int)( ( System.currentTimeMillis() - lastUpdated ) / 1000 ) ;
		}

		public static int convertDataToIndex( float x, float y, boolean fire ) {

			int xx = (int)( (x-SCREEN_X_MIN) / X_BLOCK ) ;
			int yy = (int)( (y-SCREEN_Y_MIN) / Y_BLOCK ) ;

			int rc = ( yy * SaveData.NUM_X_BLOCKS + xx ) + (fire ? NUM_X_BLOCKS*NUM_Y_BLOCKS : 0 ) ;
			logger.debug( "{},{} {} => {}", x, y, (fire?" + FIRE":""), rc );
			return rc ;
		}

		public static ProposedMove convertIndexToData( int moveCode ) {

			int mc = moveCode ;
			boolean fire = false ;
			if( mc > (SaveData.NUM_X_BLOCKS*SaveData.NUM_Y_BLOCKS) ) {
				mc -= (SaveData.NUM_X_BLOCKS*SaveData.NUM_Y_BLOCKS) ;
				fire = true ;
			}
			int x = ( mc % NUM_X_BLOCKS ) * ( SCREEN_X_RANGE / NUM_X_BLOCKS  ) + SCREEN_X_MIN ;
			int y = ( mc / NUM_X_BLOCKS ) * ( SCREEN_Y_RANGE / NUM_Y_BLOCKS  ) + SCREEN_Y_MIN  ; 

			return new ProposedMove(x, y, fire) ;
		}

		public void write( DataBuffer image, int score, float x, float y, boolean fire ) throws IOException {

			lastUpdated = System.currentTimeMillis() ;

			int moveCode = convertDataToIndex( x, y, fire ) ;

			StringBuilder sb = new StringBuilder() ;

			for( int i=0 ; i<image.getSize() ; i++ ) {
				sb.append( image.getElem(i) ).append( ',' ) ;
			}
			sb.setCharAt( sb.length()-1, '\n' ) ;
			featureWriter.write( sb.toString() ) ;

			sb.setLength(0);
			sb.append( score ).append(',')
			.append( x ).append(',')
			.append( y ) ;

			for( int i=0 ; i<SaveData.NUM_OUTPUTS ; i++ ) {
				sb.append(',').append( i==moveCode ? 1 : 0 ) ;
			}
			sb.append('\n') ;

			labelWriter.write( sb.toString() ) ; 
		}

		public void close() throws IOException {
			featureWriter.close(); 
			labelWriter.close(); 
		}
	}
}