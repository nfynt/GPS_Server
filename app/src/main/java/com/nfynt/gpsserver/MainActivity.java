package com.nfynt.gpsserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.*;

public class MainActivity extends AppCompatActivity {

    ServerSocket serverSocket;
    boolean serverIsActive;
    Thread Thread1 = null;
    TextView tvIP, tvPort;
    TextView tvStatus;
    TextView tvMessages;
    //EditText etMessage;
    Button btnSend;

    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 50000;

    String message;

    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int REQUEST_CODE_PERMISSION = 2;
    // GPSTracker class
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvStatus = findViewById(R.id.tvConnectionStatus);
        tvMessages = findViewById(R.id.tvMessages);
        //etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission) != PackageManager.PERMISSION_GRANTED) {
                // if the permission is not granted
                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvMessages.append(e.getMessage());
        }
        gps = new GPSTracker(MainActivity.this);

        try {
            SERVER_IP = getLocalIpAddress();
            tvStatus.setText("Offline");
            tvIP.setText("IP: " + SERVER_IP);
            tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            tvMessages.append(e.getMessage());
        }


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // message = etMessage.getText().toString().trim();
                if(serverIsActive)
                    btnSend.setText("Start");
                else
                    btnSend.setText("Stop");
                ToggleServerState();
            }
        });
    }

    private void ToggleServerState()
    {
        serverIsActive=!serverIsActive;
        if(serverIsActive) {
            tvStatus.setText("Online");
            gps.initLocationTracker();
            Thread1 = new Thread(new Thread1());
            Thread1.start();
        }else{
            tvStatus.setText("Offline");
            gps.stopUsingGPS();
        }
        //new Thread(new Thread3(message)).start();
    }
    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    private PrintWriter output;
    private BufferedReader input;

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                double latitude=0;
                double longitude=0;
                double altitude=0;
                Timestamp time = new Timestamp(System.currentTimeMillis());
                String t_stamp;
                serverSocket = new ServerSocket(SERVER_PORT);
                socket = serverSocket.accept();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream());

                while (true) {
                    if(!serverIsActive) break;

                    // check if GPS enabled
                    if(gps.canGetLocation()){
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        altitude = gps.getAltitude();

                        //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
                        //+ latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                    }else{
                        // Ask user to enable GPS/network in settings
                        gps.showSettingsAlert();
                    }
                    time = new Timestamp(System.currentTimeMillis());
                    //return number of milliseconds since January 1, 1970, 00:00:00 GMT
                    t_stamp = String.valueOf(time.getTime());
                    message = "$<time,lat,lon,alt>: ("+t_stamp+"," + String.valueOf(latitude) + ","+String.valueOf(longitude)+","+String.valueOf(altitude)+")";

                    output.write(message);
                    output.flush();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText(message);
                        }
                    });
                    Thread.sleep(1000);
                }
                output.flush();
                serverSocket.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("client:" + message + "\n");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message);
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("server: " + message + "\n");
                    //etMessage.setText("");
                }
            });
        }
    }
}



/*
 __  _ _____   ____  _ _____  
|  \| | __\ `v' /  \| |_   _| 
| | ' | _| `. .'| | ' | | |   
|_|\__|_|   !_! |_|\__| |_|
 
*/