/**
 * @file MainActivity.java
 * @author Josh Bacon
 * @version 1.0
 * @date 3/29/14
 * 
 * Description: This main activity creates a UI for a openCV camera view. 
 * An ActionBar and ButtonBar are used for various features
 * openCV feature detection modes, image processing modes, taking a picture, an optional compass overlay, and settings
 * Upon taking a picture, activity will start a new Intent.
 * 
 * Feature Detection Modes Supported (ActionBar Buttons):
 * 		--Keypoints (Highlights keypoints in the cameraView)
 * 		--Gonzaga Bulldog symbol image detection (A Toast will popup when a bulldog symbol is detected in a frame.
 * 		--Real-time Face Detection (Coming Soon)
 * Image Processing Modes Supported (ButtonBar Buttons):
 * 		--RGB
 * 		--Sephia
 * 		--Canny-Edge
 * 		--Pixelization
 * 		--Posterization
 * 	Settings:
 * 		--Preview Resolution: Creates a pop up dialog to select a supported preview resolution for the camera.
 * 	Take Picture:
 * 		--New Activity for picture options
 * 		--Right now the picture is taken in the same resolution as the preview display (Fix to take advantage of full camera ability).
 */

package com.project.cameratoolsapp;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.project.util.SystemUiHider;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.support.v4.app.Fragment;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnClickListener, View.OnTouchListener, SensorEventListener {
	public static final String TAG	= "project.allinone.MainActivity";
	
	//Instances Variables for UI
	private PreviewView 		mOpenCvCameraView;
	private List<Camera.Size> 	previewResolutionsList;
	private List<Camera.Size> 	pictureResolutionsList;
    private View 				controlsView;
    private AlertDialog.Builder dialogBuilderPreview;
    private AlertDialog.Builder dialogBuilderPicture;
	private ArrayAdapter<String> previewAdapter;
	private ArrayAdapter<String> pictureAdapter;
	private AlertDialog			previewResolutionSelector;
	private AlertDialog			pictureResolutionSelector;
    
    
    //Instance Variables for Compass
    private ImageView 		compassImage;
    private float 			currentDegree = -90f;
    private SensorManager 	mSensorManager;
    private TextView 		textViewHeading;
    private boolean 		compassOn = false;
	
	//Instance Variables for UiHider
	private SystemUiHider 			mSystemUiHider;
	private static final boolean 	AUTO_HIDE = true;
	private static final int 		AUTO_HIDE_DELAY_MILLIS = 3000;
	private static final boolean 	TOGGLE_ON_CLICK = true;
	private static final int 		HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	
	//Instance Variables for Matcher
	public static final int 	MATCHING = 5;
	public static final int		KEYPOINTS = 6;
	public static final int 	FACE_DETECTION = 7;
	public static final int		NO_FEATURE_DETECTION = 8;
	private int					featureDetectionMode = NO_FEATURE_DETECTION;
	
	private static final float 	THREATHOLD = 0.08f;
    private Mat 				mReferenceImage;
    private MatOfKeyPoint 		mReferenceKeypoints;
    private Mat 				mReferenceDescriptors;
    private MatOfKeyPoint 		mSceneKeypoints;
    private Mat 				mSceneDescriptors;
    private MatOfDMatch 		mMatches;
    private FeatureDetector 	mFeatureDetector;
    private DescriptorExtractor mDescriptorExtractor;
    private DescriptorMatcher 	mDescriptorMatcher;
    
    
    //Instance Variables for Camera filters
    public static final int 	RGB = 0;
    public static final int 	CANNY_EDGE = 1;
	public static final int 	POSTERIZE = 2;
	public static final int 	PIXELIZE = 3;
	public static final int 	SEPIA = 4;
    private Mat                 mZoomWindow;
    private Mat                 mZoomCorner;
    private Mat                 mGray;
    private Mat                 mRgba;
    private	Mat					gray;
    private Mat                 mIntermediateMat;
    private Mat                 mRgbaInnerWindow;
    private Mat                 mGrayInnerWindow;
    private Mat                 mSepiaKernel;
    private static int 			imageProcessingMode = RGB;
    
    
    /**
     * Creates/Initializes the activity UI for first time.
     * Uses helper functions: initSystemUiHider(), initSystemUi(), initOpenCvCameraView(), and initCompass().
     * 
     * @param savedInstanceState	The saved state of the activity for recreation.
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		controlsView = findViewById(R.id.fullscreen_content_controls);
		controlsView.setVisibility(View.VISIBLE);
		initOpenCvCameraView();
		initCompass();
		initSystemUi();
		initSystemUiHider();
		
	}
	
	/**
	 * OpenCV library initialization for activity.
	 * 
	 */
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
	    @Override
	    public void onManagerConnected(int status) {
	        switch (status) {
	            case LoaderCallbackInterface.SUCCESS:
	                Log.i("TTT", "OpenCV loaded successfully");
	                mOpenCvCameraView.enableView();
	                initMatching();
	                break;
	            default:
	                super.onManagerConnected(status);
	                break;
	        }
	    }
	};
	
	/**
	 * Initializes OpenCV library and re-registers the Sensor listener again
	 */
	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
	    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
	    mSensorManager.registerListener(this
	    		, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
	    		, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	/**
	 * Disables the mOpenCvCameraView and unregisters the sensor listener
	 */
	protected void onPause() {
		super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	     mSensorManager.unregisterListener(this);
	}
	
	/**
	 * Disables the mOpenCvCameraView
	 */
	protected void onDestory() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	}
	
	/**
	 * Used to delay hiding the system UI after activity UI has been created
	 * To reveal to the user the hidden system UI controls
	 */
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		delayedHide(100);
	}
	/**
	 * Creates and inflates menu
	 */
	
	/**
	 * Description: Creates the actionBar menu by inflating the activity_main_menu.xml.
	 * 		--Also uses a Builder to create an AlertDialog for when resolution item is selected from settings
	 * 		--An ArrayAdapter object is created to give the AlertDialog the resolution strings
	 * 		--A list of capatible preview camera resolutions is also created. Used for making the strings for the adapter.
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * ActionBar menu item buttons. 
	 * Determines the action on each actionbar item selection. 
	 * Buttons: Resolution (Settings), Matching Mode, Picture, Compass On/Off, Keypoint Mode
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_preview_resolutions:
			previewResolutionSelector.show();
			break;
		case R.id.action_picture_resolutions:
			pictureResolutionSelector.show();
			break;
		case R.id.action_camera:
			takeandSavePicture();
			break;
		case R.id.action_matching:
			featureDetectionMode = MainActivity.MATCHING;
			break;
		case R.id.action_keypoints:
			featureDetectionMode = MainActivity.KEYPOINTS;
			break;
		case R.id.action_compass:
			if(compassOn == true) {
				compassOn = false;
				item.setTitle("Show Compass");
				//textViewHeading.animate().translationY(controlsView.getHeight()).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
				//compassImage.animate().translationY(controlsView.getHeight()).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
				compassImage.clearAnimation();
				compassImage.setVisibility(ImageView.INVISIBLE);
				textViewHeading.setVisibility(TextView.INVISIBLE);
				
			}
			else {
				compassOn = true;
				item.setTitle("Hide Compass");
				compassImage.setVisibility(ImageView.VISIBLE);
				textViewHeading.setVisibility(TextView.VISIBLE);
				
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * When button bar button is clicked, changes the imageProcessingMode
	 * which is used in onCameraFrame()
	 */
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.cameraPreviewView:
			if (TOGGLE_ON_CLICK) {
				mSystemUiHider.toggle();
			} else {
				mSystemUiHider.show();
			}
			break;
		case R.id.rgb_button:
			imageProcessingMode = RGB;
			break;
		case R.id.cannyedge_button:
			imageProcessingMode = CANNY_EDGE;
			break;
		case R.id.posterize_button:
			imageProcessingMode = POSTERIZE;
			break;
		case R.id.pixelize_button:
			imageProcessingMode = PIXELIZE;
			break;
		case R.id.sepia_button:
			imageProcessingMode = SEPIA;
			break;
		default:
			imageProcessingMode = RGB;
			break;
		}
	}

	/**
	 *  Touch listener to use for in-layout UI controls. Delays hiding the system
	 *	UI to prevent jarring behavior of controls while interacting with activity UI.
	*/
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (AUTO_HIDE) {
			delayedHide(AUTO_HIDE_DELAY_MILLIS);
		}
		return false;
	}
	
	Handler mHideHandler = new Handler();
	
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};
	
	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	/**
	 * Initializes the Compass view for the activity UI. Uses SensorManager to get service
	 */
	private void initCompass() {
		compassImage = (ImageView) findViewById(R.id.imageViewCompass);
		//Tells degrees user is heading
		textViewHeading = (TextView) findViewById(R.id.textViewHeading);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}
	
	/**
	 * Initializes the System UI hiding control for activity.
	 */
	private void initSystemUiHider() {
		mSystemUiHider = SystemUiHider.getInstance(this, mOpenCvCameraView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {	
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;
			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources()
							.getInteger(android.R.integer.config_shortAnimTime);
					}
					controlsView
							.animate()
							.translationY(visible ? 0 : mControlsHeight)
							.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE
							: View.GONE);
				}
				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});
	}

	/**
	 * Initializes the System UI button bar buttons and sets touch and click listeners
	 */
	private void initSystemUi() {
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.rgb_button).setOnTouchListener(this);
		findViewById(R.id.cannyedge_button).setOnTouchListener(this);
		findViewById(R.id.posterize_button).setOnTouchListener(this);
		findViewById(R.id.pixelize_button).setOnTouchListener(this);
		findViewById(R.id.sepia_button).setOnTouchListener(this);
		//On Click (Different then touch?)
		findViewById(R.id.rgb_button).setOnClickListener(this);
		findViewById(R.id.cannyedge_button).setOnClickListener(this);
		findViewById(R.id.posterize_button).setOnClickListener(this);
		findViewById(R.id.pixelize_button).setOnClickListener(this);
		findViewById(R.id.sepia_button).setOnClickListener(this);
	}
	
	/**
	 * Initializes the openCv camera view for the activity UI
	 */
	private void initOpenCvCameraView() {
		mOpenCvCameraView = (PreviewView) findViewById(R.id.cameraPreviewView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}
	
	/**
	 * Initializes Matching reference resources, objects.
	 * Loads the reference image resource, and converts color space of matrix to gray and rgba.
	 * Creates a FeatureDetector and DescriptorExtractor object, then...
	 * Creates matrix of keypoints of reference image
	 * Creates matrix of descriptors of keypoints
	 * 
	 */
	private void initMatching() {
		try {
        	mReferenceImage = Utils.loadResource(this, R.drawable.bulldog, Highgui.CV_LOAD_IMAGE_COLOR);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
       final Mat referenceImageGray = new Mat();
       Imgproc.cvtColor(mReferenceImage, referenceImageGray, Imgproc.COLOR_BGR2GRAY);
       Imgproc.cvtColor(mReferenceImage, mReferenceImage, Imgproc.COLOR_BGR2RGBA);
        
       mReferenceKeypoints = new MatOfKeyPoint();
       mReferenceDescriptors = new Mat();
       mSceneKeypoints = new MatOfKeyPoint();
       mSceneDescriptors = new Mat();
       mMatches = new MatOfDMatch();
        
       mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
       mDescriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
       mDescriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
       mFeatureDetector.detect(referenceImageGray, mReferenceKeypoints);
       mDescriptorExtractor.compute(referenceImageGray, mReferenceKeypoints, mReferenceDescriptors);
	}
	
	/**
	 * When the orientation sensor (software based) detects a changed, it will update the compass view rotation on the screen,
	 * using the android RotateAnimation for views.
	 */
	public void onSensorChanged(SensorEvent event) {
		if(compassOn) {
		     // get the angle around the z-axis rotated
	        float degree = Math.round(event.values[0]) + 90f;
	
	        textViewHeading.setText("Heading: " + Float.toString(degree) + " degrees");
	
	        // create a rotation animation (reverse turn degree degrees)
	        RotateAnimation ra = new RotateAnimation(
	                currentDegree, 
	                -degree,
	                Animation.RELATIVE_TO_SELF, 0.5f, 
	                Animation.RELATIVE_TO_SELF,
	                0.5f);
	
	        // how long the animation will take place
	        ra.setDuration(210);
	
	        // set the animation after the end of the reservation status
	        ra.setFillAfter(true);
	
	        // Start the animation
	        compassImage.startAnimation(ra);
	        currentDegree = -degree;
		}
	}
	
	/** 
	 * Not used
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation/keyboard change
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * When camera view starts, the Sepia kernel matrix is initialized for use in the onCameraFrame() method
	 * Several matrix' objects are created for use in onCameraFrame();
	 */
	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
        mRgba = new Mat();
        mIntermediateMat = new Mat();

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
        
        //Now that the mOpenCvCameraView has been initialized I can now create 
  		//get and use the .getParameters method of the view to create alert dialogs...
  		//This needs to be done here, because onCreateOptionsMenu might fire before mOpenCvCamera is created.
  		//Initializes the selector for Preview Resolution button
  	    previewResolutionsList = mOpenCvCameraView.getPreviewResolutionsList();
  	    ListIterator<Camera.Size> previewResolutionItr = previewResolutionsList.listIterator();
  	    previewAdapter = new ArrayAdapter<String>(this, 
  	    		android.R.layout.select_dialog_singlechoice);
  	    while(previewResolutionItr.hasNext()) {
  	    	Camera.Size element = previewResolutionItr.next();
  	    	previewAdapter.add(Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
  	    }
  	    dialogBuilderPreview = new AlertDialog.Builder(this);
  	    dialogBuilderPreview.setCancelable(true);
  	    dialogBuilderPreview.setTitle("Choose Preview Resolution");
  	    dialogBuilderPreview.setAdapter(previewAdapter, new DialogInterface.OnClickListener() {
          	@Override
          	public void onClick(DialogInterface dialog, int which) {
          		mOpenCvCameraView.setPreviewResolution(previewResolutionsList.get(which));
          	}
        });
        previewResolutionSelector = dialogBuilderPreview.create();
          
        //Initializes the selector for Picture Resolution button
        pictureResolutionsList = mOpenCvCameraView.getPictureResolutionsList();
  	    ListIterator<Camera.Size> pictureResolutionItr = pictureResolutionsList.listIterator();
  	    pictureAdapter = new ArrayAdapter<String>(this, 
  	    		android.R.layout.select_dialog_singlechoice);
  	    while(pictureResolutionItr.hasNext()) {
  	    	Camera.Size element = pictureResolutionItr.next();
  	    	pictureAdapter.add(Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
  	    }
  	    dialogBuilderPicture = new AlertDialog.Builder(this);
  	    dialogBuilderPicture.setCancelable(true);
        dialogBuilderPicture.setTitle("Choose Picture Resolution");
        dialogBuilderPicture.setAdapter(pictureAdapter, new DialogInterface.OnClickListener() {
          	@Override
          	public void onClick(DialogInterface dialog, int which) {
          		mOpenCvCameraView.setPictureResolution(pictureResolutionsList.get(which));
          	}
        });
        pictureResolutionSelector = dialogBuilderPicture.create();
          
	}
	
	/**
	 * Deallocates/releases Matrix' used for the camera view (mZoomWindow,mGrayInnerWindow, mRgba, mIntermediateMat)
	 */
	public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mZoomWindow != null)
            mZoomWindow.release();
        if (mZoomCorner != null)
            mZoomCorner.release();
        if (mGrayInnerWindow != null)
            mGrayInnerWindow.release();
        if (mRgbaInnerWindow != null)
            mRgbaInnerWindow.release();
        if (mRgba != null)
            mRgba.release();
        if (mGray != null)
            mGray.release();
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mRgba = null;
        mGray = null;
        mIntermediateMat = null;
        mRgbaInnerWindow = null;
        mGrayInnerWindow = null;
        mZoomCorner = null;
        mZoomWindow = null;
	}
	
	/**
	 * Description: For each camera frame, the input frame is converted to rgba and gray. 
	 * Converts the gray/rgba matrix and applies selected image processing mode using openCv methods.
	 * Performs specified feature detection mode on the frame result after image processing.
	 * If a photo capture is pending calls the method takeandSavePicture(), otherwise returns the new rgba frame
	 * 
	 * OpenCV methods used: Imgproc.Canny  (edge detection), Imgproc.cvtColor (color conversion), Core.convertScaleAbs (scaling, and 8-bit conversions), 
	 * Imgproc.resize (scaling/resizing), Core.transform (conversion of matrix).
	 */
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		gray = inputFrame.gray();
		switch(imageProcessingMode) {
			case RGB:
				break;
			case CANNY_EDGE:
				Imgproc.Canny(gray, mIntermediateMat, 80, 100);
	            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
				break;
			case POSTERIZE:
				Imgproc.Canny(mRgba, mIntermediateMat, 80, 100);
	            mRgba.setTo(new Scalar(0, 0, 0, 255), mIntermediateMat);
	            Core.convertScaleAbs(mRgba, mIntermediateMat, 1./16, 0);
	            Core.convertScaleAbs(mIntermediateMat, mRgba, 16, 0);
				break;
			case PIXELIZE:
				Size size = mRgba.size();
				Imgproc.resize(mRgba, mRgba, new Size(), 0.03, 0.03, Imgproc.INTER_NEAREST);
				Imgproc.resize(mRgba, mRgba, size, 0., 0., Imgproc.INTER_NEAREST);
				break;
			case SEPIA:
				Core.transform(mRgba, mRgba, mSepiaKernel);
				break;
		}
		switch(featureDetectionMode) {
			case MATCHING:
				findMatch(gray);
				break;
			case KEYPOINTS:
				drawKeyPoints(mRgba);
				break;
			case NO_FEATURE_DETECTION:
				break;
		}
		return mRgba;
	}
	
	/**
	 * Description: This method will attempt to save a matrix in color format BGR to a photo path from the RGBA frame matrix. 
	 * Photo gallery path is the standard directory in which to place pictures specified by Environment.
	 * Activity will finish if... the photo album path does not exist or the matrix could not be saved to the photo path.
	 * If successful, the method will attempt to insert photo into MediaStore and start a new activity with new Intent.
	 * Extras for new Intent include: photo uri (specified by MediaStore insert) and photo path string.
	 * If fails to insert into MediaStore, photo file deleted and activity finishes.
	 * 
	 */
	private void takeandSavePicture() {
		final long currentTimeMillis = System.currentTimeMillis();
		final String appName = getString(R.string.app_name);
		final String galleryPath = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).toString();
		final String albumPath = galleryPath + "/" + appName;
		final String photoPath = albumPath + "/" + currentTimeMillis + ".png";
		final ContentValues values = new ContentValues();
		Log.e(TAG, "Photo Path" + photoPath);
		values.put(MediaStore.MediaColumns.DATA, photoPath);
		values.put(Images.Media.MIME_TYPE, "image/png");
		values.put(Images.Media.TITLE, appName);
		values.put(Images.Media.DESCRIPTION, appName);
		values.put(Images.Media.DATE_TAKEN, currentTimeMillis);
		
		// Ensure that the album directory exists.
		//	Might not be needed
		File album = new File(albumPath);
		if (!album.isDirectory() && !album.mkdirs()) {
			Log.e(TAG, "Failed to make directory for photo at" + albumPath);
			finish();
			return;
		}
		/*
		// This Code was used to take a picture (as a matrix) and save it to a file path, 
		//	but takes a picture of the Preview screen instead
		//	which is of lower quality than the highest quality capable by the Camera.
		//  NO LONGER USED BEECAUSE... REPLACED BY mOpenCvCameraView.takePicture
		Mat bgr = new Mat();
		// Try to create the photo.
		Imgproc.cvtColor(mRgba, bgr,Imgproc.COLOR_RGBA2BGR, 3);
		if (!Highgui.imwrite(photoPath, bgr)) {
			Log.e(TAG, "Failed to save photo to " + photoPath);
			finish();
		}
		Log.d(TAG, "Photo saved successfully to " + photoPath);
		
		// This code was used to insert the image into MediaStore and make a URI to the photo.
		// It would then start the new activity to the PictureActivity. 
		// NO LONGER NEEDED BECAUSE... this all takes place from mOpenCvCameraView.takePicture(photoPath)
		Uri uri;
		try {
			uri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
			Log.e(TAG, "Uri "+ uri);
			final Intent intent = new Intent(this, PictureActivity.class);
			intent.putExtra("EXTRA_PHOTO_URI", uri);
			intent.putExtra("EXTRA_PHOTO_DATA_PATH", photoPath);
			startActivity(intent);
		} catch (final Exception e) {
			Log.e(TAG, "Failed to insert photo into MediaStore");
			e.printStackTrace();
			// Since the insertion failed, delete the photo.
			File photo = new File(photoPath);
			if (!photo.delete()) {
				Log.e(TAG, "Failed to delete non-inserted photo");
			}
			finish();
			return;
		}
		*/
		mOpenCvCameraView.takePicture(photoPath);
		return;
	}
	
	/** 
	 * Description: This method will attempt to find a match for the gray matrix provided from the onCameraFrame to the reference image (Gonzaga Bulldog icon)
	 * established in the initialization. 
	 * First finds number of matches from comparison of keypoint descriptors, if too few return (no match).
	 * Second, finds distances between keypoints for matches, if not within range return (no match).
	 * Third, looks at number of good reference and good frame keypoint points, if too few, return (no match).
	 * Fourth, find ratio of number of good reference keypoints to number of frame keypoints, if less then THREASHOLD return (no match).
	 * If frame keypoints and descriptors pass all requirements, a runnable method makes a Toast displayed from UI thread
	 * @param gray The gray color conversion matrix from onCameraFrame()
	 */
	private void findMatch(Mat gray) {
	    mFeatureDetector.detect(gray, mSceneKeypoints);	//Finds keypoints in frame from gray matrix
	    mDescriptorExtractor.compute(gray, mSceneKeypoints, mSceneDescriptors);	//Finds Descriptor of keypoints in frame
	    mDescriptorMatcher.match(mSceneDescriptors, mReferenceDescriptors, mMatches); //Finds potential matches from descriptor comparisons
	    
        List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 8) {	//too few
            return;
        }
        
        List<KeyPoint> referenceKeypointsList = mReferenceKeypoints.toList(); //Could be put somewhere else
        List<KeyPoint> sceneKeypointsList = mSceneKeypoints.toList();
        
        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for(DMatch match : matchesList) {
            double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        
        if (minDist > 50.0) { //don't match
            return;
        } 
        
        ArrayList<Point> goodReferencePointsList = new ArrayList<Point>();
        ArrayList<Point> goodScenePointsList = new ArrayList<Point>();
        double maxGoodMatchDist = 2 * minDist;
        for(DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
               goodReferencePointsList.add(referenceKeypointsList.get(match.trainIdx).pt);
               goodScenePointsList.add(sceneKeypointsList.get(match.queryIdx).pt);
            }
        }
        
        if (goodReferencePointsList.size() < 4 || goodScenePointsList.size() < 4) {
            return;
        }
        float ratio = (float)goodScenePointsList.size()/sceneKeypointsList.size();
        Log.d("cv", Float.toString(ratio));
        
        if (ratio<THREATHOLD) {	//Puts the TOAST on the Main Thread instead of the openCV thread
        	this.runOnUiThread(new Runnable() {
        		  public void run() {
        		    Toast.makeText(getApplicationContext(), "Hello Bulldog", Toast.LENGTH_SHORT).show();
        		  }
        		});
        }
	}
	
	/**
	 * Draws keypoints on the onCameraFrame rgba matrix. 
	 * 
	 * @param rgba	The matrix from the onCameraFrame method
	 */
	public static void drawKeyPoints(Mat rgba) {
		 Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
		 MatOfKeyPoint keypoints = new MatOfKeyPoint();
		 FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB); 
		 detector.detect(rgba, keypoints);
		 Features2d.drawKeypoints(rgba, keypoints, rgba);
		 Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2RGBA);
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	/**
	 * 
	 * @return	The image processing mode that is current selected for this camera applications
	 */
	public static int getImageProcessingMode() {
		return MainActivity.imageProcessingMode;
	}
}


