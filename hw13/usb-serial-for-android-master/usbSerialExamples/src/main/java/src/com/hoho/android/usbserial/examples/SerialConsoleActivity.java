/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.Color.rgb;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity implements TextureView.SurfaceTextureListener {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;
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
    private TextView myTextView3;
    static long prevtime = 0; // for FPS calculation
    SeekBar myControl;
    SeekBar myControlPWM;
    TextView myTextView;

    Button btnSwitch;
    Button pwrSwitch;

    private boolean isPowerOn = false;
    private boolean isFlashOn;
    Camera.Parameters params;
    private int LeftPWM;
    private int RightPWM;
    private TextView mTitleTextView;
//    private TextView mDumpTextView;
    private ScrollView mScrollView;
//    private CheckBox chkDTR;
//    private CheckBox chkRTS;

    private TextView myTextViewPWMoutL;
    private TextView myTextViewPWMoutR;
//    private TextView myTextViewPWMinL;
//    private TextView myTextViewPWMinR;
//    private SeekBar myControlPWML;
//    private SeekBar myControlPWMR;
    private int i = RightPWM;
    private int j = LeftPWM;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        //mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

//        chkDTR = (CheckBox) findViewById(R.id.checkBoxDTR);
//        chkRTS = (CheckBox) findViewById(R.id.checkBoxRTS);

//        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                try {
//                    sPort.setDTR(isChecked);
//                }catch (IOException x){}
//            }
//        });

//        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                try {
//                    sPort.setRTS(isChecked);
//                }catch (IOException x){}
//            }
//        });
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();

        mTextureView = (TextureView) findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener((TextureView.SurfaceTextureListener) this);

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

        myTextView2 = (TextView) findViewById(R.id.textViewDeg);
        myTextView3 = (TextView) findViewById(R.id.textViewPos);

//        myControlPWMR = (SeekBar) findViewById(R.id.seekPWMR);
        myTextViewPWMoutL = (TextView) findViewById(R.id.textViewPWMoutL);
        myTextViewPWMoutL.setText("PWM sent L");

//        myControlPWML = (SeekBar) findViewById(R.id.seekPWML);
        myTextViewPWMoutR = (TextView) findViewById(R.id.textViewPWMoutR);
        myTextViewPWMoutR.setText("PWM sent R");

        btnSwitch = (Button) findViewById(R.id.flashBut);
        pwrSwitch = (Button) findViewById(R.id.powerBut);

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    // turn off flash
                    turnOffFlash();
                } else {
                    // turn on flash
                    turnOnFlash();
                }
            }
        });
        pwrSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPowerOn) {
                    // turn off power
                    turnOffPower();

                } else {
                    // turn on power
                    turnOnPower();
                }
            }
        });


//        myTextViewPWMinL = (TextView) findViewById(R.id.textViewPWMinL);
//        myTextViewPWMinL.setText("PWM recvd L");
//        myTextViewPWMinR = (TextView) findViewById(R.id.textViewPWMinR);
//        myTextViewPWMinR.setText("PWM recvd R");

//        setMyControlListenerPWMR();
//        setMyControlListenerPWML();

    }
    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
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



    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

//                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
//                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
//                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
//                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
//                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
//                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
//                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());

                String sendString =
                        String.valueOf(i) + " "
                        + String.valueOf(j) + '\n';
                try {
                    sPort.write(sendString.getBytes(),10); // 10 is the timeout
                }
                catch (IOException e) {}

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText("Can's HumbleBot");
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
//        mDumpTextView.append(message);
//        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
        int i = RightPWM;
        int j = LeftPWM;
        String sendString = String.valueOf(i) + " " + String.valueOf(j) + "\n";
        try {
            sPort.write(sendString.getBytes(),10); // 10 is the timeout
            //myTextViewPWMinR.setText("PWM recvd R: " + message);
        }
        catch (IOException e) {}

    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
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
            int startY2 = 200; // which row in the bitmap to analyse to read
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

            if (isPowerOn){
                if (COM2-320 < 0){
                    LeftPWM = 2999-(320-COM2)*9;
                    RightPWM = 2999;
                }else if (COM2-320 >0){
                    LeftPWM = 2999;
                    RightPWM = 2999-(COM2-320)*9;
                }
            } else {
                LeftPWM = 3;
                RightPWM = 3;
            }
            myTextViewPWMoutL.setText(""+LeftPWM);
            myTextViewPWMoutR.setText(""+RightPWM);

            int degrees = (int) angle;
            int pos = (COM2 + COM1)/2-320;
            myTextView2.setText("Angle: " + degrees + "º");
            myTextView3.setText("Average Pos: " + pos);

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
        myControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 38;
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
        myControlR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 55;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                myTextView1.setText("Red: "+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void turnOnFlash() {
        if (!isFlashOn) {
//            if (mCamera == null || params == null) {
//                return;
//            }
            params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
//            mCamera.startPreview();
            isFlashOn = true;
        }

    }
    private void turnOffFlash() {
        if (isFlashOn) {
//            if (mCamera == null || params == null) {
//                return;
//            }
            params = mCamera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);
//            mCamera.stopPreview();
            isFlashOn = false;
        }
    }

    private void turnOnPower(){
        isPowerOn = true;
    }
    private void turnOffPower(){
        isPowerOn = false;
    }
}
