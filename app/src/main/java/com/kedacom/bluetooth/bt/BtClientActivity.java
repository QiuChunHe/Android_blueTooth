package com.kedacom.bluetooth.bt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kedacom.bluetooth.APP;
import com.kedacom.bluetooth.R;
import com.kedacom.bluetooth.model.BaseInfo;
import com.kedacom.bluetooth.record.AudioFormatConverter;
import com.kedacom.bluetooth.util.BtReceiver;
import com.kedacom.bluetooth.util.PcmToWavUtil;

//手机客户端发送信息
public class BtClientActivity extends Activity implements BtBase.Listener, BtReceiver.Listener, BtDevAdapter.Listener {
    private TextView mTips;
    private EditText mInputMsg;
    private EditText mInputFile;
    private TextView mLogs;
    private BtReceiver mBtReceiver;
    private final BtDevAdapter mBtDevAdapter = new BtDevAdapter(this);
    private final BtClient mClient = new BtClient(this);
    private String filePath;
    private String storeFileName;
    private boolean isRecording = false;
    private Button recordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btclient);
        // 检查蓝牙开关
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            APP.toast("本机没有找到蓝牙硬件或驱动！", 0);
            finish();
            return;
        } else {
            if (!adapter.isEnabled()) {
                //直接开启蓝牙
                adapter.enable();
                //跳转到设置界面
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 112);
            }
        }

        // 检查是否支持BLE蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            APP.toast("本机不支持低功耗蓝牙！", 0);
            finish();
            return;
        }

        // Android 6.0动态请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.ACCESS_COARSE_LOCATION};
            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, 111);
                    break;
                }
            }
        }
        RecyclerView rv = findViewById(R.id.rv_bt);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mBtDevAdapter);
        mTips = findViewById(R.id.tv_tips);
        mInputMsg = findViewById(R.id.input_msg);
        recordBtn = findViewById(R.id.bt_record);
        mInputFile = findViewById(R.id.input_file);
        mLogs = findViewById(R.id.tv_log);
        mBtReceiver = new BtReceiver(this, this);//注册蓝牙广播
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBtReceiver);
        mClient.unListener();
        mClient.close();
    }

    @Override
    public void onItemClick(BluetoothDevice dev) {
        if (mClient.isConnected(dev)) {
            APP.toast("已经连接了", 0);
            return;
        }
        mClient.connect(dev);
        APP.toast("正在连接...", 0);
        mTips.setText("正在连接...");
    }

    @Override
    public void foundDev(BluetoothDevice dev) {
        mBtDevAdapter.add(dev);
    }

    // 重新扫描
    public void reScan(View view) {
        mBtDevAdapter.reScan();
    }

    public void sendMsg(View view) {
        if (mClient.isConnected(null)) {
            String msg = mInputMsg.getText().toString();
            if (TextUtils.isEmpty(msg))
                APP.toast("消息不能空", 0);
            else
                mClient.sendMsg(msg);
        } else
            APP.toast("没有连接", 0);
    }

    public void sendFile(View view) {
        if (mClient.isConnected(null)) {
            String filePath = mInputFile.getText().toString();
            if (TextUtils.isEmpty(filePath) || !new File(filePath).isFile())
                APP.toast("文件无效", 0);
            else
                mClient.sendFile(filePath);
        } else
            APP.toast("没有连接", 0);
    }

    public void snapshot(View view) {
        //抓拍消息
        if (mClient.isConnected(null)) {
            mClient.sendMsg("抓拍");
        } else
            APP.toast("没有连接", 0);
    }

    public void startRecording(View view) {
        if (!mClient.isConnected(null)) {
            APP.toast("没有连接", 0);
        }
        if (isRecording) {
            clickRecordEnd();
        }else {
            clickRecordStart();
        }
    }

//    public void stopRecording(View view) {
//        //结束录像
//        if (mClient.isConnected(null)) {
//            mClient.sendMsg("结束录像");
//        } else
//            APP.toast("没有连接", 0);
//    }

    //开始录制
    private void clickRecordStart() {
        recordBtn.setText("结束录制");
        isRecording = true;
    }

    //结束录制
    private void clickRecordEnd() {
        recordBtn.setText("开始录制");
        isRecording = false;
    }

    //结束录制后发送录制文件到设备
    private void sendFileToDevice(String fileName) {
        mClient.sendFile(fileName);
    }

    @Override
    public void socketNotify(int state, final Object obj) {
        if (isDestroyed())
            return;
        String msg = null;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("与%s(%s)连接成功", dev.getName(), dev.getAddress());
                mTips.setText(msg);
                break;
            case BtBase.Listener.DISCONNECTED:
                msg = "连接断开";
                mTips.setText(msg);
                break;
            case BtBase.Listener.MSG:
                //返回信息
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                break;
            case BtBase.Listener.FILE_RECEIVE_START:
                //开始接收文件
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                break;
            case BtBase.Listener.FILE_RECEIVE_FINISH:
                //文件接受完成
                filePath = obj.toString();
                msg = String.format("\n%s", obj);
                mLogs.append(msg);
                //转wav文件
                pcmToWac(obj.toString());
                break;
        }
        APP.toast(msg, 0);
    }

    private void pcmToWac(String fileName) {
        if (fileName.contains("wav")){
            storeFileName = fileName;
            return;
        }
        storeFileName = fileName + ".wav";
        PcmToWavUtil.getInstance().pcmToWav(fileName, storeFileName);
    }

}