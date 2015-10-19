package com.zsl.bluetoothdemo.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.zsl.bluetoothdemo.activitys.MyBluetoothDevice;
import com.zsl.bluetoothdemo.R;
import com.zsl.bluetoothdemo.utils.adapter.UniversalAdapter;
import com.zsl.bluetoothdemo.utils.adapter.ViewHolder;

import java.util.List;

/**
 * Created by zsl on 15/5/25.
 */
public class DevicesAdapter extends UniversalAdapter<MyBluetoothDevice> {

    /**
     * 通用的Adapter
     *
     * @param context 上下文
     * @param mlists  数据集
     */

    public DevicesAdapter(Context context, List<MyBluetoothDevice> mlists) {
        super(context, mlists, R.layout.listview_item);
    }

    @Override
    public void convert(ViewHolder holder, MyBluetoothDevice myBluetoothDevice, int position) {
        BluetoothDevice bluetoothDevice=myBluetoothDevice.getBluetoothDevice();
        holder.setText(R.id.listview_item_tv_name, bluetoothDevice.getName())
                .setText(R.id.listview_item_tv_address, bluetoothDevice.getAddress());


    }
}
