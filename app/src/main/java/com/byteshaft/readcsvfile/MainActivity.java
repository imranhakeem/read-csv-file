package com.byteshaft.readcsvfile;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.siegmar.fastcsv.reader.CsvContainer;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int MULTIPLE_PERMISSIONS = 100;
    private String[] permissions;
    private Button mButtonManualSearch;
    private Button mScanButton;
    private Button mLoadCsvButton;
    private TextView mTextviewFileName;
    private TextView mTextViewModuleNumber;
    private TextView mAbsNumber;
    private TextView mPosition;
    private EditText mInputFieldEAN;
    private final int PICKFILE_RESULT_CODE = 7;
    Uri uri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        //initialize UI
        mTextviewFileName = findViewById(R.id.textview_filename);
        mTextViewModuleNumber = findViewById(R.id.text_view_module_number);
        mAbsNumber = findViewById(R.id.text_view_abs_number);
        mPosition = findViewById(R.id.text_view_position);
        mInputFieldEAN = findViewById(R.id.edit_text_ean);

        mScanButton = findViewById(R.id.button_scan);
        mLoadCsvButton = findViewById(R.id.button_load_csv);
        mButtonManualSearch = findViewById(R.id.button_manual_search);

        mScanButton.setOnClickListener(this);
        mButtonManualSearch.setOnClickListener(this);
        mLoadCsvButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_manual_search:
                // TODO: 25/12/2018
                if (uri != null) {
                    if (!mInputFieldEAN.getText().toString().trim().isEmpty()) {
                        doSHit(getPath(MainActivity.this, uri), mInputFieldEAN.getText().toString());
                    } else {
                        Toast.makeText(MainActivity.this, "Please input EAN Number", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_LONG).show();
                }

                break;

            case R.id.button_scan:
                if (checkPermissions()) {
                    startActivity(new Intent(MainActivity.this, ScannerActivity.class));
                }
                break;

            case R.id.button_load_csv:
                if (checkPermissions()) {
                    // TODO: 25/12/2018 open file manager to select csv file
                    // for now csv file is on device  /mnt/sdcard
//                    String path = Environment.getExternalStorageDirectory().toString() + "/garten.csv";
//                    doSHit(path);
                    openFileChooser();
                }
                break;

        }
    }

    private void doSHit(String file, String eanNumber) {
        File f = new File(file);
        CsvReader csvReader = new CsvReader();
        csvReader.setFieldSeparator(';');
//        csvReader.setTextDelimiter('\'');
        csvReader.setContainsHeader(true);

        CsvContainer csv;
        try {
            csv = csvReader.read(f, StandardCharsets.UTF_8);
//            List<CsvRow> rows = csv.getRows();
//            Log.e("ok", "doSHit: " + csv.getRows());
            for (CsvRow row : csv.getRows()) {
                if (row.getField("EAN").equals(eanNumber)) {
                    System.out.println(row.getField("Position"));
                    mPosition.setText("Position Number: " + row.getField("Position"));
                    mAbsNumber.setText("Abschnitt Number: " + row.getField("Abschnitt"));
                    mTextViewModuleNumber.setText("Modul Number: " + row.getField("Modul"));
                } else {
                    Toast.makeText(MainActivity.this, "No record found", Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(
                    new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a CSV File"), PICKFILE_RESULT_CODE);
    }

    public static String getPath(Context context, Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
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
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if (isMediaDocument(uri)) {
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
                final String[] selectionArgs = new String[] {split[1]};
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

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
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

    private void setSelectedFileName(Intent dataIntent) {
        Uri uri = dataIntent.getData();
        String uriString = uri.toString();
        File myFile = new File(uriString);
        String displayName = null;

        if (uriString.startsWith("content://")) {
            Cursor cursor = null;
            try {
                cursor = MainActivity.this.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        } else if (uriString.startsWith("file://")) {
            displayName = myFile.getName();
        }
        // set name of the selected file
        mTextviewFileName.setText("FILE NAME: " + displayName);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissionsList,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per;
                        }
                    }
                    System.out.println("permission granted");
                }
                return;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    uri = data.getData();
                    setSelectedFileName(data);
                }
                break;

        }
    }
}
