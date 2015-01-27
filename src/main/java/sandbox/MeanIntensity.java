package sandbox;

import java.io.IOException;

import net.imagej.ops.AbstractOutputFunction;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops.Map;
import net.imagej.ops.Ops.Mean;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@Plugin(type = Op.class, name = "mean")
public class MeanIntensity<T extends RealType<T> & NativeType<T>>
		extends
		AbstractOutputFunction<RandomAccessibleInterval<T>, RandomAccessibleInterval<DoubleType>>
		implements PixelFeatureComputer<T, DoubleType> {

	@Parameter
	private OpService ops;

	@Parameter
	private int sigma;

	@Override
	public RandomAccessibleInterval<DoubleType> createOutput(
			RandomAccessibleInterval<T> input) {
		return new ArrayImgFactory<DoubleType>()
				.create(input, new DoubleType());
	}

	@Override
	protected RandomAccessibleInterval<DoubleType> safeCompute(
			RandomAccessibleInterval<T> input,
			RandomAccessibleInterval<DoubleType> output) {

		Shape s = new RectangleShape(sigma, false);

		IntervalView<T> interval = Views
				.interval(Views.extendMirrorSingle(input),
						Intervals.expand(input, sigma));
		
		ops.run(Map.class, output, s.neighborhoods(interval),
				ops.op( Mean.class, DoubleType.class, Iterable.class));

		return output;
	}

	public static void main(String[] args) {
		Context context = new Context();

		OpService ops = context.service(OpService.class);
		UIService ui = context.service(UIService.class);
		IOService io = context.service(IOService.class);

		try {
			Object image = 
				io.open( "/Users/Nachete/data/challenge-2d/train-volume-1.tif" );

			long start = System.currentTimeMillis();

			Img<DoubleType> run = (Img<DoubleType>) ops.run(
					MeanIntensity.class, Img.class, image, 3);

			long end = System.currentTimeMillis();

			System.out.println("MeanIntensity took " + (end - start) + " ms");

			ui.show(run);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
