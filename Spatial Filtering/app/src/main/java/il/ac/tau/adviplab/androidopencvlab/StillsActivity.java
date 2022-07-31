package il.ac.tau.adviplab.androidopencvlab;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;

import il.ac.tau.adviplab.myimageproc.MyImageProc;
import il.ac.tau.adviplab.myimageproc.Util;

@SuppressWarnings("deprecation")
public class StillsActivity extends MainActivity {

    //Intent tags
    private static final int SELECT_PICTURE = 1;

    // Members
    private Uri mURI;
    private Bitmap mBitmap;
    private ImageView mImageView;

    private SeekBar mSeekBarSpatial;
    private SeekBar mSeekBarIntensity;
    private TextView mTextViewSpatial;
    private TextView mTextViewIntensity;
    private MenuItem mSelectedItem;


    private final Mat mImToProcess = new Mat();
    private final Mat mImGray = new Mat();
    private final Mat mFilteredImage = new Mat();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stills);

        mImageView = findViewById(R.id.imageView1);
        Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(v -> {
            //Open gallery to select image
            final Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, SELECT_PICTURE);
        });

        mSeekBarSpatial = findViewById(R.id.seekBarSpatial);
        mTextViewSpatial = findViewById(R.id.sigmaSpatialTextView);
        setSeekBar(mSeekBarSpatial, mTextViewSpatial, getString(R.string.stringSpatial), MyImageProc.SIGMA_SPATIAL_MAX);
        mSeekBarIntensity = findViewById(R.id.seekBarIntensity);
        mTextViewIntensity = findViewById(R.id.sigmaIntensityTextView);
        setSeekBar(mSeekBarIntensity, mTextViewIntensity, getString(R.string.stringIntensity), MyImageProc.SIGMA_INTENSITY_MAX);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        mSelectedItem = item;

        int groupId = item.getGroupId();
        int id = item.getItemId();
        switch (groupId) {
            case SETTINGS_GROUP_ID:
                Toast.makeText(this, getString(R.string.notAvailable), Toast.LENGTH_SHORT).show();
                return true;
            case DEFAULT_GROUP_ID:
                if (mURI != null) {
                    mBitmap = Util.getBitmap(this, mURI);
                    if (mBitmap != null) {
                        mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap, 1000));
                        return true;
                    }
                }
                setSeekBar(mSeekBarSpatial, mTextViewSpatial, getString(R.string.stringSpatial), 0);
                setSeekBar(mSeekBarIntensity, mTextViewIntensity, getString(R.string.stringIntensity), 0);            case COLOR_GROUP_ID:
                if (mURI != null) {
                    mBitmap = Util.getBitmap(this, mURI);
                    if (mBitmap != null) {
                        if (id == CameraListener.VIEW_MODE_GRAYSCALE) {
                            Utils.bitmapToMat(mBitmap, mImToProcess);
                            Imgproc.cvtColor(mImToProcess, mImToProcess, Imgproc.COLOR_RGBA2GRAY);
                            mBitmap = Bitmap.createBitmap(mImToProcess.cols(), mImToProcess.rows(), Bitmap.Config.RGB_565);
                            Utils.matToBitmap(mImToProcess, mBitmap);
                        }
                        mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap, 1000));
                        return true;
                    }
                }
            case FILTER_GROUP_ID:
                launchRingDialog(id);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                mURI = data.getData();
                if (mURI != null) {
                    try {
                        mBitmap = Util.getBitmap(this, mURI);
                        if (mBitmap != null) {
                            mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap, 1000));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void filterImage(int id, Mat imToDisplay, Mat imToProcess, Mat filteredImage, float sigmaSpatial, float sigmaIntensity) {
        switch (id) {
            case CameraListener.VIEW_MODE_SOBEL:
                MyImageProc.sobelCalcDisplay(imToDisplay, imToProcess, filteredImage);
                break;
            case CameraListener.VIEW_MODE_GAUSSIAN:
                if (sigmaSpatial > 0) {
                    MyImageProc.gaussianCalcDisplay(imToDisplay, imToProcess, filteredImage, sigmaSpatial);
                }
                break;
            case CameraListener.VIEW_MODE_BILATERAL:
                if (sigmaSpatial > 0) {
                    MyImageProc.bilateralCalcDisplay(imToDisplay, imToProcess, filteredImage, sigmaSpatial, sigmaIntensity);
                }
                break;
        }
    }

    private void setSeekBar(final SeekBar seekbar, final TextView textview,
                            final String string, final float sigmaMax) {
        float sigma = ((float) seekbar.getProgress() / (float) seekbar.getMax()) * sigmaMax;
        textview.setText(String.format(Locale.ENGLISH, "%s%.2f", string, sigma));
        seekbar.setOnSeekBarChangeListener(new
                                                   SeekBar.OnSeekBarChangeListener() {
                                                       @Override
                                                       public void onProgressChanged(SeekBar seekBar, int
                                                               progressValue, boolean fromUser) {
                                                       }
                                                       @Override
                                                       public void onStartTrackingTouch(SeekBar seekBar) {
                                                       }
                                                       @Override
                                                       public void onStopTrackingTouch(SeekBar seekBar) {
                                                           float sigma = ((float) seekbar.getProgress() / (float) seekbar.getMax()) * sigmaMax;
                                                           textview.setText(String.format(Locale.ENGLISH, "%s%.2f", string, sigma));
                                                            //Call the filter again
                                                           if (mSelectedItem != null) {
                                                               int groupId = mSelectedItem.getGroupId();
                                                               int id = mSelectedItem.getItemId();
                                                               if (groupId == FILTER_GROUP_ID) {
                                                                   launchRingDialog(id);
                                                               }
                                                           }
                                                       }
                                                   });
    }

    public void launchRingDialog(final int id) {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(this, "Please wait ...", "Processing Image ...");
        ringProgressDialog.setCancelable(false);
        Thread filterThread = new Thread(() -> {
            String TAG = "launcherDialogTag";
            try {
                float sigmaSpatial =  ((float) mSeekBarSpatial.getProgress() / (float) mSeekBarSpatial.getMax()) * MyImageProc.SIGMA_SPATIAL_MAX;
                float sigmaIntensity =  ((float) mSeekBarIntensity.getProgress() / (float) mSeekBarIntensity.getMax()) * MyImageProc.SIGMA_INTENSITY_MAX;

                mBitmap = Util.getBitmap(StillsActivity.this, mURI);
                Utils.bitmapToMat(mBitmap, mImToProcess);
                Imgproc.cvtColor(mImToProcess, mImGray, Imgproc.COLOR_RGBA2GRAY);
                filterImage(id, mImToProcess, mImGray, mFilteredImage, sigmaSpatial, sigmaIntensity);
                mBitmap = Bitmap.createBitmap(mImToProcess.cols(), mImToProcess.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mImToProcess, mBitmap);

                mImageView.post(() -> mImageView.setImageBitmap(Util.getResizedBitmap(mBitmap, 1000)));
                Log.i(TAG, "filter finished");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            ringProgressDialog.dismiss();
        });
        filterThread.start();
    }
}