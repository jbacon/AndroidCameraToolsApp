/**
 * Description: This activity receives extras from MainActivity which includes a photo URI.
 * Photo is displayed it on the screen. A action menu bar gives the option to..
 * Delete, Edit, or Share the photo, upon deletion, the activity will end and 
 * return to MainActivity. The Edit and Share activities will open a chooser menu for selection.
 * 
 * @author Josh Bacon
 */

package com.project.cameratoolsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;

public class PictureActivity extends Activity{
	private static final String TAG = "CameraProject.PictureActivity";
	private  static final String PHOTO_MIME_TYPE = "image/png";
	private String photoPath;
	private Uri photoUri;
	private String photoTitle;
	private String photoDesc;
	private long photoDate;
	private Bundle extras;
	private Drawable picture;
	
	/**
	 * Description: Creates UI with activity_picture.xml. Gets extras from Intent.
	 * Sets ImageURI of pictureview from layout to the photoUri from extra.
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture);
		extras = getIntent().getExtras();
		if(extras == null) {
			Log.e(TAG, "extras are null");
		}
		else {
			photoTitle = 	extras.getString(Images.Media.TITLE);
			photoDesc =  	extras.getString(Images.Media.DESCRIPTION);
			photoDate =  	extras.getLong(Images.Media.DATE_TAKEN);
			photoPath = 	extras.getString("EXTRA_PHOTO_DATA_PATH");
			photoUri = 		extras.getParcelable("EXTRA_PHOTO_URI");
		}
		Log.e(TAG, "photoUri: " + photoUri);
		Log.e(TAG,"photoPath: " + photoPath);
		((ImageView) findViewById(R.id.pictureview)).setImageURI(photoUri);
	}

	/**
	 * Description: Creates Options ActionBar Menu by inflating activity_picture_menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is pre  sent.
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_picture_menu, menu);
	    return super.onCreateOptionsMenu(menu);
	}
    
	/**
	 * Handles ActionBar menu selections.
	 * If Delete selected, deletePhoto() method used
	 * If Edit selected, editPhoto() method called.
	 * If Share selected, sharePhoto() method called.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_delete:
				deletePhoto();
				break;
			case R.id.action_edit:
				editPhoto();
				break;
			case R.id.action_share:
				sharePhoto();
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * Description: Deletes the photo at the photo path specified by the URI provided in the Intent.
	 * Makes an AlertDialog.Builder that makes a popup confirmation window for deleting the photo.
	 * If the photo is successfully deleted or fails to delete, the activity finishes.
	 */
	private void deletePhoto() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.photo_delete_prompt_title);
		alert.setMessage(R.string.photo_delete_prompt_message);
		alert.setCancelable(false);
		alert.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				//This does work
				int rows = getContentResolver().delete(photoUri, null, null);
				if(rows == 0) {
					Log.e(TAG, "Failed to delete photoUri");
				}
				else {
					Log.e(TAG, "Deleted photoUri");
				}
				//getContentResolver().delete(Images.Media.EXTERNAL_CONTENT_URI,
				//		MediaStore.MediaColumns.DATA + "=?",
				//		new String[] { photoPath });
				finish();
			}
		});
		alert.setNegativeButton(android.R.string.cancel, null);
		alert.show();
	}
	
	/**
	 * Creates a new Intent with Intent.ACTION_EDIT and sets the Data and Type to the photoUri and PHOTO_MIME_TYPE (Image/Png)
	 * Starts a new Activity that is specified by an Intent chooser and the specified intent.
	 * Finishes current activity.
	 */
	private void editPhoto() {
		final Intent intent = new Intent(Intent.ACTION_EDIT); //fill in here...
		intent.setDataAndType(photoUri, PHOTO_MIME_TYPE);
		startActivity(Intent.createChooser(intent,getString(R.string.photo_edit_chooser_title)));
		finish();
	}
	
	/**
	 * Creates a new Intent with Intent.ACTION_SEND and starts a new acivity based on users selection from an Intent chooser.
	 * Finishes current activity.
	 */
	private void sharePhoto() {
		final Intent intent = new Intent(Intent.ACTION_SEND); // fill in here
		intent.setType(PHOTO_MIME_TYPE);
		intent.putExtra(Intent.EXTRA_STREAM, photoUri);
		intent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.photo_send_extra_subject));
		intent.putExtra(Intent.EXTRA_TEXT,getString(R.string.photo_send_extra_text));
		startActivity(Intent.createChooser(intent,getString(R.string.photo_send_chooser_title)));
		finish();
	}
	
	/**
	 * Description: Not used anymore. Kept for reference purposes. Gets the absolute path of the image that is specified by the URI. It uses MediaStore to get meta data
	 * 
	 * @param uri	The photo URI that was received from the Intent extras
	 * @return		The String absolute path of the URI.
	 */
    public String getImageAbsPath(Uri uri) {
    	Cursor cursor = getContentResolver().query(uri, null, null, null, null);
    	cursor.moveToFirst();
    	String file_id = cursor.getString(0);
    	file_id = file_id.substring(file_id.lastIndexOf(":")+1);
    	cursor.close();
    	cursor = getContentResolver().query(
    			MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
    			null, 
    			MediaStore.Images.Media._ID + " = ? ", 
    			new String[]{file_id}, 
    			null);
    	cursor.moveToFirst();
    	String absolutePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
    	cursor.close();
    	return absolutePath;
    }
}
