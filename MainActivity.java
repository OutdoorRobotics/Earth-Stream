package com.examples.user.or_gopro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "gopro";
    public static final String PREFS_NAME = "MyPrefsFile";
    // Turn on camera : http://<ip>/bacpac/PW?t=<password>&p=%01
    WifiManager wifiManager;
    WifiReceiver wifiReceiver;
    Button wifiButton;
    TextView log;
    URL url;
//http://10.5.5.9:8080/videos/DCIM/100GOPRO/hero2
//Start /*Streaming http://10.5.5.9/gp/gpControl/execute?p1=gpStream&c1=start
   // Restart Streaming http://10.5.5.9/gp/gpControl/execute?p1=gpStream&c1=restart
   // Stop Streaming http://10.5.5.9/gp/gpControl/execute?p1=gpStream&c1=stop
   // Video can be streamed by using aplay or ffplay on udp://:8554, connection must be kept alive using hero4-udp-keep-alive-send.py script.*/

    String wifiSSID;
    String wifiPass;
VideoView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = (TextView) findViewById(R.id.log);
        videoView = (VideoView) findViewById(R.id.videoView1);
        wifiButton = (Button) findViewById(R.id.wifi);
        wifiButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                refreshWifi();
            }
        });

        // Restore preferences
     /*  SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        wifiSSID = settings.getString("wifiSSID", "");
        wifiPass = settings.getString("wifiPass", "");

        if (wifiSSID.equals("") || wifiPass.equals("")) {
            displaySettingPopup();
        }*/
// Restore preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        wifiSSID = preferences.getString("SSID", "n/a");
        wifiPass = preferences.getString("PW", "n/a");
      //  Log.e("wifiSSID",wifiPass);

        if (wifiSSID.equals("") || wifiPass.equals("")) {
            Intent i = new Intent(MainActivity.this, MyPreferencesActivity.class);
            startActivity(i);
        }
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        log.setText(wifiInfo.getSSID());
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            log.setText("Disconnected");
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + wifiSSID + "\"";
        conf.preSharedKey = "\"" + wifiPass + "\"";
        wifiManager.addNetwork(conf);

        refreshWifi();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // We have only one menu option
            case R.id.action_settings:
                // Launch Preference activity
                Intent i = new Intent(MainActivity.this, MyPreferencesActivity.class);
                startActivity(i);
                break;
        }
        return true;
    }
    public void onCaptureClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            sendcommand("http://10.5.5.9/bacpac/SH?t=" + wifiPass + "&p=%01"," Capture pic");
        } else {
            sendcommand("http://10.5.5.9/bacpac/SH?t=" + wifiPass	+ "&p=%00","");
        }
    }
    public void onPowerClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            sendcommand("http://10.5.5.9/bacpac/PW?t=" + wifiPass	+ "&p=%01"," Start camera");
        } else {
            sendcommand("http://10.5.5.9/bacpac/PW?t=" + wifiPass	+ "&p=%00"," Stop camera");
        }
    }
    private void sendcommand(final String StrUrl,String txt)
    {
        log.setText(txt);
        (new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    url = new URL(StrUrl);
                    java.net.URLConnection con = url.openConnection();
                    con.connect();
                    java.io.BufferedReader in =new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()));

                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        })).start();
    }
    private void PlayVideo()
    {
        try
        {
            final VideoView videoView = (VideoView) findViewById(R.id.videoView1);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            Uri video= Uri.parse("http://10.5.5.9:8080/live/amba.m3u8");
            // http://10.5.5.9/gp/gpExec?p1=gpStreamA9&c1=restart
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(video);
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.stop();
                    PlayVideo();
                }
            });
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return true;
                }
            });
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    videoView.start();
                }
            });

        }
        catch(Exception e)
        {
            System.out.println("Video Play Error :"+e.toString());
            finish();
        }

    }
//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }


   /* public boolean onMenuItemSelected(int featureId, MenuItem item) {
        displaySettingPopup();
        return super.onMenuItemSelected(featureId, item);
    }*/


    protected void refreshWifi() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo.getSSID() == null || !wifiInfo.getSSID().equals(wifiSSID)) {

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + wifiSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();

                    break;
                }
            }
        }

    }
    public void onToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            sendcommand("http://10.5.5.9/camera/PV?t=" + wifiPass	+ "&p=%02"," Enable streaming");
            PlayVideo();
        } else {
            videoView.stopPlayback();
            sendcommand("http://10.5.5.9/camera/PV?t=" + wifiPass	+ "&p=%00"," Disable streaming");
        }
    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReceiver, new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            NetworkInfo wifiNetworkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            Log.v(TAG, "mWifiNetworkInfo: " + wifiNetworkInfo.toString());

            if (wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                log.setText("Connected to: "
                        + wifiManager.getConnectionInfo().getSSID());
                if (wifiManager != null
                        && wifiManager.getConnectionInfo() != null
                        && wifiManager.getConnectionInfo().getSSID() != null
                        && !wifiManager.getConnectionInfo().getSSID()
                        .equals(wifiSSID)) {
                    Log.v(TAG, wifiManager.getConnectionInfo().getSSID());

                    refreshWifi();
                }
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTING) {
                log.setText("Connecting...");
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.DISCONNECTING) {
                log.setText("Disconnecting...");
            } else if (wifiNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                log.setText("Disconnected");
            }

        }


    }
}