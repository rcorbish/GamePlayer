package com.rc;

import java.awt.image.DataBuffer;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dense {

	private static Logger logger = LoggerFactory.getLogger(Dense.class);
		
	public static int NUM_OUTPUTS = 50 ;   // 5 x 5 * 2   === see convertMoveToIndex for details
	private Random random = new Random( 300 ) ;

	private int numInputs  ;
	private int numOutputs  ;

	private INDArray features ;
	private INDArray labels ;

	private float previousX ;
	private float previousY ;
	private int batchIndex ;

	private volatile boolean processingAFrame ;
	private final File parentDir ;

	MultiLayerNetwork network ;
	
	public Dense( int imageWidth, int imageHeight ) throws ClassNotFoundException, IOException {

		parentDir = new File( "src/main/resources/data/" ) ;

		previousX = 0 ;
		previousY = 0 ;
		numOutputs = NUM_OUTPUTS  ;
		numInputs = imageWidth * imageHeight  ;
		network = loadModel();
		
		logger.info( "Loaded network with {} parameters.", network.numParams() ) ;
	
		features = Nd4j.create( 1, numInputs ) ;
		batchIndex = 0 ;
		processingAFrame = false ;
		
	}
	
	public ProposedMove processData( DataBuffer image ) {
		ProposedMove rc = ProposedMove.NULL_MOVE ;
		if( !processingAFrame ) {
			processingAFrame = true ;
			
			for( int i=0 ; i<HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH ; i++ ) {
				features.putScalar( 0, i, image.getElem(i) ) ;
			}
			INDArray guessed = network.output( features ) ;
			int moveCode = 0 ;
			float maxProb = 0.f ;
			int ix[] = new int[3] ;
			ix[0] = 0 ;
			ix[2] = 0 ;
			for( int i=0 ; i<guessed.length() ; i++ ) {
				ix[1] = i ;
				if( maxProb < guessed.getFloat( ix ) ) {
					maxProb = guessed.getFloat( ix ) ;
					moveCode = i ;
				}
			}
			rc = convertMoveCode( moveCode ) ;
			processingAFrame = false ;			
		}
		
		return rc ;
	}

	// dx & dy should be +/- 2
	// giving 50 possible outputs 5 x 5 * 2
	// 5 = -2, -1, 0, 1, 2
	// so 5 moves for x & y  = 25 moves
	// then x2 for fire or not
	
	protected ProposedMove convertMoveCode( int moveCode ) {
		
		boolean fire = false ;
		if( moveCode > 24 ) {
			moveCode -= 25 ;
			fire = true ;
		}
		
		int dy = ( moveCode / 5 ) - 2 ;
		int dx = ( moveCode % 5 ) - 2 ;
		
		return new ProposedMove( dx, dy, fire ) ;
	}
	
	
	public MultiLayerNetwork loadModel() throws IOException, ClassNotFoundException {

		logger.info( "Loading model" ); 

		Path coeffs  = Paths.get( parentDir.getAbsolutePath(), "coefficients.dat" ) ;
		Path config  = Paths.get( parentDir.getAbsolutePath(), "config.json" ) ;

		MultiLayerConfiguration conf = MultiLayerConfiguration.fromJson( new String( Files.readAllBytes(config) ) ) ;
		MultiLayerNetwork rc = new MultiLayerNetwork( conf ) ;
		logger.info( "Model config. loaded. Expect a few mins to load the parameters..." ); 
		
		try(DataInputStream dis = new DataInputStream( Files.newInputStream( coeffs ) ) ){
			INDArray newParams = Nd4j.read(dis);
			rc.init();
			rc.setParameters(newParams);
		}
		logger.info( "... parameters initialized" ); 
		
		return rc ;
	}	
}
