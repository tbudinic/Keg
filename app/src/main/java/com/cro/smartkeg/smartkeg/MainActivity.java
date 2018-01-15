package com.cro.smartkeg.smartkeg;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    Switch battRechargeSwitch = null;
    SeekBar battSeekbar = null;
    Switch kegSize = null;
    SharedPreferences mySharedPreferences;
    int battLevel = 100;
    boolean battRecharge = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //define and set toolbar
        Toolbar myToolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(myToolbar);

        mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE); //get the shared preference to load and save data

        ImageView bluetoothSignalImage = findViewById(R.id.imageViewBluetoothSignal);
        bluetoothSignalImage.setImageResource(R.drawable.bluetooth_wireless_connecting);
        AnimationDrawable frameAnimation = (AnimationDrawable) bluetoothSignalImage.getDrawable();     //set battery critical animation
        frameAnimation.start();

        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_keg, null))
                .setTitle("Select Keg Size")
                .setMessage("Which size keg do you have?");
        AlertDialog dialog = builder.create();
        dialog.show();*/

        //define seekbars
        battSeekbar = findViewById(R.id.seekbarBattery);

        //seekbar listener
        SeekBar.OnSeekBarChangeListener osbcl = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (seekBar.getId()){
                    case R.id.seekbarBattery:
                        setBatteryLevel(progress, battRecharge);
                        battLevel = progress;
                        break;
                }

            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        // set seekbars to listen
        battSeekbar.setOnSeekBarChangeListener(osbcl);

        //define switches
        battRechargeSwitch = findViewById(R.id.switchBatteryCharging);

        //switch listener
        CompoundButton.OnCheckedChangeListener soccl = new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setBatteryLevel(battLevel, true);
                    battRecharge = true;
                }
                else {
                    setBatteryLevel(battLevel, false);
                    battRecharge = true;
                }
            }

        };

        //set switches to listen
        battRechargeSwitch.setOnCheckedChangeListener(soccl);
        //kegSize.setOnCheckedChangeListener(soccl);

        //bluetooth
        /*ImageView blImage = findViewById(R.id.imageViewKegBluetooth);
        blImage.setVisibility(View.VISIBLE);
        blImage.setOnClickListener(new ImageView.OnClickListener(){
          public void onClick (View v) {
              switch (v.getId()) {
                  case R.id.imageViewKegBluetooth:
                      // TESTING ONLY: Bluetooth not available on virtual device
                      if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                          Toast.makeText(getApplicationContext(), "TBluetooth LE not supported", Toast.LENGTH_SHORT).show();
                          startActivity(new Intent(Settings.ACTION_SETTINGS));
                      }
                      else {
                          startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                      }
                      break;
              }
          }
        });*/



    }



    @Override
    protected void onResume() {
        super.onResume();

        //SharedPreferences data = getSharedPreferences("preference_key_storage_settings", MODE_PRIVATE);
        //int battlevel = data.getInt("batteryLevel",0);
        //boolean battcharging = data.getBoolean("batteryRecharging", false);
        //setBatteryLevel(battlevel,battcharging);

        //setKegLevel(level);
    }



    //called to set battery/recharging level images
    public void setBatteryLevel (int batt, boolean c) {
        ImageView battImage = findViewById(R.id.imageViewBattery);  //define battery level image

        int[] batteryLevelValues = {100, 75, 50, 25, getResources().getInteger(R.integer.battery_critical)};     //define array of battery levels
        int[] batteryLevelIds = {R.drawable.battery100, R.drawable.battery075, R.drawable.battery050, R.drawable.battery025, R.drawable.batterycriticalblinking}; //define array of corresponding battery images

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

        if (c && batt == 100) {
            if (battReImage.getTag() != R.drawable.batterycharged +"") {
                battReImage.setImageResource(R.drawable.batterycharged);
                if (battReImage.getVisibility() != View.VISIBLE)
                    battReImage.setVisibility(View.VISIBLE);
                battReImage.setTag(R.drawable.batterycharged +"");
            }
        }
        else if (c && batt < 100) {
            if (battReImage.getTag() != R.drawable.batterycharging +"") {
                battReImage.setImageResource(R.drawable.batterycharging);
                if (battReImage.getVisibility() != View.VISIBLE)
                    battReImage.setVisibility(View.VISIBLE);
                battReImage.setTag(R.drawable.batterycharging +"");
            }
        }
        else
            if (battReImage.getTag() != "") {
                if (battReImage.getVisibility() != View.GONE)
                    battReImage.setVisibility(View.GONE);
                battReImage.setTag("");
            }


        //Send notification if battery is low or fully recharged
        /*String mNotificationId = "001";
                NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, mNotificationId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(1, mBuilder.build());
        */

        /*Works but not ready
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel("ID", "Name", importance);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext(),"ID");
        }

        builder = builder
                .setSmallIcon(R.drawable.gauge)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle("Title")
                .setTicker("Ticker")
                .setContentText("Battery")
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);
        notificationManager.notify(0, builder.build()); */



    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    //Called to create toolbar overflow menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater myMenuInflater =  getMenuInflater();
        myMenuInflater.inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    //Called when a toolbar item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pressure:
                startActivity(new Intent(this, PressureActivity.class));
                return true;

            case R.id.action_beerfacts:

                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, MyPreferencesActivity.class));
                return true;

            case R.id.action_faq:

                return true;

            case R.id.action_about:

                return true;

            case R.id.action_tap_simulation:
                startActivity(new Intent(this, TapSimPreferencesActivity.class));
                return true;

            default:

                return super.onOptionsItemSelected(item);

        }
    }

}
