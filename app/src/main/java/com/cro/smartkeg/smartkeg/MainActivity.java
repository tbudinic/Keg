package com.cro.smartkeg.smartkeg;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //private Switch battRechargeSwitch,
    //               kegSize;
    //private SeekBar battSeekbar,
    //                beerSeekbar;
    private SharedPreferences mySharedPreferences,
                              myPreferences;
    private int battLevel,
                beerLevel,
                remeasuretime;
    private boolean battRecharge,
                    wasBluetoothOn = false,
                    isBluetoothOK = false,
                    enableBTAlreadyShown = false,
                    currentlyMeasuring = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private ProgressBar measuringKegProgressBar;
    //private Button newKegButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //See I took out all the dumb comments
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("Permission Bluetooth", ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)+"");
        Log.i("Permission Admin", ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)+"");
        Log.i("Permission Coarse", ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)+"");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3); //TODO check if not already granted, follow tutorial at https://developer.android.com/training/permissions/requesting.html#java

        //define and set toolbar
        Toolbar myToolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(myToolbar);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE); //get the shared preference to load and save data
        currentlyMeasuring = mySharedPreferences.getBoolean("CURRENTLY_MEASURING", false);

        measuringKegProgressBar = findViewById(R.id.progressBarKeg);

        ImageView kegImage = findViewById(R.id.imageViewKeg);
        kegImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //if (!currentlyMeasuring) //TODO enable in final version
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Remeasure Beer Level")
                            .setMessage("Are you sure you want to remeasure the keg? It was last measured " + remeasuretime + "\n\nThis will take 30 seconds.\n\nDo not pour a drink while measuring.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent i = new Intent("RemeasureLevel");
                                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                                    measuringKegProgressBar.setVisibility(View.VISIBLE);
                                    currentlyMeasuring = true;
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    measuringKegProgressBar.setVisibility(View.GONE); //TODO remove in final version
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                //}
            }
        });

        temp();
    }

    //TODO Remove for final version
    private void temp() {
        //define seekbars
        SeekBar battSeekbar = findViewById(R.id.seekbarBattery);
        SeekBar beerSeekbar = findViewById(R.id.seekbarBeer);

        //seekbar listener
        SeekBar.OnSeekBarChangeListener osbcl = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (seekBar.getId()) {
                    case R.id.seekbarBattery:
                        setBatteryLevel(progress);
                        break;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) {
                switch (seekBar.getId()) {
                    case R.id.seekbarBeer:
                        setBeerLevel(seekBar.getProgress()*25);
                        break;
                }
            }
        };

        // set seekbars to listen
        battSeekbar.setOnSeekBarChangeListener(osbcl);
        beerSeekbar.setOnSeekBarChangeListener(osbcl);

        //define switches
        Switch battRechargeSwitch = findViewById(R.id.switchBatteryCharging);

        //switch listener
        CompoundButton.OnCheckedChangeListener soccl = new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    setBatteryRecharging(true);
                else
                    setBatteryRecharging(false);
            }
        };

        //set switches to listen
        battRechargeSwitch.setOnCheckedChangeListener(soccl);
        //kegSize.setOnCheckedChangeListener(soccl);

        Button newKegButton = findViewById(R.id.buttonNewKeg);
        newKegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newKegDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i("Main Activity", "onResume started");

        IntentFilter myIntentFilter = new IntentFilter("BATTERY_LEVEL");
        myIntentFilter.addAction("BEER_LEVEL");
        myIntentFilter.addAction("KEG_SIZE");

        LocalBroadcastManager.getInstance(this).registerReceiver(onBroadcastReceive, myIntentFilter);
        registerReceiver(onBroadcastReceive, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(onBroadcastReceive, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));

        SharedPreferences myPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean autoBluetoothOn = myPreferences.getBoolean("preference_key_bluetooth_on", false);
        if ((autoBluetoothOn && !bluetoothAdapter.isEnabled()) || (bluetoothAdapter.isEnabled() && isPaired())) {
            Log.i("Main Activity", "starting service");
            isBluetoothOK = true;
            bluetoothAdapter.enable();
            if (!isMyServiceRunning(MainService.class))
                startService(new Intent(MainActivity.this, MainService.class));
        }
        else if (!autoBluetoothOn && !bluetoothAdapter.isEnabled() && !enableBTAlreadyShown) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        //Intent grantPermission = new Intent(Permission.ACCESS_COARSE_LOCATION);
        //startActivityForResult(grantPermission, 2);


        //SharedPreferences data = getSharedPreferences("preference_key_storage_settings", MODE_PRIVATE);
        //int battlevel = data.getInt("batteryLevel",0);
        //boolean battcharging = data.getBoolean("batteryRecharging", false);
        battLevel = mySharedPreferences.getInt("BATTERY_LEVEL-LEVEL", 100);
        battRecharge = mySharedPreferences.getBoolean("BATTERY_RECHARGE", false);
        setBatteryImage(battLevel,battRecharge);

        beerLevel = mySharedPreferences.getInt("BEER_LEVEL-LEVEL", 100);
        setBeerLevel(beerLevel);

        if (!bluetoothAdapter.isEnabled())
            setBluetoothImage("OFF");
        else if (isKegConnected())
            setBluetoothImage("CONNECTED");
        else
            setBluetoothImage("SEARCHING");

        setKegImage(mySharedPreferences.getString("KEG_SIZE","BIG"));

        //TODO remove in final version
        SeekBar battSeekbar = findViewById(R.id.seekbarBattery);
        battSeekbar.setProgress(battLevel);
        Switch battRechargeSwitch = findViewById(R.id.switchBatteryCharging);
        battRechargeSwitch.setChecked(battRecharge);

        Log.i("Main Activity", "onResume finished");

    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onBroadcastReceive);
    }


    /*onStop save the values so the can be recalled in onResume*/
    @Override
    protected void onStop() {
        super.onStop();

        //TODO DO NOT SAVE VALUES, WILL OVERRIDE SERVICES VALUES
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt("BATTERY_LEVEL-LEVEL", battLevel);
        editor.putBoolean("BATTERY_RECHARGE", battRecharge);
        editor.putInt("BEER_LEVEL-LEVEL", beerLevel);
        editor.putBoolean("CURRENTLY_MEASURING",currentlyMeasuring);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, MainService.class));
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1)
            if (resultCode == RESULT_OK) {
                isBluetoothOK = true;
            }
            else {
                isBluetoothOK = false;
                enableBTAlreadyShown = true;
            }
    }

    BroadcastReceiver onBroadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Main Activity", "Broadcast received");
            String action = intent.getAction();
            Log.i("Action is", action);
            if (action != null)
                switch (action) {
                    case "BATTERY_LEVEL":
                        Log.i("Battery is", intent.getIntExtra("BATTERY", 100)+"");
                        setBatteryLevel(intent.getIntExtra("BATTERY", 100)); //TODO parse int...
                        break;

                    case "SetBatteryRechargeState":
                        setBatteryRecharging(intent.getBooleanExtra("BatteryRecharging", false));
                        break;

                    case "BEER_LEVEL":
                        if (measuringKegProgressBar.getVisibility() == View.VISIBLE)
                            measuringKegProgressBar.setVisibility(View.GONE);
                            setBeerLevel(intent.getIntExtra("LEVEL", 100));
                        break;

                    case "MeasuringLevel":
                        measuringKegProgressBar.setVisibility(View.GONE);
                        currentlyMeasuring = false;
                        break;

                    case "NEW_KEG":
                        newKegDialog();
                        break;

                    case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                        int connectionstate = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);

                        if (connectionstate == BluetoothAdapter.STATE_CONNECTED && isKegConnected()) {
                            setBluetoothImage("CONNECTED");
                            Log.i("MainActivity", "Bluetooth Connected");
                        }
                        else if (connectionstate == BluetoothAdapter.STATE_DISCONNECTED && !isKegConnected() && bluetoothAdapter.isEnabled()) {
                            setBluetoothImage("SEARCHING");
                            Log.i("MainActivity", "Bluetooth Disconnected");
                        }
                        break;

                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                        switch (state) {
                            case BluetoothAdapter.STATE_ON:
                                setBluetoothImage("SEARCHING");
                                if (!isPaired())
                                    showPairingHelperDialog();
                                else if (!isMyServiceRunning(MainService.class))
                                    startService(new Intent(MainActivity.this, MainService.class));
                                Log.i("MainActivity", "Bluetooth On");
                                break;
                            case BluetoothAdapter.STATE_OFF:
                                setBluetoothImage("OFF");
                                Log.i("MainActivity", "Bluetooth Off");
                                break;
                        }
                    break;
                }
        }
    };

    private boolean isKegConnected () {
        boolean connected = false;
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (devices != null)
            for(BluetoothDevice device : devices)
                if(device.getName().equals(getResources().getString(R.string.device_name)))
                    connected = true;
        return connected;
    }

    // Methods for setting the battery level, state and image
    private void setBatteryLevel (int b) { setBatteryImage(b,battRecharge); }
    private void setBatteryRecharging (boolean c) { setBatteryImage(battLevel,c); }
    private void setBatteryImage (int batt, boolean c) {
        Log.i("Main Activity", "Changing Battery Image");
        ImageView battImage = findViewById(R.id.imageViewBattery);  //define battery level image

        int[] batteryLevelValues = {100, 75, 50, 25, 10};     //define array of battery levels
        int[] batteryLevelIds = {R.drawable.battery100_2, R.drawable.battery075_2, R.drawable.battery050_2, R.drawable.battery025_2, R.drawable.batterycriticalblinking,};    //define array of corresponding battery images

        int b = 0;
        for (int i = 0; i < batteryLevelIds.length; i++)      //iterate through the array to set the correct image
            if (batt <= batteryLevelValues[i])
                b = i;

        if (battImage.getTag() != batteryLevelValues[b] + "") {     //display battery image if it has changed
            battImage.setImageResource(batteryLevelIds[b]);     //display the image

            if (batteryLevelValues[b] == getResources().getInteger(R.integer.battery_critical)) {   //check if battery is critical
                AnimationDrawable frameAnimation = (AnimationDrawable) battImage.getDrawable();     //set battery critical animation
                frameAnimation.start();     //set battery critical animation
            }
            else
                battImage.clearAnimation(); // clear animation

            battImage.setTag(batteryLevelValues[b] + "");       //set tag to remember image resource
        }

        ImageView battReImage = findViewById(R.id.imageViewBatteryCharging);
        TextView battReText = findViewById(R.id.textViewBatteryRechargingIn);
        if (c && batt == 100) {
            if (battReImage.getTag() != R.drawable.batterycharged +"") {
                battReImage.setImageResource(R.drawable.batterycharged);
                if (battReImage.getVisibility() != View.VISIBLE)
                    battReImage.setVisibility(View.VISIBLE);
                battReImage.setTag(R.drawable.batterycharged +"");
                if (battReText.getVisibility() != View.VISIBLE)
                    battReText.setVisibility(View.VISIBLE);
            }
        }
        else if (c && batt < 100) {
            if (battReImage.getTag() != R.drawable.batterycharging +"") {
                battReImage.setImageResource(R.drawable.batterycharging);
                if (battReImage.getVisibility() != View.VISIBLE)
                    battReImage.setVisibility(View.VISIBLE);
                battReImage.setTag(R.drawable.batterycharging +"");
                if (battReText.getVisibility() != View.VISIBLE)
                    battReText.setVisibility(View.VISIBLE);
            }
        }
        else {
            if (battReImage.getTag() != "") {
                if (battReImage.getVisibility() != View.GONE)
                    battReImage.setVisibility(View.GONE);
                battReImage.setTag("");
                if (battReText.getVisibility() != View.GONE)
                    battReText.setVisibility(View.GONE);
            }
        }

        battLevel = batt;
        battRecharge = c;
    }

    private void setBeerLevel (int level) {
        if (level != beerLevel) {
            //Set top margin in the layout file
            final int bottomMargin = 90; //in dp

            ImageView kegImage = findViewById(R.id.imageViewKeg);
            int heightpx = kegImage.getHeight();
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int heightdp = Math.round(heightpx / (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));

            int translationYdp = heightdp - bottomMargin;

            int translationYpx = Math.round(translationYdp * (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));

            ImageView beerImage = findViewById(R.id.imageViewKegBeer);
            beerImage.clearAnimation();
            ObjectAnimator animation = ObjectAnimator.ofFloat(beerImage, "translationY", (100-level)/100f * translationYpx);
            animation.setDuration(Math.abs(beerLevel - level) * 100);
            animation.start();
            beerLevel = level;
        }
    }

    private void newKegDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_keg, null))
                .setTitle("Select Keg Size")
                .setMessage("Which size keg do you have?");
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        ImageView BigKeg = dialog.findViewById(R.id.imageViewKegFull);
        ImageView SmallKeg = dialog.findViewById(R.id.imageViewKegMini);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences.Editor editor = mySharedPreferences.edit();
                switch (view.getId()) {
                    case R.id.imageViewKegMini:
                        editor.putString("KEG_SIZE", "SMALL");
                        setKegImage("SMALL");
                        break;
                    case R.id.imageViewKegFull:
                        editor.putString("KEG_SIZE", "BIG");
                        setKegImage("BIG");
                        break;
                }
                editor.apply();
                dialog.dismiss();
            }
        };

        BigKeg.setOnClickListener(click);
        SmallKeg.setOnClickListener(click);
    }

    private void setKegImage (String size) {
        ImageView keg = findViewById(R.id.imageViewKeg);
        switch (size) {
            case "SMALL":
                keg.setImageResource(R.drawable.kegminisize_outline);
                break;
            case "BIG":
                keg.setImageResource(R.drawable.kegfullsize_outline);
                break;
        }
    }

    private boolean isPaired() {
        boolean bonded = false;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices != null)
            for (BluetoothDevice device : bondedDevices)
                if (device.getName().equals(getResources().getString(R.string.device_name)))
                    bonded = true;
        return bonded;
    }

    private void showPairingHelperDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SmartKeg Not Paired")
               .setMessage("You need to pair with the SmartKeg in order to use this app.\n\nPress continue to go to your device settings and select the SmartKeg.\n\nHint: The password is 1111.")
               .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), -1);
                    }
               })
               .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                   }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void doBluetoothStuff() {


        /*if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }*/


        /*List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (devices != null)
            for(BluetoothDevice device : devices)
                if(device.getType() == BluetoothDevice.DEVICE_TYPE_LE && device.getName().equals("SmartKeg"))
                    true;*/

        //bluetooth
        /*ImageView blImage = findViewById(R.id.imageViewKegBluetooth);
        blImage.setVisibility(View.VISIBLE);
        blImage.setOnClickListener(new ImageView.OnClickListener(){
          public void onClick (View v) {
              switch (v.getId()) {
                  case R.id.imageViewKegBluetooth:
                      // TESTING ONLY: Bluetooth not available on virtual device
                      if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                          Toast.makeText(MainActivity.this, "TBluetooth LE not supported", Toast.LENGTH_SHORT).show();
                          startActivity(new Intent(Settings.ACTION_SETTINGS));
                      }
                      else {
                          startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                      }
                      break;
              }
          }
        });*/

                /*BluetoothAdapter myBluetoothAdapter;
        final BluetoothManager myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();
        if (myBluetoothAdapter == null || !myBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }*/

    }

    private void setBluetoothImage(String s) {
        ImageView bluetoothSignalImage = findViewById(R.id.imageViewBluetoothSignal);
        TextView bluetoothText = findViewById(R.id.textViewBluetoothStatus);
        switch (s) {
            case "SEARCHING":
                bluetoothSignalImage.setImageResource(R.drawable.bluetooth_wireless_connecting);
                AnimationDrawable frameAnimation = (AnimationDrawable) bluetoothSignalImage.getDrawable();
                frameAnimation.start();
                bluetoothText.setText("Searching");
                break;
            case "CONNECTED":
                bluetoothSignalImage.clearAnimation();
                bluetoothSignalImage.setImageResource(R.drawable.wireless_icon_ok);
                bluetoothText.setText("Connected");
                break;
            case "OFF":
                bluetoothSignalImage.clearAnimation();
                bluetoothSignalImage.setImageResource(R.drawable.wireless_icon_off);
                bluetoothText.setText("Disabled");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        showOnExitDialog(true);
    }

    private void showOnExitDialog (final boolean back) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit?")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        stopService(new Intent(MainActivity.this, MainService.class));
                        if (back)
                            MainActivity.super.onBackPressed();
                        else
                            finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Called to create toolbar overflow menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater myMenuInflater = getMenuInflater();
        myMenuInflater.inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    //Called when a toolbar item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pressure:
                //if (isKegConnected()) TODO renable in final version
                    startActivity(new Intent(this, PressureActivity.class));
                //else
                    //Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_beerfacts:
                beerFacts();
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;

            case R.id.action_faq:
                //TODO go to faq activity
                Toast.makeText(this, "to FAQ activity", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_about:
                //TODO go to about activity
                Toast.makeText(this, "to About activity", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_exit:
                showOnExitDialog(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void beerFacts() {
        final String[] facts = getResources().getStringArray(R.array.beerFacts);
        final Random r = new Random();
        int i = r.nextInt(facts.length-1);

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Did you know?...")
                .setMessage(facts[i])
                .setPositiveButton("Next", null)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.setMessage(facts[r.nextInt(facts.length-1)]);
                    }
                });
            }
        });
        dialog.show();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
