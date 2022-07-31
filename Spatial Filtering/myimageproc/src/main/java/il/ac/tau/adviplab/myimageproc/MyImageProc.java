package il.ac.tau.adviplab.myimageproc;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MyImageProc {

    public static float SIGMA_SPATIAL_DEFAULT = 10;
    public static float SIGMA_INTENSITY_DEFAULT = 50;
    public static float SIGMA_SPATIAL_MAX = 50;
    public static float SIGMA_INTENSITY_MAX = 50;



    private static void sobelFilter(Mat inputImage, Mat outputImage) {
        //Applies Sobel filter to image
        Mat grad_x = new Mat();
        Mat grad_y = new Mat();
        int ddepth = CvType.CV_16SC1;
        Imgproc.Sobel(inputImage, grad_x, ddepth, 1, 0);
        Core.convertScaleAbs(grad_x, grad_x, 10, 0);
        Imgproc.Sobel(inputImage, grad_y, ddepth, 0, 1);
        Core.convertScaleAbs(grad_y, grad_y, 10, 0);
        Core.addWeighted(grad_x, 0.5, grad_y, 0.5, 0, outputImage);
        grad_x.release();
        grad_y.release();
    }


    private static void displayFilter(Mat displayImage, Mat filteredImage,
                                      int[] window) {
        Mat displayInnerWindow = displayImage.submat(window[0], window[1],
                window[2], window[3]);
        Mat filteredInnerWindow = filteredImage.submat(window[0],
                window[1], window[2], window[3]);
        if (displayImage.channels() > 1) {
            Imgproc.cvtColor(filteredInnerWindow, displayInnerWindow,
                    Imgproc.COLOR_GRAY2BGRA, 4);
        } else {
            filteredInnerWindow.copyTo(displayInnerWindow);
        }
        displayInnerWindow.release();
        filteredInnerWindow.release();
    }

    private static int[] setWindow(Mat image) {
        int top, bottom, left, right;
        int h, w;

        h = image.height();
        w = image.width();

        top = Math.max(10, h/20);
        bottom = Math.max(10,h-h/20);
        left = Math.max(10, w/20);
        right = Math.max(10, w-w/20);
        // make sure the height of the window is divisible by 16
        int d = (bottom - top) % 16;
        top += d/2;
        bottom -= (d/2 + (d%2));
        return new int[]{top, bottom, left, right};
    }

    public static void sobelCalcDisplay(Mat displayImage, Mat inputImage,
                                        Mat filteredImage) {
    //The function applies the Sobel filter and returns the result in format suitable for display.
        sobelFilter(inputImage, filteredImage);
        int[] window = setWindow(displayImage);
        displayFilter(displayImage, filteredImage, window);
    }

    private static void gaussianFilter(Mat inputImage, Mat outputImage, float sigma) {
        //Applies Gaussian filter to image
        Size ksize = new Size(4 * (int) sigma + 1, 4 * (int) sigma + 1);
        try {
            // sigmaX = sigmaY = sigma
            Imgproc.GaussianBlur(inputImage, outputImage, ksize, sigma, sigma);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void gaussianFilter(Mat inputImage, Mat outputImage) {
        gaussianFilter(inputImage, outputImage, SIGMA_SPATIAL_DEFAULT);
    }

    public static void gaussianCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage, float sigma) {
        int[] window = setWindow(displayImage);
        gaussianFilter(inputImage, filteredImage, sigma);
        displayFilter(displayImage, filteredImage, window);
    }

    public static void gaussianCalcDisplay(Mat displayImage, Mat inputImage, Mat filteredImage) {
        gaussianCalcDisplay(displayImage, inputImage, filteredImage, SIGMA_SPATIAL_DEFAULT);
    }

    private static void bilateralFilter(Mat inputImage, Mat outputImage, float sigmaSpatial, float sigmaIntensity) {
        //Applies bilateralFilter to image
        int d = 4 * (int) sigmaSpatial + 1;
        try {
            Imgproc.bilateralFilter(inputImage, outputImage, d,
                    sigmaIntensity, sigmaSpatial);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void bilateralFilter(Mat inputImage, Mat outputImage) {
        bilateralFilter(inputImage, outputImage, SIGMA_SPATIAL_DEFAULT, SIGMA_INTENSITY_DEFAULT);
    }

    public static void bilateralCalcDisplay(Mat imToProcess, Mat gray, Mat filteredImage, float sigmaSpatial, float sigmaIntensity) {
        int[] window = setWindow(imToProcess);
        bilateralFilter(gray, filteredImage, sigmaSpatial, sigmaIntensity);
        displayFilter(imToProcess, filteredImage, window);
    }

    public static void bilateralCalcDisplay(Mat imToProcess, Mat gray, Mat filteredImage) {
        bilateralCalcDisplay(imToProcess, gray, filteredImage, SIGMA_SPATIAL_DEFAULT, SIGMA_INTENSITY_DEFAULT);
    }
}
