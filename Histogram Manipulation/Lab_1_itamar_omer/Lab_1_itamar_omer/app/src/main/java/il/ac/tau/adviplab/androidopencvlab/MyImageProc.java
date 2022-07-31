package il.ac.tau.adviplab.androidopencvlab;

import android.util.Log;

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

@SuppressWarnings("SameParameterValue")
public class MyImageProc extends CameraListener{

    static final int HIST_NORMALIZATION_CONST = 1;

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
        int thickness = Math.min(image.width() / (histSizeNum + 10) / 5, 5);
        showHist(image, histList, histSizeNum, thickness, (image.width() - (5 * histSizeNum + 4 * 10) * thickness) / 2);
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
        Mat srcCumHist = new Mat();
        Mat dstCumHist = new Mat();

        calcCumulativeHist(histSrc, srcCumHist);
        calcCumulativeHist(histDst, dstCumHist);

        srcCumHist.get(0 ,0, srcHistArr);
        dstCumHist.get(0 ,0, dstHistArr);
        int j = 0;

        for(int i=0; i<N; i++){
            while(srcHistArr[i] > dstHistArr[j]) j++;
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
        // calc histogram
        calcHist(srcImage, srcHistArray, 256);
        // find mapping
        matchHistogram(srcHistArray[0], dstHistArray[0], lookupTable);
        // apply mapping
        applyIntensityMapping(srcImage, lookupTable);

        lookupTable.release();

        if(histShow){
            //TODO: check this
            showHist(dstImage, dstHistArray, 100);
        }
    }

}
