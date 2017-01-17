package com.rc;

import java.awt.image.DataBuffer;
import java.util.Random;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSTM {

	private static Logger logger = LoggerFactory.getLogger(LSTM.class);
		
	public static int NUM_FRAMES_TO_CONSIDER = 10 ;
	
	public static int NUM_LAYERS  = 3 ;
	public static int SCORE_GRADUATIONS = 10 ;
	public static int NUM_OUTPUTS = 26 ;
	
	private volatile int batchIndex ;
	private Random random = new Random( 300 ) ;
	private int previousScore ;
	private ProposedMove previousMove ;
	private int numInputs  ;
	private int numOutputs  ;
	private int batchID ; 

	private INDArray features ;
	private INDArray labels ;

	MultiLayerNetwork network ;
	
	public LSTM( int imageWidth, int imageHeight ) {
		batchID = 100000 ;
		
		numOutputs = NUM_OUTPUTS  ;
		numInputs = imageWidth * imageHeight + NUM_OUTPUTS ;
		network = createModelConfig( NUM_LAYERS, numInputs, NUM_OUTPUTS ) ;
		network.init();
		logger.info( "Created network with {} parameters.", network.numParams() ) ;
		
		previousMove = new ProposedMove(0, 0, false) ;
		previousScore = 0 ;
		
		startBatch();
	}
	
	public ProposedMove processData( DataBuffer image, int score ) {
		if( batchIndex>=NUM_FRAMES_TO_CONSIDER ) {
			logger.info( "Too busy - ignoring frame" ) ;
			return ProposedMove.NULL_MOVE ; // still processing something
		}

		float db[] = new float[ numInputs ] ;
		for( int i=0 ; i<HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH ; i++ ) {
			db[i+numOutputs] = image.getElem(i) ;
		}
		
		int moves[] = previousMove.toNetworkFormat() ;  // range of movement is -2,-1,0,1,2
		for( int i=0 ; i<numOutputs ; i++ ) {
			db[i] = moves[i] ;
		}		

		addBatch( db, score, previousMove ) ;
		
		previousMove = new ProposedMove( random.nextInt(5)-2, random.nextInt(5)-2, random.nextFloat()<0.1 ) ;
		return previousMove ;
	}

	protected void addBatch( float db[], int score, ProposedMove lastMove ) {
		logger.debug( "Processing batch element {} of {} (batchID = {})", batchIndex, NUM_FRAMES_TO_CONSIDER, batchID ) ;
	/*	
		int scoreChange = score - previousScore ;
		for( int i=0 ; i<SCORE_GRADUATIONS ; i++ ) {
			if( scoreChange >= (20 * i) ) {
				db[i] = 1 ;
			} else {
				db[i] = 0 ;
			}
		}		
*/
		
		int index[] = new int[] { 0, 0, batchIndex } ;
		for( int i=0 ; i<db.length ; i++ ) {
			index[1] = i ;
			features.putScalar( index, db[i] ) ;
		}
		
		int op[] = lastMove.toNetworkFormat() ;
		for( int i=0 ; i<op.length ; i++ ) {
			index[1] = i ;
			labels.putScalar( index, op[i] ) ;
		}
		
		batchIndex++ ;
		if( batchIndex >= NUM_FRAMES_TO_CONSIDER ) {
			endBatch(); 
		}
	}
	
	protected void endBatch() {
		logger.info( "Sending to brain (Batch ID ={})", batchID ) ;		
		network.fit( features, labels );
		startBatch(); 
	}
	
	protected void startBatch() {
		batchID++ ;

		logger.debug( "Batch start issued Batch ID ={})", batchID ) ;
		features = Nd4j.create(1, numInputs, NUM_FRAMES_TO_CONSIDER ) ;
		labels = Nd4j.create(1, numOutputs, NUM_FRAMES_TO_CONSIDER ) ;
		batchIndex = 0 ;
	}
	
	
	protected MultiLayerNetwork createModelConfig( int numLayers, int numInputs, int numOutputs ) {
		
		ListBuilder lb = new NeuralNetConfiguration.Builder()
				.seed( 100 )
				.iterations( 1 )
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT )
				.learningRate(0.1)
				.regularization(true)  
				.l2(0.01)
				.dropOut( 0.001 )
				.weightInit(WeightInit.XAVIER )
				.updater(Updater.RMSPROP )
				.list()
				;

		float layerScaling = 1.f ;
		int ni = numInputs ;
		int no = (int)(numInputs * layerScaling) ;
		for( int i=0 ; i<numLayers-1 ; i++ ) {
			lb.layer(i, new GravesLSTM.Builder()
					.nIn(ni)
					.nOut(no)
					.activation( Activation.RELU )
					.build()
					) ;
			ni = no ;
			no *= layerScaling ;
		}

		lb.layer( numLayers-1, new RnnOutputLayer.Builder()
				.activation( Activation.SOFTMAX )
				.lossFunction(LossFunctions.LossFunction.MCXENT )
				.weightInit(WeightInit.XAVIER)
				.nIn(ni)
				.nOut(numOutputs)
				.build()
				) ;

		MultiLayerConfiguration conf = lb
				.backprop(true)
//				.backpropType(BackpropType.TruncatedBPTT)
//				.tBPTTForwardLength( BPTT_LENGTH )
//				.tBPTTBackwardLength(BPTT_LENGTH)
				.pretrain(false)
				.build();

		return new MultiLayerNetwork( conf ) ;
	}
}
