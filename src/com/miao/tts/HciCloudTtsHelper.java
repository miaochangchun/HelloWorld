package com.miao.tts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sinovoice.hcicloudsdk.api.HciLibPath;
import com.sinovoice.hcicloudsdk.api.tts.HciCloudTts;
import com.sinovoice.hcicloudsdk.common.ApiInitParam;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.tts.ITtsSynthCallback;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;
import com.sinovoice.hcicloudsdk.common.tts.TtsSynthResult;

public class HciCloudTtsHelper {
	String sPath = System.getProperty("user.dir");
    private String mCapKey = "tts.local.synth";
    private static HciCloudTtsHelper mInstance;
    
    private FileOutputStream mFos;
    
    static {
		// 灵云sdk中dll文件目录
		String libPath = System.getProperty("user.dir") + File.separator + "lib";
		// 指定dll文件路径，顺序表示加载顺序
		String ttsLibPath[] = new String[] { 
				libPath + File.separator + "libhci_curl.dll",
				libPath + File.separator + "hci_sys.dll",
				libPath + File.separator + "hci_tts.dll",
                libPath + File.separator + "hci_tts_jni.dll",
                libPath + File.separator + "hci_tts_local_synth.dll"
				};
		// 再加载hci_tts.dll
		HciLibPath.setTtsLibPath(ttsLibPath);
    }
    
    private HciCloudTtsHelper() {
       
    }

    public static HciCloudTtsHelper getInstance() {
        if (mInstance == null) {
            mInstance = new HciCloudTtsHelper();
        }
        return mInstance;
    }
    
    public int init() {
		TtsInitParam ttsInitParam = new TtsInitParam();

        String dataPath = sPath + File.separator + "data";
        ttsInitParam.addParam(ApiInitParam.PARAM_KEY_DATA_PATH, dataPath);
        ttsInitParam.addParam(ApiInitParam.PARAM_KEY_FILE_FLAG, "none");
        ttsInitParam.addParam(ApiInitParam.PARAM_KEY_INIT_CAP_KEYS, mCapKey);
		
    	// 调用初始化方法, 返回值为错误码:
		// 如果为HCI_ERR_NONE(0) 则表示MT初始化成功,否则请根据SDK帮助文档中"常量字段值"中的
		// 错误码的含义检查错误所在
		int initResult = HciCloudTts.hciTtsInit(ttsInitParam.getStringConfig());

        return initResult;
    }
    
    public int release() {
        int nRet = HciCloudTts.hciTtsRelease();
        return nRet;
    }
    
    /**
     * 引擎合成过程中,每合成一段文字都会调用该回调方法通知外部并传回音频数据 音频数据保存在对象
     * TtsSynthResult中,通过该对象的getVoieceData()方法可以获取 合成是设定音频格式的音频数据
     */
    private ITtsSynthCallback mTtsSynthCallback = new ITtsSynthCallback() {
        @Override
        public boolean onSynthFinish(int errorCode, TtsSynthResult result) {
            // errorCode 为当前合成操作返回的错误码,如果返回值为HciErrorCode.HCI_ERR_NONE则表示合成成功
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                System.out.println("synth error, code = " + errorCode);
                return false;
            }
            
            if (mFos == null) {
                initFileOutputStream();
            }

            // 将本次合成的数据写入文件
            // 每次合成的数据，可能不是需要合成文本的全部，需要多次写入
            if(result != null && result.getVoiceData() != null){
                int length = result.getVoiceData().length;
                if (length > 0) {
                    try {
                        mFos.write(result.getVoiceData(), 0, length);
                        mFos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (!result.isHasMoreData()) {
                flushOutputStream();
            }

            // 返回true表示处理结果成功,通知引擎可以继续合成并返回下一次的合成结果; 如果不希望引擎继续合成, 则返回false
            // 该方法在引擎中的同步的,即引擎会持续阻塞一直到该方法执行结束
            return true;
        }
    };

    /**
     * 语言合成函数
     * @param text
     */
    public void synth(String text) {
        // Tts合成的配置字符串, 用户也可以借助帮助类 TtsConfig来生成该字符串
        String sConfig = "capKey=" + mCapKey;

        // 创建会话
        Session session = new Session();
        // 开始会话
        int nRet = HciCloudTts.hciTtsSessionStart(sConfig, session);
        if (nRet != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("session start failed: " + nRet);
            return;
        }

        //需要合成的文本
        byte[] synthText = text.getBytes();
        // 开始合成
        nRet = HciCloudTts.hciTtsSynthEx(session, synthText, sConfig, mTtsSynthCallback);
        if (nRet != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("hciTtsSynth failed: " + nRet);
            HciCloudTts.hciTtsSessionStop(session);
            return;
        }

        // 停止会话
        // 合成结束后应该调用该方法通知引擎该会话已经结束
        nRet = HciCloudTts.hciTtsSessionStop(session);
        if (nRet != HciErrorCode.HCI_ERR_NONE) {
            System.out.println("session stop failed: " + nRet);

            return;
        }
    }
    
    /**
     * TTS合成动作完毕，将合成的数据输出到文件中
     */
    private void flushOutputStream() {
        try {
            mFos.close();
            mFos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化FileOutputStream
     */
    private void initFileOutputStream() {
    	
    	String filePath = System.getProperty("user.dir") + File.separator +
                "testdata" + File.separator + "synth.pcm";
    	 
        try {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            mFos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
