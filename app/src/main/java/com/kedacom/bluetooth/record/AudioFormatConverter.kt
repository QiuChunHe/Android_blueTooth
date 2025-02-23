package com.kedacom.bluetooth.record
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by TanJiaJun on 2024/4/8.
 */
object AudioFormatConverter {

    /**
     * 将PCM文件转换为WAV文件
     *
     * @param inputPCMFilePath 输入的PCM文件路径
     * @param outputWAVFilePath 输出的WAV文件路径
     * @param sampleRateInHz 采样率，单位：频率
     * @param bitDepth 位深度
     * @param channelCount 声道数
     * @return WAV文件
     */
    @JvmStatic
    suspend fun convertPCMToWAV(
        // 要转换的文件路径
        inputPCMFilePath: String,
        // 转换成pcm文件的保存路径
        outputWAVFilePath: String,
        sampleRateInHz: Int,
        bitDepth: Int,
        channelCount: Int
    ): File? {
        if (inputPCMFilePath.isEmpty() || outputWAVFilePath.isEmpty()) {
            return null
        }
//        return withIO {
            val outputWAVFile = File(outputWAVFilePath)
            if (!outputWAVFile.exists()) {
                outputWAVFile.parentFile?.mkdirs()
                outputWAVFile.createNewFile()
            }
            FileInputStream(inputPCMFilePath).use { fileInputStream ->
                FileOutputStream(outputWAVFilePath).use { fileOutputStream ->
                    BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                        val totalAudioSize = fileInputStream.channel.size()
                        // 写入WAV文件头
                        writeWAVFileHeader(
                            bufferedOutputStream,
                            totalAudioSize,
                            sampleRateInHz,
                            bitDepth,
                            channelCount
                        )
                        // Data：音频数据
                        BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (bufferedInputStream.read(buffer).also { length = it } > 0) {
                                bufferedOutputStream.write(buffer, 0, length)
                            }
                        }
                    }
                }
            }
           return outputWAVFile
//        }
    }

    /**
     * 把WAV文件头写入缓冲输出流
     *
     * @param bufferedOutputStream 缓冲输出流
     * @param totalAudioSize 整个音频PCM数据大小
     * @param sampleRateInHz 采样率，单位：频率
     * @param bitDepth 位深度
     * @param channelCount 声道数
     * @throws IOException IO异常
     */
    @Throws(IOException::class)
    private fun writeWAVFileHeader(
        bufferedOutputStream: BufferedOutputStream,
        totalAudioSize: Long,
        sampleRateInHz: Int,
        bitDepth: Int,
        channelCount: Int
    ) {
        val header: ByteArray = getWAVHeader(totalAudioSize, sampleRateInHz, bitDepth, channelCount)
        bufferedOutputStream.write(header, 0, 44)
    }

    /**
     * 获取WAV文件头
     *
     * @param totalAudioSize 音频数据的大小
     * @param sampleRateInHz 采样率，单位：频率
     * @param bitDepth 位深度
     * @param channelCount 声道数
     * @return 字节数组
     * @throws IOException IO异常
     */
    @Throws(IOException::class)
    private fun getWAVHeader(
        totalAudioSize: Long,
        sampleRateInHz: Int,
        bitDepth: Int,
        channelCount: Int
    ): ByteArray {
        val header = ByteArray(44)
        // ChunkID：字母“RIFF”
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        /**
         * 总数据大小：36+Subchunk2Size（值为PCM文件大小，也就是totalAudioSize），更准确地说就是
         * 4 + (8 + Subchunk1Size（值为16）) + (8 + Subchunk2Size（值为PCM文件大小，也就是totalAudioSize）)
         */
        val totalDataSize = 36 + totalAudioSize
        // ChunkSize：总数据大小
        header[4] = (totalDataSize and 0xff).toByte()
        header[5] = (totalDataSize shr 8 and 0xff).toByte()
        header[6] = (totalDataSize shr 16 and 0xff).toByte()
        header[7] = (totalDataSize shr 24 and 0xff).toByte()
        // Format：字母“WAVE”
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // Subchunk1ID：字符“fmt ”，要注意的是，最后是一位空格
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // Subchunk1Size：如果是PCM，值为16
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat：如果是PCM，值为1，表示线性量化
        header[20] = 1
        header[21] = 0
        // NumChannels：声道数
        header[22] = channelCount.toByte()
        header[23] = 0
        // SampleRate：采样率
        header[24] = (sampleRateInHz and 0xff).toByte()
        header[25] = (sampleRateInHz shr 8 and 0xff).toByte()
        header[26] = (sampleRateInHz shr 16 and 0xff).toByte()
        header[27] = (sampleRateInHz shr 24 and 0xff).toByte()
        // 字节率：采样率 * 位深度 / 8 * 声道数
        val byteRate: Long = (sampleRateInHz * bitDepth / 8 * channelCount).toLong()
        // ByteRate：字节率
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // 每次采样的大小：声道数 * 位深度 / 8
        val blockAlign: Int = channelCount * bitDepth / 8
        // BlockAlign：每次采样的大小
        header[32] = blockAlign.toByte()
        header[33] = 0
        // BitsPerSample：每个采样的位数
        header[34] = 16
        header[35] = 0
        // Subchunk2ID：字母“data”
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        // Subchunk2Size：音频数据的大小
        header[40] = (totalAudioSize and 0xff).toByte()
        header[41] = (totalAudioSize shr 8 and 0xff).toByte()
        header[42] = (totalAudioSize shr 16 and 0xff).toByte()
        header[43] = (totalAudioSize shr 24 and 0xff).toByte()
        return header
    }

    /**
     * 将PCM文件编码为MP3文件
     *
     * @param inputPCMFilePath 输入的PCM文件路径
     * @param outputMP3FilePath 输出的MP3文件路径
     * @param sampleRateInHz 采样率，单位：赫兹
     * @param bitDepth 位深度
     * @param channelCount 声道数
     * @return MP3文件
     */
//    suspend fun encodePCMToMP3(
//        inputPCMFilePath: String,
//        outputMP3FilePath: String,
//        sampleRateInHz: Int,
//        bitDepth: Int,
//        channelCount: Int
//    ): File? {
//        if (inputPCMFilePath.isEmpty() || outputMP3FilePath.isEmpty()) {
//            return null
//        }
//        return withIO {
//            val outputMP3File = File(outputMP3FilePath)
//            if (!outputMP3File.exists()) {
//                outputMP3File.parentFile?.mkdirs()
//                outputMP3File.createNewFile()
//            }
//            val result: Boolean = LAMEUtils.init(
//                inputPCMFilePath = inputPCMFilePath,
//                outputMP3FilePath = outputMP3FilePath,
//                sampleRateInHz = sampleRateInHz,
//                channelCount = channelCount,
//                bitRate = AudioUtils.getBitRate(sampleRateInHz, bitDepth, channelCount)
//            )
//            if (!result) {
//                return@withIO null
//            }
//            LAMEUtils.encode()
//            LAMEUtils.destroy()
//            outputMP3File
//        }
//    }

}