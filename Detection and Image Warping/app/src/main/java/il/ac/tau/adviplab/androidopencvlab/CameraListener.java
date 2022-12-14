package il.ac.tau.adviplab.androidopencvlab;

import android.graphics.Bitmap;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import il.ac.tau.adviplab.myimageproc.MyImageProc;

class CameraListener implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Constants:
    static final int VIEW_MODE_DEFAULT = 0;
    static final int VIEW_MODE_RGBA = 1;
    static final int VIEW_MODE_GRAYSCALE = 2;
    static final int VIEW_MODE_START = 3;

    //Mode selectors:
    private int mViewMode = VIEW_MODE_DEFAULT;
    private int mColorMode = VIEW_MODE_RGBA;


    //members
    private Mat mImToProcess;
    private Mat mImageToWarp;

    //Getters and setters

    // not used
    /*
    int getColorMode() {
        return mColorMode;
    }
    */

    void setColorMode(int colorMode) {
        mColorMode = colorMode;
    }

    // not used
    /*
    int getViewMode() {
        return mViewMode;
    }
    */

    void setViewMode(int viewMode) {
        mViewMode = viewMode;
    }

    void setImageToWarp(Bitmap bitmap) {
        mImageToWarp = new Mat();
        Utils.bitmapToMat(bitmap, mImageToWarp);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mImToProcess = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mImToProcess.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        switch (mColorMode) {
            case VIEW_MODE_RGBA:
                mImToProcess = inputFrame.rgba();
                break;

            case VIEW_MODE_GRAYSCALE:
                mImToProcess = inputFrame.gray();
                break;
        }

        switch (mViewMode) {
            case VIEW_MODE_DEFAULT:
                break;

            case VIEW_MODE_START:
                MyImageProc.detectAndReplaceChessboard(mImToProcess, mImageToWarp);
                break;
        }
        return mImToProcess;
    }

}
