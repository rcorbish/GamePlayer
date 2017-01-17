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
	public static int TARGET_WIDTH = 80 ;
	public static int TARGET_HEIGHT = 50 ;
	
	private Dense dense ;
	private SaveData saver ;
	private int numi = 100 ;
	
	public HandleImage() throws ClassNotFoundException, IOException {
		dense = null ;
		saver = null ;
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
			//File outputfile = new File("saved" + numi + ".png");
			//ImageIO.write(colorImage, "png", outputfile);
			
			BufferedImage scaledImage = new BufferedImage( TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_BYTE_GRAY ) ;
			Graphics2D gi = scaledImage.createGraphics();
		    gi.setRenderingHint(
		    	    RenderingHints.KEY_INTERPOLATION,
		    	    RenderingHints.VALUE_INTERPOLATION_BILINEAR
		    	);
		    gi.drawImage(colorImage, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null );
		    
		    DataBuffer db = scaledImage.getRaster().getDataBuffer() ;
		    logger.debug( "Image reduced and decolored" ) ;
		    ProposedMove rc = ProposedMove.NULL_MOVE ;
		    if( "play".equals(mode) ) {
		    	if( dense == null ) dense = new Dense( TARGET_WIDTH, TARGET_HEIGHT ) ;
			    rc = dense.processData( db ) ;
		    } else if( "save".equals(mode) ) {
		    	if( saver == null ) saver = new SaveData( TARGET_WIDTH, TARGET_HEIGHT ) ;
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
