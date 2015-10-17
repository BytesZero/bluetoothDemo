package com.zsl.bluetoothdemo;

import android.bluetooth.BluetoothDevice;

import com.zsl.bluetoothdemo.utils.ble.ParsedAd;

/**
 * Created by zsl on 15/9/23.
 * 自定义的BluetoothDevice，包含了解析后的广播报文信息
 */
public class MyBluetoothDevice {

    private BluetoothDevice bluetoothDevice;
    private ParsedAd ParsedAd;

    public MyBluetoothDevice() {

    }

    public MyBluetoothDevice(BluetoothDevice bluetoothDevice, com.zsl.bluetoothdemo.utils.ble.ParsedAd parsedAd) {
        this.bluetoothDevice = bluetoothDevice;
        ParsedAd = parsedAd;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public com.zsl.bluetoothdemo.utils.ble.ParsedAd getParsedAd() {
        return ParsedAd;
    }

    public void setParsedAd(com.zsl.bluetoothdemo.utils.ble.ParsedAd parsedAd) {
        ParsedAd = parsedAd;
    }

    @Override
    public String toString() {
        return "MyBluetoothDevice{" +
                "bluetoothDevice=" + bluetoothDevice +
                ", ParsedAd=" + ParsedAd +
                '}';
    }
}