package com.rc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzeFile {
	private static Logger logger = LoggerFactory.getLogger(AnalyzeFile.class);

	private MultiLayerNetwork network ;
	private File parentDir ;
	private final int BATCH_SIZE = 50 ;
	
	public final static int NUM_OUTPUTS = 50 ;
	public final static int NUM_INPUTS = HandleImage.TARGET_HEIGHT * HandleImage.TARGET_WIDTH ;
	
	public static void main(String[] args) {
		AnalyzeFile self = new AnalyzeFile() ;
		try {
			self.analyze() ;
		} catch( Throwable t ) {
			logger.error( "Failure in main()", t ) ;
		}
	}

	public AnalyzeFile() {
		network = createModelConfig( 7, NUM_INPUTS, NUM_OUTPUTS ) ;
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
		dsi.reset(); 
		dsi.nextFile() ;
		
		int n = 0 ;
		DataSet ds ;
		while( (ds = dsi.next()) != null ) {
			n += ds.numExamples() ;
			network.fit(ds); 
			logger.info( "Processed {} datasets", n );
		}
		saveModel() ;
	}

	public void saveModel( ) throws IOException {
			
		Path coeffs  = Paths.get( parentDir.getAbsolutePath(), "coefficients.dat" ) ;
		Path config  = Paths.get( parentDir.getAbsolutePath(), "config.json" ) ;
		Path updater = Paths.get( parentDir.getAbsolutePath(), "updater.dat" ) ;
		
		//Write the network parameters:
		try(DataOutputStream dos = new DataOutputStream( Files.newOutputStream( coeffs ) ) ){
			Nd4j.write( network.params(), dos ) ;
		}

		//Write the network configuration:
		Files.write( config, network.getLayerWiseConfigurations().toJson().getBytes() );

		try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream( updater ) ) ){
			oos.writeObject( network.getUpdater() );
		}
	}

	
	public void loadModel() throws IOException, ClassNotFoundException {

		Path coeffs  = Paths.get( parentDir.getAbsolutePath(), "coefficients.dat" ) ;
		Path config  = Paths.get( parentDir.getAbsolutePath(), "config.json" ) ;
		Path updater = Paths.get( parentDir.getAbsolutePath(), "updater.dat" ) ;

		MultiLayerConfiguration conf = MultiLayerConfiguration.fromJson( new String( Files.readAllBytes(config) ) ) ;
		network = new MultiLayerNetwork( conf ) ;
		
		try(DataInputStream dis = new DataInputStream( Files.newInputStream( coeffs ) ) ){
			INDArray newParams = Nd4j.read(dis);
			network.init();
			network.setParameters(newParams);
		}
		try(ObjectInputStream ois = new ObjectInputStream( Files.newInputStream( updater ))){
			network.setUpdater( (org.deeplearning4j.nn.api.Updater)ois.readObject() ) ;
		}
	}

	
	protected MultiLayerNetwork createModelConfig( int numLayers, int numInputs, int numOutputs ) {

		ListBuilder lb = new NeuralNetConfiguration.Builder()
				.seed( 100 )
				.iterations( 3 )
				.optimizationAlgo( OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT )
				.learningRate(0.1)
				.regularization(true)  
				.l2( 0.01 )
				.dropOut( 0.001 )
				.weightInit(WeightInit.XAVIER )
				.updater(Updater.NESTEROVS).momentum(0.9)
				.list()
				;

		float layerScaling = 0.7f ;
		int ni = numInputs ;
		int no = (int)(numInputs * layerScaling) ;
		for( int i=0 ; i<numLayers-1 ; i++ ) {
			lb.layer(i, new DenseLayer.Builder()
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
				.pretrain(false)
				.build();

		return new MultiLayerNetwork( conf ) ;
	}

}


class DSI {
	private final int batchSize  ;
	private final File parentDir ;
	private final String features[] ;
	private final String labels[] ;
	private int currentIndex ;

	BufferedReader featureReader ;
	BufferedReader labelReader ;
	
	public DSI( File parentDir, int batchSize, String features[], String labels[] ) {
		this.features = features ;
		this.labels = labels ;
		this.batchSize = batchSize ;
		this.parentDir = parentDir ;
	}
	
	public DataSet next() throws IOException {
		DataSet rc = null ;		
		int batchIndex = 0 ;
		
		INDArray f = Nd4j.create( batchSize, AnalyzeFile.NUM_INPUTS ) ;
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
			String fcols[] = features.split(",") ;
			for( int c=0 ; c<fcols.length ; c++ ) {
				float v = Float.parseFloat( fcols[c] ) ;
				f.putScalar(batchIndex, c, v ) ;
			}
			String lcols[] = labels.split(",") ;
			for( int c=3 ; c<lcols.length ; c++ ) {
				float v = Float.parseFloat( lcols[c-3] ) ;
				l.putScalar(batchIndex, c-3, v ) ;
			}
			batchIndex++ ;
		}
		
		if( batchIndex > 0 ) {
			f = f.get(NDArrayIndex.interval(0,batchIndex), NDArrayIndex.all()) ;
			l = l.get(NDArrayIndex.interval(0,batchIndex), NDArrayIndex.all()) ;
			rc = new DataSet(f, l) ;
		}
		return rc ;
	}
	
	
	public void reset() throws IOException {
		if( featureReader != null ) featureReader.close(); 
		if( labelReader != null ) labelReader.close();
		featureReader = null ;
		labelReader = null ;
		currentIndex = 0 ;	
	}

	
	protected boolean nextFile() throws IOException {

		if( featureReader != null ) featureReader.close(); 
		if( labelReader != null ) labelReader.close();
		featureReader = null ;
		labelReader = null ;
		
		boolean rc = currentIndex < features.length ;
		if( rc ) {
			File f1 = new File( parentDir, features[currentIndex] ) ;
			Reader rf = new FileReader( f1 ) ;
			File f2 = new File( parentDir, labels[currentIndex] ) ;
			Reader rl = new FileReader( f2 ) ;
			currentIndex++ ;
			
			featureReader = new BufferedReader( rf ) ;
			labelReader = new BufferedReader( rl ) ;
		}
		return rc ;
	}
}