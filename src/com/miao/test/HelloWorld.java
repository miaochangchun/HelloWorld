package com.miao.test;

import com.miao.tts.HciCloudSysHelper;
import com.miao.tts.HciCloudTtsHelper;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;

/**
 * Created by 10048 on 2016/11/9.
 */
public class HelloWorld {
    public static void main(String[] args) {
        /**********************************************************************/
        //合成函数初始化
        HciCloudSysHelper mHciCloudSysHelper = HciCloudSysHelper.getInstance();
        HciCloudTtsHelper mHciCloudTtsHelper = HciCloudTtsHelper.getInstance();
        int errorCode = mHciCloudSysHelper.init();
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("mHciCloudSysHelper.init failed and return " + errorCode);
            return;
        }
        errorCode = mHciCloudTtsHelper.init();
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("mHciCloudTtsHelper.init failed and return " + errorCode);
            return;
        }

        //开始合成
        mHciCloudTtsHelper.synth("琅琊榜，呵呵呵");

        play();

        /***************************************************************************************/
        //反初始化函数
        errorCode = mHciCloudTtsHelper.release();
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("mHciCloudTtsHelper.release failed and return " + errorCode);
            return;
        }
        errorCode = mHciCloudSysHelper.release();
        if (errorCode != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("mHciCloudSysHelper.release failed and return " + errorCode);
            return;
        }
    }

    private static void play() {
        File file = new File(System.getProperty("user.dir") + File.separator + "testdata" + File.separator + "synth.pcm");
        int offset = 0;
        int bufferSize = Integer.valueOf(String.valueOf(file.length()));
        byte[] audioData = new byte[bufferSize];
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            inputStream.read(audioData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        float sampleRate = 16000f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, bufferSize);
        try {
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            while (offset < audioData.length) {
                offset += sourceDataLine.write(audioData, offset, bufferSize);
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

}
