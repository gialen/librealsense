package com.intel.realsense.capture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.Pointcloud;
import com.intel.realsense.librealsense.Points;
import com.intel.realsense.librealsense.Option;
import com.intel.realsense.librealsense.Sensor;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.PipelineProfile;




public class MainActivity extends AppCompatActivity {
    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
    private GLRsSurfaceView mGLSurfaceView;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;
    private  Pointcloud mPointcloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        mPermissionsGranted = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }
        mPermissionsGranted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPermissionsGranted)
            init();
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRsContext != null)
            mRsContext.close();
        stop();
        mPipeline.close();
    }

    private void init(){
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();
        mColorizer = new Colorizer();
        mPointcloud = new Pointcloud();

        try(DeviceList dl = mRsContext.queryDevices()){
            if(dl.getDeviceCount() > 0) {
                showConnectLabel(false);
                start();
            }
        }
    }

    private void showConnectLabel(final boolean state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);
            }
        });
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            showConnectLabel(false);
        }

        @Override
        public void onDeviceDetach() {
            showConnectLabel(true);
            stop();
        }
    };

    boolean flag_debug = true;
    Runnable mStreaming = new Runnable() {
        @Override
        public void run() {
            try {
                try(FrameSet frames = mPipeline.waitForFrames(1000)) {


                    try(Frame test = frames.first(StreamType.DEPTH))
                    {

                        try(Frame processed = test.applyFilter(mColorizer)) {
                            mGLSurfaceView.upload(processed);
                        }
                        try(Frame processed_pc = test.applyFilter(mPointcloud)) {
                            Points test_points = processed_pc.as(Extension.POINTS);
                            //mGLSurfaceView.upload(test_points);

                            /*float[] vertices = test_points.getVertices();
                            if(flag_debug)
                            {
                                Log.d(TAG,  "verticessssssssssssssssssssssssssssssssssssssssssssssssssss: " + vertices[0]);
                                flag_debug = false;
                            }*/
                        }


                        /*int size_test = test.getDataSize();
                        byte[] return_buff = new byte[size_test];
                        test.getData(return_buff);
                        Log.d(TAG, "msg sizeeeeeeeeeeeeeeeeeeeee: " + size_test);


                        DepthFrame depth = test.as(Extension.DEPTH_FRAME);
                        int size_depth = depth.getDataSize();
                        byte[] return_buff_depth = new byte[size_depth];
                        depth.getData(return_buff_depth);

                        Pointcloud pc_test = new Pointcloud();
                        pc_test.process(test);
                        if(flag_debug)
                        {
                            Log.d(TAG,  "Frame return_bufffffffffffffffffffffffffffffffffffffff: " + return_buff);
                            flag_debug = false;
                        }*/

                    }
                    /*try(FrameSet processed = frames.applyFilter(mColorizer)) {
                        mGLSurfaceView.upload(processed);
                    }*/
                }
                mHandler.post(mStreaming);
            }
            catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private void configAndStart() throws Exception {
        try(Config config  = new Config())
        {
            config.enableStream(StreamType.DEPTH, 640, 480);
            config.enableStream(StreamType.COLOR, 640, 480);
            //config.enableStream(StreamType.INFRARED, 640, 480);


            PipelineProfile pipe_profile = mPipeline.start(config);
            Device test_device = pipe_profile.getDevice();
            if (!test_device.isInAdvancedMode()){
                test_device.toggleAdvancedMode(true);
            }
            Sensor mSensor = test_device.querySensors().get(0);
            mSensor.setValue(Option.VISUAL_PRESET, 3);
            float visual = mSensor.getValue(Option.VISUAL_PRESET);
            //Log.d(TAG, "visualllllllllllllllllllllll: " + visual);
        }
    }

    private synchronized void start() {
        if(mIsStreaming)
            return;
        try{
            Log.d(TAG, "try start streaming");
            mGLSurfaceView.clear();
            configAndStart();
            mIsStreaming = true;
            mHandler.post(mStreaming);
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    private synchronized void stop() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }
}
