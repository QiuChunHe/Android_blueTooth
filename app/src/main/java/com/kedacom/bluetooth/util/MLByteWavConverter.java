package com.kedacom.bluetooth.util;
import java.io.*;

public class MLByteWavConverter {

    public static void convertToWav(byte[] audioData, String filePath) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

            // 添加WAV文件头
            dataOutputStream.writeBytes("RIFF");
            dataOutputStream.writeInt(36 + audioData.length);
            dataOutputStream.writeBytes("WAVE");
            dataOutputStream.writeBytes("fmt ");
            dataOutputStream.writeInt(16);
            dataOutputStream.writeShort(1);
            dataOutputStream.writeShort(1);
            dataOutputStream.writeInt(44100);
            dataOutputStream.writeInt(44100 * 2);
            dataOutputStream.writeShort(2);
            dataOutputStream.writeShort(16);
            dataOutputStream.writeBytes("data");
            dataOutputStream.writeInt(audioData.length);

            // 写入音频数据
            dataOutputStream.write(audioData);

            // 写入文件
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            byteArrayOutputStream.writeTo(fileOutputStream);

            dataOutputStream.close();
            fileOutputStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        byte[] audioData =  ;
//
//    String filePath = "output.wav";
//    convertToWav(audioData, filePath);
//    }

}
