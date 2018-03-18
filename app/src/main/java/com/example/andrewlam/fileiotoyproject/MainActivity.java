package com.example.andrewlam.fileiotoyproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LongDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.example.andrewlam.fileiotoyproject.glide.GlideApp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class MainActivity extends AppCompatActivity {

    // RC code for picking a file
    private static final String LOG_TAG = "FileIO";
    private static final int REQUEST_PICK_FILE = 1;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.andrewlam.fileiotoyproject.fileprovider";

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO Setup imageview for preview
        mImageView = findViewById(R.id.preview_iv);

        // TODO 1) Create a button to add picture, when clicked opens UI system picker (onPick)
        Button pickFileBtn = findViewById(R.id.pick_file_btn);
        pickFileBtn.setOnClickListener(view -> handlePickFiles());


        // TODO 4) Create a button to save the cache uri to local uri (onSave)
        Button saveFileBtn = findViewById(R.id.save_file_btn);
        saveFileBtn.setOnClickListener(view -> handleSave());
    }

    private void handlePickFiles(){
        Log.d(LOG_TAG,"clicked pickFile btn");
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("image/*");

        showFilePickerActivity(intent, REQUEST_PICK_FILE);
    }

    // TODO Handle starting the activity for result
    private void showFilePickerActivity(Intent intent, int requestCode){
        Log.d(LOG_TAG,"launching system file picker ui");
        startActivityForResult(intent,requestCode);
    }

    // TODO Handle user selection on activity result, handle the returned data (uri)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED){
            // user cancelled the operation
        }else if (resultCode == RESULT_OK) {
            // received result, check code
            switch (requestCode) {
                case REQUEST_PICK_FILE:
                    Log.d(LOG_TAG,"got file result from file picker");
                    if (data != null && data.getData() !=null) {
                        Uri sourceUri = data.getData();
                        Uri cacheUri = getCacheUri();
                        writeSourceToCache(sourceUri,cacheUri, cacheUri1 -> {
                            Log.w(LOG_TAG,"File I/O finished time: " + System.currentTimeMillis());
                            // Set the new cacheUri as the new instance cacheUri
                            Log.d(LOG_TAG,"writeSourceToCache() completed and returned cacheUri that points to a newly written file");
                            mCacheUri = cacheUri1;
                        });
                    }
                    return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // TODO 2) Create cache file and create uri for it
    @Nullable
    private Uri getCacheUri(){
        Log.d(LOG_TAG,"getCacheUri() called to create cache file and its uri");
        String prefix = UUID.randomUUID().toString();
        File cacheDir = getCacheDir();
        File cacheFile = null;
        Uri cacheUri = null;
        try {
            cacheFile = File.createTempFile(prefix, null, cacheDir);
            cacheUri = FileProvider.getUriForFile(this,FILE_PROVIDER_AUTHORITY,cacheFile);
        }catch (IOException | IllegalArgumentException ex){
            Log.e(LOG_TAG, "Unable to create cache file");
            ex.printStackTrace();
        }
        Log.d(LOG_TAG,"getCacheUri() returned uri: " + cacheUri.toString());
        return cacheUri;
    }

    // TODO 3) Copy source file to the cache file using their uris
    private void writeSourceToCache(Uri sourceUri, Uri cacheUri, OnWriteSourceToCacheComplete listener){
        Log.d(LOG_TAG,"writeSourceToCache() called to copy source to cache file using uris");
        Log.d(LOG_TAG,"source uri: " + sourceUri.toString());
        Log.d(LOG_TAG,"cache uri: " + cacheUri.toString());
        Log.w(LOG_TAG,"File I/O start time:" + System.currentTimeMillis());
        ContentResolver cr = getContentResolver();
        try (InputStream in = cr.openInputStream(sourceUri);
             OutputStream out = cr.openOutputStream(cacheUri)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[2048];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            listener.onComplete(cacheUri);
        } catch (IOException | NullPointerException ex) {
            Log.e(LOG_TAG, "I/O Error in copy file from source uri to cache uri");
            ex.printStackTrace();
        }
    }

    @Nullable
    private Uri mCacheUri;

    private void handleSave(){
        Log.d(LOG_TAG,"handleSave() called for saving cache file to local file");
        Uri localUri = getLocalUri();
        writeCacheToLocal(mCacheUri, localUri, localUri1 -> {
            Log.w(LOG_TAG,"File I/O finished time: " + System.currentTimeMillis());
            Log.d(LOG_TAG,"writeCacheToLocal() completed");
            Log.d(LOG_TAG,"with local uri, loading it as preview image with Glide");
            // TODO 7) When saved to local file, load the the Uri content using Glide
            GlideApp.with(this)
                    .asBitmap()
                    .load(localUri1)
                    .into(mImageView);

            // TODO 8) When image clicked, launch an implicit intent and view the content using system handlers.
            setupImageClickEvent(localUri1);
        });
    }

    // TODO 5) Create local file and create uri for it
    private Uri getLocalUri(){
        Log.d(LOG_TAG,"getLocalUri() called to get a local file with an uri");
        String prefix = UUID.randomUUID().toString();
        String suffix = ".webp";
        File picturesDir = new File(getFilesDir(),"pictures");
        File pictureFile = null;
        Uri pictureUri = null;
        try {
            pictureFile = provideFile(picturesDir,prefix,suffix);
            pictureUri = FileProvider.getUriForFile(this,FILE_PROVIDER_AUTHORITY,pictureFile);
        }catch (IllegalArgumentException | IOException ex){
            Log.e(LOG_TAG, "Unable to create picture file");
            ex.printStackTrace();
        }

        Log.d(LOG_TAG,"getLocalUri() returned uri: " + pictureUri.toString());
        return pictureUri;
    }



    // TODO 6) Copy cache file to the local file using their uris
    private void writeCacheToLocal(Uri cacheUri, Uri localUri, OnWriteCacheToLocalComplete listener){
        Log.d(LOG_TAG,"writeCacheToLocal() called to copy cache to local file using uris");
        Log.d(LOG_TAG,"cache uri: " + cacheUri.toString());
        Log.d(LOG_TAG,"local uri: " + localUri.toString());
        Log.w(LOG_TAG,"File I/O start time:" + System.currentTimeMillis());
        ContentResolver cr = getContentResolver();
        try (InputStream in = cr.openInputStream(cacheUri);
             OutputStream out = cr.openOutputStream(localUri)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[2048];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            listener.onComplete(localUri);
        } catch (IOException | NullPointerException ex) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "I/O Error in copy file from cache uri to local uri");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Method to set the clickListener on the imageView
     * @param localUri
     */
    private void setupImageClickEvent(Uri localUri){
        Log.d(LOG_TAG,"setting up the image click event to launch image-view");
        mImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = getContentResolver().getType(localUri);
            intent.setDataAndType(localUri, type);
            intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(this.getPackageManager()) != null) {
                startActivity(intent);
            }
        });
    }

    // Async callbacks
    public interface OnWriteSourceToCacheComplete{
        void onComplete(Uri cacheUri);
    }

    public interface OnWriteCacheToLocalComplete{
        void onComplete(Uri localUri);
    }

    /**
     * Method to provide file path to an existing file given the directory and file name
     * or create on exclusively if it doesn't exist in the file system
     * @param dir the directory path for the file
     * @param prefix the file prefix, usually the display name of the file
     * @param suffix the file suffix, usually the file extension and denotes the MIME type of the
     *               file
     * @return a file path to an existing file, or if the path doesn't exist, path to an
     * newly created file.
     * @throws IOException thrown when when there error in creating directory and/or a new
     * file (in the case that directory or file doesn't exist in the filesystem).
     */
    private File provideFile(File dir, String prefix, String suffix) throws IOException {
        // Check if the dir exists, if not, make dir
        if (!dir.exists()) {
            // try to make the directory
            if (!dir.mkdir()) {
                String msg = "Unable to create the directory";
                Log.e(LOG_TAG,msg);
                throw new IOException(msg);
            }
        }

        // Check if dir is actually a directory
        if (!dir.isDirectory()){
            String msg = "argument dir is not a directory";
            Log.e(LOG_TAG,msg);
            throw new IOException(msg);
        }

        String fileName = prefix.concat(suffix);
        File f = new File(dir,fileName);
        if (!f.exists()) {
            // try creating a new file
            if (!f.createNewFile()) {
                throw new IOException("Unable to create file");
            }
        }

        if (!f.setWritable(true) || !f.setReadable(true)){
            throw new IOException("Can't set read and/or write write to the file");
        }
        return f;
    }
}
