package sandbox;

import net.imagej.ops.OutputFunction;
import net.imglib2.RandomAccessibleInterval;

public interface PixelFeatureComputer<I, O>
		extends
		OutputFunction<RandomAccessibleInterval<I>, RandomAccessibleInterval<O>> {

}
