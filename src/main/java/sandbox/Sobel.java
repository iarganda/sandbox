package sandbox;

import java.io.IOException;

import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imagej.ops.AbstractOutputFunction;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.gauss.GaussRAI2RAI;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin( type = Op.class, name = "sobel" )
public class Sobel<T extends RealType<T>>
extends
AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
implements PixelFeatureComputer<T, DoubleType>{

	@Parameter
	private OpService ops;
	
	@Parameter ( required = false )
	private int sigma;
		
	@Override
	public RandomAccessibleInterval<DoubleType> createOutput(
			RandomAccessibleInterval<T> input ) {
		return new ArrayImgFactory<DoubleType>()
				.create(input, new DoubleType());
	}

	@Override
	protected RandomAccessibleInterval<DoubleType> safeCompute(
			RandomAccessibleInterval<T> input,
			RandomAccessibleInterval<DoubleType> output) {

		RandomAccessibleInterval<DoubleType> preprocessedInput 
			= Converters.convert( input, new ConvertToDoubleType(), new DoubleType());
		
		if( sigma != 0 )
		{
			preprocessedInput = new ArrayImgFactory<DoubleType>().create(input, new DoubleType());			
			ops.run( GaussRAI2RAI.class, preprocessedInput, input, sigma );
		}
		
		// For each dimension
		for( int i=0; i<input.numDimensions(); i++ )
		{
			RandomAccessibleInterval<DoubleType> aux  
				= new ArrayImgFactory<DoubleType>().create( preprocessedInput, new DoubleType() );
			
			ops.run( "derivative", aux, preprocessedInput, i );
			
			ops.run( "square", aux );
			ops.add( output, aux );
		}		
		
		ops.run( "sqrt", output );
		
		return output;
	}
	
	public class ConvertToDoubleType implements Converter<T, DoubleType>{
		@Override
		public void convert(T input, DoubleType output) {
			output.setReal( input.getRealDouble() );			
		}		
	}
	

	public static void main(String[] args) {
		Context context = new Context();
		
		OpService ops = context.service(OpService.class);
		
		//System.out.println( ops.help( "convolve" ) );
		
		UIService ui = context.service(UIService.class);
		IOService io = context.service(IOService.class);
		
		try {
			//Dataset image = (Dataset) io.open( "/Users/Nachete/data/challenge-2d/train-volume-1.tif" );
			Dataset image = (Dataset) io.open( "/Users/Nachete/data/Bikesgray.jpg" );
			Object img = image.getImgPlus();
			
			long start = System.currentTimeMillis();
			
			Img<DoubleType> run = (Img<DoubleType>)ops.run( Sobel.class, null, img );
			
			long end = System.currentTimeMillis();
			System.out.println("Sobel took " + (end - start) + " ms");
			
			ui.show(run);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}
	
}
