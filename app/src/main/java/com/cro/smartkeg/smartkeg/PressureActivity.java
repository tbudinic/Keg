package com.cro.smartkeg.smartkeg;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DecimalFormat;

public class PressureActivity extends AppCompatActivity {

    double custompressure;
    AlertDialog dialog;
    EditText myEditText;
    int previousID;
    SharedPreferences mySharedPreferences;
    int max, min;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure);


        Toolbar myToolbar = findViewById(R.id.toolbarBasic);
        setSupportActionBar(myToolbar);
        ActionBar myActionBar = getSupportActionBar(); // Get a support ActionBar corresponding to this toolbar
        myActionBar.setDisplayHomeAsUpEnabled(true); // Enable the Up button

        max = getResources().getInteger(R.integer.pressure_custom_max); //get the max pressure
        min = getResources().getInteger(R.integer.pressure_custom_min); //get the min pressure

        mySharedPreferences = getSharedPreferences("SMARTKEG", MODE_PRIVATE); //get the shared preference to load and save data

        custompressure = mySharedPreferences.getFloat("CUSTOM_PRESSURE", (float) getResources().getInteger(R.integer.pressure_default)); //get the saved value of the custom pressure
        custompressure = Math.round(custompressure * 100.00) / 100.00;

        tv = findViewById(R.id.textviewPressure);
        tv.setText(custompressure+"");

        RadioButton rb = findViewById(R.id.radio_custom);
        rb.setText("Custom Pressure (" +custompressure+ " psi)");

        previousID = mySharedPreferences.getInt("PRESSURE_SELECTEDID",R.id.radio_default); //get the saved ID of the last selected radiobutton
        rb = findViewById(previousID);
        rb.setChecked(true); //check the previously selected radiobutton

        /*Build the custom pressure dialog*/
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_pressure, null))
                .setTitle("Custom Pressure")
                .setMessage("Enter desired pressure\nbetween "+min+" and "+max+" psi")
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        double p = Math.round(Double.parseDouble(myEditText.getText().toString())*100.00)/100.00;
                        custompressure = p;
                        Intent i = new Intent();
                        i.putExtra("SET_PRESSURE",p);
                        sendBroadcast(i);
                        RadioButton rb = findViewById(R.id.radio_custom);
                        rb.setText("Custom Pressure (" +custompressure+ " psi)");
                        tv.setText(custompressure+"");
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
                myEditText.setText(custompressure+"", TextView.BufferType.EDITABLE);
                myEditText.requestFocus();
                myEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {   //Listen to edittext changes
                        if (charSequence.length() != 0) {   //catch if there is no text in the edittext
                            double a = Math.round(Double.parseDouble(charSequence.toString()) * 100.00) / 100.00;   //convert the edittext to a double
                            if (a >= min && a <= max)   //check if the edittext value is between the min and max
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);     //if the value is ok, enable the ENTER button
                            else
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);    //if the value is not ok, disable the ENTER button
                        }
                        else
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //if the edittext is empty, disable the ENTER button
                    }

                    @Override
                    public void afterTextChanged(Editable editable) { }
                });
            }
        });

    }

    /*onStop save the values so the can be recalled in onCreate*/
    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putInt("PRESSURE_SELECTEDID", previousID);
        editor.putFloat("CUSTOM_PRESSURE", (float) custompressure);
        editor.apply();
    }

    /*Called when a radiobutton is selected*/
    public void onRadioButtonClicked (View v) {
        Intent i = new Intent();    //create an intent to broadcast when selecting a radiobutton
        switch (v.getId()) {
            case R.id.radio_default:
                i.putExtra("SET_PRESSURE", getResources().getInteger(R.integer.pressure_default));
                sendBroadcast(i);
                previousID = R.id.radio_default;
                tv.setText(getResources().getInteger(R.integer.pressure_default)+"");
                break;
            case R.id.radio_lightbeer:
                i.putExtra("SET_PRESSURE", getResources().getInteger(R.integer.pressure_light));
                sendBroadcast(i);
                previousID = R.id.radio_lightbeer;
                tv.setText(getResources().getInteger(R.integer.pressure_light)+"");
                break;
            case R.id.radio_darkbeer:
                i.putExtra("SET_PRESSURE", getResources().getInteger(R.integer.pressure_dark));
                sendBroadcast(i);
                previousID = R.id.radio_darkbeer;
                tv.setText(getResources().getInteger(R.integer.pressure_dark)+"");
                break;
            case R.id.radio_custom:
                dialog.show();  //show the custom pressure dialog
                break;
        }
    }
}
