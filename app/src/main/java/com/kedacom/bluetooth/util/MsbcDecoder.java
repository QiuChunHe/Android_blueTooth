package com.kedacom.bluetooth.util;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class MsbcDecoder {

    private MediaCodec mediaCodec;
    private boolean isDecoderConfigured = false;

    public void configureDecoder() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 8000, 1);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 320);
            mediaCodec.configure(mediaFormat, null, null, 0);
            mediaCodec.start();
            isDecoderConfigured = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseDecoder() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    public void decodeMsbc(byte[] inputBuffer, int size) {
        if (!isDecoderConfigured) {
            configureDecoder();
        }

        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer1 = inputBuffers[inputBufferIndex];
            inputBuffer1.clear();
            inputBuffer1.put(inputBuffer, 0, size);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            // 处理解码后的数据，例如写入文件或者送入音频输出
            // ...

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}

