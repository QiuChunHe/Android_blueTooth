package com.kedacom.bluetooth.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.fastjson.JSON;

import com.kedacom.bluetooth.APP;
import com.kedacom.bluetooth.model.SnapshotInfo;
import com.kedacom.bluetooth.model.StartVideoInfo;
import com.kedacom.bluetooth.model.StopVideoInfo;
import com.kedacom.bluetooth.util.Util;

/**
 * 客户端，与服务端建立长连接
 */
public class BtClient extends BtBase {
    StartVideoInfo startVideoInfo;
    StopVideoInfo stopVideoInfo;
    SnapshotInfo snapshotInfo;
    BtClient(Listener listener) {
        super(listener);
    }

    /**
     * 与远端设备建立长连接
     *
     * @param dev 远端设备
     */
    public void connect(BluetoothDevice dev) {
        close();
        try {
             final BluetoothSocket socket = dev.createRfcommSocketToServiceRecord(SPP_UUID); //加密传输，Android系统强制配对，弹窗显示配对码
//            final BluetoothSocket socket = dev.createInsecureRfcommSocketToServiceRecord(SPP_UUID); //明文传输(不安全)，无需配对
//            final  BluetoothSocket socket = dev.createInsecureRfcommSocketToServiceRecord(s)
            // 开启子线程
            Util.EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    loopRead(socket); //循环读取
                }
            });
        } catch (Throwable e) {
            close();
        }
    }

    public void startRecording(String content) {
        startVideoInfo =new StartVideoInfo();
        startVideoInfo.setContent(content);
        startVideoInfo.setMsgld("00001101-0000-1000-8000-00805F9B34FB");
            sendMsg(JSON.toJSONString(startVideoInfo));
    }

    public void stopRecording(String content) {
        stopVideoInfo =new StopVideoInfo();
        stopVideoInfo.setContent(content);
        stopVideoInfo.setMsgld("00001101-0000-1000-8000-00805F9B34FB");
        sendMsg(JSON.toJSONString(stopVideoInfo));
    }
    public void snapshotInfo(String content) {
        snapshotInfo =new SnapshotInfo();
        snapshotInfo.setContent(content);
        snapshotInfo.setMsgld("00001101-0000-1000-8000-00805F9B34FB");
        sendMsg(JSON.toJSONString(snapshotInfo));
    }
}