package com.kedacom.bluetooth.util;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ByteArrayToWav {

    public static String toWav(byte[] audioData, int num) {
        String outputFilePath = path();
        String successStr = "111";
        try {
//            byte[] audioData = {0x52, 0x49, 0x46, 0x46, 0x24, 0x08, 0x00, 0x00, /*...*/};

            writeWavFile(audioData, outputFilePath);
            System.out.println("WAV file generated successfully.");
            successStr = "WAV file generated successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            successStr = "fail";
        }
        return successStr;

    }

    private static void writeWavFile(byte[] audioData, String outputFilePath) throws IOException {
        try (FileOutputStream wavFile = new FileOutputStream(outputFilePath)) {
            // 写入WAV文件头
            writeWavHeader(wavFile, audioData.length);

            // 写入音频数据
            wavFile.write(audioData);
        }
    }

    private static void writeWavHeader(FileOutputStream wavFile, int dataSize) throws IOException {
        // WAV 文件格式头
        wavFile.write("RIFF".getBytes());
        wavFile.write(intToByteArray(36 + dataSize)); // Chunk size
        wavFile.write("WAVEfmt ".getBytes());
        wavFile.write(intToByteArray(16)); // Subchunk1 size
        wavFile.write(shortToByteArray((short) 1)); // Audio format (1 for PCM)
        wavFile.write(shortToByteArray((short) 1)); // Number of channels
        wavFile.write(intToByteArray(44100)); // Sample rate (e.g., 44.1kHz)
        wavFile.write(intToByteArray(44100 * 2)); // Byte rate (Sample rate * Number of channels * Bits per sample / 8)
        wavFile.write(shortToByteArray((short) 2)); // Block align (Number of channels * Bits per sample / 8)
        wavFile.write(shortToByteArray((short) 16)); // Bits per sample
        wavFile.write("data".getBytes());
        wavFile.write(intToByteArray(dataSize)); // Subchunk2 size
    }

    private static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value),
                (byte) (value >> 8),
                (byte) (value >> 16),
                (byte) (value >> 24)
        };
    }

    private static byte[] shortToByteArray(short value) {
        return new byte[]{
                (byte) (value),
                (byte) (value >> 8)
        };
    }

    public static String path() {
        //保持文件的路径
        String path =  Environment.getExternalStorageDirectory()+"/audioRecordData.mp3" ;
        return path;
    }

    public static void clearFileData() {
        File cacheFile = new File(path());
        if (cacheFile.length() > 0) {
            cacheFile.delete();
        }
    }

}

