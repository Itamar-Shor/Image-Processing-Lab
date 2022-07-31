package il.ac.tau.adviplab.androidopencvlab;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    //menu members
    private SubMenu mResolutionSubMenu;
    private SubMenu mCameraSubMenu;

    //flags
    private Boolean mSettingsMenuAvailable = false;

    // menu IDs
    private static final int SETTINGS_GROUP_ID   = 1;
    private static final int RESOLUTION_GROUP_ID = 2;
    private static final int CAMERA_GROUP_ID     = 3;
    private static final int DEFAULT_GROUP_ID    = 4;
    private static final int COLOR_GROUP_ID      = 5;
    private static final int START_GROUP_ID      = 6;

    private MyJavaCameraView mOpenCvCameraView;
    private final CameraListener mCameraListener = new CameraListener();

    private final String[] mCameraNames = {"Rear", "Front"};
    private final int[] mCameraIDarray = {CameraBridgeViewBase.CAMERA_ID_BACK,
            CameraBridgeViewBase.CAMERA_ID_FRONT};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = findViewById(R.id.Java_Camera_View);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(mCameraListener);

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.i(TAG,"onClick event");
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateandTime = sdf.format(new Date());
            String fileName = Environment.getExternalStorageDirectory().getPath() +
                    "/sample_picture_" + currentDateandTime + ".jpg";
            mOpenCvCameraView.takePicture(fileName);
            addImageToGallery(fileName, MainActivity.this);
            Toast.makeText(MainActivity.this, fileName + " saved",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCVLoader success");
            mOpenCvCameraView.enableView();
        } else {
            Log.e(TAG, "OpenCVLoader failure");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "Options menu created");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu);

        Menu settingsMenu = menu.addSubMenu("Settings");
        mResolutionSubMenu = settingsMenu.addSubMenu(SETTINGS_GROUP_ID, Menu.NONE, Menu.NONE,
                "Resolution");
        mCameraSubMenu = settingsMenu.addSubMenu(SETTINGS_GROUP_ID, Menu.NONE, Menu.NONE,
                "Camera");

        menu.add(DEFAULT_GROUP_ID, CameraListener.VIEW_MODE_DEFAULT, Menu.NONE, "Default");


        Menu colorMenu = menu.addSubMenu("Color");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_RGBA, Menu.NONE, "RGBA");
        colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_GRAYSCALE, Menu.NONE,
                "Grayscale");

        menu.add(START_GROUP_ID, CameraListener.VIEW_MODE_START, Menu.NONE, "Start");

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mOpenCvCameraView.isCameraOpen() && !mSettingsMenuAvailable) {
            setResolutionMenu(mResolutionSubMenu);
            setCameraMenu(mCameraSubMenu);
            mSettingsMenuAvailable = true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int groupId = item.getGroupId();
        int id = item.getItemId();

        switch (groupId) {
            case DEFAULT_GROUP_ID:
                mCameraListener.setViewMode(id);
                return true;

            case COLOR_GROUP_ID:
                mCameraListener.setColorMode(id);
                return true;

            case RESOLUTION_GROUP_ID:
                // we chose a new resolution
                Camera.Size res = mOpenCvCameraView.getResolutionList().get(id);
                mOpenCvCameraView.setResolution(res);
                res = mOpenCvCameraView.getResolution();
                Toast.makeText(this, res.width + "x" + res.height,
                        Toast.LENGTH_SHORT).show();
                return true;

            case CAMERA_GROUP_ID:
                mOpenCvCameraView.changeCameraIndex(mCameraIDarray[id]);
                String caption = mCameraNames[id] + " camera";
                Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
                setResolutionMenu(mResolutionSubMenu);
                return true;

            case START_GROUP_ID:
                //load a bitmap and pass it to CameraListener
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.monalisa);
                mCameraListener.setImageToWarp(bitmap);
                mCameraListener.setViewMode(id);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setResolutionMenu(SubMenu resMenu) {
        resMenu.clear();
        int i=0;
        for (Camera.Size res : mOpenCvCameraView.getResolutionList()) {
            resMenu.add(RESOLUTION_GROUP_ID, i++, Menu.NONE, res.width + "x" + res.height);
        }
    }

    private void setCameraMenu(SubMenu camMenu) {
        for (int i = 0; i < Math.min(mOpenCvCameraView.getNumberOfCameras(), 2); i++) {
            camMenu.add(CAMERA_GROUP_ID, i, Menu.NONE, mCameraNames[i]);
        }
    }

    @SuppressLint("InlinedApi")
    private static void addImageToGallery(final String filePath, final Context context) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}