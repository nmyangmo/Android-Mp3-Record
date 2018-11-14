package com.yangmo.mp3recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import com.yangmo.mp3recorder.util.LameUtil;
import com.yangmo.mp3recorder.util.SP;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Created by hss01248 on 12/29/2015.
 */
public class AudioRecorder extends Thread {

    private static final String TAG = "@@@ AudioRecorder";

    private final int sampleRates[] = {44100, 22050, 11025, 8000};
    private final int configs[] = {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO};
    private final int formats[] = {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT};

    // ======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 128kbps
     */
    public static final int DEFAULT_LAME_MP3_BIT_RATE = 128;

    private AudioRecord audioRecord = null;
    int bufsize = AudioRecord.ERROR_BAD_VALUE;
    private boolean mShouldRun = false;
    private boolean mShouldRecord = false;

    private long startTime = 0L;
    private long duration = 0L;

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    private int maxDuration;

    /**
     * 自定义 每220帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 220;
    private short[] mPCMBuffer;
    private DataEncodeThread mEncodeThread;

    private File outputFile;
    private double mDuration;//录音时间,单位为毫秒
    private Mp3Recorder.Callback mDurationListener;
    Mp3Recorder mMyMp3Recorder;
    boolean reallyStart;
    Handler handler;
    Runnable sendNoPermission;
    int waitingTime;
    boolean havePermission;

    public AudioRecorder(File file, Mp3Recorder myMp3Recorder) {
        outputFile = file;
        mMyMp3Recorder = myMp3Recorder;
        handler = new Handler();

        if (myMp3Recorder.getRecorderState() == Mp3Recorder.State.PREPARED) {
            waitingTime = 1000;
        } else {
            waitingTime = 10000;
        }
        havePermission = SP.getBoolean("mp3permission", true);
        if (!havePermission) {
            waitingTime = 1000;
        }
        sendNoPermission = new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new AudioNoPermissionEvent());
                SP.setBoolean("mp3permission", false);
                Log.e(waitingTime / 1000 + "s等待时间已到,么有权限:");
            }
        };
    }

    public void setCallback(Mp3Recorder.Callback mDurationListener) {
        this.mDurationListener = mDurationListener;
    }


    public void startRecording() {
        mShouldRecord = true;
    }

    public void resumeRecord() {
        mShouldRecord = true;
    }

    public void pauseRecord() {
        mShouldRecord = false;
    }

    public void stopRecord() {
        mShouldRecord = false;
        mShouldRun = false;
       /* if(handler!=null && sendNoPermission!=null){
            handler.removeCallbacks(sendNoPermission);
        }*/
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            ///audioRecord.
        }
        // stop the encoding thread and try to wait until the thread finishes its job
        Message msg = Message.obtain(mEncodeThread.getHandler(),
                DataEncodeThread.PROCESS_STOP);
        msg.sendToTarget();
    }

    private int mapFormat(int format) {
        switch (format) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 8;
            case AudioFormat.ENCODING_PCM_16BIT:
                return 16;
            default:
                return 0;
        }
    }

    private void cancel() {
        stopRecord();
    }

    public int getDuration() {
        return (int) mDuration;
    }

    @Override
    public void run() {
        android.util.Log.d(TAG, "run() called");
        super.run();
        if (!isFound()) {
            Log.e(TAG, "Sample rate, channel config or format not supported!");
            EventBus.getDefault().post(new AudioNoPermissionEvent());
            //SP.setBoolean("mp3permission",false);
            return;
        }
        init();
        mShouldRun = true;
        boolean oldShouldRecord = false;

        int bytesPerSecond = audioRecord.getSampleRate() * mapFormat(audioRecord.getAudioFormat()) / 8 * audioRecord.getChannelCount();
        mDuration = 0;
        while (mShouldRun) {
            if (mShouldRecord != oldShouldRecord) {//只有状态切换的那一次会走这里
                if (mShouldRecord) {
                    //监测8s内音频振幅大小,以判断是否拿到录音权限,还是空文件
                    startTime = System.currentTimeMillis();
                    Log.e("开始调用系统录音audioRecord.startRecording()  时间:" + startTime);
                    try {
                        handler.postDelayed(sendNoPermission, waitingTime);
                        audioRecord.startRecording();//调用本地录音方法,如果有权限管理软件,会向系统申请权限

                        //没有异常,就是拿到了权限
                        if (handler != null) {//点击恢复时也会走这里
                            handler.removeCallbacks(sendNoPermission);
                        }
                        if (mDuration == 0) {//第一次点击开始录音
                            reallyStart = true;
                            Log.e("拿到权限,真正开始录音");
                            SP.setBoolean("mp3permission", true);
                            Mp3RecorderUtil.postTaskSafely(new Runnable() {
                                @Override
                                public void run() {
                                    // mDurationListener.onReallyStart();//真正开始
                                    mMyMp3Recorder.onstart();
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("没有拿到权限：系统录音audioRecord.startRecording() 异常  :");
                        e.printStackTrace();
                        EventBus.getDefault().post(new AudioNoPermissionEvent());
                        //SP.setBoolean("mp3permission",false);
                    }
                } else {
                    audioRecord.stop();
                }
                oldShouldRecord = mShouldRecord;
            }

            if (mShouldRecord) {
                int readSize = audioRecord.read(mPCMBuffer, 0, bufsize);
                if (readSize > 0) {
                    final double read_ms = (1000.0 * readSize * 2) / bytesPerSecond;

                    final double volume = calVolume(mPCMBuffer, readSize);

                    mDuration += read_ms;
                    if (mDurationListener != null) {
                        Mp3RecorderUtil.postTaskSafely(new Runnable() {
                            @Override
                            public void run() {
                                //mDurationListener.onRecording(mDuration);
                                if (mDurationListener != null) {
                                    mDurationListener.onRecording(mDuration, volume);
                                    if (maxDuration > 0 && mDuration >= maxDuration) {
                                        mMyMp3Recorder.stop(Mp3Recorder.ACTION_STOP_ONLY);
                                        mDurationListener.onMaxDurationReached();
                                        mDurationListener = null;
                                    }
                                }

                            }
                        });

                    } else {
                        Log.e("mDurationListener in audioRecorder is null!");
                    }

                    if (audioRecord != null && audioRecord.getChannelCount() == 1) {
                        mEncodeThread.addTask(mPCMBuffer, readSize);
                    } else if (audioRecord != null && audioRecord.getChannelCount() == 2) {
                        short[] leftData = new short[readSize / 2];
                        short[] rightData = new short[readSize / 2];
                        for (int i = 0; i < readSize / 2; i = i + 2) {
                            leftData[i] = mPCMBuffer[2 * i];
                            if (2 * i + 1 < readSize) {
                                leftData[i + 1] = mPCMBuffer[2 * i + 1];
                            }
                            if (2 * i + 2 < readSize) {
                                rightData[i] = mPCMBuffer[2 * i + 2];
                            }
                            if (2 * i + 3 < readSize) {
                                rightData[i + 1] = mPCMBuffer[2 * i + 3];
                            }
                        }
                        mEncodeThread.addTask(leftData, rightData, readSize / 2);
                    }
                }
            }
        }
    }

    private double calVolume(short[] buffer, double readSize) {
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < buffer.length; i++) {
            v += buffer[i] * buffer[i];
        }
        // 平方和除以数据总长度，得到音量大小。
        double mean = v / readSize;
        double volume = 10 * Math.log10(mean);
        return volume;
    }


    public boolean isRecording() {
        return mShouldRecord;
    }

    private void init() {
        int bytesPerFrame = audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT ? 2
                : 1;
        int frameSize = bufsize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            bufsize = frameSize * bytesPerFrame;
        }
        mPCMBuffer = new short[bufsize];
        /*
		 * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate
		 * The bit rate is 128kbps
		 */
        LameUtil.init(audioRecord.getSampleRate(), audioRecord.getChannelCount(), audioRecord.getSampleRate(), DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);

        // Create and run thread used to encode data
        // The thread will
        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            mEncodeThread = new DataEncodeThread(outputFile, bufsize);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mEncodeThread.start();
        audioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);


    }

    /**
     * get the available AudioRecord
     *
     * @return
     */
    private boolean isFound() {
        boolean isFound = false;

        int sample_rate = -1;
        int channel_config = -1;
        int format = -1;

        for (int x = 0; !isFound && x < formats.length; x++) {
            format = formats[x];
            for (int y = 0; !isFound && y < sampleRates.length; y++) {
                sample_rate = sampleRates[y];
                for (int z = 0; !isFound && z < configs.length; z++) {
                    channel_config = configs[z];

                    Log.i(TAG, "Trying to create AudioRecord use: " + format + "/" + channel_config + "/" + sample_rate);
                    bufsize = AudioRecord.getMinBufferSize(sample_rate, channel_config, format);
                    Log.i(TAG, "Bufsize: " + bufsize);
                    if (AudioRecord.ERROR_BAD_VALUE == bufsize) {
                        Log.i(TAG, "invaild params!");
                        continue;
                    }
                    if (AudioRecord.ERROR == bufsize) {
                        Log.i(TAG, "Unable to query hardware!");
                        continue;
                    }

                    try {
                        audioRecord = new AudioRecord(
                                MediaRecorder.AudioSource.MIC, sample_rate,
                                channel_config, format, bufsize);
                        int state = audioRecord.getState();
                        if (state != AudioRecord.STATE_INITIALIZED) {
                            continue;
                        }
                    } catch (IllegalStateException e) {
                        Log.i(TAG, "Failed to set up recorder!");
                        audioRecord = null;
                        continue;
                    }
                    isFound = true;
                    break;
                }
            }
        }

        return isFound;
    }

    boolean isGetVoiceRun;
    Object mLock = new Object();
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

    public void getNoiseLevel() {
        if (isGetVoiceRun) {
            Log.e(TAG, "还在录着呢");
            return;
        }

        if (audioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }

        if (!isRecording()) {
            Log.e("sound", "mAudioRecord不在录制呢");
        }
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[bufsize];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    if (audioRecord == null) {
                        Log.e("sound", "mAudioRecord为空");
                        return;
                    }
                    duration = System.currentTimeMillis() - startTime;
                    if (duration > 20000) {//监测0-20s之间的大小内
                        isGetVoiceRun = false;
                        return;
                    }

                    int r = audioRecord.read(buffer, 0, bufsize);
                    if (r == 0) {
                        Mp3RecorderUtil.showDebugToast("r ==0");
                    }
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    Log.d(TAG, "分贝值buffer.length:" + buffer.length);
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    Log.d(TAG, "分贝值r:" + r);
                    double mean = v / (double) r;
                    if (v == 0) {
                        if (duration > 9000) {
                            Mp3RecorderUtil.showDebugToast("v ==0,没有拿到权限");
                            EventBus.getDefault().post(new AudioNoPermissionEvent());

                        }
                    }
                    Log.d(TAG, "分贝值v:" + v);
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "分贝值volume:" + volume);
                    // ToastUtils.showDebugToast("分贝值:"+ volume);
                    if (volume == 0) {
                        Mp3RecorderUtil.showDebugToast("音量为0,请检查录音权限");
                    }
                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }).start();
    }
}
