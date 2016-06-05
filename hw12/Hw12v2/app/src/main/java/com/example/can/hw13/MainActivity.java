package com.example.can.hw13;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import java.lang.Math;

import java.io.IOException;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.Color.rgb;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private Camera mCamera;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap bmp = Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);
    private Canvas canvas = new Canvas(bmp);
    private Paint paint1 = new Paint();
    private TextView mTextView;

    private SeekBar myControlR;

    private TextView myTextView1;
    private TextView myTextView2;
    static long prevtime = 0; // for FPS calculation
    SeekBar myControl;
    TextView myTextView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeps the screen from turning off

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();

        mTextureView = (TextureView) findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        mTextView = (TextView) findViewById(R.id.cameraStatus);

        //--//
        myControlR = (SeekBar) findViewById(R.id.seekR);
        myTextView1 = (TextView) findViewById(R.id.textViewR);
        myTextView1.setText("R: "+myControlR.getProgress());
        setMyControlListener1();

        paint1.setColor(0xffff0000); // red
        paint1.setTextSize(24);


        myControl = (SeekBar) findViewById(R.id.seek0);
        myTextView = (TextView) findViewById(R.id.textView00);
        myTextView.setText("Gen Tresh: 38");
        setMyControlListener();

        myTextView2 = (TextView) findViewById(R.id.textViewDel);
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        parameters.setColorEffect(Camera.Parameters.EFFECT_NONE); //color mode // not black and white
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY); // no autofocusing
        parameters.setAutoWhiteBalanceLock(true);//disable auto white balance
        parameters.setAutoExposureLock(true);//disable auto exposure
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90); // rotate to portrait mode

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    // the important function
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
        mTextureView.getBitmap(bmp);

        final Canvas c = mSurfaceHolder.lockCanvas();
        if (c != null) {
            int TRed = myControlR.getProgress();
            int TGen = myControl.getProgress();
            int[] pixels1 = new int[bmp.getWidth()];
            int[] pixels2 = new int[bmp.getWidth()];
            int startY1 = 400; // which row in the bitmap to analyse to read
            int startY2 = 100; // which row in the bitmap to analyse to read
            // only look at one row in the image
            bmp.getPixels(pixels1, 0, bmp.getWidth(), 0, startY1, bmp.getWidth(), 1); // (array name, offset inside array, stride (size of row), start x, start y, num pixels to read per row, num rows to read)
            bmp.getPixels(pixels2, 0, bmp.getWidth(), 0, startY2, bmp.getWidth(), 1); // (array name, offset inside array, stride (size of row), start x, start y, num pixels to read per row, num rows to read)
            // pixels[] is the RGBA data (in black an white).
            // instead of doing center of mass on it, decide if each pixel is dark enough to consider black or white
            // then do a center of mass on the thresholded array
            int[] thresholdedPixels1 = new int[bmp.getWidth()];
            int[] thresholdedColor1 = new int[bmp.getWidth()];
            int[] thresholdedPixels2 = new int[bmp.getWidth()];
            int[] thresholdedColor2 = new int[bmp.getWidth()];
            int wbTotal1 = 0; // total mass
            int wbCOM1 = 0; // total (mass time position)
            int wbTotal2 = 0; // total mass
            int wbCOM2 = 0; // total (mass time position)
            for (int i = 0; i < bmp.getWidth(); i++) {
                // sum the red, green and blue, subtract from 255 to get the darkness of the pixel.
                // if it is greater than some value (600 here), consider it black
                // play with the 600 value if you are having issues reliably seeing the line
                //if (255*3-(red(pixels[i])+green(pixels[i])+blue(pixels[i])) > 600) {
                if ((red(pixels1[i])>TRed*2.55)&&((255-(green(pixels1[i])-red(pixels1[i])) > 6*TGen))){
                    thresholdedPixels1[i] = 255*3;
                    thresholdedColor1[i] = rgb(255,0,0);
                }
                else {
                    thresholdedPixels1[i] = 0;
                    thresholdedColor1[i] = rgb(0,0,255);
                }
                wbTotal1 = wbTotal1 + thresholdedPixels1[i];
                wbCOM1 = wbCOM1 + thresholdedPixels1[i]*i;
            }

            for (int i = 0; i < bmp.getWidth(); i++) {
                // sum the red, green and blue, subtract from 255 to get the darkness of the pixel.
                // if it is greater than some value (600 here), consider it black
                // play with the 600 value if you are having issues reliably seeing the line
                //if (255*3-(red(pixels[i])+green(pixels[i])+blue(pixels[i])) > 600) {
                if ((red(pixels2[i])>TRed*2.55)&&((255-(green(pixels2[i])-red(pixels2[i])) > 6*TGen))){
                    thresholdedPixels2[i] = 255*3;
                    thresholdedColor2[i] = rgb(255,0,0);
                }
                else {
                    thresholdedPixels2[i] = 0;
                    thresholdedColor2[i] = rgb(0,0,255);
                }
                wbTotal2 = wbTotal2 + thresholdedPixels2[i];
                wbCOM2 = wbCOM2 + thresholdedPixels2[i]*i;
            }
            int COM1;
            //watch out for divide by 0
            if (wbTotal1<=0) {
                COM1 = bmp.getWidth()/2;
            }
            else {
                COM1 = wbCOM1/wbTotal1;
            }
            int COM2;
            if (wbTotal2<=0) {
                COM2 = bmp.getWidth()/2;
            }
            else {
                COM2 = wbCOM2/wbTotal2;
            }
            float delta;
            delta = COM2 - COM1;
            double angle = Math.atan(delta/(startY1-startY2))*180/3.14;
            int degrees = (int) angle;
            int pos = (COM2 + COM1)/2-320;
            myTextView2.setText("angle: " + degrees + "º Av. Pos: " + pos);

            // draw a circle where you think the COM is
            canvas.drawCircle(COM1, startY1, 6, paint1);
            bmp.setPixels(thresholdedColor1, 0, bmp.getWidth(), 0, startY1, bmp.getWidth(), 1);
            canvas.drawCircle(COM2, startY2, 6, paint1);
            bmp.setPixels(thresholdedColor2, 0, bmp.getWidth(), 0, startY2, bmp.getWidth(), 1);

            // also write the value as text
            canvas.drawText("COM1 = " + COM1, 10, startY1, paint1);
            canvas.drawText("COM2 = " + COM2, 10, startY2, paint1);
            c.drawBitmap(bmp, 0, 0, null);
            mSurfaceHolder.unlockCanvasAndPost(c);

            // calculate the FPS to see how fast the code is running
            long nowtime = System.currentTimeMillis();
            long diff = nowtime - prevtime;
            mTextView.setText("FPS " + 1000/diff);
            prevtime = nowtime;
        }
    }
    private void setMyControlListener() {
        myControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                myTextView.setText("Gen Tresh: "+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void setMyControlListener1() {
        myControlR.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                myTextView1.setText("R: "+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

}

