package sandbox;

import java.io.IOException;

import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.ops.AbstractOutputFunction;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.arithmetic.add.AddRandomAccessibleIntervalToIterableInterval;
import net.imagej.ops.gauss.GaussRAI2RAI;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin( type = Op.class, name= "dog" )
public class DoG <T extends RealType<T>>
extends
AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
implements PixelFeatureComputer<T, DoubleType>{
	
	@Parameter
	private OpService ops;

	@Parameter
	private int sigma1;
	
	@Parameter
	private int sigma2;
	
	
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
		
		ops.run( GaussRAI2RAI.class, output, input, sigma1 );
		
		RandomAccessibleInterval<DoubleType> output2 
		   = new ArrayImgFactory<DoubleType>().create(input, new DoubleType());
		
		ops.run( GaussRAI2RAI.class, output2, input, sigma2 );
		
		ops.run( MultiplyConstantToImageFunctional.class, output, input, new DoubleType( -1.0 ) );
		
		ops.run( AddRandomAccessibleIntervalToIterableInterval.class, output, output2 );
				
		return output;
	}
	
	public static void main(String[] args) {
		Context context = new Context();
		
		OpService ops = context.service(OpService.class);
		UIService ui = context.service(UIService.class);
		IOService io = context.service(IOService.class);		
		
		try {
			long start = System.currentTimeMillis();
			Object image = io.open( "/Users/Nachete/data/challenge-2d/train-volume.tif" );
			Img<DoubleType> run = (Img<DoubleType>)ops.run( DoG.class, Img.class, image, 3, 5 );
			
			long end = System.currentTimeMillis();
			
			System.out.println( "DoG took " + (end-start) + " ms");
			
			ui.show(run);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}

