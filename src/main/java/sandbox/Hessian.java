package sandbox;

import java.io.IOException;

import net.imagej.ops.AbstractOutputFunction;
import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@Plugin( type = Op.class, name= "hessian" )
public class Hessian <T extends RealType<T>>
	extends
	AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
	implements PixelFeatureComputer<T, DoubleType>{
	
	@Parameter
	private OpService ops;
		
	@Override
	public RandomAccessibleInterval<DoubleType> createOutput(
			RandomAccessibleInterval<T> input ) {
		return new ArrayImgFactory<DoubleType>()
				.create(input, new DoubleType());
	}

	@Override
	protected RandomAccessibleInterval<DoubleType> safeCompute(
			RandomAccessibleInterval<T> input,
			RandomAccessibleInterval<DoubleType> output){
		
		RandomAccessibleInterval<DoubleType> doubleInput 
			= Converters.convert( input, new ConvertToDoubleType(), new DoubleType());
		
		// Hessian matrix
		RandomAccessibleInterval<DoubleType>[][] hessianMatrix =
				new RandomAccessibleInterval[ input.numDimensions() ][ input.numDimensions() ];
		
		for( int i = 0; i < input.numDimensions(); i ++ )
		{
			RandomAccessibleInterval<DoubleType> di
			   = new ArrayImgFactory<DoubleType>().create( doubleInput, new DoubleType() );
			ops.run( "derivative", di, doubleInput, i );
			
			for( int j = 0; j < input.numDimensions(); j++ )
			{
				hessianMatrix[ i ][ j ] = 
						new ArrayImgFactory<DoubleType>().create( doubleInput, new DoubleType() );
				ops.run( "derivative", hessianMatrix[ i ][ j ], di, j );
				hessianMatrix[ j ][ i ] = hessianMatrix[ i ][ j ];
			}
		}
		

		
		// Hessian module
		for( int i=0; i<input.numDimensions(); i++ )
			for( int j=i; j<input.numDimensions(); j++ )
			{
				ops.run( "square", hessianMatrix[i][j] );
			}

		ops.run( "sqrt", output );
		
		
		// Hessian trace (n-dimensional)
//		for( int i=0; i<input.numDimensions(); i++ )
//			ops.add( output, hessianMatrix[ i ][ i ] );
		
		// Hessian determinant (n = 2)
//		if( input.numDimensions() == 2 )
//		{
//			// determinant = dxdx * dydy - dxdy * dydx
//			ops.run( "multiply", output, hessianMatrix[ 0 ][ 0 ], hessianMatrix[ 1 ][ 1 ] );
//			ops.run( "square", hessianMatrix[ 0 ][ 1 ] );
//			ops.run( MultiplyConstantToImageFunctional.class, hessianMatrix[ 0 ][ 1 ], hessianMatrix[ 0 ][ 1 ], new DoubleType( -1.0 ) );
//			ops.add( output, hessianMatrix[ 0 ][ 1 ] );
//		}
		
		
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
		UIService ui = context.service(UIService.class);
		IOService io = context.service(IOService.class);		
		
		try {
			long start = System.currentTimeMillis();
			Object image = io.open( "/Users/Nachete/data/challenge-2d/train-volume-1.tif" );
			Img<DoubleType> run = (Img<DoubleType>)ops.run( Hessian.class, image );
			
			long end = System.currentTimeMillis();
			
			System.out.println( "Hessian2D took " + (end-start) + " ms");
			
			ui.show(run);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}


}

