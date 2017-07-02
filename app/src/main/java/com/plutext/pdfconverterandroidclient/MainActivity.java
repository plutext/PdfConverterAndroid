
/**
 * Contains code from https://github.com/plutext/AndroidPdfViewer/blob/master/sample/src/main/java/com/github/barteksc/sample/PDFViewActivity.java
 *
 * portions Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plutext.pdfconverterandroidclient;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.plutext.services.client.android.ConversionException;
import com.plutext.services.client.android.Converter;
import com.plutext.services.client.android.ConverterHttp;
import com.plutext.services.client.android.Format;

import java.io.ByteArrayOutputStream;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.options)
public class MainActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorListener {

    // Configure this property to point to your own Converter instance.
    private static final String URL = "http://converter-eval.plutext.com:80/v1/00000000-0000-0000-0000-000000000000/convert";

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int REQUEST_CODE = 42;
    public static final int PERMISSION_CODE = 42042;

    public static final String SAMPLE_FILE = "sample-docxv2.docx";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";

    static {

        /* Avoid:

            W/System.err: android.os.NetworkOnMainThreadException
            W/System.err:     at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork(StrictMode.java:1425)

            We shouldn't perform a networking operation on our main thread, but
            we do so at the moment for simplicity.

            See further http://www.androiddesignpatterns.com/2012/06/app-force-close-honeycomb-ics.html
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @ViewById
    PDFView pdfView;

    View progressOverlay;

    @NonConfigurationInstance
    Uri uri;

    @NonConfigurationInstance
    Integer pageNumber = 0;

    String fileName;

    @OptionsItem(R.id.pickFile)
    void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    PERMISSION_CODE
            );

            return;
        }

        launchPicker();
    }

    void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("application/pdf");
        intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    @AfterViews
    void afterViews() {
        //pdfView.setBackgroundColor(Color.LTGRAY);
        if (uri != null) {
            displayFromUri(uri);
        } else {
            displayFromAsset(SAMPLE_FILE);
        }
        setTitle(fileName);
    }

    private void displayFromAsset(String assetFileName) {
        fileName = assetFileName;

        if (assetFileName.toLowerCase().endsWith("pdf")) {

            pdfView.fromAsset(SAMPLE_FILE)
                    .defaultPage(pageNumber)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .spacing(10) // in dp
                    .load();

        } else if (assetFileName.toLowerCase().endsWith("doc")
                || assetFileName.toLowerCase().endsWith("docx")) {

            // Convert it
            ClassLoader loader = MainActivity.class.getClassLoader();

            java.net.URL url = loader.getResource("assets/" + SAMPLE_FILE);
            if (url==null) {
                displayError(null, "no file");
                return;
            }

            try {
                java.io.InputStream is = url.openConnection().getInputStream();
                viewWordDocumentAsPDF(is);

            } catch (Exception e) {
                e.printStackTrace();
                displayError(e, null);
            }
        }
    }

    private void displayFromUri(Uri uri) {
        fileName = getFileName(uri);

        if (fileName.toLowerCase().endsWith("pdf")) {

            pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .spacing(10) // in dp
                    .load();

        } else if (fileName.toLowerCase().endsWith("doc")
                || fileName.toLowerCase().endsWith("docx")) {

            try {

                File file = FileUtils.getFile(this, uri);
                viewWordDocumentAsPDF(file);

            } catch (Exception e) {
                e.printStackTrace();
                displayError(e, null);
            }
        }
    }

    byte[] bytes;

    private void viewWordDocumentAsPDF(Object input) throws IOException, ConversionException {

        Toast.makeText(this, "uploading", Toast.LENGTH_SHORT).show();

        progressOverlay = findViewById(R.id.progress_overlay);
        progressOverlay.forceLayout();
        progressOverlay.setVisibility(View.VISIBLE);
        progressOverlay.bringToFront();
        if (progressOverlay==null) {
            System.out.println("progressOverlay null");
        } else if (!progressOverlay.isShown()) {
            System.out.println("progressOverlay not shown");
        }
        TextView tvName = (TextView)findViewById(R.id.hName);
        tvName.setText("uploading..");
        setTitle("uploading..");
        animateView(progressOverlay, View.VISIBLE, 0.4f, 200);
        if (progressOverlay==null) {
            System.out.println("progressOverlay null");
        } else if (!progressOverlay.isShown()) {
            System.out.println("progressOverlay not shown");
        }

        // Convert it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Converter converter = new ConverterHttp(URL);
            if (input instanceof java.io.InputStream) {
                // API using InputStream
                converter.convert((InputStream) input, Format.DOCX, Format.PDF, baos);
            } else if (input instanceof File) {
                // API using File
                converter.convert((File) input, Format.DOCX, Format.PDF, baos);
            }
        } catch (ConversionException ce) {

            if (bytes.toString().length()>80) {
                displayError(ce, "Error in conversion process \n\r"
                        + baos.toString().substring(0, 80)
                        + "\n\r");
            } else {
                displayError(ce, "Error in conversion process \n\r"
                        + baos.toString()
                        + "\n\r");
            }
            return;

            // overtlay shows here!!
        }

        bytes = baos.toByteArray();
        tvName.setText("converted .. " + bytes.length + " bytes; now view it...");
        setTitle("converted .. " + bytes.length + " bytes; now view it...");
        Toast.makeText(this, "converted .. " + bytes.length + " bytes; now view it...",
                Toast.LENGTH_SHORT).show();

        // Display the result
        pdfView.fromBytes(bytes)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .onError(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .load();

        animateView(progressOverlay, View.GONE, 0, 200);

    }

    public void onError(Throwable t) {

        if (bytes.toString().length()>80) {
            displayError(t, "Error viewing converter output \n\r"
                    + bytes.toString().substring(0, 80)
                    + "\n\r");
        } else {
            displayError(t, "Error viewing converter output \n\r"
                    + bytes.toString()
                    + "\n\r");
        }
    }

    private void displayError(Throwable t, String commentary) {

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        if (commentary!=null) pw.append(commentary  + "\n\r");
        if (t!=null && t.getCause()!=null) {
            pw.append(t.getCause().getMessage() + "\n\r");
            t.getCause().printStackTrace(pw);
        } else if (t!=null) {
            pw.append(t.getMessage() + "\n\r");
            t.printStackTrace(pw);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(sw.toString())
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * @param view         View to animate
     * @param toVisibility Visibility at the end of animation
     * @param toAlpha      Alpha at the end of animation
     * @param duration     Animation duration in ms
     */
    public static void animateView(final View view, final int toVisibility, float toAlpha, int duration) {

        // from https://stackoverflow.com/questions/18021148/display-a-loading-overlay-on-android-screen

        boolean show = toVisibility == View.VISIBLE;
        if (show) {
            view.setAlpha(0);
        }
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
        view.animate()
                .setDuration(duration)
                .alpha(show ? toAlpha : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(toVisibility);
                    }
                });

    }

    @OnActivityResult(REQUEST_CODE)
    public void onResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            uri = intent.getData();
            displayFromUri(uri);
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", fileName, page + 1, pageCount));
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    @Override
    public void loadComplete(int nbPages) {

        Log.e(TAG, "got PDF; page count= " + nbPages);
//        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

//    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
//        for (PdfDocument.Bookmark b : tree) {
//
//            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));
//
//            if (b.hasChildren()) {
//                printBookmarksTree(b.getChildren(), sep + "-");
//            }
//        }
//    }

    /**
     * Listener for response to user permission request
     *
     * @param requestCode  Check that permission request code matches
     * @param permissions  Permissions that requested
     * @param grantResults Whether permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            }
        }
    }

}