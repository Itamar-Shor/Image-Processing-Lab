package il.ac.tau.adviplab.androidopencvlab;

import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("SameParameterValue")
public class MyImageProc extends CameraListener{

    static final int HIST_NORMALIZATION_CONST = 1;
    private static int COMP_MATCH_DISTANCE = 99;

    static void calcHist(Mat image, Mat[] histList, int histSizeNum, int normalizationConst, int normalizationNorm) {
        int numberOfChannels = Math.min(image.channels(), 3);
        // if the image is RGBA,
        // ignore the last channel (Alpha channel)
        MatOfInt[] channels = new MatOfInt[numberOfChannels];
        for (int i = 0; i < numberOfChannels; i++) {
            channels[i] = new MatOfInt(i);
        }
        Mat mat0 = new Mat();
        MatOfInt histSize = new MatOfInt(histSizeNum);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        // calc histogram for each color channel
        int chIdx = 0;
        for (MatOfInt channel : channels) {
            Imgproc.calcHist(Collections.singletonList(image), channel, mat0, histList[chIdx], histSize, ranges);
            Core.normalize(histList[chIdx], histList[chIdx], normalizationConst, 0, normalizationNorm);
            chIdx++;
        }


        // Here will come the function Imgproc.calcHist
        for (MatOfInt channel : channels) {
            channel.release();
        }
        mat0.release();
        histSize.release();
        ranges.release();
    }

    static void calcHist(Mat image, Mat[] histList, int histSizeNum) {
        calcHist(image, histList, histSizeNum, 1, Core.NORM_L1);
    }

    private static void showHist(Mat image, Mat[] histList, int histSizeNum, int offset, int thickness) {
        int numberOfChannels = Math.min(image.channels(), 3); // if image is RGBA, ignore the last channel (Alpha channel)
        float[] buff = new float[histSizeNum];
        Point mP1 = new Point();
        Point mP2 = new Point();
        Scalar[] mColorsRGB;
        mColorsRGB = new Scalar[] {new Scalar(255, 0, 0), new Scalar(0, 255, 0), new Scalar(0, 0, 255)};
        for (int chIdx = 0; chIdx < numberOfChannels; chIdx++) {
            Core.normalize(histList[chIdx], histList[chIdx], image.height() / 2.0, 0, Core.NORM_INF);
            histList[chIdx].get(0, 0, buff);
            for (int h = 0; h < histSizeNum; h++) {
                mP1.x = mP2.x = offset + (chIdx * (histSizeNum + 10) + h) * thickness;
                mP1.y = image.height() - 50;
                mP2.y = mP1.y - 2 - (int) buff[h];
                Imgproc.line(image, mP1, mP2, mColorsRGB[chIdx], thickness);
            }
        }
    }

    static void showHist(Mat image, Mat[] histList, int histSizeNum) {
        int thickness = Math.min(image.width() / (100+10) / 5, 5);
        int offset = image.width() / 2 + (image.width() - (5*100 + 4*10) * thickness) - 300;
        showHist(image, histList, histSizeNum, offset, thickness);
    }

    private static void calcCumulativeHist(Mat hist, Mat cumuHist) {
        int N = (int)hist.total();
        float[] temp = new float[N];
        // copy hist to temp
        hist.get(0 ,0, temp);
        for (int i=1; i<N; i++){
            temp[i] += temp[i-1];
        }
        // copy temp to cumHist
        cumuHist.put(0, 0, temp);
    }

    static void calcCumulativeHist(Mat[] hist, Mat[] cumuHist, int numberOfChannels) {
        for (int chIdx = 0; chIdx < numberOfChannels; chIdx++) {
            cumuHist[chIdx].create(hist[chIdx].size(), hist[chIdx].type());
            calcCumulativeHist(hist[chIdx], cumuHist[chIdx]);
        }
    }


    static void equalizeHist(Mat image) {
        int numberOfChannels = image.channels();
        if (numberOfChannels > 1) {
            List<Mat> RGBAChannels = new ArrayList<>(numberOfChannels);
            Core.split(image, RGBAChannels);
            // Equalize the channels R,G,B,
            // Don’t equalize the channel A`
            int i = 0;
            for (Mat colorChannel : RGBAChannels) {
                if (i != 3) {
                    Imgproc.equalizeHist(colorChannel, colorChannel);
                    i++;
                }
            }
            Core.merge(RGBAChannels, image);
            for (Mat colorChannel : RGBAChannels) {
                colorChannel.release();
            }
        } else {
            Imgproc.equalizeHist(image, image);
        }
    }

    private static void matchHistogram(Mat histSrc, Mat histDst, Mat lookUpTable) {
        // Mat histSrc - source histogram
        // Mat histDst - destination histogram
        // Mat lookUpTable - look-up table
        // Add your implementation here
        int N = (int)histSrc.total();
        float[] srcHistArr = new float[N];
        float[] dstHistArr = new float[N];
        int[] lutArr     = new int[N];

        Mat srcCumHist = new Mat(histSrc.size() ,histSrc.type());
        Mat dstCumHist = new Mat(histSrc.size() ,histSrc.type());

        calcCumulativeHist(histSrc, srcCumHist);
        calcCumulativeHist(histDst, dstCumHist);

        srcCumHist.get(0 ,0, srcHistArr);
        dstCumHist.get(0 ,0, dstHistArr);
        int j = 0;

        for(int i=0; i<N; i++){
            while(j < N && srcHistArr[i] > dstHistArr[j]) j++;
            lutArr[i] = j;
        }
        
        lookUpTable.put(0, 0, lutArr);

        srcCumHist.release();
        dstCumHist.release();
    }

    private static void applyIntensityMapping(Mat srcImage, Mat lookUpTable) {
        Mat tempMat = new Mat();
        Core.LUT(srcImage, lookUpTable, tempMat);
        tempMat.convertTo(srcImage, CvType.CV_8UC1);
        tempMat.release();
    }

    static void matchHist(Mat srcImage, Mat dstImage, Mat[] srcHistArray, Mat[]
            dstHistArray, boolean histShow)
    {
        Mat lookupTable = new Mat(256, 1, CvType.CV_32SC1);
        //Mat[] afterSrcHist = new Mat[] {new Mat(), new Mat(), new Mat()};
        // calc histogram
        calcHist(srcImage, srcHistArray, 256);

        compareHistograms(srcImage, srcHistArray[0], dstHistArray[0], new Point(3, 27),COMP_MATCH_DISTANCE, "Before: ");

        // find mapping
        matchHistogram(srcHistArray[0], dstHistArray[0], lookupTable);
        // apply mapping
        applyIntensityMapping(srcImage, lookupTable);

        calcHist(srcImage, srcHistArray, 256);

        compareHistograms(srcImage, srcHistArray[0], dstHistArray[0], new Point(3, 50),COMP_MATCH_DISTANCE, "After: ");

        if(histShow){
            calcHist(dstImage, srcHistArray, 100, MyImageProc.HIST_NORMALIZATION_CONST, Core.NORM_L1);
            int thickness = Math.min(srcImage.width() / (100+10) / 5, 5);
            int offset = srcImage.width() / 2 + (srcImage.width() - (5*100 + 4*10) * thickness);
            showHist(srcImage, srcHistArray, 100, offset, thickness);
        }

        lookupTable.release();
    }

    private static void compareHistograms(Mat image, Mat h1, Mat h2, Point
            point, int compType, String string) {
        double dist;
        if (compType == COMP_MATCH_DISTANCE) {
            dist = matchDistance(h1, h2); //Computes the match distance
        } else {
            dist = Imgproc.compareHist(h1, h2, compType);
        }
        Imgproc.putText(image, string + String.format(Locale.ENGLISH, "%.2f",
                dist),
                point,
                Imgproc.FONT_HERSHEY_COMPLEX_SMALL,
                0.8,
                new Scalar(255, 255, 255),
                1);
    }

    private static double matchDistance(Mat h1, Mat h2) {
        double dist = 0;
        int N = (int)h1.total();
        float[] h1HistArr = new float[N];
        float[] h2HistArr = new float[N];
        Mat h1CumHist = new Mat(h1.size() ,h1.type());
        Mat h2CumHist = new Mat(h2.size() ,h2.type());

        calcCumulativeHist(h1, h1CumHist);
        calcCumulativeHist(h2, h2CumHist);

        h1CumHist.get(0 ,0, h1HistArr);
        h2CumHist.get(0 ,0, h2HistArr);

        for(int i=0; i<N; i++){
            dist += Math.abs(h1HistArr[i] - h2HistArr[i]);
        }

        h1CumHist.release();
        h2CumHist.release();

        return dist;
    }

}
