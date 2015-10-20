package com.zsl.bluetoothdemo.activitys;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zsl.bluetoothdemo.R;
import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;
import com.zsl.bluetoothdemo.utils.ble.oad.Conversion;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/10/19.
 * 历史数据
 */
public class HistoryDataActivity extends BaseActivity{

    private String logTag="HistoryDataActivity";

    //历史数据的Service uuid
    public static final UUID UUID_HISTO_DATA_SERVICE = UUID
            .fromString("00001206-0000-1000-8000-00805f9b34fb");

    TextView tv_data;
    Button bt_getHeader;


    //BluetoothLeService
    private BluetoothLeService mLeService = null;
    //设备连接配置界面
    DeviceHomeActivity deviceHomeActivity;
    //所有的Service
    List<BluetoothGattService> serviceList;
    //历史数据的service
    BluetoothGattService historyDataService;
    //历史数据下service对应的所有BluetoothGattCharacteristic
    List<BluetoothGattCharacteristic> historyDataCharList;
    //NotifyCharacteristic
    BluetoothGattCharacteristic mCharNotify;
    //Block
    BluetoothGattCharacteristic mCharBlock;
    //length
    private int length;
    //是否第一次读取到需要读取的次数
    private boolean isFirstSetLength;
    //目前的总量
    private short number;
    //接收到的数据综合
    private byte[] buffer;
    //BLOCK_SIZE
    private final int BLOCK_SIZE=16;
    //crc
    short crc;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historydata);
        initView();
        initEvent();
        initData();
    }

    private void initView() {
        tv_data= (TextView) findViewById(R.id.history_tv_data);
        bt_getHeader= (Button) findViewById(R.id.history_bt_getHeader);

    }

    private void initEvent() {
        //获得Header
        bt_getHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCmd((byte) 0xa0, (byte) 0x00, (byte) 0x00);
            }
        });
    }

    /**
     * 发送命令
     * @param cmd0
     * @param cmd1
     * @param cmd2
     */
    private void sendCmd(byte cmd0,byte cmd1,byte cmd2) {
        byte[] headerValue = {cmd0,cmd1,cmd2};
        mCharNotify.setValue(headerValue);
        mLeService.writeCharacteristic(mCharNotify);
    }

    private void initData() {
        //注册广播
        registerReceiver(historyDataBroadcastReceiver,SettingIntentFilter());
        //获得到BluetoothLeService对象
        mLeService = BluetoothLeService.getInstance();

        deviceHomeActivity= DeviceHomeActivity.getmDeviceHomeActivity();
        //获得到所有的service
        serviceList=deviceHomeActivity.getServiceList();

        getHistoryDataService();
    }

    /**
     * 获得到历史数据同步的service和Characteristic
     */
    private void getHistoryDataService() {
        if (serviceList==null)
            return;
        for (BluetoothGattService service:serviceList) {
            Log.e("BluetoothGattService",service.getUuid()+"");
            if (service.getUuid().equals(UUID_HISTO_DATA_SERVICE)){
                //历史数据的service
                historyDataService=service;
            }
        }

        if (historyDataService!=null){
            historyDataCharList=historyDataService.getCharacteristics();
            if (historyDataCharList!=null&&historyDataCharList.size()==2){
                mCharNotify=historyDataCharList.get(0);
                mCharBlock=historyDataCharList.get(1);
                mLeService.setCharacteristicNotification(mCharNotify,true);
            }
        }


    }

    @Override
    protected void onDestroy() {
        //注销广播
        unregisterReceiver(historyDataBroadcastReceiver);
        super.onDestroy();
    }

    /**
     * 设置IntentFilter
     *
     * @return
     */
    private IntentFilter SettingIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }



    /**
     * Ble的广播
     */
    private BroadcastReceiver historyDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //连接断开
                tv_data.setText("连接断开");
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                tv_data.append("收到Notify");
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                Log.e(logTag,"uuidStr:"+uuidStr+":mCharNotify.getUuid():"+mCharNotify.getUuid());
                if (uuidStr.equals(mCharNotify.getUuid().toString())){
                    Log.e(logTag, value.toString());
//                  sendCmd((byte) 0xa1);
                    //read block
                    mLeService.readCharacteristic(mCharBlock);
                }

            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(context, "GATT error: status=" + status, Toast.LENGTH_SHORT).show();
                }
            }else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                if (uuidStr.equals(mCharBlock.getUuid().toString())) {
                    if (!isFirstSetLength){//读取header
                        length=Conversion.buildUint16(value[1],value[0]);
                        crc=Conversion.buildUint16(value[3],value[2]);
                        isFirstSetLength=true;
                        buffer=new byte[length*BLOCK_SIZE];
                    }else{//读取数据
                        System.arraycopy(value,0,buffer,number*BLOCK_SIZE,BLOCK_SIZE);
                        number++;
                    }
                    if (number<length) {
                        Log.e(logTag, "number:" +number);
                        sendCmd((byte) 0xa1, Conversion.loUint16(number), Conversion.hiUint16(number));
                    }else{
                        Log.e(logTag,"buffer:"+ Arrays.toString(buffer));
                        tv_data.append("接收完毕:\n");
                        //crc校验
                        if (crc==calcImageCRC(0,buffer)){
                            tv_data.append("crc校验成功:\n");
                            sendCmd((byte)0xa2,(byte)0x00,(byte)0x00);
                        }else{
                            sendCmd((byte)0xa3,(byte)0x00,(byte)0x00);
                        }
                    }
                }
            }
        }
    };


    /**
     * 获取crc
     * @param page
     * @param buf
     * @return
     */
    public short calcImageCRC(int page, byte[] buf) {
        short crc = 0;
        int len=buf.length/4;
        long addr = page * 0x1000;

        byte pageBeg = (byte) page;
        byte pageEnd = (byte) (len / (0x1000 / 4));
        int osetEnd = ((len - (pageEnd * (0x1000 / 4))) * 4);

        pageEnd += pageBeg;


        while (true) {
            int oset;

            for (oset = 0; oset < 0x1000; oset++) {
                if ((page == pageBeg) && (oset == 0x00)) {
                    oset += 3;
                } else if ((page == pageEnd) && (oset == osetEnd)) {
                    crc = this.crc16(crc, (byte) 0x00);
                    crc = this.crc16(crc, (byte) 0x00);

                    return crc;
                } else {
                    crc = this.crc16(crc, buf[(int) (addr + oset)]);
//                        Log.e(oadTag, buf[(int) (addr + oset)] + ":" + crc);
                }
            }
            page += 1;
            addr = page * 0x1000;
        }


    }

    short crc16(short crc, byte val) {
        final int poly = 0x1021;
        byte cnt;
        for (cnt = 0; cnt < 8; cnt++, val <<= 1) {
            byte msb;
            if ((crc & 0x8000) == 0x8000) {
                msb = 1;
            } else msb = 0;

            crc <<= 1;
            if ((val & 0x80) == 0x80) {
                crc |= 0x0001;
            }
            if (msb == 1) {
                crc ^= poly;
            }
        }

        return crc;
    }
}
