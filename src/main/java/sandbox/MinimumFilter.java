package sandbox;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.io.Opener;
import ij.plugin.Filters3D;
import ij.util.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.Context;

import net.imagej.ops.OpService;
import net.imagej.ops.Ops.Map;
import net.imagej.ops.Ops.Min;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class MinimumFilter
{
	private final Img< FloatType > image;

	private Img< FloatType > output;

	private final long sigma;
	
	public static String fileName = "/Users/Nachete/data/flybrain-32bit.tif";

	public MinimumFilter( final long sigma )
	{
		// define the file to open
		final File file = new File( fileName );

		// open a file with ImageJ
		final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

		// display it via ImageJ
		// imp.show();

		// wrap it into an ImgLib image (no copying)
		image = ImagePlusAdapter.wrap( imp );

		// create a new Image with the same properties
		output = image.factory().create( image, image.firstElement() );

		this.sigma = sigma;
	}

	public void run()
	{
		// get mirror view
		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite = Views.extendMirrorSingle( image );

		final Cursor< FloatType > cursorInput = image.cursor();
		final Cursor< FloatType > cursorOutput = output.cursor();

		final FloatType min = image.firstElement().createVariable();

		// iterate over the input
		while ( cursorInput.hasNext() )
		{
			cursorInput.fwd();
			cursorOutput.fwd();

			// define a hypersphere (n-dimensional sphere)
			final HyperSphere< FloatType > hyperSphere = new HyperSphere< FloatType >( infinite, cursorInput, sigma );

			// create a cursor on the hypersphere
			final HyperSphereCursor< FloatType > cursor2 = hyperSphere.cursor();

			cursor2.fwd();
			min.set( cursor2.get() );

			while ( cursor2.hasNext() )
			{
				cursor2.fwd();
				if ( cursor2.get().compareTo( min ) <= 0 )
					min.set( cursor2.get() );
			}

			// set the value of this pixel of the output image to the minimum
			// value of the sphere
			cursorOutput.get().set( min );
		}
	}

	public void run2()
	{
		// get mirror view
		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite = Views.extendMirrorSingle( image );

		final Cursor< FloatType > cursorInput = image.localizingCursor();
		final Cursor< FloatType > cursorOutput = output.cursor();

		final FloatType min = new FloatType();

		// define a hypersphere (n-dimensional sphere)
		final HyperSphere< FloatType > hyperSphere = new HyperSphere< FloatType >( infinite, cursorInput, sigma );

		// create a cursor on the hypersphere
		final HyperSphereCursor< FloatType > cursor2 = hyperSphere.cursor();

		// iterate over the input
		while ( cursorInput.hasNext() )
		{
			cursorInput.fwd();
			cursorOutput.fwd();

			hyperSphere.updateCenter( cursorInput );
			cursor2.reset();
			min.set( cursor2.next() );

			while ( cursor2.hasNext() )
			{
				cursor2.fwd();
				if ( cursor2.get().compareTo( min ) <= 0 )
					min.set( cursor2.get() );
			}

			// set the value of this pixel of the output image to the minimum
			// value of the sphere
			cursorOutput.next().set( min );
		}
	}

	public void run3()
	{
		// get mirror view
		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite = Views.extendMirrorSingle( image );

		final Cursor< FloatType > cursorOutput = output.localizingCursor();

		final FloatType min = new FloatType();

		// define a hypersphere (n-dimensional sphere)
		final HyperSphere< FloatType > hyperSphere = new HyperSphere< FloatType >( infinite, cursorOutput, sigma );

		// create a cursor on the hypersphere
		final HyperSphereCursor< FloatType > cursor2 = hyperSphere.cursor();

		// iterate over the input
		while ( cursorOutput.hasNext() )
		{
			cursorOutput.fwd();
			hyperSphere.updateCenter( cursorOutput );
			cursor2.reset();
			min.set( cursor2.next() );

			while ( cursor2.hasNext() )
			{
				cursor2.fwd();
				if ( cursor2.get().compareTo( min ) <= 0 )
					min.set( cursor2.get() );
			}

			// set the value of this pixel of the output image to the minimum
			// value of the sphere
			cursorOutput.get().set( min );
		}
	}
	

	public Interval shrink( final Interval interval, final long border )
	{
		final int n = interval.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		interval.min( min );
		interval.max( max );
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] += border;
			max[ d ] -= border;
		}
		return new FinalInterval( min, max );
	}

	public Interval expand( final Interval interval, final long border )
	{
		final int n = interval.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		interval.min( min );
		interval.max( max );
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] -= border;
			max[ d ] += border;
		}
		return new FinalInterval( min, max );
	}

	private ArrayList< Interval > splitCenterBordersRecursively( final Interval interval, final long border, final int d, final ArrayList< Interval > intervals )
	{
		if ( d >= 0 )
		{
			final int n = interval.numDimensions();
			final long[] min = new long[ n ];
			final long[] max = new long[ n ];
			interval.min( min );
			interval.max( max );
			min[ d ] = interval.min( d );
			max[ d ] = interval.min( d ) + border - 1;
			intervals.add( new FinalInterval( min, max ) );
			min[ d ] = interval.max( d ) - border + 1;
			max[ d ] = interval.max( d );
			intervals.add( new FinalInterval( min, max ) );
			max[ d ] = interval.max( d ) - border;
			min[ d ] = interval.min( d ) + border;
			return splitCenterBordersRecursively( new FinalInterval( min, max ), border, d - 1, intervals );
		}
		intervals.add( interval );
		return intervals;
	}

	public ArrayList< Interval > splitCenterBorders( final Interval interval, final long border )
	{
		return splitCenterBordersRecursively( interval, border, interval.numDimensions() - 1, new ArrayList< Interval >() );
	}

	public void run4()
	{
		// get mirror view
		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite = Views.extendMirrorSingle( image );

		// split output into regions which require out-of-bounds and regions
		// which don't
		final long border = sigma + 1;
		final ArrayList< Interval > outputIntervals = splitCenterBorders( output, border );

		for ( final Interval outputInterval : outputIntervals )
		{
			// take the input interval (only the region which we will access)
			// out of infinite
			// it needs additional border pixels, therefore expand()...
			// HyperSphere will get a randomAccess() from inputView and Views
			// will decide whether out-of-bounds is required or not.
			final RandomAccessibleInterval< FloatType > inputView = Views.interval( infinite, expand( outputInterval, border ) );

			final Cursor< FloatType > cursorOutput = Views.iterable( Views.interval( output, outputInterval ) ).localizingCursor();

			final FloatType min = new FloatType();

			// define a hypersphere (n-dimensional sphere)
			final HyperSphere< FloatType > hyperSphere = new HyperSphere< FloatType >( inputView, cursorOutput, sigma );

			// create a cursor on the hypersphere
			final HyperSphereCursor< FloatType > cursor2 = hyperSphere.cursor();

			// iterate over the input
			while ( cursorOutput.hasNext() )
			{
				cursorOutput.fwd();
				hyperSphere.updateCenter( cursorOutput );
				cursor2.reset();
				min.set( cursor2.next() );

				while ( cursor2.hasNext() )
				{
					cursor2.fwd();
					if ( cursor2.get().compareTo( min ) <= 0 )
						min.set( cursor2.get() );
				}

				// set the value of this pixel of the output image to the
				// minimum
				// value of the sphere
				cursorOutput.get().set( min );
			}
		}
	}
	
	// Multi-threaded version of run3
	public void runMT()
	{
		// get mirror view
		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite = Views.extendMirrorSingle( image );

		long numElements = 1;
		
		for( int i=0; i < image.numDimensions(); i++ )
			numElements *= image.dimension( i );
		
		final long lastElement = numElements;
		
		final AtomicInteger ai = new AtomicInteger(0);
        final int n_cpus = Prefs.getThreads();
        
        final int dec = (int) Math.ceil( numElements / (double) n_cpus);
        
        Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
        for (int ithread = 0; ithread < threads.length; ithread++) 
        {
            threads[ithread] = new Thread() {
            	 public void run() {
                 	for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) 
                 	{
                 		long minPos = dec * k;
                 		long maxPos = dec * ( k + 1 );
                 		if (minPos<0)
                             minPos = 0;
                         if (maxPos > lastElement - 1 )
                             maxPos = lastElement - 1;
                         
                         min( minPos, maxPos, infinite );
                 		
                    }
                 }
             };
         }
         ThreadUtil.startAndJoin(threads);            		
				
	}
	
    private void min( 
    		final long minPos, 
    		final long maxPos, 
    		final ExtendedRandomAccessibleInterval< FloatType, Img< FloatType >> infinite )
    {
		final Cursor< FloatType > cursorOutput = output.cursor();
		
		cursorOutput.jumpFwd(minPos);
		
		// define a hypersphere (n-dimensional sphere)
		final HyperSphere< FloatType > hyperSphere = new HyperSphere< FloatType >( infinite, cursorOutput, sigma );
		
		// create a cursor on the hypersphere
		final HyperSphereCursor< FloatType > cursor2 = hyperSphere.cursor();
		
		final FloatType min = image.firstElement().createVariable();				

		long processedPos = minPos-1;
		
		// iterate over the input
		while ( cursorOutput.hasNext() && processedPos < maxPos )
		{
			cursorOutput.fwd();

			hyperSphere.updateCenter( cursorOutput );
			cursor2.reset();
			min.set( cursor2.next() );

			while ( cursor2.hasNext() )
			{
				cursor2.fwd();
				if ( cursor2.get().compareTo( min ) <= 0 )
					min.set( cursor2.get() );
			}

			// set the value of this pixel of the output image to the minimum
			// value of the sphere
			cursorOutput.next().set( min );
			
			processedPos ++;
		}
    }
            
            
            
	public void show()
	{
		// display it via ImgLib using ImageJ
		ImageJFunctions.show( output );
	}
	
	
	void runShape()
	{
		// get mirror view
		final IntervalView<FloatType>  infinite = Views.interval( Views.extendMirrorSingle( image ), image );

		final Shape shape = new RectangleShape( (int) sigma, false );
		
		final RandomAccess<FloatType> ra = output.randomAccess();
		
		final FloatType min = new FloatType();

		for ( final Neighborhood<FloatType> neighborhood : shape.neighborhoods( infinite ) ) 
		{

			final Cursor<FloatType> cursor = neighborhood.cursor();
			min.setReal( min.getMaxValue() );

			while ( cursor.hasNext() ) 
			{
				cursor.fwd();

				final FloatType val = cursor.get();
				if (val.compareTo( min ) < 0) {
					min.set(val);
				}
			}

			ra.setPosition(neighborhood);
			ra.get().set( min );
		}		
	}
	
	void runImageJ1( ImagePlus im )
	{
		final ImageStack is = Filters3D.filter(im.getImageStack(),Filters3D.MIN, (float)sigma, (float)sigma, (float)sigma);
		final ImagePlus ip = new ImagePlus( "Minimum_sigma=" + sigma, is );							
		output = ImagePlusAdapter.wrap( ip );
	}

	public static void main( final String[] args )
	{
		// open an ImageJ window
		new ImageJ();

		int numThreads = 1;
		
		ImagePlus im = IJ.openImage( MinimumFilter.fileName );
		Prefs.setThreads( numThreads );
		
		
		for ( long l = 1; l <= 4; l *= 2 )
		{
			final MinimumFilter filter = new MinimumFilter( l );
			final long start = System.currentTimeMillis();
			// run the example
			filter.runShape();
			final long end = System.currentTimeMillis();
			//filter.show();
			System.out.println( "ImgLib2 Minimum filter with sigma = " + l + 
					" took " + ( end - start ) + "ms." );
			// run the ImageJ1 example
			filter.runImageJ1( im );
			final long end2 = System.currentTimeMillis();
			
			System.out.println( "ImageJ1 Minimum filter with sigma = " + l + 
					" took " + ( end2 - end ) + "ms. (# threads = " + numThreads + ")" );
		}
	}
}