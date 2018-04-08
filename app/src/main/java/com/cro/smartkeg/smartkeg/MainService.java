package com.cro.smartkeg.smartkeg;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;
import java.util.Set;

public class MainService extends Service {

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    boolean wasBluetoothOn;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("Service oncreate called", "service started");

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        wasBluetoothOn = bluetoothAdapter.isEnabled();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"SmartKegChannelID")
                .setSmallIcon(R.drawable.kegfullsize)
                .setContentTitle("SmartKeg")
                .setContentText("App is running in the background, monitoring your keg")
                .setContentIntent(pendingIntent);
        Notification notification=builder.build();
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel = new NotificationChannel("SmartKegChannelID", "Notifications", NotificationManager.IMPORTANCE_LOW);
            //channel.setDescription("Channel Description");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(1, notification);



        IntentFilter myIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(onBroadcastReceive, myIntentFilter);

        if (isKegConnected())
            startService(new Intent(getApplicationContext(), BLE_Service.class));

        Log.i("Service oncreate called", "oncreate finished");

                /*Log.i("MyBroadcastReceiver", "Broadcast received");
        String action = intent.getAction();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        //BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (action != null)
            switch (action) {
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    Boolean connected = false;
                    List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                    if (devices != null)
                        for(BluetoothDevice device : devices)
                            if(device.getName().equals(context.getResources().getString(R.string.device_name)))
                                connected = true;

                    int connectionstate = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);

                    if (connectionstate == BluetoothAdapter.STATE_CONNECTED && connected) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("BluetoothConnected"));
                        Log.i("MyBroadcastReceiver", "Bluetooth Connected");
                    }
                    else if (connectionstate == BluetoothAdapter.STATE_DISCONNECTED && !connected && BluetoothAdapter.isEnabled()) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("BluetoothConnected"));
                        Log.i("MyBroadcastReceiver", "Bluetooth Disconnected");
                    }
                    break;

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (state == BluetoothAdapter.STATE_ON) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("BluetoothOn"));
                        Log.i("MyBroadcastReceiver", "Bluetooth On");
                    }
                    else if (state == BluetoothAdapter.STATE_OFF) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("BluetoothOff"));
                        Log.i("MyBroadcastReceiver", "Bluetooth Off");
                    }
                    break;
                }*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        return super.onStartCommand(intent, flags, startId);
    }

    BroadcastReceiver onBroadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null)
                switch (action) {
                    case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                        int connectionstate = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);

                        if (connectionstate == BluetoothAdapter.STATE_CONNECTED && isKegConnected()) {
                            startService(new Intent(getApplicationContext(), BLE_Service.class));
                        }
                        else if (connectionstate == BluetoothAdapter.STATE_DISCONNECTED && !isKegConnected() && bluetoothAdapter.isEnabled()) {
                            stopService(new Intent(getApplicationContext(), BLE_Service.class));
                        }
                        break;


                }
        }
    };

    private boolean isKegConnected () {
        Boolean connected = false;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices != null)
            for(BluetoothDevice device : bondedDevices) {
                Log.i("Bluetooth devices", device.toString());
                if (device.getName().equals(getResources().getString(R.string.device_name)))
                    connected = true;
            }
        Log.i("isKegConnected", connected.toString());
        return connected;
    }


    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onBroadcastReceive);

        SharedPreferences myPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean autoBluetoothOff = myPreferences.getBoolean("preference_key_bluetooth_off", true);
        if (!wasBluetoothOn && autoBluetoothOff)
            bluetoothAdapter.disable();

        /*BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        Boolean bluetoothOff = myPreferences.getBoolean("preference_key_bluetooth_off", false);
        Boolean bluetoothWasOn = myPreferences.getBoolean("preference_key_bluetooth_off", false);

        if (bluetoothOff && !bluetoothWasOn)
            bluetoothAdapter.disable();

        stopForeground(true);
        stopSelf();*/

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
