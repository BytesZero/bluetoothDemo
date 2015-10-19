package com.zsl.bluetoothdemo.activitys;

import android.bluetooth.BluetoothDevice;

import com.zsl.bluetoothdemo.utils.ble.ParsedAd;

/**
 * Created by zsl on 15/9/23.
 * 自定义的BluetoothDevice，包含了解析后的广播报文信息
 */
public class MyBluetoothDevice {

    private BluetoothDevice bluetoothDevice;
    private String address;
    private ParsedAd ParsedAd;

    public MyBluetoothDevice() {

    }

    public MyBluetoothDevice(BluetoothDevice bluetoothDevice, com.zsl.bluetoothdemo.utils.ble.ParsedAd parsedAd,String address) {
        this.bluetoothDevice = bluetoothDevice;
        ParsedAd = parsedAd;
        this.address=address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "MyBluetoothDevice{" +
                "bluetoothDevice=" + bluetoothDevice +
                ", address='" + address + '\'' +
                ", ParsedAd=" + ParsedAd +
                '}';
    }
}
