package com.zsl.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zsl.bluetoothdemo.base.BaseActivity;
import com.zsl.bluetoothdemo.utils.ble.oad.BluetoothLeService;
import com.zsl.bluetoothdemo.utils.ble.oad.Conversion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by zsl on 15/9/21.
 */
public class DeviceHomeActivity extends BaseActivity {
    //oad的Service
    public static final UUID UUID_OAD_SERVICE = UUID
            .fromString("f000ffc0-0451-4000-b000-000000000000");
    //连接控制的Service
    public static final UUID UUID_CONNCONTROL_SERVICE = UUID
            .fromString("f000ccc0-0451-4000-b000-000000000000");


    TextView tv_connect_state;
    Button bt_oad,bt_oad_stop;
    //btDevice
    BluetoothDevice bleDevice;


    /**
     * OAD
     */
    private BluetoothLeService mLeService = null;
    // BLE
    private  BluetoothGatt bleGatt;
    private BluetoothGattService mOadService;
    private BluetoothGattService mConnControlService;
    private List<BluetoothGattCharacteristic> mCharListOad;
    private List<BluetoothGattCharacteristic> mCharListCc;
    private BluetoothGattCharacteristic mCharIdentify = null;
    private BluetoothGattCharacteristic mCharBlock = null;
    private BluetoothGattCharacteristic mCharConnReq = null;

    //所有的Service
    List <BluetoothGattService> serviceList=new ArrayList<BluetoothGattService>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_home);

        initView();
        initEvent();
        initData();


    }

    private void initView() {
        tv_connect_state = (TextView) findViewById(R.id.device_home_tv_connect_state);
        bt_oad = (Button) findViewById(R.id.devices_bt_oad);
        bt_oad_stop= (Button) findViewById(R.id.devices_bt_oad_stop);
    }

    private void initEvent() {
        bt_oad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initData() {
        //注册广播
        registerReceiver(BleBroadcastReceiver, SettingIntentFilter());
        //获得到BluetoothLeService对象
        mLeService = BluetoothLeService.getInstance();
        bleDevice = getIntent().getParcelableExtra("BluetoothDevice");
        boolean isConnect=mLeService.connect(bleDevice.getAddress());
        if (isConnect){
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"连接失败",Toast.LENGTH_SHORT).show();
        }
        //获得Gatt服务
        bleGatt = BluetoothLeService.getBtGatt();
        //扫描服务
        discoverServices(bleGatt);
    }

    /**
     * 初始化OAD
     */
    private void initOAD(List<BluetoothGattService> mserviceList){

        for (int i=0;i<mserviceList.size();i++){
            BluetoothGattService mGattService=mserviceList.get(i);
            //oadService
            if (mGattService.getUuid().equals(UUID_OAD_SERVICE)){
                mOadService=mGattService;
            }else if (mGattService.getUuid().equals(UUID_CONNCONTROL_SERVICE)){
            //连接控制的Service
                mConnControlService=mGattService;
            }
        }
    }

    private void discoverServices(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt.discoverServices()) {
            serviceList.clear();
        } else {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //取消广播
        if (BleBroadcastReceiver!=null){
            unregisterReceiver(BleBroadcastReceiver);
        }
        mLeService.disconnect(bleDevice.getAddress());

        super.onDestroy();
    }

    //    /**
//     * 加载image
//     *
//     * @param filepath
//     * @param isAsset
//     * @return
//     */
//    private boolean loadFile(String filepath, boolean isAsset) {
//        boolean fSuccess = true;
//        byte[] mBuffer;
//        try {
//            // Read the file raw into a buffer
//            InputStream stream = getAssets().open(filepath);
//            mBuffer = new byte[stream.available()];
//            stream.read(mBuffer);
//            stream.close();
//        } catch (IOException e) {
//            Log.e(oadTag, "File open failed: " + filepath + "\n");
//            return false;
//        }
//        bufferLenth = mBuffer.length / 16 + 1;
//        //赋值到全局mFileBuffer
//        mFileBuffer = mBuffer;
//        mFileImgHdr = new ImgHdr(mBuffer);
//
//        // Log
//        Log.e(oadTag, "Image 路径:" + filepath + "\n");
//
//        return fSuccess;
//    }
//
//
//
//    private class ProgTimerTask extends TimerTask {
//        @Override
//        public void run() {
//            mProgInfo.iTimeElapsed += TIMER_INTERVAL;
//        }
//    }


    private class ImgHdr {
        short crc0;
        short crc1;
        short ver;
        int len;
        byte[] uid = new byte[4];
        short addr;
        byte imgType;

        ImgHdr(byte[] buf) {
//            this.len = ((32 * 0x1000) / (16 / 4));
            this.len = buf.length / 4;
            this.ver = 0;
            this.uid[0] = this.uid[1] = this.uid[2] = this.uid[3] = 'E';
            this.addr = 0;
            this.imgType = 1; //EFL_OAD_IMG_TYPE_APP
            this.crc0 = calcImageCRC((int) 0, buf);
            crc1 = (short) 0xFFFF;
        }

        byte[] getRequest() {
            byte[] tmp = new byte[16];
            tmp[0] = Conversion.loUint16((short) this.crc0);
            tmp[1] = Conversion.hiUint16((short) this.crc0);
            tmp[2] = Conversion.loUint16((short) this.crc1);
            tmp[3] = Conversion.hiUint16((short) this.crc1);
            tmp[4] = Conversion.loUint16(this.ver);
            tmp[5] = Conversion.hiUint16(this.ver);
            tmp[6] = Conversion.loUint16((short) this.len);
            tmp[7] = Conversion.hiUint16((short) this.len);
            tmp[8] = tmp[9] = tmp[10] = tmp[11] = this.uid[0];
            tmp[12] = Conversion.loUint16(this.addr);
            tmp[13] = Conversion.hiUint16(this.addr);
            tmp[14] = imgType;
            tmp[15] = (byte) 0xFF;
            return tmp;
        }

        short calcImageCRC(int page, byte[] buf) {
            short crc = 0;
            long addr = page * 0x1000;

            byte pageBeg = (byte) page;
            byte pageEnd = (byte) (this.len / (0x1000 / 4));
            int osetEnd = ((this.len - (pageEnd * (0x1000 / 4))) * 4);

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

    private class ProgInfo {
        int iBytes = 0; // Number of bytes programmed
        short iBlocks = 0; // Number of blocks programmed
        short nBlocks = 0; // Total number of blocks
        int iTimeElapsed = 0; // Time elapsed in milliseconds

        void reset() {
            iBytes = 0;
            iBlocks = 0;
            iTimeElapsed = 0;
//            nBlocks = (short) (mFileImgHdr.len / (OAD_BLOCK_SIZE / HAL_FLASH_WORD_SIZE));
        }
    }


    /**
     * 设置IntentFilter
     * @return
     */
    private IntentFilter SettingIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }


    /**
     * Ble的广播
     */
    private BroadcastReceiver BleBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Service 发现完毕
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED==action){
                // Gatt的连接状态
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_SUCCESS);
                if (status == BluetoothGatt.GATT_SUCCESS){
                    //获得到此蓝牙设备的所有支持的Service
                    serviceList=mLeService.getSupportedGattServices();
                    if (serviceList!=null){
                        tv_connect_state.append("serviceList:"+serviceList.size());
                    }
                    //初始化OAD
                    initOAD(serviceList);
                }

            }
        }
    };
}
