package com.rc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer {

	static Logger logger = LoggerFactory.getLogger( WebServer.class ) ;
	int img_seq = 0 ;

	public static void main(String[] args)  {
		try {
			HandleImage imageHandler = new HandleImage() ;
			logger.info( "image handler prepared" ); 
			spark.Spark.staticFiles.externalLocation( "src/main/resources" ); 				
			spark.Spark.post( "/send_img/:score/:x/:y/:fire", imageHandler::post ) ;
			
		} catch( Throwable t ) {
			logger.error( "Error in main()", t ) ;
		}
    }
	
}
