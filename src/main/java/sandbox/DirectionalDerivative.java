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
import net.imagej.ops.convolve.ConvolveNaive;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = Op.class, name = "derivative" )
public class DirectionalDerivative<T extends RealType<T>>
extends
AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
implements PixelFeatureComputer<T, DoubleType>{

	@Parameter
	private OpService ops;
	
	@Parameter ( required = true )
	private int dimension;
		
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
			
		long[] dim = new long[ input.numDimensions() ];
		
		dim[0] = 3;
		dim[1] = 1;
		
		for( int i=2; i<dim.length; i++ )
			dim[ i ] = 1;
		
		Img<DoubleType> h = ArrayImgs.doubles( new double[]{ 1, 2, 1 }, dim );
		Img<DoubleType> hp = ArrayImgs.doubles( new double[]{ -1, 0, 1 }, dim ); 
		
		RandomAccessibleInterval<DoubleType> doubleInput 
			= Converters.convert( input, new ConvertToDoubleType(), new DoubleType());

		RandomAccessibleInterval<DoubleType> aux  
			= doubleInput;

		// calculate derivative on that direction with 1-d filter
		for( int j=dim.length-1; j>=0; j-- )
		{								
			RandomAccessibleInterval<DoubleType> derivative 
				= new ArrayImgFactory<DoubleType>().create( doubleInput, new DoubleType() );

			if( j != 0 )
			{
				IntervalView<DoubleType> filter = dimension == j ? 
					Views.rotate( hp, 0, j ) : Views.rotate( h, 0, j );
	
				ops.run( ConvolveNaive.class, derivative, Views.extendMirrorSingle( aux ), filter );
			}
			else
			{
				if( dimension == j )
					ops.run( ConvolveNaive.class, derivative, Views.extendMirrorSingle( aux ), hp );
				else
					ops.run( ConvolveNaive.class, derivative, Views.extendMirrorSingle( aux ), h );
			}
			aux = derivative;
		}

		ops.add( output, aux );

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
			Dataset image = (Dataset) io.open( "/Users/Nachete/data/challenge-2d/train-volume-1.tif" );
			//Dataset image = (Dataset) io.open( "/Users/Nachete/data/small.tif" );
			//Dataset image = (Dataset) io.open( "/Users/Nachete/data/mitosis.tif" );
			Object img = image.getImgPlus();
			
			long start = System.currentTimeMillis();
			
			Img<DoubleType> run = (Img<DoubleType>)ops.run( DirectionalDerivative.class, null, img , 0 );
			
			long end = System.currentTimeMillis();
			System.out.println("Directional derivative took " + (end - start) + " ms");
			
			ui.show(run);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}
	
}
