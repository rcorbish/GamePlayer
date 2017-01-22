package com.rc;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

public class HandleImage {
	static Logger logger = LoggerFactory.getLogger( HandleImage.class ) ;
	public static int TARGET_WIDTH = 128 ;
	public static int TARGET_HEIGHT = 96 ;
	public static int TARGET_CHANNELS = 3 ;
	
	private Dense dense ;
	private SaveData saver ;
	private int numi = 100 ;
	
	public HandleImage() throws ClassNotFoundException, IOException {
		dense = null ;
		saver = null ;
		new Thread( new Runnable() {
			public void run() {
				Thread.currentThread().setName( "Network Creator" ) ;
				try {
					logger.info( "Creating network in the background" );
					dense = new Dense( TARGET_WIDTH, TARGET_HEIGHT ) ;
					logger.info( "Network ready to play" ) ;
				} catch (ClassNotFoundException | IOException e) {
					logger.error( "Cannot create dense network", e ); 
				}
			}
		} ).start() ;
	}
	
	public String post( Request req, Response rsp ) {
		try { 
			String from = req.headers( "X-Game-Instance" ) ;
			String mode = req.headers( "X-Server-Mode" ) ;
			if( mode==null ) mode = "save" ;
			//logger.info( "Missing game instance" );
			String tmp = req.params( "score" ) ;
			int score = Integer.parseInt(tmp) ;
			/*
			tmp = req.headers( "X-Image-Width" ) ;
			int imgWidth = Integer.parseInt(tmp) ;
			tmp = req.headers( "X-Image-Height" ) ;
			int imgHeight = Integer.parseInt(tmp) ;
			*/
			tmp = req.params( "x" ) ;			
			float x = tmp==null ? 0 : Float.parseFloat(tmp) ;
			tmp = req.params( "y" ) ;			
			float y = tmp==null ? 0 : Float.parseFloat(tmp) ;
			tmp = req.params( "fire" ) ;			
			boolean fire = Boolean.parseBoolean(tmp) ;
			
			logger.debug( "POST: score={}, x={}, y={}, fire={}", score,x,y,fire ) ;

			String base64 = req.body() ;
			//byte raw[] = req.bodyAsBytes() ;
			int ix = base64.indexOf(',') ;
			byte raw[] = Base64.getDecoder().decode( base64.substring( ix+1 ) ) ;
			BufferedImage colorImage = ImageIO.read( new ByteArrayInputStream(raw) ) ;
			numi++ ;
			
			BufferedImage scaledImage = new BufferedImage( TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_3BYTE_BGR ) ;
			Graphics2D gi = scaledImage.createGraphics();
		    gi.setRenderingHint(
		    	    RenderingHints.KEY_INTERPOLATION,
		    	    RenderingHints.VALUE_INTERPOLATION_BILINEAR
		    	);
		    gi.drawImage(colorImage, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null );

			File outputfile = new File("src/main/resources/data/saved" + numi + ".png");
			//ImageIO.write(scaledImage, "png", outputfile);

		    DataBuffer db = scaledImage.getRaster().getDataBuffer() ;
		    logger.debug( "Image reduced and decolored" ) ;
		    ProposedMove rc = ProposedMove.NULL_MOVE ;
		    if( "play".equals(mode) ) {
		    	if( dense != null ) {
		    		rc = dense.processData( db ) ;
		    	}
		    } else if( "save".equals(mode) ) {
		    	if( saver == null ) saver = new SaveData( TARGET_WIDTH, TARGET_HEIGHT, TARGET_CHANNELS ) ;
		    	rc = saver.processData( db, from, score, x, y, fire ) ;
		    }
		    logger.debug( "Sending {} as a response.", rc.toJson() ) ;
			return rc.toJson() ;
		} catch( Throwable t ) {
			t.printStackTrace(); 
			throw new RuntimeException( t ) ;
		}
	}
}
