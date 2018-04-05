package com.cro.smartkeg.smartkeg;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.os.Handler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.ArrayUtils;

public class BLE_Service extends Service {

    boolean deviceConnected=false;

    float MAX_VOLTAGE = 4.2f;
    float MIN_VOLTAGE = 2.5f;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private Queue<BluetoothGattDescriptor> descriptorSetQueue = new ConcurrentLinkedQueue<>();
    private final Object serviceLock = new Object();
    private final Object initWriteLock = new Object();
    boolean BLEservicesRunning = false;
    final setDescriptorThread q = new setDescriptorThread();
    initialReadThread init = new initialReadThread();
    List<BLE_Characteristic> allBLE = new ArrayList<>();


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("ScanLeDevice", "Scanning...");
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.i("Address", btDevice.getAddress());
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
            deviceConnected =true;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //status of 0 if operation succeeds
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    android.os.SystemClock.sleep(600);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt.close();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            int[] acceptable_services = getResources().getIntArray(R.array.Services);
            int[] notification_characteristics = getResources().getIntArray(R.array.basicNotification);
            int[] write_characteristics = getResources().getIntArray(R.array.basicWrite);
            for (BluetoothGattService service : services) {
                if (ArrayUtils.contains(acceptable_services, BLE_Characteristic.getAssignedNumber(service.getUuid()))) {
                    Log.i("Service found", service.getUuid().toString());
                    allBLE.clear();
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    List<BluetoothGattDescriptor> Descr;
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Integer shorthandChar = BLE_Characteristic.getAssignedNumber(characteristic.getUuid());
                        if (ArrayUtils.contains(notification_characteristics, shorthandChar)) {
                            allBLE.add(new BLE_Characteristic(shorthandChar, characteristic));
                            Log.i("Characteristic found", characteristic.getUuid().toString());

                            Descr = characteristic.getDescriptors();

                            for (BluetoothGattDescriptor d : Descr) {
                                boolean set = d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                String result = String.valueOf(set);
                                Log.i("DidItSet", result);
                                if (descriptorSetQueue.isEmpty()) {
                                    descriptorSetQueue.add(d);
                                    synchronized (q) {
                                        q.notify();
                                    }
                                } else {
                                    descriptorSetQueue.add(d);
                                }

                            }

                            mGatt.setCharacteristicNotification(characteristic, true);

                        } else if (ArrayUtils.contains(write_characteristics, shorthandChar)) {
                            allBLE.add(new BLE_Characteristic(shorthandChar, characteristic));
                            Log.i("Read Characteristic:", characteristic.getUuid().toString());
                        }
                    }
                    new Thread(init).start();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicReadV", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString());
            //Log.i("onCharacteristicReadP1", Integer.toString(characteristic.getPermissions()));
            //Log.i("onCharacteristicReadP2", Integer.toString(characteristic.getProperties()));
            Log.i("onCharacteristicReadS", Integer.toString(status));
            //gatt.disconnect();
            updateCharacteristic((characteristic));
            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            if(descriptorSetQueue.isEmpty()) {
                updateCharacteristic(characteristic);
            }
            else {
                Log.i("Characteristic changed", "Waiting for all descriptors to be set");
            }
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("Written", Integer.toHexString(BLE_Characteristic.getAssignedNumber(characteristic.getUuid())));
            Log.i("WrittenStatus", Integer.toString(status));

            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status) { }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BLEservicesRunning = false;
            synchronized (serviceLock) {
                serviceLock.notify();
            }
        }
    };

    private void updateCharacteristic(BluetoothGattCharacteristic characteristic){
        Integer char_id = BLE_Characteristic.getAssignedNumber(characteristic.getUuid());
        String updateString = null;
        if (char_id >= 0xF000) {
            byte[] bytes = characteristic.getValue();
            ByteBuffer so1 = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4)).order(ByteOrder.LITTLE_ENDIAN);
            Float so1a = so1.getFloat();
            updateString = so1a.toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Float Method", so1a.toString());
        } else if ((char_id < 0xF000) & (char_id >= 0xE000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 8", updateString);
        } else if ((char_id < 0xE000) & (char_id >= 0xD000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 16", updateString);
        } else if ((char_id < 0xD000) & (char_id >= 0xC000)) {
            updateString = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).toString();
            Log.i("Received characteristic", Integer.toHexString(char_id));
            Log.i("Int 32", updateString);
        }else {
            Log.i("Unknown characteristic", Integer.toHexString(char_id));
        }
        if (updateString != null) {
            Runnable t = new updateViews(char_id, updateString);
            t.run();
        }
    }


    private class setDescriptorThread implements Runnable{
        setDescriptorThread(){ }
        public void run() {
            Log.i("Descriptor Thread", "Thread Started");
            while(!Thread.currentThread().isInterrupted()){
                while (descriptorSetQueue.isEmpty())
                    try{
                        Log.i("Descriptor Thread", "Queue has no objects, will wait");
                        synchronized (initWriteLock){
                            initWriteLock.notify();
                        }
                        synchronized (q) {
                            q.wait();
                        }
                    } catch (InterruptedException iex) {
                        Log.i("Descriptor Thread: ", iex.getMessage());
                    }
                Log.i("Descriptor Thread", "Must be something in queue");
                while (BLEservicesRunning){
                    try{
                        Log.i("Descriptor Thread", "BLE Services being used, will wait");
                        synchronized (serviceLock) {
                            serviceLock.wait();
                        }
                    } catch (InterruptedException iex) {
                        Log.i("Descriptor Thread: ", iex.getMessage());
                    }
                }
                BluetoothGattDescriptor nextToWrite = descriptorSetQueue.peek();
                BLEservicesRunning = true;
                boolean write = mGatt.writeDescriptor(nextToWrite);
                String resultW = String.valueOf(write);
                Log.i("DidItWrite", resultW);
                if (write){
                    descriptorSetQueue.remove(nextToWrite);
                }
                Log.i("Queue size", Integer.toString(descriptorSetQueue.size()));
            }
        }
    }

    private class updateViews implements Runnable {
        Integer charID;
        String stringToSet;
        private updateViews(Integer char_ID, String stringToSet) {
            this.charID = char_ID;
            this.stringToSet = stringToSet;
        }

        public void run() {
            if (charID == 0xF000){
                Log.i("Pressure is", stringToSet);
                int pressure = Integer.parseInt(stringToSet);
                sendLocalBroadcastAndSave("CURRENT_PRESSURE", "PRESSURE", pressure);
            }
            else if (charID == 0xE001){
                Log.i("Fullness is", stringToSet);
                int beer = Integer.parseInt(stringToSet);
                sendLocalBroadcastAndSave("REFERENCE_PRESSURE", "PRESSURE", beer);
            }

            else if (charID == 0xE003){
                Log.i("Keg Size is", stringToSet);
                if (stringToSet.equals("1")) {
                    sendLocalBroadcastAndSave("KEG_SIZE","SIZE","FULL");
                }
                else if (stringToSet.equals("0")){
                    sendLocalBroadcastAndSave("KEG_SIZE","SIZE","MINI");
                }
                else {
                    Log.i("Error: Keg Size is", stringToSet);
                }
            }

            else if (charID == 0xF001){
                Log.i("Ref Pressure is", stringToSet);
                int ref_pressure = Integer.parseInt(stringToSet);
                sendLocalBroadcastAndSave("REFERENCE_PRESSURE", "PRESSURE", ref_pressure);
            }
            else if (charID == 0xF00B){
                Log.i("Battery is", stringToSet);
                Float battery_voltage = Float.parseFloat(stringToSet);
                Float adjusted = battery_voltage - MIN_VOLTAGE;
                Float range = MAX_VOLTAGE - MIN_VOLTAGE;
                Float percentage = adjusted / range;
                sendLocalBroadcastAndSave("SET_BATTERY","BATTERY", Math.round(percentage*100));
            }
        }
    }

    private class initialReadThread implements Runnable{

        initialReadThread(){

        }
        public void run() {
            Log.i("Initial Read Thread", "Thread Started");
            while (!descriptorSetQueue.isEmpty())
                try{
                    Log.i("Initial Read Thread", "Queue has objects, will wait");
                    synchronized (initWriteLock) {
                        initWriteLock.wait();
                    }
                } catch (InterruptedException iex) {
                    Log.i("Initial Write Thread: ", iex.getMessage());
                }
            Log.i("Initial Write Thread", "Queue must be empty");
            int[] write_characteristics = getResources().getIntArray(R.array.basicWrite);
            for (int temp_char:write_characteristics){
                for (BLE_Characteristic temp_collection:allBLE){
                    if(temp_char == temp_collection.BLE_Short){
                        while (BLEservicesRunning){
                            try{
                                Log.i("Initial Write Thread", "BLE Services being used, will wait");
                                synchronized (serviceLock) {
                                    serviceLock.wait();
                                }
                            } catch (InterruptedException iex) {
                                Log.i("Initial Write Thread: ", iex.getMessage());
                            }
                        }
                        Log.i("Match of", Integer.toHexString(temp_collection.BLE_Short));
                        BLEservicesRunning = true;
                        Boolean resp = mGatt.readCharacteristic(temp_collection.BLE_Full);
                        Log.i("Successful?", resp.toString());
                        break;
                    }
                }
            }

            Log.i("Initial Write Thread", "Thread Done");

        }
    }








    //-----------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();

        new Thread(q).start();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BluetoothLE Service", "Service Started");
        LocalBroadcastManager.getInstance(this).registerReceiver(onBroadcastReceive, new IntentFilter("SmartKeg"));

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        ScanFilter scanFilter = new ScanFilter.Builder()
                        .setDeviceName(getApplicationContext().getString(R.string.device_name))
                        .build();
        filters = new ArrayList<>();
        filters.add(scanFilter);
        Log.i("onResume", "We've got our filters");

        scanLeDevice(true);

        //return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    private BroadcastReceiver onBroadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("BLE Service", "Broadcast received");
            String action = intent.getAction();
            if (action != null)
                switch (action) {
                    case "SET_PRESSURE":
                        int pressure = intent.getIntExtra("PRESSURE",1600);
                        sendReferencePressure(pressure/100f);
                        break;
                    case "KEG_SIZE":
                        String size = intent.getStringExtra("SIZE");
                        sendKegSize(size);
                }
        }
    };

    @Override
    public void onDestroy() {
        Log.i("BluetoothLE Service", "Service Ended");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onBroadcastReceive);
        if (deviceConnected) {
            mGatt.close();
        }
        super.onDestroy();
    }

    private void sendLocalBroadcastAndSave (String action, String extraName, String extraData) {
        Intent i = new Intent(action);
        i.putExtra(extraName, extraData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        SharedPreferences mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putString(action+"-"+extraName, extraData);
        editor.apply();
    }
    private void sendLocalBroadcastAndSave (String action, String extraName, int extraData) {
        Intent i = new Intent(action);
        i.putExtra(extraName, extraData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        SharedPreferences mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt(action+"-"+extraName, extraData);
        editor.apply();
    }
    private void sendLocalBroadcastAndSave (String action, String extraName, boolean extraData) {
        Intent i = new Intent(action);
        i.putExtra(extraName, extraData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        SharedPreferences mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putBoolean(action+"-"+extraName, extraData);
        editor.apply();
    }

    private void sendKegSize (String size) {

        int value = 0;
        if (size.equals("FULL")){
            value = 1;
        }
        else if (size.equals("MINI")) {
            value = 0;
        }

        BluetoothGattCharacteristic toWriteTo = null;

        for (BLE_Characteristic temp : allBLE) {
            if (temp.BLE_Short == 0xE003) {
                toWriteTo = temp.BLE_Full;
                break;
            }
        }

        if (toWriteTo != null) {
            toWriteTo.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);

            String total_string = Integer.toString(value);

            Log.i("OnClickSize", "Sending " + total_string);
            BLEservicesRunning = true;
            boolean write = mGatt.writeCharacteristic(toWriteTo);
            String result = String.valueOf(write);
            Log.i("DidItSend", result);
            while (BLEservicesRunning) {
                try {
                    Log.i("Write Thread", "BLE Services being used, will wait");
                    synchronized (serviceLock) {
                        serviceLock.wait();
                    }
                } catch (InterruptedException iex) {
                    Log.i("Write Thread: ", iex.getMessage());
                }
            }
            mGatt.readCharacteristic(toWriteTo);
        }
        else {
            Log.i("onClickSend", "No char found!");
        }
    }
    private void sendReferencePressure (float pressure) {

        BluetoothGattCharacteristic toWriteTo = null;

        for (BLE_Characteristic temp : allBLE) {
            if (temp.BLE_Short == 0xF001) {
                toWriteTo = temp.BLE_Full;
                break;
            }
        }

        if (toWriteTo != null) {
            int bits = Float.floatToIntBits(pressure);
            toWriteTo.setValue(bits, BluetoothGattCharacteristic.FORMAT_UINT32, 0);

            Log.i("OnClickSend", "Sending " + pressure);
            BLEservicesRunning = true;
            boolean write = mGatt.writeCharacteristic(toWriteTo);
            String result = String.valueOf(write);
            Log.i("DidItSend", result);
            while (BLEservicesRunning) {
                try {
                    Log.i("Write Thread", "BLE Services being used, will wait");
                    synchronized (serviceLock) {
                        serviceLock.wait();
                    }
                } catch (InterruptedException iex) {
                    Log.i("Write Thread: ", iex.getMessage());
                }
            }
            mGatt.readCharacteristic(toWriteTo);
        }
        else {
            Log.i("onClickSend", "No char found!");
        }
    }

    //TODO send notifications

}


