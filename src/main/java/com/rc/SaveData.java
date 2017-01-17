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
	
	public static int NUM_LAYERS  = 5 ;
	public static int SCORE_GRADUATIONS = 10 ;
	public static int NUM_OUTPUTS = 50 ;   // 5 x 5 * 2   === see convertMoveToIndex for details


	private File parentDir ;
	private volatile boolean processingAFrame ;
	private final int numInputs ;
	private final int numOutputs ;
	private int batchIndex ;

	private final Map<String,DataWriter> dataWriters ;
	
	public SaveData( int imageWidth, int imageHeight ) {
		
		numOutputs = NUM_OUTPUTS  ;
		numInputs = imageWidth * imageHeight + NUM_OUTPUTS ;
	
		batchIndex = 0 ;
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

	// dx & dy should be +/- 2
	// giving 50 possible outputs 5 x 5 * 2
	// 5 = -2, -1, 0, 1, 2
	// so 5 moves for x & y  = 25 moves
	// then x2 for fire or not
	
}

class DataWriter implements Closeable {
	
	private final File featuresFile ;
	private final File labelsFile ;
	private final BufferedWriter featureWriter ;
	private final BufferedWriter labelWriter ;
	private float previousX ;
	private float previousY ;
	private long lastUpdated ;
	
	public DataWriter( String gameInstance, File parentDir ) throws IOException {

		parentDir.mkdirs() ;
		
		featuresFile = new File( parentDir, gameInstance + ".features" );
		labelsFile = new File( parentDir, gameInstance + ".labels" ); 

		featureWriter = new BufferedWriter( new FileWriter(featuresFile,true) ) ;
		labelWriter = new BufferedWriter( new FileWriter(labelsFile,true) ) ;
		
		previousX = 0.f ;
		previousY = 0.f ;
		
	}

	public int secondsSinceUpdate() {
		return (int)( ( System.currentTimeMillis() - lastUpdated ) / 1000 ) ;
	}
	
	protected int convertMoveToIndex( int dx, int dy, boolean fire ) {
		
		int clampedX = dx>2 ? 2 : ( dx<-2 ? -2 : dx ) ; 
		int clampedY = dy>2 ? 2 : ( dy<-2 ? -2 : dy ) ;
		
		clampedX += 2 ;
		clampedY += 2 ;
		
		int rc = ( clampedY * 5 + clampedX ) * (fire ? 2 : 1 ) ;
		
		return rc ;
	}

	public void write( DataBuffer image, int score, float x, float y, boolean fire ) throws IOException {

		lastUpdated = System.currentTimeMillis() ;

		int dx = (int)( x - previousX ) ; 
		int dy = (int)( y - previousY ) ;
		previousX = x ;
		previousY = y ;
		
		int moveCode = convertMoveToIndex( dx, dy, fire ) ;

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
		
		for( int i=0 ; i<50 ; i++ ) {
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
