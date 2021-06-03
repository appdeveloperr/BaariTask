package com.techreneur.baaritask;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.abedelazizshe.lightcompressorlibrary.CompressionListener;
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor;
import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.potyvideo.library.AndExoPlayerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import VideoHandle.EpEditor;
import VideoHandle.EpVideo;
import VideoHandle.OnEditorListener;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button btn;
    private Button btnLoadVideo;
    private TextView loadingTV;
    private TextView beforeCompTV;
    private TextView afterCompTV;
    private List<String> selectedVideos;
    private String fileOutput;
    private String compressedfileOutput;
    private ProgressBar progressBar;
    private AndExoPlayerView andExoPlayerView;
    private static final int SELECT_VIDEOS = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermissions();
        initViews();
        initListners();

    }



    private void askPermissions() {

        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        String rationale = "Please provide required permission so that we can provide you best app experience.. Thanks";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning");

        Permissions.check(this/*context*/, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                // do your task.
                Toast.makeText(MainActivity.this, "Success: Permission granted!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                // permission denied, block the feature.
                Toast.makeText(MainActivity.this, "Error: Permission are not granted!", Toast.LENGTH_SHORT).show();

            }
        });
    }


    private void initViews() {

        btn = (Button) findViewById(R.id.btn);
        btnLoadVideo = (Button) findViewById(R.id.btnLoadVideo);
        loadingTV = (TextView) findViewById(R.id.loadingTV);
        beforeCompTV = (TextView) findViewById(R.id.beforeCompTV);
        afterCompTV = (TextView) findViewById(R.id.afterCompTV);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        andExoPlayerView = findViewById(R.id.andExoPlayerView);
        videoView = (VideoView) findViewById(R.id.vv);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
    }


    private void initListners() {

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("video/mp4");
                startActivityForResult(intent, SELECT_VIDEOS);
            }
        });



        btnLoadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> extraHeaders = new HashMap<>();
                extraHeaders.put("foo", "bar");
                andExoPlayerView.setSource(compressedfileOutput, extraHeaders);
            }
        });


    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            selectedVideos = getSelectedVideos(requestCode, data);
            Log.d("path", selectedVideos.toString());

            if (selectedVideos.size() > 1) {
                mergeVideos(selectedVideos);
            } else {
                Toast.makeText(this, "Single Video Cannot be Merged", Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void mergeVideos(List<String> selectedVideos) {


        progressBar.setVisibility(View.VISIBLE);
        loadingTV.setVisibility(View.VISIBLE);

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String formattedDate = df.format(c);
        File file = new File(getFilesDir(), formattedDate + "_merged_output.mp4");
        fileOutput = file.getAbsolutePath();


        ArrayList<EpVideo> epVideos = new ArrayList<>();
        //Adding All Selected Videos in epVideos List
        for (int i = 0; i < selectedVideos.size(); i++) {
            epVideos.add(new EpVideo(selectedVideos.get(i)));
        }

        EpEditor.OutputOption outputOption = new EpEditor.OutputOption(file.getAbsolutePath());
        outputOption.setWidth(720);
        outputOption.setHeight(1280);
        outputOption.frameRate = 25;
        outputOption.bitRate = 10; //Default
        EpEditor.merge(epVideos, outputOption, new OnEditorListener() {
            @Override
            public void onSuccess() {
                Log.d("Status", "Success Video Has Merged: " + fileOutput);
                setViewVisible();

            }

            @Override
            public void onFailure() {
                Log.d("Status", "Success Video Has Not Merged");
                setErrorView();
            }

            @Override
            public void onProgress(float progress) {
                // Get processing progress here
                Log.d("Progress", "" + progress);

            }
        });

    }

    private void setErrorView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                loadingTV.setText("Error While Merging Videos");
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setViewVisible() {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                beforeCompTV.setVisibility(View.VISIBLE);
                beforeCompTV.setText("Before compression Merged File size: " + checkFileSizeInMB(fileOutput) + " MB");
                CompressFile();

            }
        });


    }

    private String checkFileSizeInMB(String fileOutput) {


        File file = new File(fileOutput);
        // Get length of file in bytes
        long fileSizeInBytes = file.length();
        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        long fileSizeInKB = fileSizeInBytes / 1024;
        //  Convert the KB to MegaBytes (1 MB = 1024 KBytes)
        long fileSizeInMB = fileSizeInKB / 1024;

        return String.valueOf(fileSizeInMB);
    }

    private void CompressFile() {

        loadingTV.setText("Video is compressing.. Please Wait..");
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String formattedDate = df.format(c);
        File file = new File(getFilesDir(), formattedDate + "_compressed_merged_output.mp4");
        compressedfileOutput = file.getAbsolutePath();


        VideoCompressor.start(
                this, // => This is required if srcUri is provided. If not, pass null.
                null, // => Source can be provided as content uri, it requires context.
                fileOutput, // => This could be null if srcUri and context are provided.
                compressedfileOutput,
                new CompressionListener() {
                    @Override
                    public void onStart() {
                        // Compression start
                    }

                    @Override
                    public void onSuccess() {
                        // On Compression success

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                loadingTV.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                                btnLoadVideo.setVisibility(View.VISIBLE);
                                afterCompTV.setVisibility(View.VISIBLE);
                                afterCompTV.setText("After compression Merged File size: " + checkFileSizeInMB(compressedfileOutput) + " MB");
                            }
                        });
                    }

                    @Override
                    public void onFailure(String failureMessage) {
                        // On Failure
                    }

                    @Override
                    public void onProgress(float v) {
                        // Update UI with progress value

                    }

                    @Override
                    public void onCancelled() {
                        // On Cancelled
                    }
                }, VideoQuality.LOW, false, false);

    }

    private List<String> getSelectedVideos(int requestCode, Intent data) {

        List<String> result = new ArrayList<>();

        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item videoItem = clipData.getItemAt(i);
                Uri videoURI = videoItem.getUri();
                String filePath = getPath(this, videoURI);
                result.add(filePath);
            }
        } else {
            Uri videoURI = data.getData();
            String filePath = getPath(this, videoURI);
            result.add(filePath);
        }

        return result;
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


}