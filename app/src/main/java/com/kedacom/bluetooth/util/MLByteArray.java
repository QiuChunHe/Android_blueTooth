package com.kedacom.bluetooth.util;

import android.os.Environment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MLByteArray {

    public static byte[] merge(byte[] array1, byte[] array2) {
        ByteBuffer buffer = ByteBuffer.allocate(array1.length + array2.length);
        buffer.put(array1);
        buffer.put(array2);
        return buffer.array();
    }

    // 合并数据byte[]
    public static byte[] combineAarry(byte[] byte1, byte[] byte2) {
        if (byte1 == null && byte2 == null) {
            return null;
        }

        int size = 0;
        if (byte1 == null) {
            size = byte2.length;
            byte[] byteNew = new byte[size];
            System.arraycopy(byte2, 0, byteNew, 0, size);
            return byteNew;
        } else if (byte2 == null) {
            size = byte1.length;
            byte[] byteNew = new byte[size];
            System.arraycopy(byte1, 0, byteNew, 0, size);
            return byteNew;
        } else {
            size = byte1.length + byte2.length;
            byte[] byteNew = new byte[size];
            System.arraycopy(byte1, 0, byteNew, 0, byte1.length);
            System.arraycopy(byte2, 0, byteNew, byte1.length, byte2.length);
            return byteNew;
        }
    }

    // 十六进制字符串转byte[]
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.trim();
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 把数据流写入文件
     * @param path
     * @param bytes
     */
    public static void writeFile(String path, byte[] bytes) {
        try {
            FileOutputStream out = new FileOutputStream(path);//指定写到哪个路径中
            FileChannel fileChannel = out.getChannel();
            fileChannel.write(ByteBuffer.wrap(bytes)); //将字节流写入文件中
            fileChannel.force(true);//强制刷新
            fileChannel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String path() {
        //保持文件的路径
        String path =  Environment.getExternalStorageDirectory()+"/recieveAudioData.wav" ;
        return path;
    }

}
