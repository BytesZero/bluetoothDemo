package com.zsl.bluetoothdemo.activitys;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    Button bt_oad, bt_oad_stop, bt_connect,bt_history,bt_realtime;
    //btDevice
    BluetoothDevice bleDevice;


    /**
     * OAD
     */
    private MainActivity myMainActivity = null;
    private BluetoothLeService mLeService = null;
    // BLE
    private static BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothGatt bleGatt;
    private BluetoothGattService mOadService;
    private BluetoothGattService mConnControlService;
    private List<BluetoothGattCharacteristic> mCharListOad;
    private List<BluetoothGattCharacteristic> mCharListCc;
    private BluetoothGattCharacteristic mCharIdentify = null;
    private BluetoothGattCharacteristic mCharBlock = null;
    private BluetoothGattCharacteristic mCharConnReq = null;

    //所有的Service
    List<BluetoothGattService> serviceList = new ArrayList<BluetoothGattService>();


    // Housekeeping
    private boolean mServiceOk = false;
    private boolean mProgramming = false;

    //fileName
    String filename = "APP_OAD_1.1.bin";
    //    String filename="APP_OAD_1.0.bin";
    String oadTag = "oadTag";

    private static final int OAD_BLOCK_SIZE = 16;
    private static final int HAL_FLASH_WORD_SIZE = 4;
    private static final int OAD_BUFFER_SIZE = 2 + OAD_BLOCK_SIZE;
    private static final long TIMER_INTERVAL = 1000;

    // Programming parameters
    private static final short OAD_CONN_INTERVAL = 6; // 7.5 milliseconds
    private static final short OAD_CONN_INTERVAL40 = 30; // 37.5 milliseconds
    private static final short OAD_SUPERVISION_TIMEOUT = 50; // 500 milliseconds
    private static final short OAD_SUPERVISION_TIMEOUT500 = 500; // 500 milliseconds

    private ImgHdr mFileImgHdr;
    private ImgHdr mTargImgHdr;
    private Timer mTimer = null;
    private ProgInfo mProgInfo = new ProgInfo();
    private TimerTask mTimerTask = null;
    private int packetsSent = 0;
    //image
    private byte[] mFileBuffer;
    private final byte[] mOadBuffer = new byte[OAD_BUFFER_SIZE];
    //长度
    private int bufferLenth = 0;

    private boolean slowAlgo = true;


    private static DeviceHomeActivity mDeviceHomeActivity;

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
        bt_oad_stop = (Button) findViewById(R.id.devices_bt_oad_stop);
        bt_connect = (Button) findViewById(R.id.devices_bt_connect);

        bt_history= (Button) findViewById(R.id.devices_bt_history);

        bt_realtime= (Button) findViewById(R.id.devices_bt_realtime);
    }

    private void initEvent() {
        bt_oad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setConnectionParameters();
                startProgramming();
            }
        });

        bt_oad_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopProgramming();
            }
        });

        bt_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice();
            }
        });

        bt_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseStartActivity(HistoryDataActivity.class);
            }
        });

        bt_realtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseStartActivity(RealTimeDataActivity.class);
            }
        });
    }

    private void initData() {
        mDeviceHomeActivity=this;
        myMainActivity = MainActivity.getInstance();


        //注册广播
        registerReceiver(BleBroadcastReceiver, SettingIntentFilter());
        //获得到BluetoothLeService对象
        mLeService = BluetoothLeService.getInstance();
        mBluetoothManager = myMainActivity.getmBluetoothManager();
        mBtAdapter = myMainActivity.getmBtAdapter();
        bleDevice = getIntent().getParcelableExtra("BluetoothDevice");

        connectDevice();
    }

    /**
     * 获得到ServiceList
     * @return
     */
    public List<BluetoothGattService> getServiceList() {
        return serviceList;
    }

    /**
     * 获得到DeviceHomeActivity对象
     * @return
     */
    public static DeviceHomeActivity getmDeviceHomeActivity() {
        return mDeviceHomeActivity;
    }

    private void connectDevice() {



        int connState = mBluetoothManager.getConnectionState(bleDevice,
                BluetoothGatt.GATT);
        switch (connState) {
            case BluetoothGatt.STATE_CONNECTED:

                Toast.makeText(this, "已连接", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                tv_connect_state.setText("正在连接。。。");
                boolean ok = mLeService.connect(bleDevice.getAddress());
                if (!ok) {
                    Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 初始化OAD
     */
    private void initOAD(List<BluetoothGattService> mserviceList) {

        for (int i = 0; i < mserviceList.size(); i++) {
            BluetoothGattService mGattService = mserviceList.get(i);
            //oadService
            if (mGattService.getUuid().equals(UUID_OAD_SERVICE)) {
                mOadService = mGattService;
            } else if (mGattService.getUuid().equals(UUID_CONNCONTROL_SERVICE)) {
                //连接控制的Service
                mConnControlService = mGattService;
            }
        }

        // 获得到对应Service的Characteristics list
        mCharListOad = mOadService.getCharacteristics();
        mCharListCc = mConnControlService.getCharacteristics();
        if (mCharListOad == null || mCharListCc == null) {
            return;
        }
        //验证Characteristics list
        mServiceOk = mCharListOad.size() == 2 && mCharListCc.size() >= 3;
        if (mServiceOk) {
            mCharIdentify = mCharListOad.get(0);
            mCharBlock = mCharListOad.get(1);
            mCharBlock.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mCharConnReq = mCharListCc.get(1);
        }

        mLeService.setCharacteristicNotification(mCharBlock, true);
        setConnectionParameters();
        loadFile(filename, false);
    }

    /**
     * 扫描Service
     *
     * @param bluetoothGatt
     */
    private void discoverServices(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt != null) {
            if (bluetoothGatt.discoverServices()) {
                serviceList.clear();
            }
        }

    }

    /**
     * 发送连接间隔
     */
    private void setConnectionParameters() {
        if (mCharConnReq == null)
            return;
        // Make sure connection interval is long enough for OAD (Android default connection interval is 7.5 ms)
        byte[] value = {Conversion.loUint16(OAD_CONN_INTERVAL), Conversion.hiUint16(OAD_CONN_INTERVAL), Conversion.loUint16(OAD_CONN_INTERVAL),
                Conversion.hiUint16(OAD_CONN_INTERVAL), 0, 0, Conversion.loUint16(OAD_SUPERVISION_TIMEOUT), Conversion.hiUint16(OAD_SUPERVISION_TIMEOUT)};
        mCharConnReq.setValue(value);
        mLeService.writeCharacteristic(mCharConnReq);
    }

    private void setConnectionParameters100() {
        // Make sure connection interval is long enough for OAD (Android default connection interval is 7.5 ms)
        byte[] value = {Conversion.loUint16(OAD_CONN_INTERVAL40), Conversion.hiUint16(OAD_CONN_INTERVAL40), Conversion.loUint16(OAD_CONN_INTERVAL40),
                Conversion.hiUint16(OAD_CONN_INTERVAL40), 0, 0, Conversion.loUint16(OAD_SUPERVISION_TIMEOUT500), Conversion.hiUint16(OAD_SUPERVISION_TIMEOUT500)};
        mCharConnReq.setValue(value);
        mLeService.writeCharacteristic(mCharConnReq);
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
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //取消广播
        if (BleBroadcastReceiver != null) {
            unregisterReceiver(BleBroadcastReceiver);
        }
        mLeService.disconnect(bleDevice.getAddress());
        super.onDestroy();
    }


    /**
     * 启动oad
     */
    private void startProgramming() {
        Log.e(oadTag, "启动oad");
        mProgramming = true;
        packetsSent = 0;
        mCharIdentify.setValue(mFileImgHdr.getRequest());
        mLeService.writeCharacteristic(mCharIdentify);

        // Initialize stats
        mProgInfo.reset();
        mTimer = new Timer();
        mTimerTask = new ProgTimerTask();
        mTimer.scheduleAtFixedRate(mTimerTask, 0, TIMER_INTERVAL);
    }

    /**
     * 停止oad
     */
    private void stopProgramming() {
        Log.e(oadTag, "停止oad");
        mTimer.cancel();
        mTimer.purge();
        mTimerTask.cancel();
        mTimerTask = null;

        mProgramming = false;

        mLeService.setCharacteristicNotification(mCharBlock, false);
        if (mProgInfo.iBlocks == mProgInfo.nBlocks) {
            Log.e(oadTag, "Programming complete!\n");
        } else {
            Log.e(oadTag, "Programming cancelled!\n");
        }
    }

    /**
     * 加载image
     *
     * @param filepath
     * @param isAsset
     * @return
     */
    private boolean loadFile(String filepath, boolean isAsset) {
        boolean fSuccess = true;
        byte[] mBuffer;
        try {
            // Read the file raw into a buffer
            InputStream stream = getAssets().open(filepath);
            mBuffer = new byte[stream.available()];
            stream.read(mBuffer);
            stream.close();
        } catch (IOException e) {
            Log.e(oadTag, "File open failed: " + filepath + "\n");
            return false;
        }
        bufferLenth = mBuffer.length / 16 + 1;
        //赋值到全局mFileBuffer
        mFileBuffer = mBuffer;
        mFileImgHdr = new ImgHdr(mBuffer);

        displayStats();
        // Log
        Log.e(oadTag, "Image 路径:" + filepath + "\n");

        return fSuccess;
    }

    /**
     * 显示转台
     */
    private void displayStats() {
        String txt;
        int byteRate;
        int sec = mProgInfo.iTimeElapsed / 1000;
        if (sec > 0) {
            byteRate = mProgInfo.iBytes / sec;
        } else {
            byteRate = 0;
            return;
        }
        float timeEstimate;

        timeEstimate = ((float) (mFileImgHdr.len * 4) / (float) mProgInfo.iBytes) * sec;

        txt = String.format("Time: %d / %d sec", sec, (int) timeEstimate);
        txt += String.format("    Bytes: %d (%d/sec)", mProgInfo.iBytes, byteRate);
        Log.e(oadTag, txt);
    }

    private class ProgTimerTask extends TimerTask {
        @Override
        public void run() {
            mProgInfo.iTimeElapsed += TIMER_INTERVAL;
        }
    }


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
            nBlocks = (short) (mFileImgHdr.len / (OAD_BLOCK_SIZE / HAL_FLASH_WORD_SIZE));
        }
    }


    /**
     * 设置IntentFilter
     *
     * @return
     */
    private IntentFilter SettingIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        fi.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }


    /**
     * Ble的广播
     */
    private BroadcastReceiver BleBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {//连接成功
                tv_connect_state.setText("连接成功1");
                bleGatt=BluetoothLeService.getBtGatt();
                discoverServices(bleGatt);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //连接断开
                tv_connect_state.setText("连接断开");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {// Service 发现完毕

                // Gatt的连接状态
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_SUCCESS);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //获得到此蓝牙设备的所有支持的Service
                    serviceList = mLeService.getSupportedGattServices();
                    if (serviceList != null) {
                        tv_connect_state.append("serviceList:" + serviceList.size());
                    }
                    //初始化OAD
                    initOAD(serviceList);
                }

            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

                if (uuidStr.equals(mCharIdentify.getUuid().toString())) {

                }
                if (uuidStr.equals(mCharBlock.getUuid().toString())) {
                    // Block check here :
                    String block = String.format("%02x%02x", value[1], value[0]);

                    programBlock();

//                    if (slowAlgo == true) {
//                        programBlock();
//                    } else {
//                        if (packetsSent != 0) packetsSent--;
//                        if (packetsSent > 10) return;
//                        while (packetsSent < fastAlgoMaxPackets) {
//                            waitABit();
//                            programBlock();
//                        }
//                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(context, "GATT error: status=" + status, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    private void programBlock() {
        if (!mProgramming)
            return;
        //  mProgInfo.nBlocks=(short)bufferLenth;

        Log.e(oadTag, "mProgInfo.iBlocks:" + mProgInfo.iBlocks + "mProgInfo.nBlocks:" + mProgInfo.nBlocks);
        if (mProgInfo.iBlocks < mProgInfo.nBlocks) {
            mProgramming = true;
            String msg = new String();

            //Prepare block
            mOadBuffer[0] = Conversion.loUint16(mProgInfo.iBlocks);
            mOadBuffer[1] = Conversion.hiUint16(mProgInfo.iBlocks);
            System.arraycopy(mFileBuffer, mProgInfo.iBytes, mOadBuffer, 2, OAD_BLOCK_SIZE);

            // Send block
            mCharBlock.setValue(mOadBuffer);
            boolean success = mLeService.writeCharacteristicNonBlock(mCharBlock);
            Log.d("FwUpdateActivity_CC26xx", "Sent block :" + mProgInfo.iBlocks);
            if (success) {
                // Update stats
                packetsSent++;
                mProgInfo.iBlocks++;
                mProgInfo.iBytes += OAD_BLOCK_SIZE;

                int total = (mProgInfo.iBlocks * 100) / mProgInfo.nBlocks;

                Log.e(oadTag, "已上传总量：" + total + "%");
                if (mProgInfo.iBlocks == mProgInfo.nBlocks) {
                    Log.e(oadTag, "OAD完毕");
                }
            } else {
                mProgramming = false;
                msg = "GATT writeCharacteristic failed\n";
            }
            if (!success) {
                Log.e(oadTag, msg);
            }
        } else {
            mProgramming = false;
        }
        if ((mProgInfo.iBlocks % 100) == 0) {
            // Display statistics each 100th block
            runOnUiThread(new Runnable() {
                public void run() {
                    displayStats();
                }
            });
        }

        if (!mProgramming) {
            runOnUiThread(new Runnable() {
                public void run() {
                    displayStats();
                    stopProgramming();
                }
            });
        }
    }

}
