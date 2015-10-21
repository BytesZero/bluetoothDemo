package com.zsl.bluetoothdemo.activitys;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;

import com.zsl.bluetoothdemo.R;
import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;

import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/10/21.
 * 实时数据的传输
 */
public class RealTimeDataActivity extends BaseActivity {

    private String logTag="RealTimeDataActivity";

    //实时数据的Service uuid
    public static final UUID UUID_HISTO_DATA_SERVICE = UUID
            .fromString("00001204-0000-1000-8000-00805f9b34fb");


    //BluetoothLeService
    private BluetoothLeService mLeService = null;
    //设备连接配置界面
    DeviceHomeActivity deviceHomeActivity;
    //所有的Service
    List<BluetoothGattService> serviceList;
    //实时数据的service
    BluetoothGattService realTimeDataService;
    //实时数据下service对应的所有BluetoothGattCharacteristic
    List<BluetoothGattCharacteristic> realTimeDataCharList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtimedata);

        //获得到BluetoothLeService对象
        mLeService = BluetoothLeService.getInstance();
        deviceHomeActivity = DeviceHomeActivity.getmDeviceHomeActivity();
        //获得到所有的service
        serviceList = deviceHomeActivity.getServiceList();

        if (serviceList==null)
            return;
        for (BluetoothGattService service:serviceList) {
            Log.e(logTag, service.getUuid() + "");
            if (service.getUuid().equals(UUID_HISTO_DATA_SERVICE)){
                //历史数据的service
                realTimeDataService=service;
            }
        }

        if (realTimeDataService!=null){
            realTimeDataCharList=realTimeDataService.getCharacteristics();
            Log.e(logTag,"realTimeDataCharList_count:"+realTimeDataCharList.size());
        }

    }
}
