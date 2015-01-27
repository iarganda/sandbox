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
import net.imagej.ops.gauss.GaussRAI2RAI;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin( type = Op.class, name= "gaussian" )
public class Gaussian <T extends RealType<T>>
extends
AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
implements PixelFeatureComputer<T, DoubleType>{
	
	@Parameter
	private OpService ops;

	@Parameter
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
			RandomAccessibleInterval<DoubleType> output){
		
		ops.run( GaussRAI2RAI.class, output, input, sigma );
				
		return output;
	}
	
	public static void main(String[] args) {
		Context context = new Context();
		
		OpService ops = context.service(OpService.class);
		UIService ui = context.service(UIService.class);
		IOService io = context.service(IOService.class);
		
		try {
			Object image = io.open( "/Users/Nachete/data/challenge-2d/train-volume-1.tif" );
			Img<DoubleType> run = (Img<DoubleType>)ops.run( Gaussian.class, Img.class, image, 7);
			
			ui.show(run);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
