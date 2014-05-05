package com.example.giacchettodropboxapitest;


import java.io.File;
import java.io.IOException;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Build;
import android.provider.MediaStore;


public class MainActivity extends ActionBarActivity {

	private static final String appKey = "qib50d9h7ay8o2a";
    private static final String appSecret = "kql7disubj3llup";
    private static final int REQUEST_LINK_TO_DBX = 0;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    
    private Button btnLink;
    private Button btnPhoto;
    private TextView lblWelcomeMsg;
    private DbxAccountManager dbxAcctManager;
    private DbxAccount dbxAcct;
    private DbxAccountInfo dbxAcctInfo;
    private DbxFileSystem dbxFileSystem;
    private static String imageFilePath;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLink = (Button) findViewById(R.id.btnLink);
        btnPhoto = (Button) findViewById(R.id.btnPhoto);
        lblWelcomeMsg = (TextView) findViewById(R.id.lblWelcomeMsg);
        
        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }*/
        
        btnLink.setOnClickListener(new OnClickListener () {
        	@Override
            public void onClick(View v) {
                onClickLinkToDropbox();
            }
        });
        btnPhoto.setOnClickListener(new OnClickListener () {
        	@Override
            public void onClick(View v) {
                onClickTakePhoto();
            }
        });
        
        //Initialize account manager
        dbxAcctManager = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);
        
        //if we are linked to a DropBox account already, execute the linking code
        if (dbxAcctManager.hasLinkedAccount()) {
        	onLinkToDropBox();
    	}
    }
    
    private void onClickLinkToDropbox() {
    	//first unlink an account if it already exists
    	if (dbxAcctManager.hasLinkedAccount()) {
    		dbxAcct.unlink();
    	}
		//authenticate and link to the dropbox account
    	dbxAcctManager.startLink((Activity)this, REQUEST_LINK_TO_DBX);
	}
    
    private void onClickTakePhoto() {
    	//need to have a linked account first
    	if (dbxAcctManager.hasLinkedAccount()) {
    		//open the take picture activity in android and return back here with the file
    		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    		imageFilePath = "img_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
    		Uri fileUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), imageFilePath));
    		imageFilePath = fileUri.getPath();
    		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
    		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    	} else {
    		//no linked account, display default message
    		lblWelcomeMsg.setText(R.string.default_message);
    	}
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_LINK_TO_DBX) {
    		//we requested a link to the dropbox account
    		if (resultCode == Activity.RESULT_OK) {
    			//can allow other operations as we have a successful link
    			onLinkToDropBox();
    		} else {
    			//we failed to authenticate, so display the error message
    			lblWelcomeMsg.setText(R.string.failed_to_link);
    		}
    	} else if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
    		//we requested a picture from the external camera app
    		if (resultCode == Activity.RESULT_OK) {
    			//upload the saved image at the directory
    			uploadImage(imageFilePath);
    		}
    	}
    }
    
    private void uploadImage(String imageFilePath) {
    	//use the file system and upload the new image
    	if (!imageFilePath.equals("")) {
	    	String fileName = imageFilePath;
	    	fileName = fileName.substring(fileName.lastIndexOf("/"));
	    	
	    	DbxPath dbxPath = new DbxPath(fileName);
	    	try {
				DbxFile dbxFile = dbxFileSystem.create(dbxPath);
				File imgFile = new File(imageFilePath);
				dbxFile.writeFromExistingFile(imgFile, false);
				dbxFile.close();
			} catch (DbxException e) {
				//error in creating the dropbox file
				lblWelcomeMsg.setText("Error in creating the dropbox file.");
			} catch (IOException e) {
				//error writing the file to dropbox
				lblWelcomeMsg.setText("Error when writing the image to dropbox.");
			}
	    	imageFilePath = "";	//reset the image path so we don't upload the image twice
    	}
    }
    
    private void onLinkToDropBox() {
    	//executed once we have a confirmed link to dropbox
    	dbxAcct = dbxAcctManager.getLinkedAccount();
    	dbxAcctInfo = dbxAcct.getAccountInfo();
		lblWelcomeMsg.setText("Welcome, " + dbxAcctInfo.displayName + ".");
		//get the file system information
		try {
			dbxFileSystem = DbxFileSystem.forAccount(dbxAcct);
		} catch (Unauthorized e) {
			//unauthorized to use the file system
			lblWelcomeMsg.setText("Error in initializing the file system.");
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
