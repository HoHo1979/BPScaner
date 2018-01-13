package com.iotarch.bpscaner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //Needed before the class is loaded , otherwise it will not find Imgproc from the c++
    static {
        System.loadLibrary("opencv_java3");
    }
    private static final int SELECT_PICTURE = 8;
    private static final int REQUEST_EXTERNAL_STORAGE = 100;
    private static final int MEAN_BLUR = 9;
    private static final int GAUSSIAN_BLUR = 200;
    private static final int MEDIAN_BLUR = 3;
    private static final int PICK_PHOTO = 500;
    private String selectImagePath;
    private final int SELECT_PHOTO = 1;
    private ImageView ivImage, ivImageProcessed;

    static int ACTION_MODE = 0;
    private Mat originalMat;
    private Mat sampleImage;
    private Mat mat;
    private Mat src;
    private Bitmap currentBitmap;
    private Bitmap originalBitmap;

//    private TextView mTextMessage;

    private static final String TAG = MainActivity.class.getSimpleName();


    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:

//                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }

        }
    };

//    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
//            = new BottomNavigationView.OnNavigationItemSelectedListener() {
//
//        @Override
//        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//            switch (item.getItemId()) {
//                case R.id.navigation_home:
//
//                    return true;
//                case R.id.navigation_dashboard:
//                    mTextMessage.setText(R.string.title_dashboard);
//                    return true;
//                case R.id.navigation_notifications:
//                    mTextMessage.setText(R.string.title_notifications);
//                    return true;
//                case R.id.action_openGallary:
//                    openGallary(PICK_PHOTO);
//                    return true;
//                case R.id.DoG:
//                    openGallary(GAUSSIAN_BLUR);
//                    return true;
//
//            }
//            return false;
//        }
//
//    };



    private void openGallary(int x) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, x);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);

        int requestPermission = ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(requestPermission!= PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_EXTERNAL_STORAGE);

        }else {


          //  openCVView();
        }

        ivImage = (ImageView)findViewById(R.id.ivImage);
        ivImageProcessed = (ImageView)findViewById(R.id.ivImageProcessed);
        Intent intent = getIntent();

        if(intent.hasExtra("ACTION_MODE")) {
            ACTION_MODE = intent.getIntExtra("ACTION_MODE", 0);
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
//        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    private void openCVView() {
//        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloVisionView);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setCvCameraViewListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_EXTERNAL_STORAGE:
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){

                    openCVView();

                }else{
                    new AlertDialog.Builder(this)
                            .setMessage("Need write permission")
                            .setPositiveButton("OK",null)
                            .show();
                    return;
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if(!OpenCVLoader.initDebug()){
//            Log.d(TAG, "The open cv is not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0,this,mLoaderCallback);
//        }else{
//            Log.d(TAG, "Open CV is loaded");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }


    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat = inputFrame.rgba();
        double pixel[] = mat.get(0,0);
        double red=pixel[0];
        double green=pixel[1];
        double blue=pixel[2];
        Log.i(TAG, "red channel value: "+red);
        Log.i(TAG, "green channel value: "+green);
        Log.i(TAG, "blue channel value: "+blue);

        return inputFrame.rgba();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id==R.id.action_openGallary){
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case SELECT_PHOTO:
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    originalMat = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(selectedImage,originalMat);
                    ivImage = (ImageView) findViewById(R.id.ivImage);
                    ivImage.setImageBitmap(selectedImage);

                    Mat grayMat = new Mat();
                    Mat blur1 = new Mat();
                    Mat blur2 = new Mat();



                    //Converting the image to grayscale
                    Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
                    Imgproc.GaussianBlur(grayMat,blur1,new Size(15,15),5);
                    Imgproc.GaussianBlur(grayMat,blur2,new Size(21,21),5);

                    Mat DoG = new Mat();
                    Core.absdiff(blur1,blur2,DoG);
                    Core.multiply(DoG,new Scalar(100), DoG);
                    Imgproc.threshold(DoG,DoG,50,255,Imgproc.THRESH_BINARY_INV);

                    currentBitmap = selectedImage.copy(Bitmap.Config.ARGB_8888,false);

                    Utils.matToBitmap(DoG, currentBitmap);

//                    Utils.matToBitmap(originalMat,selectedImage);
                    ivImageProcessed = (ImageView) findViewById(R.id.ivImageProcessed);
                    ivImageProcessed.setImageBitmap(currentBitmap);

                }catch(Exception e){
                    e.printStackTrace();
                }
                break;

        }
    }



    public void differenceOfGaussian()
    {
        Mat grayMat = new Mat();
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();

        //originalMat = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8U);
        //Converting the image to grayscale

        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);

        //Bluring the images using two different blurring radius
        Imgproc.GaussianBlur(grayMat,blur1,new Size(15,15),5);
        Imgproc.GaussianBlur(grayMat,blur2,new Size(21,21),5);

        //Subtracting the two blurred images
        Mat DoG = new Mat();
        Core.absdiff(blur1, blur2,DoG);

        //Inverse Binary Thresholding
        Core.multiply(DoG,new Scalar(100), DoG);
        Imgproc.threshold(DoG,DoG,50,255,Imgproc.THRESH_BINARY_INV);


        //Converting Mat back to Bitmap
        Utils.matToBitmap(DoG, originalBitmap);



        Log.d(TAG, "DifferenceOfGaussian: finished and put back to imageView");

        ImageView imgView = (ImageView) findViewById(R.id.ivImage);
        imgView.setImageBitmap(originalBitmap);


    }



    private void loadImageToImageView() {

        //Convert Bitmap to Mat

        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888,false);

        ImageView imgView = (ImageView) findViewById(R.id.ivImageProcessed);
        imgView.setImageBitmap(currentBitmap);

        ImageView imgView2 = (ImageView) findViewById(R.id.ivImage);
        imgView2.setImageBitmap(originalBitmap);

    }


}
