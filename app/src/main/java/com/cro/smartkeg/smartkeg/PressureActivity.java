package com.cro.smartkeg.smartkeg;

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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PressureActivity extends AppCompatActivity {

    AlertDialog dialog;
    EditText myEditText;
    TextView tv;
    int previousID, customMin, customMax, customPressure, currentPressure, referencePressure;
    SharedPreferences mySharedPreferences;
    SeekBar sb;

    //TODO pressure image, array modularity of radio buttons

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure);

        Toolbar myToolbar = findViewById(R.id.toolbarBasic);
        setSupportActionBar(myToolbar);
        ActionBar myActionBar = getSupportActionBar(); // Get a support ActionBar corresponding to this toolbar
        if (myActionBar != null)
            myActionBar.setDisplayHomeAsUpEnabled(true); // Enable the Up button

        customMax = getResources().getInteger(R.integer.pressure_custom_max); //get the customMax pressure
        customMin = getResources().getInteger(R.integer.pressure_custom_min); //get the customMin pressure

        mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE); //get the shared preference to load and save data

        customPressure = mySharedPreferences.getInt("CUSTOM_PRESSURE", getResources().getInteger(R.integer.pressure_default));

        tv = findViewById(R.id.textviewPressure);
        tv.setText(customPressure /100.00+"");

        RadioButton rb = findViewById(R.id.radio_custom);
        rb.setText("Custom Pressure (" + customPressure /100.00+ " psi)");

        previousID = mySharedPreferences.getInt("PRESSURE_SELECTEDID",R.id.radio_default); //get the saved ID of the last selected radiobutton
        rb = findViewById(previousID);
        rb.setChecked(true); //check the previously selected radiobutton

        /*Build the custom pressure dialog*/
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_pressure, null))
                .setTitle("Custom Pressure")
                .setMessage("Enter desired pressure\nbetween "+ customMin/100.00 +" and "+ customMax/100.00 +" psi")
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int p = (int) Math.round(Double.parseDouble(myEditText.getText().toString())*100.00);
                        customPressure = p;
                        Intent i = new Intent("SET_PRESSURE");
                        i.putExtra("PRESSURE",p);
                        LocalBroadcastManager.getInstance(PressureActivity.this).sendBroadcast(i);
                        RadioButton rb = findViewById(R.id.radio_custom);
                        rb.setText("Custom Pressure (" + customPressure /100.00+ " psi)");
                        tv.setText(customPressure /100.00+"");
                        setReferencePressure(customPressure);
                        previousID = R.id.radio_custom;
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {   //on CANCEL, reselect the last radiobutton
                        RadioButton rb = findViewById(previousID); //get the id of the last radiobutton
                        rb.setChecked(true);    //select the last radiobutton
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {    //on dialog DISMISS, reselect the last radiobutton
                        RadioButton rb = findViewById(previousID);//get the id of the last radiobutton
                        rb.setChecked(true);    //select the last radiobutton
                    }
                });
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                myEditText = dialog.findViewById(R.id.edittextPressure);
                myEditText.setText(customPressure/100.00+"", TextView.BufferType.EDITABLE);
                myEditText.requestFocus();
                myEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {   //Listen to edittext changes
                        if (charSequence.length() != 0 && charSequence.charAt(0) != '.') {   //catch if there is no text in the edittext
                            double a = Math.round(Double.parseDouble(charSequence.toString()) * 100.00);   //convert the edittext to a double
                            if (a >= customMin && a <= customMax)   //check if the edittext value is between the customMin and customMax
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);     //if the value is ok, enable the ENTER button
                            else
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);    //if the value is not ok, disable the ENTER button
                        }
                        else
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);    //if the edittext is empty, disable the ENTER button
                    }

                    @Override
                    public void afterTextChanged(Editable editable) { }
                });
            }
        });

        temp();

    }

    private void temp () {
        sb = findViewById(R.id.seekbarPressure);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setCurrentPressure(seekBar.getProgress()+1400);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).registerReceiver(onBroadcastReceive, new IntentFilter("SmartKeg"));
    }


    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onBroadcastReceive);
        registerReceiver(onBroadcastReceive, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(onBroadcastReceive, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));

        /*if (!isKegConnected()) { //TODO Re-enable in final version
            finish();
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
        }
        else {*/

            referencePressure = mySharedPreferences.getInt("REFERENCE_PRESSURE-PRESSURE", 1600);
            currentPressure = mySharedPreferences.getInt("SET_PRESSURE-PRESSURE", 1600);
            setPressureImage(referencePressure, currentPressure);
            sb.setProgress(currentPressure-1400);
        //}



    }


    /*onStop save the values so the can be recalled in onCreate*/
    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt("PRESSURE_SELECTEDID", previousID);
        editor.putInt("CUSTOM_PRESSURE", customPressure);

        // TODO temp remove in final version
        editor.putInt("REFERENCE_PRESSURE-PRESSURE", referencePressure);
        editor.putInt("SET_PRESSURE-PRESSURE", currentPressure);

        editor.apply();
    }


    public BroadcastReceiver onBroadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null)
            switch (action) {
                case "PRESSURE":
                    String p = intent.getStringExtra("Pressure");
                    int psi = Integer.parseInt(p);
                    setCurrentPressure(psi);
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                    int connectionstate = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    if (connectionstate == BluetoothAdapter.STATE_DISCONNECTED && !isKegConnected() && bluetoothAdapter.isEnabled()) {
                        finish();
                        Log.i("PressureActivity", "Bluetooth Disconnected");
                    }
                    break;

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            finish();
                            Log.i("PressureActivity", "Bluetooth Off");
                            break;
                    }
                    break;
            }
        }
    };

    /*Called when a radiobutton is selected*/
    public void onRadioButtonClicked(View v) {
        Intent i = new Intent("SET_PRESSURE");    //create an intent to broadcast when selecting a radiobutton
        switch (v.getId()) {
            case R.id.radio_default:
                i.putExtra("PRESSURE", (double) getResources().getInteger(R.integer.pressure_default));
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                previousID = R.id.radio_default;
                tv.setText(getResources().getInteger(R.integer.pressure_default)/100.00+"");
                setReferencePressure(getResources().getInteger(R.integer.pressure_default));
                break;
            case R.id.radio_lightbeer:
                i.putExtra("PRESSURE", (double) getResources().getInteger(R.integer.pressure_light));
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                previousID = R.id.radio_lightbeer;
                tv.setText(getResources().getInteger(R.integer.pressure_light)/100.00+"");
                setReferencePressure(getResources().getInteger(R.integer.pressure_light));
                break;
            case R.id.radio_darkbeer:
                i.putExtra("PRESSURE", (double) getResources().getInteger(R.integer.pressure_dark));
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                previousID = R.id.radio_darkbeer;
                tv.setText(getResources().getInteger(R.integer.pressure_dark)/100.00+"");
                setReferencePressure(getResources().getInteger(R.integer.pressure_dark));
                break;
            case R.id.radio_custom:
                dialog.show();  //show the custom pressure dialog
                break;
        }
    }

    //TODO array for pressure settings
    /*String route[] = {"1", "3", "4"};
    for (int i = 0; i < route.length; i++)
    {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText("Route " + String.valueOf(route[i]));
        radioButton.setId(i);
        rprms = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
        rgp.addView(radioButton, rprms);
    }

    int selection = rgp.getCheckedRadioButtonId();
    for (int i = 0; i < route.length; i++)
    {
        if (i == selection)
            showToast("" + route[i]);
    }*/

    public void setReferencePressure(int r) { setPressureImage(r,currentPressure); }
    public void setCurrentPressure(int c) { setPressureImage(referencePressure,c); }
    public void setPressureImage(int reference, int current) {

        TextView psiTextView = findViewById(R.id.textviewPressureCurrent);
        psiTextView.setText(current/100.00+"");

        int degreeRotationFromCenter = 70; //For 2 psi

        float rotation = (current-reference) / 100f * (degreeRotationFromCenter/2);
        if (rotation > degreeRotationFromCenter)
            rotation = degreeRotationFromCenter;
        else if (rotation < -degreeRotationFromCenter)
            rotation = -degreeRotationFromCenter;

        ImageView needle = findViewById(R.id.imageViewPressureNeedle);
        needle.setPivotY(needle.getHeight());
        needle.setPivotX(needle.getWidth()/2f);
        needle.setRotation(rotation);

        currentPressure = current;
        referencePressure = reference;
    }

    private boolean isKegConnected () {
        boolean connected = false;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            if (devices != null)
                for (BluetoothDevice device : devices)
                    if (device.getName().equals(getResources().getString(R.string.device_name)))
                        connected = true;
        }
        return connected;
    }
}