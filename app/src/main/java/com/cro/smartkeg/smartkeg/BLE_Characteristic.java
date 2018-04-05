package com.cro.smartkeg.smartkeg;

import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.TextView;
import java.util.UUID;

public class BLE_Characteristic {

    public int BLE_Short;
    public BluetoothGattCharacteristic BLE_Full;

    //constructor
    public BLE_Characteristic(int BLE_Short_in, BluetoothGattCharacteristic BLE_Full_in) {
        BLE_Short = BLE_Short_in;
        BLE_Full = BLE_Full_in;
    }

    public static int getAssignedNumber(UUID uuid) {
        // Keep only the significant bits of the UUID
        return (int) ((uuid.getMostSignificantBits() & 0x0000FFFF00000000L) >> 32);
    }

}