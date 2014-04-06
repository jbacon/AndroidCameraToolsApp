/**
 * Description: A class to use for mOpenCvCameraView. It extends JavaCameraView, and makes for easier access to the different camera parameters.
 * The main use of the class is for taking a picture with the camera that is not just a snapshot of the preview view.
 * takePicture() method will start the PictureActivity and pass the picture taken in Intent extras.
 * 
 * @author Josh Bacon
 * 
 * */

package com.project.cameratoolsapp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class PreviewView extends JavaCameraView {

    private static final String TAG = "com.project.cameratoolsapp.PreviewView";

    private Mat pictureMat;
    
    /**
     * Description: Constructor for the PreviewView which extends JavaCameraView.
     * 
     * @param context	Context of the view
     * @param attrs		Attributes of the view
     */
    public PreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
   }

    /**
     * 
     * @return	A List<String> of supported camera color effects
     */
    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    /**
     * 
     * @return	A Boolean determining whether color effects are supported by the camera.
     */
    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    /**
     * 
     * @return	A String of the current camera color effect
     */
    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    /**
     * 
     * @param effect	The camera color effect to apply to the camera
     */
    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    /**
     * 
     * @return	A List<Camera.Size> of the supported camera preview resolutions.
     */
    public List<Size> getPreviewResolutionsList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    /**
     * 
     * @param resolution	The Camera.Size resolution to set the camera preview to.
     */
    public void setPreviewResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    /**
     * 
     * @return	The current Camera.Size resolution of the camera preview.
     */
    public Size getPreviewResolution() {
        return mCamera.getParameters().getPreviewSize();
    }
    
    /**
     * 
     * @return	A List<Camera.Size> of the supported camera picture resolutions.
     */
    public List<Size> getPictureResolutionsList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    /**
     * 
     * @param resolution	The Camera.Size resolution to set the camera picture to.
     */
    public void setPictureResolution(Camera.Size resolution) {
        mCamera.getParameters().setPictureSize(resolution.width, resolution.height);
    }

    /**
     * 
     * @return	The Camera.Size that the camera picture resolution is set to
     */
    public Size getPictureResolution() {
        return mCamera.getParameters().getPictureSize();
    }
    
    /**
     * Description: Takes a picture with Camera. Picture is saved to fileName location and
     * added to the MediaStore. Image processing is applied to the picture.
     * 
     * @param fileName	The path of the photofile
     */
    public void takePicture(final String fileName) {
        Log.i(TAG, "Tacking picture");
        PictureCallback callback = new PictureCallback() {
            private String mPictureFileName = fileName;
            /**
             * Data is used to save a photo from data to a file in JPEG format, which is inserted to MediaStore
             * the PictureActivity is then started and sent an Intent with extras: URI and Path
             * If picture fails to be saved a Toast is shown
             */
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.i(TAG, "Saving a bitmap to file");
                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
                
                //Converts Bitmap to Mat
                Bitmap bitmap32 = picture.copy(Bitmap.Config.ARGB_8888, true);
                pictureMat = new Mat(bitmap32.getWidth(), bitmap32.getHeight(), CvType.CV_8UC4);
                Utils.bitmapToMat(bitmap32, pictureMat);
                //Apply Image Processing to Mat
                doImageProcessing();
                //Converts Mat to Bitmap
                pictureMat.convertTo(pictureMat, CvType.CV_8UC4);
                Utils.matToBitmap(pictureMat, bitmap32);
                picture = bitmap32;
                
                //Saves Bitmap to a file and adds to MediaStore
                FileOutputStream out;
                try {
                	out = new FileOutputStream(mPictureFileName);
                	picture.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    picture.recycle();
                    String stringUri = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), mPictureFileName, "All in One Picture", "Taken with All in One App");
                    Uri uri = Uri.parse(stringUri);
                	Log.e(TAG, "Uri: "+ uri);
                	Log.e(TAG, "Path: "+ mPictureFileName);
                	final Intent intent = new Intent(getContext(), PictureActivity.class);
                	intent.putExtra("EXTRA_PHOTO_URI", uri);
                	intent.putExtra("EXTRA_PHOTO_DATA_PATH", mPictureFileName);
                	getContext().startActivity(intent);
                } catch (FileNotFoundException e) {
                	// Since the insertion failed, delete the photo.
                	File photo = new File(mPictureFileName);
                	if (!photo.delete()) {
                		Log.e(TAG, "Failed to delete non-inserted photo");
                	}
                	Log.e(TAG, "ERROR: File could not be found: "+mPictureFileName);
	                Toast.makeText(getContext(), "Photo could not be saved", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
					((Activity) getContext()).finish();
				}
                mCamera.startPreview();
            }
        };
        mCamera.takePicture(null, null, callback);
    }
    
    /**
     * Description: Performs ImageProcessing on the Mat picture taken from takePicture method.
     * The processing mode is received from getImageProcessingMode() from MainActivity.
     * This image processing needs to be performed, because the quality of the image taken from takePicture is different 
     * from the quality of the camera preview image.
     */
    private void doImageProcessing() {
    	int procMode = MainActivity.getImageProcessingMode();
    	Mat mIntermediateMat = new Mat();
    	if(procMode == MainActivity.CANNY_EDGE) {
    		Imgproc.Canny(pictureMat, mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, pictureMat, Imgproc.COLOR_GRAY2BGRA, 4);
    	}
    	else if(procMode == MainActivity.POSTERIZE) {
    		Imgproc.Canny(pictureMat, mIntermediateMat, 80, 100);
    		pictureMat.setTo(new Scalar(0, 0, 0, 255), mIntermediateMat);
            Core.convertScaleAbs(pictureMat, mIntermediateMat, 1./16, 0);
            Core.convertScaleAbs(mIntermediateMat, pictureMat, 16, 0);
    	}
    	else if(procMode == MainActivity.PIXELIZE) {
    		org.opencv.core.Size size = pictureMat.size();
			Imgproc.resize(pictureMat, pictureMat, new org.opencv.core.Size(), 0.03, 0.03, Imgproc.INTER_NEAREST);
			Imgproc.resize(pictureMat, pictureMat, size, 0., 0., Imgproc.INTER_NEAREST);
    	}
    	else if(procMode == MainActivity.RGB) {
    		
    	}
    	else if(procMode == MainActivity.SEPIA) {
    		Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
            mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
            mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
            mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
            mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    		Core.transform(pictureMat, pictureMat, mSepiaKernel);
    	}
    	
    }
}
