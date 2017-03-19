package com.rc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer.AlgoMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator ;
import org.nd4j.linalg.dataset.api.iterator.TestDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzeFile {
	private static Logger logger = LoggerFactory.getLogger(AnalyzeFile.class);

	private MultiLayerNetwork network ;
	private File parentDir ;
	private final int BATCH_SIZE = 100 ;
	private final int EPOCHS = 35 ;
	private final int ITERATIONS = 2 ;
	
	public final static int NUM_OUTPUTS = SaveData.NUM_OUTPUTS  ;
	public final static int NUM_INPUTS = HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH * HandleImage.TARGET_CHANNELS ;
	
	public static void main(String[] args) {
		AnalyzeFile self = new AnalyzeFile() ;
		try {
			self.analyze() ;
		} catch( Throwable t ) {
			logger.error( "Failure in main()", t ) ;
		}
	}

	public AnalyzeFile() {
		network = createModelConfig( NUM_INPUTS, NUM_OUTPUTS ) ;
		parentDir = new File( "src/main/resources/data/" ) ;
	}

	public void analyze() throws IOException, InterruptedException, URISyntaxException {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".features" ) ;
			}
		};

		String features[] = parentDir.list( filter ) ;
		String labels[] = new String[ features.length ] ;
		
		for( int i=0 ; i<features.length ; i++ ) {
			labels[i] = features[i].replace(".features",".labels" ) ;
		}
		DSI dsi = new DSI( parentDir, BATCH_SIZE, features, labels) ;
		for( int i=0 ; i<EPOCHS ; i++ ) {
			logger.info( "****************************");
			logger.info( "*   E P O C H   {}          *", i ) ;			
			logger.info( "****************************");
			dsi.reset(); 
			dsi.nextFile() ;

			int n = 0 ;
			DataSet ds ;
			while( (ds = dsi.next()) != null ) {
				n += ds.numExamples() ;
				network.fit(ds); 
				logger.info( "Processed {} datasets", n );
			}
			
			Evaluation eval = network.evaluate( dsi.test() ) ;
			logger.info( eval.stats( true ) ) ;
			if( i >= EPOCHS-1 ) {
				File f = new File( parentDir, "stats.txt" ) ;
				FileOutputStream fos = new FileOutputStream(f) ;
				fos.write( eval.stats(true).getBytes() ) ;
				fos.flush();
				fos.close();
			}
		}		
		saveModel() ;
	}

	public void saveModel( ) throws IOException {
			
		Path coeffs  = Paths.get( parentDir.getAbsolutePath(), "coefficients.dat" ) ;
		Path config  = Paths.get( parentDir.getAbsolutePath(), "config.json" ) ;
		Path updater = Paths.get( parentDir.getAbsolutePath(), "updater.dat" ) ;
		
		logger.info( "Saving {} parameters", network.params().length() ) ;
		//Write the network parameters:
		try(DataOutputStream dos = new DataOutputStream( Files.newOutputStream( coeffs ) ) ){
			Nd4j.write( network.params(), dos ) ;
		}
		
		logger.info( "Saving network config" ) ;
		//Write the network configuration:
		Files.write( config, network.getLayerWiseConfigurations().toJson().getBytes() );

		logger.info( "Saving network updater" ) ;
		try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream( updater ) ) ){
			oos.writeObject( network.getUpdater() );
		}
	}

	/*
	public void loadModel() throws IOException, ClassNotFoundException {

		Path coeffs  = Paths.get( parentDir.getAbsolutePath(), "coefficients.dat" ) ;
		Path config  = Paths.get( parentDir.getAbsolutePath(), "config.json" ) ;
		Path updater = Paths.get( parentDir.getAbsolutePath(), "updater.dat" ) ;

		MultiLayerConfiguration conf = MultiLayerConfiguration.fromJson( new String( Files.readAllBytes(config) ) ) ;
		network = new MultiLayerNetwork( conf ) ;
		network.init();
		
		try(DataInputStream dis = new DataInputStream( Files.newInputStream( coeffs ) ) ){
			INDArray newParams = Nd4j.read(dis);
			network.setParameters(newParams);
		}
		try(ObjectInputStream ois = new ObjectInputStream( Files.newInputStream( updater ))){
			network.setUpdater( (org.deeplearning4j.nn.api.Updater)ois.readObject() ) ;
		}
	}
*/
	
	protected MultiLayerNetwork createModelConfig( int numInputs, int numOutputs ) {
			
		ListBuilder lb = new NeuralNetConfiguration.Builder()
				.seed( 100 )
				.iterations( ITERATIONS )
				.optimizationAlgo( OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT )
				.learningRate( 0.001 )
				.regularization(true)  
				.l2( 0.05 )
				.weightInit(WeightInit.XAVIER )
				.updater(Updater.NESTEROVS).momentum(0.9)
				.list()
				;

		int layerIndex = 0 ;
		lb
		.layer(layerIndex++, new ConvolutionLayer.Builder(3,3)
                .nIn( HandleImage.TARGET_CHANNELS )
                .name( "Convolution" )
                .cudnnAlgoMode( AlgoMode.NO_WORKSPACE )
                .convolutionMode( ConvolutionMode.Same )
                .stride( 1, 1 )
                .nOut(4)
                .activation(Activation.IDENTITY)
                .build() )
		.layer(layerIndex++, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                .name( "Pool " + layerIndex )
                .kernelSize(2,2)
                .stride(2,2)
                .build())
		.layer(layerIndex++, new DenseLayer.Builder()
                .name( "Dense " + layerIndex )
				.nOut(8000)
				.activation( Activation.TANH )
				.build() ) 
		.layer(layerIndex++, new DenseLayer.Builder()
                .name( "Dense " + layerIndex )
				.nOut(2000)
				.activation( Activation.TANH )
				.build() ) 
		.layer(layerIndex++, new DenseLayer.Builder()
                .name( "Dense " + layerIndex )
				.nOut(1000)
				.activation( Activation.TANH )
				.build() ) 
		.layer( layerIndex++, new OutputLayer.Builder()
                .name( "Output" )
				.activation( Activation.SOFTMAX )
				.lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD )
				.nOut(numOutputs)
				.build()
				) ;

		MultiLayerConfiguration conf = lb
				.setInputType(InputType.convolutionalFlat( HandleImage.TARGET_HEIGHT, HandleImage.TARGET_WIDTH, HandleImage.TARGET_CHANNELS ))
				.backprop(true)
				.pretrain(false)
				.build();

		MultiLayerNetwork rc = new MultiLayerNetwork( conf ) ;
		rc.init();
		return rc ;
	}

}


class DSI {
	private final int batchSize  ;
	private final File parentDir ;
	private final String features[] ;
	private final String labels[] ;
	private int currentIndex ;
	private int testIndex ;
	private int sampleClock ;
	private int useAsTestCount ;
	private float TEST_RATIO = 0.05f ;
	
	BufferedReader featureReader ;
	BufferedReader labelReader ;
	
	private INDArray featuresTest ;
	private INDArray labelsTest ;
	
	public DSI( File parentDir, int batchSize, String features[], String labels[] ) {
		this.features = features ;
		this.labels = labels ;
		this.batchSize = batchSize ;
		this.parentDir = parentDir ;
		testIndex = 0 ;
		sampleClock = 0 ;
		useAsTestCount = (int)(batchSize * TEST_RATIO) ;
		featuresTest = Nd4j.create( batchSize, HandleImage.TARGET_CHANNELS * HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH ) ;
		labelsTest = Nd4j.create( batchSize, AnalyzeFile.NUM_OUTPUTS ) ;
	}
	
	public DataSet next() throws IOException {
		DataSet rc = null ;		
		int batchIndex = 0 ;
		
		INDArray f = Nd4j.create( batchSize, HandleImage.TARGET_CHANNELS * HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH ) ;
		INDArray l = Nd4j.create( batchSize, AnalyzeFile.NUM_OUTPUTS ) ;
		
		while( featureReader != null && batchIndex < batchSize ) {
			String features = featureReader.readLine() ;
			String labels = labelReader.readLine() ;
				
			if( features == null ) {
				if( !nextFile() ) {
					break ;
				}
				features = featureReader.readLine() ;
				labels = labelReader.readLine() ;				
			}
			if( sampleClock>useAsTestCount && testIndex<batchSize ) {
				String fcols[] = features.split(",") ;
				int n = Math.min( fcols.length, f.columns() ) ;
				for( int c=0 ; c<n ; c++ ) {
					float v = Float.parseFloat( fcols[c] ) ;
					featuresTest.putScalar( testIndex, c, v ) ;
				}
				String lcols[] = labels.split(",") ;
				for( int c=3 ; c<lcols.length ; c++ ) {
					float v = Float.parseFloat( lcols[c] ) ;
					labelsTest.putScalar(testIndex, c-3, v ) ;
				}
				testIndex++ ;
			} else {
				String fcols[] = features.split(",") ;
				int n = Math.min( fcols.length, f.columns() ) ;
				for( int c=0 ; c<n ; c++ ) {
					float v = Float.parseFloat( fcols[c] ) ;
					f.putScalar( batchIndex, c, v ) ;
				}
				String lcols[] = labels.split(",") ;
				for( int c=3 ; c<lcols.length ; c++ ) {
					float v = Float.parseFloat( lcols[c] ) ;
					l.putScalar(batchIndex, c-3, v ) ;
				}
				batchIndex++ ;
			}
			sampleClock++ ;
		}
		
		if( batchIndex > 0 ) {
			f = f.get(NDArrayIndex.interval(0,batchIndex), NDArrayIndex.all() ) ;
			l = l.get(NDArrayIndex.interval(0,batchIndex), NDArrayIndex.all() ) ;
			rc = new DataSet(f, l) ;
		}
		return rc ;
	}
	
	public DataSetIterator test() {
		return new TestDataSetIterator( 
				new DataSet( 
						featuresTest.get(NDArrayIndex.interval(0,testIndex), NDArrayIndex.all() ) , 
						labelsTest.get(NDArrayIndex.interval(0,testIndex), NDArrayIndex.all() ) ) ) ;		
	}
	
	public void reset() throws IOException {
		if( featureReader != null ) featureReader.close(); 
		if( labelReader != null ) labelReader.close();
		featureReader = null ;
		labelReader = null ;
		currentIndex = 0 ;	
		testIndex = 0 ;
		testIndex = 0 ;
		sampleClock = 0 ;
	}

	
	protected boolean nextFile() throws IOException {

		if( featureReader != null ) featureReader.close(); 
		if( labelReader != null ) labelReader.close();
		featureReader = null ;
		labelReader = null ;
		
		boolean rc = currentIndex < features.length ;
		if( rc ) {
			File f1 = new File( parentDir, features[currentIndex] ) ;
			if( !f1.canRead() || f1.length()<100 ) {
				rc = false ;
			} else {
				Reader rf = new FileReader( f1 ) ;
				File f2 = new File( parentDir, labels[currentIndex] ) ;
				Reader rl = new FileReader( f2 ) ;
				
				featureReader = new BufferedReader( rf ) ;
				labelReader = new BufferedReader( rl ) ;
			}
			currentIndex++ ;
		}
		return rc ;
	}
}