package com.kedacom.bluetooth.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import com.kedacom.bluetooth.APP;
import com.kedacom.bluetooth.R;
import com.kedacom.bluetooth.util.AudioRecordUtil;
import com.kedacom.bluetooth.util.ByteArrayToWav;
import com.kedacom.bluetooth.util.MLByteArray;
import com.kedacom.bluetooth.util.MLByteWavConverter;
import com.kedacom.bluetooth.util.PcmToWavUtil;

import com.kedacom.bluetooth.util.MLAudioPlayer;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * BLE客户端(主机/中心设备/Central)
 */
public class BleClientActivity extends Activity {
    private static final String TAG = BleClientActivity.class.getSimpleName();
    private EditText mWriteET;
    private TextView mTips;
    private Button recordBtn;
    private BleDevAdapter mBleDevAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private byte[] mReceicedData;
    private MediaPlayer mediaPlayer; // 声明MediaPlayer对象
    private boolean isRecording = false;
    private int dataTotalCount;
    private boolean isRecieveFiling = false;
//    private byte[] endBytes = byte[4]{0xAD, 0xBE, 0xBE,0xEF};

    private String endByteStr = "DEADBEEF";
    private String mReceicedDataStr; // 16进制流数据
    private int reciveDataLen = 0; // 记录总接收数据的大小
    private int bagCount = 0;
    private int dataFileLen = 0; // 记录当前接收文件的大小
    private int preDataFileLen = 0; // 记录当前接收文件和之前接收文件的总大小
    private int fileIndex = 1; // 记录当前文件的索引
    private int fileCount = 0; // 记录文件的个数
    private byte[] sendData ; // 记录需要发送的包
    private byte byte1; // 记录指令第一位
    private byte byte2; // 记录指令第二位
    private byte byte4; // 记录需要发送流第四位
    private byte byte5; // 记录需要发送流第五位
    private int lenMax = 240000;

    // 测试数据（音频）
    private String recordedByteStr = "80 FA FF FF 90 FC FF FF 40 F9 FF FF 70 FA FF FF C0 FB FF FF 50 FD FF FF 60 FB FF FF 40 FE FF FF 90 FE FF FF A0 FC FF FF 10 00 00 00 00 FE FF FF 90 FC FF FF 50 00 00 00 40 FD FF FF 40 FC FF FF 10 FF FF FF 00 FD FF FF F0 FB FF FF D0 FE FF FF 50 FC FF FF D0 FB FF FF 80 FE FF FF 90 FD FF FF 70 FC FF FF E0 FF FF FF C0 FF FF FF 90 FD FF FF 00 02 00 00 A0 FF FF FF 40 FE FF FF 60 03 00 00 00 00 00 00 60 FD FF FF 60 02 00 00 80 FF FF FF 10 FD FF FF 30 01 00 00 70 FD FF FF B0 FC FF FF C0 00 00 00 40 FE FF FF B0 FC FF FF 00 00 00 00 DD FE AA BB 00 01 00 11 03 A9 05 65 07 01 08 65 07 A8 05 10 03 00 00 F0 FC 57 FA 9B F8 FF F7 9B F8 58 FA F1 FC 00 00 10 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9B F8 58 FA F0 FC 00 00 10 03 A8 05 64 07 01 08 65 07 AA 05 11 03 00 00 F2 FC 5B FA 9F F8 03 F8 A0 F8 5C FA F4 FC 03 00 13 03 AA 05 66 07 02 08 65 07 A9 05 10 03 00 00 F1 FC 59 FA 9D F8 01 F8 9C F8 58 FA F0 FC 00 00 10 03 A8 05 64 07 00 08 64 07 A8 05 10 03 00 00 F0 FC 57 FA 9B F8 FF F7 9B F8 56 FA EE FC FE FF 0D 03 A5 05 61 07 FD 07 62 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 FE F7 9A F8 57 FA EF FC FF FF 0E 03 A6 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9D F8 59 FA F1 FC 01 00 11 03 A9 05 64 07 00 08 63 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 00 F8 9C F8 57 FA F0 FC 00 00 10 03 A9 05 65 07 02 08 66 07 AA 05 12 03 01 00 F2 FC 5A FA 9E F8 02 F8 9E F8 5A FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 64 07 A9 05 11 03 02 00 F2 FC 5A FA 9D F8 01 F8 9D F8 58 FA F0 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 10 03 00 00 F0 FC 59 FA 9E F8 02 F8 9D F8 5A FA F2 FC 01 00 11 03 A9 05 65 07 01 08 64 07 A8 05 11 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 59 FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 64 07 A8 05 10 03 00 00 F1 FC 58 FA 9C F8 FF F7 9B F8 57 FA EF FC FE FF 0D 03 A5 05 61 07 FD 07 61 07 A5 05 0E 03 FF FF EF FC 57 FA 9C F8 00 F8 9C F8 57 FA EF FC FF FF 0E 03 A6 05 62 07 FE 07 62 07 A7 05 0F 03 FF FF EF FC 57 FA 9B F8 FF F7 9B F8 57 FA F0 FC 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 00 F8 FF 00 9C F8 FF 00 57 FA FF 00 F0 FC FF 00 00 00 00 00 10 03 00 00 A9 05 00 00 65 07 00 00 02 08 00 00 66 07 00 00 AA 05 00 00 12 03 00 00 01 00 00 00 F2 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 64 07 00 00 A9 05 00 00 11 03 00 00 02 00 00 00 F2 FC FF 00 5A FA FF 00 9D F8 FF 00 01 F8 FF 00 9D F8 FF 00 58 FA FF 00 F0 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 63 07 00 00 A7 05 00 00 10 03 00 00 00 00 00 00 F0 FC FF 00 59 FA FF 00 9E F8 FF 00 02 F8 FF 00 9D F8 FF 00 5A FA FF 00 F2 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 64 07 00 00 A8 05 00 00 11 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 59 FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 64 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 58 FA FF 00 9C F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 EF FC FF 00 FE FF FF 00 0D 03 00 00 A5 05 00 00 61 07 00 00 FD 07 00 00 61 07 00 00 A5 05 00 00 0E 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9C F8 FF 00 00 F8 FF 00 9C F8 FF 00 57 FA FF 00 EF FC FF 00 FF FF FF 00 0E 03 00 00 A6 05 00 00 62 07 00 00 FE 07 00 00 62 07 00 00 A7 05 00 00 0F 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 F0 FC FF FF FF FD FF 02 00 FC FF FC FF FF FF FD FF FD FF FF FF FD FF FD FF 00 00 FF FF FD FF 00 00 FD FF FC FF FF FF FA FF FA FF FC FF FA FF FB FF FC FF F9 FF FA FF FC FF F9 FF FA FF FC FF F7 FF F9 FF FA FF F6 FF F8 FF F8 FF F6 FF F8 FF F8 FF F4 FF F7 FF F6 FF F5 FF F8 FF F8 FF F6 FF F8 FF F9 FF F4 FF F7 FF F7 FF F4 FF F7 FF F6 FF F2 FF F5 FF F4 FF F1 FF F5 FF F4 FF F1 FF F5 FF F4 FF F2 FF F5 FF F3 FF F2 FF F5 FF F3 FF F2 FF F5 FF F3 FF F4 FF F7 FF F7 FF F6 FF F7 FF F7 FF F5 FF F7 FF F7 FF F6 FF F8 FF F8 FF F7 FF F9 FF F9 FF F7 FF F8 FF F9 FF F7 FF F8 FF FA FF FA FF F9 FF FB FF F9 FF F9 FF FA FF F6 FF F9 FF F9 FF F7 FF F8 FF F8 FF F6 FF F8 FF F7 FF F6 FF F7 FF F7 FF F6 FF F7 FF F6 FF F6 FF F8 FF F8 FF F6 FF F9 FF F8 FF F6 FF F8 FF F7 FF F6 FF F7 FF F7 FF F6 FF F8 FF F8 FF F8 FF F9 FF FA FF F9 FF F9 FF FA FF F7 FF F9 FF FA FF F6 FF F8 FF F9 FF F7 FF F9 FF FA FF F7 FF F9 FF F8 FF F6 FF F8 FF F8 FF F7 FF F9 FF F8 FF F5 FF F8 FF F8 FF F6 FF F8 FF F7 FF F5 FF F8 FF F7 FF F4 FF F8 FF F6 FF F3 FF F7 FF F5 FF F2 FF F7 FF F4 FF F1 FF F5 FF F2 FF F0 FF F5 FF F2 FF F1 FF F5 FF F2 FF F0 FF F6 FF F3 FF F2 FF F6 FF F4 FF F3 FF F6 FF F4 FF F2 FF F6 FF F4 FF F3 FF F7 FF F5 FF F2 FF F6 FF F4 FF F2 FF F6 FF F3 FF F2 FF F5 FF F3 FF F0 FF F5 FF F1 FF F1 FF F5 FF F3 FF F4 FF F6 FF F5 FF F2 FF F6 FF F4 FF F2 FF F6 FF F4 FF F3 FF F6 FF F3 FF F2 FF F5 FF F2 FF F2 FF F6 FF F4 FF F3 FF F5 FF F3 FF F3 FF F6 FF F4 FF F2 FF F6 FF F4 FF F0 FF F4 FF F1 FF 20 FC FF FF 90 FB FF FF 80 FD FF FF B0 FB FF FF 70 FB FF FF F0 FD FF FF 80 FC FF FF 80 FB FF FF 50 FE FF FF 50 FD FF FF 90 FC FF FF 90 FF FF FF E0 FE FF FF 70 FD FF FF D0 00 00 00 60 01 00 00 20 FE FF FF D0 02 00 00 30 02 00 00 F0 FD FF FF 10 03 00 00 40 00 00 00 40 FD FF FF A0 01 00 00 10 01 00 00 00 FE FF FF 30 03 00 00 B0 03 00 00 50 FF FF FF 20 05 00 00 50 02 00 00 C0 FE FF FF D0 03 00 00 40 02 00 00 40 FE FF FF C0 03 00 00 70 02 00 00 20 FE FF FF 80 03 00 00 20 01 00 00 E0 FD FF FF 40 02 00 00 50 02 00 00 50 FE FF FF 50 03 00 00 C0 02 00 00 F0 FE FF FF B0 04 00 00 D0 02 00 00 C0 FF FF FF 70 05 00 00 80 03 00 00 50 FF FF FF 50 05 00 00 20 03 00 00 50 FF FF FF D0 05 00 00 30 02 00 00 00 FF FF FF 10 05 00 00 A0 02 00 00 B0 FE FF FF B0 03 00 00 20 03 00 00 A0 FF FF FF F0 04 00 00 90 03 00 00 00 00 00 00 20 06 00 00 70 04 00 00 D0 FF FF FF D0 05 00 00 F0 04 00 00 D0 00 00 00 A0 07 00 00 60 06 00 00 30 01 00 00 A0 08 00 00 60 04 00 00 B0 FF FF FF 10 06 00 00 C0 02 00 00 50 FF FF FF F0 04 00 00 80 03 00 00 70 FF FF FF 10 05 00 00 A0 03 00 00 C0 FF FF FF E0 05 00 00 D0 03 00 00 90 FF FF FF C0 04 00 00 D0 03 00 00 D0 FF FF FF E0 05 00 00 C0 03 00 00 00 FF FF FF 00 05 00 00 80 03 00 00 90 FF FF FF 80 05 00 00 20 03 00 00 30 FF FF FF F0 05 00 00 70 01 00 00 B0 FD FF FF F0 02 00 00 E0 FF FF FF 00 FE FF FF 80 03 00 00 10 00 00 00 D0 FD FF FF 30 02 00 00 50 FF FF FF D0 FD FF FF 20 02 00 00 F0 FF FF FF A0 FD FF FF B0 02 00 00 A0 FF FF FF C0 FD FF FF 60 02 00 00 D0 FC FF FF 60 FC FF FF C0 FF FF FF 90 FD FF FF 20 FD FF FF F0 FF FF FF 90 FD FF FF 70 FD FF FF D0 00 00 00 90 FF FF FF 40 FD FF FF D0 00 00 00 10 FD FF FF B0 FC FF FF B0 FF FF FF 00 FA FF FF A0 FA FF FF D0 FC FF FF 40 FA FF FF 10 FB FF FF E0 FC FF FF 10 F9 FF FF B0 FA FF FF 50 FC FF FF 60 F9 FF FF 60 FA FF FF 50 FC FF FF F0 F7 FF FF 50 F9 FF FF 00 FA FF FF 50 F6 FF FF 80 F8 FF FF 70 F8 FF FF 10 F6 FF FF 70 F8 FF FF 80 F8 FF FF 70 F4 FF FF C0 F7 FF FF E0 F6 FF FF 80 F5 FF FF A0 F8 FF FF D0 F8 FF FF 50 F6 FF FF D0 F8 FF FF 20 F9 FF FF 90 F4 FF FF D0 F7 FF FF 70 F7 FF FF 00 F4 FF FF 20 F7 FF FF 40 F6 FF FF A0 F2 FF FF E0 F5 FF FF 60 F4 FF FF D0 F1 FF FF 70 F5 FF FF 70 F4 FF FF F0 F1 FF FF A0 F5 FF FF 90 F4 FF FF 40 F2 FF FF 80 F5 FF FF E0 F3 FF FF 00 F2 FF FF B0 F5 FF FF 80 F3 FF FF 40 F2 FF FF D0 F5 FF FF F0 F3 FF FF D0 F4 FF FF 30 F7 FF FF 00 F7 FF FF 20 F6 FF FF 80 F7 FF FF 60 F7 FF FF C0 F5 FF FF A0 F7 FF FF 30 F7 FF FF 20 F6 FF FF 80 F8 FF FF F0 F8 FF FF 10 F7 FF FF 00 F9 FF FF 10 F9 FF FF 20 F7 FF FF 90 F8 FF FF 20 F9 FF FF E0 F7 FF FF C0 F8 FF FF 60 FA FF FF 70 FA FF FF E0 F9 FF FF A0 FB FF FF 10 F9 FF FF B0 F9 FF FF 50 FA FF FF B0 F6 FF FF 80 F9 FF FF 30 F9 FF FF 40 F7 FF FF B0 F8 FF FF 50 F8 FF FF C0 F6 FF FF 90 F8 FF FF D0 F7 FF FF 40 F6 FF FF E0 F7 FF FF 70 F7 FF FF 00 F6 FF FF 40 F7 FF FF 70 F6 FF FF 00 F6 FF FF D0 F8 FF FF 00 F8 FF FF 00 F6 FF FF 50 F9 FF FF A0 F8 FF FF 40 F6 FF FF 30 F8 FF FF D0 F7 FF FF 60 F6 FF FF F0 F7 FF FF D0 F7 FF FF 20 F6 FF FF 80 F8 FF FF D0 F8 FF FF E0 F8 FF FF 30 F9 FF FF 80 FA FF FF 80 F9 FF FF 40 F9 FF FF 70 FA FF FF C0 F7 FF FF 10 F9 FF FF 00 FA FF FF A0 F6 FF FF C0 F8 FF FF 70 F9 FF FF 70 F7 FF FF 60 F9 FF FF 10 FA FF FF 40 F7 FF FF 70 F9 FF FF B0 F8 FF FF 50 F6 FF FF E0 F8 FF FF 60 F8 FF FF F0 F7 FF FF 20 F9 FF FF 90 F8 FF FF E0 F5 FF FF 50 F8 FF FF 50 F8 FF FF 60 F6 FF FF 50 F8 FF FF A0 F7 FF FF 20 F5 FF FF F0 F8 FF FF 30 F7 FF FF 30 F4 FF FF 70 F8 FF FF B0 F6 FF FF A0 F3 FF FF E0 F7 FF FF 50 F5 FF FF 80 F2 FF FF 00 F7 FF FF 40 F4 FF FF F0 F1 FF FF 90 F5 FF FF 30 F2 FF FF D0 F0 FF FF F0 F5 FF FF 30 F2 FF FF 40 F1 FF FF E0 F5 FF FF C0 F2 FF FF E0 F0 FF FF 40 F6 FF FF 30 F3 FF FF F0 F2 FF FF B0 F6 FF FF 80 F4 FF FF 90 F3 FF FF D0 F6 FF FF C0 F4 FF FF D0 F2 FF FF B0 F6 FF FF 60 F4 FF FF 60 F3 FF FF 20 F7 FF FF 80 F5 FF FF 80 F2 FF FF 70 F6 FF FF 20 F4 FF FF 20 F2 FF FF 20 F6 FF FF 70 F3 FF FF 10 F2 FF FF D0 F5 FF FF 00 F3 FF FF 70 F0 FF FF 30 F5 FF FF D0 F1 FF FF B0 F1 FF FF F0 F5 FF FF 30 F3 FF FF 30 F4 FF FF 90 F6 FF FF 30 F5 FF FF A0 F2 FF FF 10 F6 FF FF 90 F4 FF FF B0 F2 FF FF 50 F6 FF FF 00 F4 FF FF 60 F3 FF FF A0 F6 FF FF F0 F3 FF FF 00 F2 FF FF 70 F5 FF FF B0 F2 FF FF 40 F2 FF FF 40 F6 FF FF 00 F4 FF FF 10 F3 FF FF C0 F5 FF FF F0 F3 FF FF 70 F3 FF FF 50 F6 FF FF 90 F4 FF FF F0 F2 FF FF 30 F6 FF FF 30 F4 FF FF A0 F0 FF FF D0 F4 FF FF C0 F1 FF FF DD FE AA BB 00 00 00 11 03 A9 05 65 07 01 08 65 07 A9 05 11 03 02 00 F3 FC 5C FA A0 F8 04 F8 9F F8 5A FA F2 FC 01 00 10 03 A8 05 64 07 00 08 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9D F8 58 FA F0 FC 00 00 0F 03 A6 05 63 07 00 08 64 07 A8 05 10 03 01 00 F2 FC 59 FA 9D F8 01 F8 9D F8 59 FA F1 FC 00 00 0F 03 A6 05 62 07 FE 07 62 07 A7 05 0F 03 00 00 F2 FC 5A FA 9E F8 02 F8 9E F8 5A FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 59 FA 9D F8 01 F8 9D F8 59 FA F2 FC 01 00 12 03 AA 05 65 07 00 08 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9C F8 58 FA F0 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9C F8 58 FA F0 FC 00 00 10 03 A7 05 63 07 FF 07 63 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 00 F8 9B F8 58 FA F0 FC 00 00 0E 03 A6 05 63 07 FF 07 63 07 A8 05 10 03 00 00 F1 FC 58 FA 9C F8 01 F8 9D F8 59 FA F1 FC 01 00 11 03 A9 05 65 07 01 08 64 07 A8 05 11 03 01 00 F2 FC 5B FA 9F F8 02 F8 9D F8 59 FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 62 07 A6 05 0E 03 FF FF EF FC 58 FA 9B F8 FF F7 9B F8 57 FA F0 FC 00 00 0F 03 A7 05 64 07 01 08 65 07 A9 05 10 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 5A FA F3 FC 02 00 11 03 A8 05 64 07 00 08 64 07 A7 05 0F 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 5A FA F2 FC 01 00 11 03 A9 05 65 07 01 08 65 07 A8 05 10 03 00 00 F0 FC 58 FA 9B F8 FF F7 9B F8 57 FA EF FC FF FF 0E 03 A6 05 62 07 FD 07 61 07 A5 05 0D 03 FF FF EF FC 57 FA 9A F8 FE F7 9A F8 56 FA EE FC 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 00 F8 FF 00 9B F8 FF 00 58 FA FF 00 F0 FC FF 00 00 00 00 00 0E 03 00 00 A6 05 00 00 63 07 00 00 FF 07 00 00 63 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 58 FA FF 00 9C F8 FF 00 01 F8 FF 00 9D F8 FF 00 59 FA FF 00 F1 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 64 07 00 00 A8 05 00 00 11 03 00 00 01 00 00 00 F2 FC FF 00 5B FA FF 00 9F F8 FF 00 02 F8 FF 00 9D F8 FF 00 59 FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 62 07 00 00 A6 05 00 00 0E 03 00 00 FF FF FF 00 EF FC FF 00 58 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 F0 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 64 07 00 00 01 08 00 00 65 07 00 00 A9 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F3 FC FF 00 02 00 00 00 11 03 00 00 A8 05 00 00 64 07 00 00 00 08 00 00 64 07 00 00 A7 05 00 00 0F 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F2 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 65 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F0 FC FF 00 58 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 EF FC FF 00 FF FF FF 00 0E 03 00 00 A6 05 00 00 62 07 00 00 FD 07 00 00 61 07 00 00 A5 05 00 00 0D 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9A F8 FF 00 FE F7 FF 00 9A F8 FF 00 56 FA FF 00 EE FC FF 08 00 01 00 0A 00 08 00 01 00 0B 00 07 00 01 00 09 00 06 00 01 00 0A 00 05 00 F6 FF F5 FF F1 FF F6 FF F4 FF EF FF F4 FF F3 FF F0 FF F5 FF F4 FF EE FF F5 FF F3 FF F1 FF F6 FF F6 FF F1 FF F6 FF F5 FF F1 FF F5 FF";
    private String byte1Str = "80 FA FF FF 90 FC FF FF 40 F9 FF FF 70 FA FF FF C0 FB FF FF 50 FD FF FF 60 FB FF FF 40 FE FF FF 90 FE FF FF A0 FC FF FF 10 00 00 00 00 FE FF FF 90 FC FF FF 50 00 00 00 40 FD FF FF 40 FC FF FF 10 FF FF FF 00 FD FF FF F0 FB FF FF D0 FE FF FF 50 FC FF FF D0 FB FF FF 80 FE FF FF 90 FD FF FF 70 FC FF FF E0 FF FF FF C0 FF FF FF 90 FD FF FF 00 02 00 00 A0 FF FF FF 40 FE FF FF 60 03 00 00 00 00 00 00 60 FD FF FF 60 02 00 00 80 FF FF FF 10 FD FF FF 30 01 00 00 70 FD FF FF B0 FC FF FF C0 00 00 00 40 FE FF FF B0 FC FF FF 00 00 00 00 DD FE AA BB 00 01 00 11 03 A9 05 65 07 01 08 65";
    private String byte2Str = "07 A8 05 10 03 00 00 F0 FC 57 FA 9B F8 FF F7 9B F8 58 FA F1 FC 00 00 10 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9B F8 58 FA F0 FC 00 00 10 03 A8 05 64 07 01 08 65 07 AA 05 11 03 00 00 F2 FC 5B FA 9F F8 03 F8 A0 F8 5C FA F4 FC 03 00 13 03 AA 05 66 07 02 08 65 07 A9 05 10 03 00 00 F1 FC 59 FA 9D F8 01 F8 9C F8 58 FA F0 FC 00 00 10 03 A8 05 64 07 00 08 64 07 A8 05 10 03 00 00 F0 FC 57 FA 9B F8 FF F7 9B F8 56 FA EE FC FE FF 0D 03 A5 05 61 07 FD 07 62 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 FE F7 9A F8 57 FA EF FC FF FF 0E 03 A6 05 63 07 FF 07 63";
    private String byte3Str = "07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9D F8 59 FA F1 FC 01 00 11 03 A9 05 64 07 00 08 63 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 00 F8 9C F8 57 FA F0 FC 00 00 10 03 A9 05 65 07 02 08 66 07 AA 05 12 03 01 00 F2 FC 5A FA 9E F8 02 F8 9E F8 5A FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 64 07 A9 05 11 03 02 00 F2 FC 5A FA 9D F8 01 F8 9D F8 58 FA F0 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 10 03 00 00 F0 FC 59 FA 9E F8 02 F8 9D F8 5A FA F2 FC 01 00 11 03 A9 05 65 07 01 08 64 07 A8 05 11 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 59 FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 64 07 A8 05 10 03 00 00 F1 FC 58 FA 9C F8 FF F7 9B F8 57 FA EF FC FE FF 0D 03 A5 05 61 07 FD 07 61 07 A5 05 0E 03 FF FF EF FC 57 FA 9C F8 00 F8 9C F8 57 FA EF FC FF FF 0E 03 A6 05 62 07 FE 07 62 07 A7 05 0F 03 FF FF EF FC 57 FA 9B F8 FF F7 9B F8 57 FA F0 FC 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 00 F8 FF 00 9C F8 FF 00 57 FA FF 00 F0 FC FF 00 00 00 00 00 10 03 00 00 A9 05 00 00 65 07 00 00 02 08 00 00 66 07 00 00 AA 05 00 00 12 03 00 00 01 00 00 00 F2 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 64 07 00 00 A9 05 00 00 11 03 00 00 02 00 00 00 F2 FC FF 00 5A FA FF 00 9D F8 FF 00 01 F8 FF 00 9D F8 FF 00 58 FA FF 00 F0 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 63 07 00 00 A7 05 00 00 10 03 00 00 00 00 00 00 F0 FC FF 00 59 FA FF 00 9E F8 FF 00 02 F8 FF 00 9D F8 FF 00 5A FA FF 00 F2 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 64 07 00 00 A8 05 00 00 11 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 59 FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 64 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 58 FA FF 00 9C F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 EF FC FF 00 FE FF FF 00 0D 03 00 00 A5 05 00 00 61 07 00 00 FD 07 00 00 61 07 00 00 A5 05 00 00 0E 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9C F8 FF 00 00 F8 FF 00 9C F8 FF 00 57 FA FF 00 EF FC FF 00 FF FF FF 00 0E 03 00 00 A6 05 00 00 62 07 00 00 FE 07 00 00 62 07 00 00 A7 05 00 00 0F 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 F0 FC FF FF FF FD FF 02 00 FC FF FC FF FF FF FD FF FD FF FF FF FD FF FD FF 00 00 FF FF FD FF 00 00 FD FF FC FF FF FF FA FF FA FF FC FF FA FF FB FF FC FF F9 FF FA FF FC FF F9 FF FA FF FC FF F7 FF F9 FF FA FF F6 FF F8 FF F8 FF F6 FF F8 FF F8 FF F4 FF F7 FF F6 FF F5 FF F8 FF F8 FF F6 FF F8 FF F9 FF F4 FF F7 FF F7 FF F4 FF F7 FF F6 FF F2 FF F5 FF F4 FF F1 FF F5 FF F4 FF F1 FF F5 FF F4 FF F2 FF F5 FF F3 FF F2 FF F5 FF F3 FF F2 FF F5 FF F3 FF F4 FF F7 FF F7 FF F6 FF F7 FF F7 FF F5 FF F7 FF F7 FF F6 FF F8 FF F8 FF F7 FF F9 FF F9 FF F7 FF F8 FF F9 FF F7 FF F8 FF FA FF FA FF F9 FF FB FF F9 FF F9 FF FA FF F6 FF F9 FF F9 FF F7 FF F8 FF F8 FF F6 FF F8 FF F7 FF F6 FF F7 FF F7 FF F6 FF F7 FF F6 FF F6 FF F8 FF F8 FF F6 FF F9 FF F8 FF F6 FF F8 FF F7 FF F6 FF F7 FF F7 FF F6 FF F8 FF F8 FF F8 FF F9 FF FA FF F9 FF F9 FF FA FF F7 FF F9 FF FA FF F6 FF F8 FF F9 FF F7 FF F9 FF FA FF F7 FF F9 FF F8 FF F6 FF F8 FF F8 FF F7 FF F9 FF F8 FF F5 FF F8 FF F8 FF F6 FF F8 FF F7 FF F5 FF F8 FF F7 FF F4 FF F8 FF F6 FF F3 FF F7 FF F5 FF F2 FF F7 FF F4 FF F1 FF F5 FF F2 FF F0 FF F5 FF F2 FF F1 FF F5 FF F2 FF F0 FF F6 FF F3 FF F2 FF F6 FF F4 FF F3 FF F6 FF F4 FF F2 FF F6 FF F4 FF F3 FF F7 FF F5 FF F2 FF F6 FF F4 FF F2 FF F6 FF F3 FF F2 FF F5 FF F3 FF F0 FF F5 FF F1 FF F1 FF F5 FF F3 FF F4 FF F6 FF F5 FF F2 FF F6 FF F4 FF F2 FF F6 FF F4 FF F3 FF F6 FF F3 FF F2 FF F5 FF F2 FF F2 FF F6 FF F4 FF F3 FF F5 FF F3 FF F3 FF F6 FF F4 FF F2 FF F6 FF F4 FF F0 FF F4 FF F1 FF 20 FC FF FF 90 FB FF FF 80 FD FF FF B0 FB FF FF 70 FB FF FF F0 FD FF FF 80 FC FF FF 80 FB FF FF 50 FE FF FF 50 FD FF FF 90 FC FF FF 90 FF FF FF E0 FE FF FF 70 FD FF FF D0 00 00 00 60 01 00 00 20 FE FF FF D0 02 00 00 30 02 00 00 F0 FD FF FF 10 03 00 00 40 00 00 00 40 FD FF FF A0 01 00 00 10 01 00 00 00 FE FF FF 30 03 00 00 B0 03 00 00 50 FF FF FF 20 05 00 00 50 02 00 00 C0 FE FF FF D0 03 00 00 40 02 00 00 40 FE FF FF C0 03 00 00 70 02 00 00 20 FE FF FF 80 03 00 00 20 01 00 00 E0 FD FF FF 40 02 00 00 50 02 00 00 50 FE FF FF 50 03 00 00 C0 02 00 00 F0 FE FF FF B0 04 00 00 D0 02 00 00 C0 FF FF FF 70 05 00 00 80 03 00 00 50 FF FF FF 50 05 00 00 20 03 00 00 50 FF FF FF D0 05 00 00 30 02 00 00 00 FF FF FF 10 05 00 00 A0 02 00 00 B0 FE FF FF B0 03 00 00 20 03 00 00 A0 FF FF FF F0 04 00 00 90 03 00 00 00 00 00 00 20 06 00 00 70 04 00 00 D0 FF FF FF D0 05 00 00 F0 04 00 00 D0 00 00 00 A0 07 00 00 60 06 00 00 30 01 00 00 A0 08 00 00 60 04 00 00 B0 FF FF FF 10 06 00 00 C0 02 00 00 50 FF FF FF F0 04 00 00 80 03 00 00 70 FF FF FF 10 05 00 00 A0 03 00 00 C0 FF FF FF E0 05 00 00 D0 03 00 00 90 FF FF FF C0 04 00 00 D0 03 00 00 D0 FF FF FF E0 05 00 00 C0 03 00 00 00 FF FF FF 00 05 00 00 80 03 00 00 90 FF FF FF 80 05 00 00 20 03 00 00 30 FF FF FF F0 05 00 00 70 01 00 00 B0 FD FF FF F0 02 00 00 E0 FF FF FF 00 FE FF FF 80 03 00 00 10 00 00 00 D0 FD FF FF 30 02 00 00 50 FF FF FF D0 FD FF FF 20 02 00 00 F0 FF FF FF A0 FD FF FF B0 02 00 00 A0 FF FF FF C0 FD FF FF 60 02 00 00 D0 FC FF FF 60 FC FF FF C0 FF FF FF 90 FD FF FF 20 FD FF FF F0 FF FF FF 90 FD FF FF 70 FD FF FF D0 00 00 00 90 FF FF FF 40 FD FF FF D0 00 00 00 10 FD FF FF B0 FC FF FF B0 FF FF FF 00 FA FF FF A0 FA FF FF D0 FC FF FF 40 FA FF FF 10 FB FF FF E0 FC FF FF 10 F9 FF FF B0 FA FF FF 50 FC FF FF 60 F9 FF FF 60 FA FF FF 50 FC FF FF F0 F7 FF FF 50 F9 FF FF 00 FA FF FF 50 F6 FF FF 80 F8 FF FF 70 F8 FF FF 10 F6 FF FF 70 F8 FF FF 80 F8 FF FF 70 F4 FF FF C0 F7 FF FF E0 F6 FF FF 80 F5 FF FF A0 F8 FF FF D0 F8 FF FF 50 F6 FF FF D0 F8 FF FF 20 F9 FF FF 90 F4 FF FF D0 F7 FF FF 70 F7 FF FF 00 F4 FF FF 20 F7 FF FF 40 F6 FF FF A0 F2 FF FF E0 F5 FF FF 60 F4 FF FF D0 F1 FF FF 70 F5 FF FF 70 F4 FF FF F0 F1 FF FF A0 F5 FF FF 90 F4 FF FF 40 F2 FF FF 80 F5 FF FF E0 F3 FF FF 00 F2 FF FF B0 F5 FF FF 80 F3 FF FF 40 F2 FF FF D0 F5 FF FF F0 F3 FF FF D0 F4 FF FF 30 F7 FF FF 00 F7 FF FF 20 F6 FF FF 80 F7 FF FF 60 F7 FF FF C0 F5 FF FF A0 F7 FF FF 30 F7 FF FF 20 F6 FF FF 80 F8 FF FF F0 F8 FF FF 10 F7 FF FF 00 F9 FF FF 10 F9 FF FF 20 F7 FF FF 90 F8 FF FF 20 F9 FF FF E0 F7 FF FF C0 F8 FF FF 60 FA FF FF 70 FA FF FF E0 F9 FF FF A0 FB FF FF 10 F9 FF FF B0 F9 FF FF 50 FA FF FF B0 F6 FF FF 80 F9 FF FF 30 F9 FF FF 40 F7 FF FF B0 F8 FF FF 50 F8 FF FF C0 F6 FF FF 90 F8 FF FF D0 F7 FF FF 40 F6 FF FF E0 F7 FF FF 70 F7 FF FF 00 F6 FF FF 40 F7 FF FF 70 F6 FF FF 00 F6 FF FF D0 F8 FF FF 00 F8 FF FF 00 F6 FF FF 50 F9 FF FF A0 F8 FF FF 40 F6 FF FF 30 F8 FF FF D0 F7 FF FF 60 F6 FF FF F0 F7 FF FF D0 F7 FF FF 20 F6 FF FF 80 F8 FF FF D0 F8 FF FF E0 F8 FF FF 30 F9 FF FF 80 FA FF FF 80 F9 FF FF 40 F9 FF FF 70 FA FF FF C0 F7 FF FF 10 F9 FF FF 00 FA FF FF A0 F6 FF FF C0 F8 FF FF 70 F9 FF FF 70 F7 FF FF 60 F9 FF FF 10 FA FF FF 40 F7 FF FF 70 F9 FF FF B0 F8 FF FF 50 F6 FF FF E0 F8 FF FF 60 F8 FF FF F0 F7 FF FF 20 F9 FF FF 90 F8 FF FF E0 F5 FF FF 50 F8 FF FF 50 F8 FF FF 60 F6 FF FF 50 F8 FF FF A0 F7 FF FF 20 F5 FF FF F0 F8 FF FF 30 F7 FF FF 30 F4 FF FF 70 F8 FF FF B0 F6 FF FF A0 F3 FF FF E0 F7 FF FF 50 F5 FF FF 80 F2 FF FF 00 F7 FF FF 40 F4 FF FF F0 F1 FF FF 90 F5 FF FF 30 F2 FF FF D0 F0 FF FF F0 F5 FF FF 30 F2 FF FF 40 F1 FF FF E0 F5 FF FF C0 F2 FF FF E0 F0 FF FF 40 F6 FF FF 30 F3 FF FF F0 F2 FF FF B0 F6 FF FF 80 F4 FF FF 90 F3 FF FF D0 F6 FF FF C0 F4 FF FF D0 F2 FF FF B0 F6 FF FF 60 F4 FF FF 60 F3 FF FF 20 F7 FF FF 80 F5 FF FF 80 F2 FF FF 70 F6 FF FF 20 F4 FF FF 20 F2 FF FF 20 F6 FF FF 70 F3 FF FF 10 F2 FF FF D0 F5 FF FF 00 F3 FF FF 70 F0 FF FF 30 F5 FF FF D0 F1 FF FF B0 F1 FF FF F0 F5 FF FF 30 F3 FF FF 30 F4 FF FF 90 F6 FF FF 30 F5 FF FF A0 F2 FF FF 10 F6 FF FF 90 F4 FF FF B0 F2 FF FF 50 F6 FF FF 00 F4 FF FF 60 F3 FF FF A0 F6 FF FF F0 F3 FF FF 00 F2 FF FF 70 F5 FF FF B0 F2 FF FF 40 F2 FF FF 40 F6 FF FF 00 F4 FF FF 10 F3 FF FF C0 F5 FF FF F0 F3 FF FF 70 F3 FF FF 50 F6 FF FF 90 F4 FF FF F0 F2 FF FF 30 F6 FF FF 30 F4 FF FF A0 F0 FF FF D0 F4 FF FF C0 F1 FF FF DD FE AA BB 00 00 00 11 03 A9 05 65 07 01 08 65 07 A9 05 11 03 02 00 F3 FC 5C FA A0 F8 04 F8 9F F8 5A FA F2 FC 01 00 10 03 A8 05 64 07 00 08 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9D F8 58 FA F0 FC 00 00 0F 03 A6 05 63 07 00 08 64 07 A8 05 10 03 01 00 F2 FC 59 FA 9D F8 01 F8 9D F8 59 FA F1 FC 00 00 0F 03 A6 05 62 07 FE 07 62 07 A7 05 0F 03 00 00 F2 FC 5A FA 9E F8 02 F8 9E F8 5A FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 59 FA 9D F8 01 F8 9D F8 59 FA F2 FC 01 00 12 03 AA 05 65 07 00 08 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9C F8 58 FA F0 FC 00 00 0F 03 A7 05 63 07 FF 07 63 07 A7 05 0F 03 00 00 F0 FC 58 FA 9C F8 00 F8 9C F8 58 FA F0 FC 00 00 10 03 A7 05 63 07 FF 07 63 07 A6 05 0E 03 FF FF EF FC 57 FA 9B F8 00 F8 9B F8 58 FA F0 FC 00 00 0E 03 A6 05 63 07 FF 07 63 07 A8 05 10 03 00 00 F1 FC 58 FA 9C F8 01 F8 9D F8 59 FA F1 FC 01 00 11 03 A9 05 65 07 01 08 64 07 A8 05 11 03 01 00 F2 FC 5B FA 9F F8 02 F8 9D F8 59 FA F1 FC 00 00 0F 03 A7 05 63 07 FF 07 62 07 A6 05 0E 03 FF FF EF FC 58 FA 9B F8 FF F7 9B F8 57 FA F0 FC 00 00 0F 03 A7 05 64 07 01 08 65 07 A9 05 10 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 5A FA F3 FC 02 00 11 03 A8 05 64 07 00 08 64 07 A7 05 0F 03 00 00 F1 FC 5A FA 9E F8 02 F8 9E F8 5A FA F2 FC 01 00 11 03 A9 05 65 07 01 08 65 07 A8 05 10 03 00 00 F0 FC 58 FA 9B F8 FF F7 9B F8 57 FA EF FC FF FF 0E 03 A6 05 62 07 FD 07 61 07 A5 05 0D 03 FF FF EF FC 57 FA 9A F8 FE F7 9A F8 56 FA EE FC 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9B F8 FF 00 00 F8 FF 00 9B F8 FF 00 58 FA FF 00 F0 FC FF 00 00 00 00 00 0E 03 00 00 A6 05 00 00 63 07 00 00 FF 07 00 00 63 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 58 FA FF 00 9C F8 FF 00 01 F8 FF 00 9D F8 FF 00 59 FA FF 00 F1 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 64 07 00 00 A8 05 00 00 11 03 00 00 01 00 00 00 F2 FC FF 00 5B FA FF 00 9F F8 FF 00 02 F8 FF 00 9D F8 FF 00 59 FA FF 00 F1 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 63 07 00 00 FF 07 00 00 62 07 00 00 A6 05 00 00 0E 03 00 00 FF FF FF 00 EF FC FF 00 58 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 F0 FC FF 00 00 00 00 00 0F 03 00 00 A7 05 00 00 64 07 00 00 01 08 00 00 65 07 00 00 A9 05 00 00 10 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F3 FC FF 00 02 00 00 00 11 03 00 00 A8 05 00 00 64 07 00 00 00 08 00 00 64 07 00 00 A7 05 00 00 0F 03 00 00 00 00 00 00 F1 FC FF 00 5A FA FF 00 9E F8 FF 00 02 F8 FF 00 9E F8 FF 00 5A FA FF 00 F2 FC FF 00 01 00 00 00 11 03 00 00 A9 05 00 00 65 07 00 00 01 08 00 00 65 07 00 00 A8 05 00 00 10 03 00 00 00 00 00 00 F0 FC FF 00 58 FA FF 00 9B F8 FF 00 FF F7 FF 00 9B F8 FF 00 57 FA FF 00 EF FC FF 00 FF FF FF 00 0E 03 00 00 A6 05 00 00 62 07 00 00 FD 07 00 00 61 07 00 00 A5 05 00 00 0D 03 00 00 FF FF FF 00 EF FC FF 00 57 FA FF 00 9A F8 FF 00 FE F7 FF 00 9A F8 FF 00 56 FA FF 00 EE FC FF 08 00 01 00 0A 00 08 00 01 00 0B 00 07 00 01 00 09 00 06 00 01 00 0A 00 05 00 F6 FF F5 FF F1 FF F6 FF F4 FF EF FF F4 FF F3 FF F0 FF F5 FF F4 FF EE FF F5 FF F3 FF F1 FF F6 FF F6 FF F1 FF F6 FF F5 FF F1 FF F5 FF";

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //启动服务发现
            } else {
                isConnected = false;
                closeConn();
            }
            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), dev));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) { //BLE服务发现成功
                // 遍历获取BLE服务Services/Characteristics/Descriptors的全部UUID
                for (BluetoothGattService service : gatt.getServices()) {
                    StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + service.getUuid().toString());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        allUUIDs.append(",\nC=").append(characteristic.getUuid());
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
                            allUUIDs.append(",\nD=").append(descriptor.getUuid());
                    }
                    allUUIDs.append("}");
                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
                    logTv("发现服务" + allUUIDs);
                }
                bleRequestMTU(512);
                super.onServicesDiscovered(gatt, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("读取Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            int dataNum = 0;
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));

            byte[] data = characteristic.getValue();
            String dataStr = byteArrayToHexString(data);
//            int dataLen = dataStr.length();
            String countStr = "";
            boolean hasHander = false;
            boolean hasEnd = false;
            if (valueStr.contains("rf")) {
                // 存在针头
                hasHander = true;
                // 计算多少个分包
                bagCount += 1;
                // 取出文件总大小
                int count7 = data[6];
                int count8 = data[7];
                int count9 = data[8];
                int count10 = data[9];
                String count = String.valueOf(count7) + String.valueOf(count8)+ String.valueOf(count9)+ String.valueOf(count10);

                dataFileLen = Integer.parseInt(count);
                // 取出总文件个数
                fileCount = data[3];
//                fileIndex = data[2];

                // 取出指令
                byte1 = data[0];
                byte2 = data[1];
                byte4 = data[5];
                byte5 = data[6];

            }

            if (dataStr.contains(endByteStr)) {
                // 存在针尾
                hasEnd = true;
            }
            logTv("通知data:\n [" + data.length +"]\n ["+ dataStr +"]");

            if (data.length > 0) {
                if(dataStr.length() == 6 && dataStr.contains(endByteStr)) {
                    // 完成数据接收
                    String Str = ByteArrayToWav.toWav(mReceicedData, 0);
                    logTv(Str+mReceicedData.length+"-----"+reciveDataLen);
                }else  {
                    handleNotifyWithRecieveData(data, hasHander, hasEnd);
//                    setRecievieData(data);
                }

            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//            sleepAcceptData();
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);
        RecyclerView rv = findViewById(R.id.rv_ble);
        mWriteET = findViewById(R.id.et_write);
        mTips = findViewById(R.id.tv_tips);
//        mWriteET.setText();
//        recordBtn = findViewById(R.id.btn_play);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mBleDevAdapter = new BleDevAdapter(new BleDevAdapter.Listener() {
            @Override
            public void onItemClick(BluetoothDevice dev) {
                closeConn();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mBluetoothGatt = dev.connectGatt(BleClientActivity.this,
                            true, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    mBluetoothGatt = dev.connectGatt(BleClientActivity.this,
                            true, mBluetoothGattCallback);
                }
//                mBluetoothGatt = dev.connectGatt(BleClientActivity.this, true, mBluetoothGattCallback); // 连接蓝牙设备
                logTv(String.format("与[%s]开始连接............", dev));
            }
        });
        rv.setAdapter(mBleDevAdapter);

    }

    // 接收分包数据流测试
    private void combineTest() {
        byte[] byte1 = StringToByteArray(byte1Str);
        byte[] byte2 = StringToByteArray(byte2Str);
        // 合并数据
        if (mReceicedData == null) {
            mReceicedData = byte1;
        }
        logTv(byteArrayToHexString(mReceicedData));
        byte[] newByte = MLByteArray.combineAarry(mReceicedData, byte2);
        mReceicedData = newByte;
        logTv(byteArrayToHexString(mReceicedData));
        logTv("recordedByteStr"+recordedByteStr.length());
    }

    // 接收的pcm流数据转wav文件测试
    private void pcmToWavTest() {
        byte[] byteRe = StringToByteArray(recordedByteStr);
        mReceicedData = byteRe;
        for (int i = 0; i<3; i++) {
            byte[] newByteA = MLByteArray.combineAarry(mReceicedData, mReceicedData);
            mReceicedData = newByteA;
        }
        logTv("\n"+byteArrayToHexString(mReceicedData).length());
        String path = MLByteArray.path();
        MLByteArray.writeFile(path,mReceicedData);
        String Str = ByteArrayToWav.toWav(mReceicedData,0);
        logTv(Str);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConn();
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    // 扫描BLE
    public void reScan(View view) {
        if (mBleDevAdapter.isScanning)
            APP.toast("正在扫描...", 0);
        else
            mBleDevAdapter.reScan();
    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 读取数据成功会回调->onCharacteristicChanged()
    public void read(View view) throws IOException {
        readData();
    }

    private void readData() throws IOException {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可读的Characteristic
            mBluetoothGatt.readCharacteristic(characteristic);
        }

    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
    public void write(View view) {
//        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
//        if (service != null) {
//            String text = mWriteET.getText().toString();
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
//            characteristic.setValue(text.getBytes()); //单次最多20个字节
//            mBluetoothGatt.writeCharacteristic(characteristic);
//        }
        WriteAudioBytesToService(0);
    }

    // 设置通知Characteristic变化会回调->onCharacteristicChanged()
    public void setNotify(View view) {
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null && mBluetoothGatt != null) {
            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_READ_NOTIFY);//通过UUID获取可通知的Characteristic
            setCharacteristicNotification(mBluetoothGatt, characteristic, true);
//            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
//
//            // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleServerActivity.UUID_DESC_NOTITY);
//            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知类似,但服务端不主动发数据,只指示客户端读取数据
//            if (descriptor != null) {
//                byte[] notifyValue = new byte[] {0x01, 0x00};
//                descriptor.setValue(notifyValue);
//                mBluetoothGatt.writeDescriptor(descriptor);
//            }

        }
    }

    public boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, boolean enable) {

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleServerActivity.UUID_DESC_NOTITY);
        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        gatt.setCharacteristicNotification(characteristic, enable);
        return gatt.writeDescriptor(descriptor);
    }


    // 获取Gatt服务
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("没有找到服务UUID=" + uuid, 0);
        return service;
    }

    // 输出日志
    private void logTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
                mTips.append(msg + "\n\n");
            }
        });
    }

//    private

    private void pcmToWac(String fileName) {
        String storeFileName;
        if (fileName.contains("wav")) {
            storeFileName = fileName;
        }
        storeFileName = fileName + ".wav";
        PcmToWavUtil.getInstance().pcmToWav(fileName, storeFileName);
    }

    // 申请MTU分包
    private void bleRequestMTU(int size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothGatt.requestMtu(size);
        }
    }

    // 处理接收的byte数据流
    private void handleNotifyWithRecieveData(byte[] data, boolean hasHander, boolean hasEnd) {

        reciveDataLen += data.length;
        if (hasHander) {
            byte[] newData = clearHander(data);
            setRecievieData(newData);
        }else if (hasEnd) {
            byte[] newData = clearEnd(data);
            setRecievieData(newData);
        }else if (hasHander && hasEnd) {
            byte[] data1 = clearHander(data);
            byte[] newData = clearEnd(data1);
            setRecievieData(newData);
        }else {
            setRecievieData(data);
        }

        if (mReceicedData.length > lenMax) {
            // 清除旧数据
            ByteArrayToWav.clearFileData();
            // 完成数据接收
            String Str = ByteArrayToWav.toWav(mReceicedData, 0);
            logTv(Str+mReceicedData.length+"-----"+reciveDataLen);
            return;
        }
        // 继续接收数据
        if (reciveDataLen >= (4000+23)*bagCount) {
            WriteAudioBytesToService(0);
        }else if (data.length < 256) {
            WriteAudioBytesToService(0);
        }


//        if (fileCount == 1) { // 一个文件
//            // 继续接收数据
//            if (reciveDataLen >= (4000+23)*bagCount) {
//                WriteAudioBytesToService(1);
//            }else if (mReceicedData.length == dataFileLen) {
//                // 数据接收完成
//                String Str = ByteArrayToWav.toWav(mReceicedData, 0);
//                logTv(Str+mReceicedData.length+"-----"+reciveDataLen);
//            }
//        }else { // 跨文件
//            if (fileCount == fileIndex) {
//                // 最后一个文件
//                if (mReceicedData.length == (preDataFileLen + dataFileLen)){
                    // 文件的最后一个流数据,数据接收完成
//                    String Str = ByteArrayToWav.toWav(mReceicedData, 0);
//                    logTv(Str+mReceicedData.length+"-----"+reciveDataLen);
//                }else if (reciveDataLen >= ((4000+23)*bagCount + preDataFileLen)) {
//                    WriteAudioBytesToService(1);
//                }

//            }else {
//                if (mReceicedData.length == (preDataFileLen + dataFileLen)){
//                    // 文件的最后一个流数据
//                    preDataFileLen = dataFileLen;
//                    //重置bagCount
//                    bagCount = 0;
//                    WriteAudioBytesToService(1);
//                }else if (reciveDataLen >= ((4000+23)*bagCount + preDataFileLen)) {
//                    fileIndex += 1;
//                    WriteAudioBytesToService(1);
//                }
//            }
//        }
    }

    // 去除针头
    private byte[] clearHander(byte[] data) {
        // 去除针头
        int startOffset = 19;
        int len = data.length;
        return Arrays.copyOfRange(data, startOffset, len);
    }

    // 去除针尾
    private byte[] clearEnd(byte[] data) {
        // 去除针头
        int endOffset = 4;
        int len = data.length;
        return Arrays.copyOfRange(data, len - 4, len);
    }

    // 赋值或合并数据
    private void setRecievieData(byte[] data) {
        if (mReceicedData == null) {
            // 清除旧文件数据
            ByteArrayToWav.clearFileData();
            mReceicedData = data;
        }else {
            byte[] newByte = MLByteArray.combineAarry(mReceicedData, data);
            mReceicedData = newByte;
            logTv("通知mReceicedData[" + mReceicedData.length + "]:\n");
        }
    }

    // byte[]写入文件
    private void writeToFile(byte[] data) {
        String wavFile = "output.wav";
        MLByteWavConverter.convertToWav(data, wavFile);
    }

    // 判断是否接收完成
    private boolean isCompletedRecieved() {
        if (mReceicedData == null) {
            return false;
        }
        int packetLen = ByteBuffer.wrap(mReceicedData).order(ByteOrder.LITTLE_ENDIAN).getInt();
        logTv("通知Characteristic[" + mReceicedData.length + "]:\n" + (packetLen+4));
        return mReceicedData.length == packetLen + 4;
    }

    private void recieveData(byte[] data) {
        int offset = 0;
        int fileStartOffset = 6; // 获取文件大小的开始位
        int endOffset = data.length - 4; // 获取文件大小的结束位
        while (offset < data.length) {
            // 获取数据包的header（前4个字节）
            byte[] header = Arrays.copyOfRange(data, offset, offset + 4);
            int packetLen = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // 获取数据包的end（后4个字节）
            byte[] end = Arrays.copyOfRange(data, endOffset, endOffset + 4);;
            // 文件大小
            byte[] dataCountByte = Arrays.copyOfRange(data, fileStartOffset, fileStartOffset + 4);
            String dataCountStr = dataCountByte.toString();

            // 接收的音频数据流
//            mReceicedData = Arrays.copyOfRange(data, fileOffset, fileOffset + 4);

                // 如果当前数据包不完整，等待下一个包到达
                if (offset + packetLen >= data.length) {
                    // 继续请求数据
                    WriteAudioBytesToService(1);
//                    break;
                }

            logTv("mReceicedData" + String.format("d%", offset));

            // 将完整的数据包存储到mReceicedData
            byte[] packet = Arrays.copyOfRange(data, offset + 4, offset + packetLen);
            mReceicedData = MLByteArray.combineAarry(mReceicedData, data);
            // 处理下一个数据包
            offset += packetLen;
            logTv("mReceicedData" + String.format("d%", offset));
        }
    }


    // 播放录音
    private void playWithByteData(byte[] data) {
       if (MLAudioPlayer.getInstance().startPlayer()) {
           MLAudioPlayer.getInstance().play(data,0,data.length);
       }
    }

    // 接收的数据btye[]
    public static String byteArrayToHexString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // String转byte[]
    private byte[] StringToByteArray(String str) {
        // 获取ASCII值的字节数组
        byte[] asciiBytes = str.getBytes();

        // 打印ASCII值的十六进制表示
        logTv(byteArrayToHexString(asciiBytes));

        return asciiBytes;
    }

    // 开始录音或结束录音
    public void startRecord(View view) {
        if (!isRecording) {
            checkPhoneAudioIsUse();
        }else {
            Button bt = (Button) view;
            bt.setText("录音");
            AudioRecordUtil.getInstance().stop();
            logTv("结束录音"+new File(AudioRecordUtil.path()).length());
            isRecording = !isRecording;

//            new Delayed(pcmToWav(),1000);
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    pcmToWav();
                }
            }, 100, TimeUnit.SECONDS);
            executor.shutdown();;
        }
    }

    private void pcmToWav() {
        // 转换成wav文件
        PcmToWavUtil.getInstance().pcmToWav(AudioRecordUtil.path(),AudioRecordUtil.wavPath());
//        File file = new File(AudioRecordUtil.wavPath());
//        logTv(file.getName()+file.length());
    }

    // 开始录音
    private void startRecording() {
        logTv("开始录音......");
//        recordBtn.setText("结束");
        AudioRecordUtil.getInstance().start(AudioRecordUtil.path());

        isRecording = !isRecording;
    }

    // 检测设备麦克风是否授权使用
    private void checkPhoneAudioIsUse() {
        if (!checkMicrophonePermission()) {
            // 没开启麦克风权限，向用户请求权限
            requestMicrophonePermission();
        }else {
            // 如果已经有麦克风权限，获取麦克风状态
            getMicrophoneStatus();
            startRecording();
        }
    }

    // 定义麦克风权限常量
    private static final String MICROPHONE_PERMISSION = Manifest.permission.RECORD_AUDIO;
    private static final int PERMISSION_REQUEST_CODE = 1;

    // 检查麦克风授权
    private boolean checkMicrophonePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, MICROPHONE_PERMISSION);
        return (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

    // 请求麦克风授权
    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, MICROPHONE_PERMISSION)) {
            // 用户已经拒绝过权限，可以在这里向用户解释为什么需要这个权限
            Toast.makeText(this, "需要打开录音权限才能使用录音功能", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{MICROPHONE_PERMISSION}, PERMISSION_REQUEST_CODE);
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 授权成功，可以继续使用AudioRecord
                getMicrophoneStatus();
            } else {
                // 授权失败，可以提示用户或者采取其他措施
            }
        }
    }

    private void getMicrophoneStatus() {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            boolean isMicrophoneMute = audioManager.isMicrophoneMute();

            if (isMicrophoneMute) {
                // 麦克风处于静音状态
                logTv("麦克风处于静音状态");
            } else {
                // 麦克风未处于静音状态
                logTv("麦克风未处于静音状态");
            }
    }

    public void play(View view) {
//        play(AudioRecordUtil.wavPath());
        play(ByteArrayToWav.path());
    }

    // pcm转wav文件播放
    private void play(String path) {
        File audioFile = new File(path);
        logTv("_______"+ audioFile.length() +"_______");

//        mediaPlayer = new MediaPlayer.create(this, Uri.fromFile(audioFile));
//        mediaPlayer.start();
        mediaPlayer = new MediaPlayer();
        try {
//            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
    }

    private void WriteAudioBytesToService(int index) {
        String rfSting = "rf1(`14096";
//        sendData = new byte[]{byte1, byte2, (byte) fileIndex, 0x28, 0x60};
        BluetoothGattService service = getGattService(BleServerActivity.UUID_SERVICE);
        if (service != null) {
            String text = rfSting;
            byte[] sendDtaBytes;
            if (index == 0) {
                sendDtaBytes = text.getBytes();
            }else {
                sendDtaBytes = sendData;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleServerActivity.UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
            characteristic.setValue(sendDtaBytes); //单次最多20个字节
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

}