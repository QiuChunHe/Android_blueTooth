package com.kedacom.bluetooth.util;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class AudioRecordUtil {

    //设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    //设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_IN_MONO为单声道
    //音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。

    public static final int sampleRateInHz = 44100;
    public static final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    // public static final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    public static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private boolean isRecording = false;

    //录制状态
    private boolean recorderState = true;
    private byte[] buffer;
    private AudioRecord audioRecord;
    private MediaPlayer mediaPlayer;
    private static AudioRecordUtil audioRecordUtil = new AudioRecordUtil();
    private String TAG = "AudioRecordUtil";

    public static AudioRecordUtil getInstance() {
        return audioRecordUtil;
    }

    private AudioRecordUtil() {
        init();
    }

    @SuppressLint("MissingPermission")
    private void init() {
        int recordMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        //指定 AudioRecord 缓冲区大小
        buffer = new byte[recordMinBufferSize];
        //根据录音参数构造AudioRecord实体对象
        if(audioRecord == null){
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
                audioFormat, recordMinBufferSize);
        }
//        MediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mediaPlayer = new MediaPlayer();

    }

    /**
     * 开始录制
     */
    public void start(String path) {
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_STOPPED) {
            recorderState = true;
            RecordThread th = new RecordThread(path);
            th.start();
        } else {
            Log.i(TAG, "start: " + audioRecord.getState());
        }
    }

    /**
     * 停止录制
     */
    public void stop() {
        recorderState = false;
//        if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
//        }

    }


    public void release() {
        recorderState = false;
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
        audioRecord = null;
    }

    private class RecordThread extends Thread {


        private String cachePath;
        private String name;
        private String path;


        public RecordThread(String path) {
//            this.path = path;
//            this.name = name;
            this.cachePath = path;
//                    + "cache.pcm";
        }

        @Override
        public void run() {

            Log.i(TAG, "run: pcm目录=path" + cachePath);
            File cacheFile = new File(cachePath);
            if (cacheFile.length() > 0) {
                cacheFile.delete();
            }
            File pcmFile = new  File(cachePath);
//                    +"cache.pcm");
            boolean file = false;
            try {
                file = pcmFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (file)
                Log.i(TAG, "run: 创建缓存文件成功:" + cachePath);
            else {
                Log.i(TAG, "run: 创建缓存文件失败:" + cachePath);
                return;
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(cachePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();

            }

            if (fos == null) {
                Log.i(TAG, "run: 未找到缓存文件" + cachePath);
                return;
            }

            //获取到的pcm数据就是buffer
            int read;
            while (recorderState && !isInterrupted()) {
                read = audioRecord.read(buffer, 0, buffer.length);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {

                    try {
                        fos.write(buffer);
                        Log.i(TAG, "run: 写录音数据->" + read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

//            PcmToWavUtil.getInstance().pcmToWav(cachePath, path + name);

        }
    }


    public byte[] convert(String path) throws IOException {

        FileInputStream fis = new FileInputStream(path);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];

        for (int readNum; (readNum = fis.read(b)) != -1; ) {
            bos.write(b, 0, readNum);
        }

        byte[] bytes = bos.toByteArray();

        fis.close();
        bos.close();
        return bytes;
    }

//    public void startRecording() {
//        audioRecord.startRecording();
//        isRecording = true;

//        Thread audioRecordThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                byte[] buffer = new byte[4096];
//                FileOutputStream outputStream = null;
//                try {
//                    outputStream = new FileOutputStream(path());
//                } catch (FileNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//
//                try {
//                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
//                    outputStream.write(buffer, 0, bytesRead);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    try {
//                        outputStream.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        audioRecordThread.start();
//    }

//    public void stopRecording() {
//        audioRecord.stop();
//        isRecording = false;
//    }

    // 播放
    public static void playAudio() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(wavPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // After playing, release the MediaPlayer
//            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 录音文件的路径
    public static String path() {
        String path =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio.pcm" ;
        return path;
    }

    // 录音文件的路径
    public static String wavPath() {
        String path =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/audio.wav" ;
        return path;
    }

    // 清除旧数据
    public static void clearFileData() {
        File wavF = new File(wavPath());
        File pcmF = new File(path());
    }

    // 获取录音文件的大小
    public static long getPcmFileCount() {
        File pcmFile = new  File(path());
        return pcmFile.length();
    }

}



