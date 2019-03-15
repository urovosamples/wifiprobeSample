package com.example.wifiprobeserviceimpl;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.udroid.device.wifiprobe.OnSwitchListener;
import com.udroid.device.wifiprobe.OnTaskStaListener;
import com.udroid.device.wifiprobe.WiFiProbeService;
import com.udroid.device.wifiprobe.WiFiProbeServiceImpl;

public class MainActivity extends Activity {
    WiFiProbeServiceImpl mWiFiProbeServiceImpl;
    WiFiProbeService mIWiFiProbeService;
    String TAG = "MainActivity";
    TextView ssid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(mWiFiProbeServiceImpl == null){
            mWiFiProbeServiceImpl = new WiFiProbeServiceImpl(this);
        }
        mIWiFiProbeService = (WiFiProbeService)mWiFiProbeServiceImpl.getDeviceBinder();
        ssid = (TextView) findViewById(R.id.ssid);
        Button open = (Button) findViewById(R.id.open);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mIWiFiProbeService.openWifiStaProbe(new OnSwitchListener.Stub() {
                        @Override
                        public void swich(int ret, String msg) throws RemoteException {
                            Log.d(TAG, "openWifiStaProbe ret= " + msg);
                        }
                    });
                } catch (Exception e) {}

            }
        });
        Button close = (Button) findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mIWiFiProbeService.closeWifiStaProbe(new OnSwitchListener.Stub() {
                        @Override
                        public void swich(int ret, String msg) throws RemoteException {
                            Log.d(TAG, "closeWifiStaProbe ret= " + msg);
                        }
                    });
                } catch (Exception e) {}

            }
        });

        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mIWiFiProbeService.registerStaCallback(new OnTaskStaListener.Stub() {
                        @Override
                        public void getWiFiProbeOfSta(final  String probeinfo, final String rssi,final long time) throws RemoteException {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ssid.append(" MAC " +probeinfo + " rssi " + rssi + " time " + time + "\n");
                                }
                            });
                        }
                    });
                    mIWiFiProbeService.startGetStaProbeInfo();
                } catch (Exception e) {}

            }
        });
        Button stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    mIWiFiProbeService.closeWifiStaProbeInfo(new OnSwitchListener.Stub() {
                        @Override
                        public void swich(int ret, String msg) throws RemoteException {
                            Log.d(TAG, "closeWifiStaProbeInfo ret= " + msg);
                        }
                    });

                } catch (Exception e) {}

            }
        });
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     * This is a good place to begin animations, open exclusive-access devices
     * (such as the camera), etc.
     *
     * <p>Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front.  Use {@link #onWindowFocusChanged} to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestoreInstanceState
     * @see #onRestart
     * @see #onPostResume
     * @see #onPause
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed.  The counterpart to
     * {@link #onResume}.
     *
     * <p>When activity B is launched in front of activity A, this callback will
     * be invoked on A.  B will not be created until A's {@link #onPause} returns,
     * so be sure to not do anything lengthy here.
     *
     * <p>This callback is mostly used for saving any persistent state the
     * activity is editing, to present a "edit in place" model to the user and
     * making sure nothing is lost if there are not enough resources to start
     * the new activity without first killing this one.  This is also a good
     * place to do things like stop animations and other things that consume a
     * noticeable amount of CPU in order to make the switch to the next activity
     * as fast as possible, or to close resources that are exclusive access
     * such as the camera.
     *
     * <p>In situations where the system needs more memory it may kill paused
     * processes to reclaim resources.  Because of this, you should be sure
     * that all of your state is saved by the time you return from
     * this function.  In general {@link #onSaveInstanceState} is used to save
     * per-instance state in the activity and this method is used to store
     * global persistent data (in content providers, files, etc.)
     *
     * <p>After receiving this call you will usually receive a following call
     * to {@link #onStop} (after the next activity has been resumed and
     * displayed), however in some cases there will be a direct call back to
     * {@link #onResume} without going through the stopped state.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onResume
     * @see #onSaveInstanceState
     * @see #onStop
     */
    @Override
    protected void onPause() {
        super.onPause();
        //mWiFiProbeServiceImpl.unregisterStaCallback();
        mWiFiProbeServiceImpl.close();
    }
}
