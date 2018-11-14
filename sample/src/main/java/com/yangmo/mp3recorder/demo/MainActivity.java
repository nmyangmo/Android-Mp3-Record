package com.yangmo.mp3recorder.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yangmo.mp3recorder.AudioNoPermissionEvent;
import com.yangmo.mp3recorder.Mp3Recorder;
import com.yangmo.mp3recorder.Mp3RecorderUtil;
import com.orhanobut.logger.LogPrintStyle;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.Settings;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.MyToast;
import io.reactivex.functions.Consumer;

/**
 * Author: nmyangmo@126.com
 * Description:
 */
public class MainActivity extends Activity {

    @Bind(R.id.btn_start)
    Button btnStart;
    @Bind(R.id.btn_pause)
    Button btnPause;
    @Bind(R.id.btn_resume)
    Button btnResume;
    @Bind(R.id.btn_stop)
    Button btnStop;
    @Bind(R.id.btn_reset)
    Button btnReset;
    @Bind(R.id.tv_progress)
    TextView tvProgress;
    private Mp3Recorder mRecorder;

    String path;

    //private MediaPlayer player;
    StatedMediaPlay play;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        Logger.initialize(
                new Settings()
                        .setStyle(new LogPrintStyle())
                        .isShowMethodLink(true)
                        .isShowThreadInfo(false)
                        .setMethodOffset(0)
                        .setLogPriority(BuildConfig.DEBUG ? Log.VERBOSE : Log.ASSERT)
        );
        MyToast.init(getApplicationContext(), true, true);


        play = new StatedMediaPlay();


        //player = new MediaPlayer();
        //Button startButton = (Button) findViewById(R.id.StartButton);

        //path = new File(Environment.getExternalStorageDirectory(), "test.mp3").getAbsolutePath();
        path = new File(getFilesDir(), "test.mp3").getAbsolutePath();

        askPermission();

        //init();
        /*startButton.setOnClickListener(new OnClickListener() {
            @Override
			public void onClick(View v) {

					mRecorder.startRecording();

			}
		});*/
        //Button stopButton = (Button) findViewById(R.id.StopButton);
		/*stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRecorder.stop(Mp3Recorder.ACTION_STOP_ONLY);
			}
		});*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AudioNoPermissionEvent event) {
        toast("没有权限,赶紧去设置吧");
    }

    private void askPermission() {
        new RxPermissions(this)
                .request(Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            init();
                        } else {
                            toast("权限被拒绝了");
                            init();
                        }
                    }
                });
    }

    public void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // mRecorder.cancel();
    }

    private void init() {
        Mp3RecorderUtil.init(getApplicationContext(), true);


        mRecorder = new Mp3Recorder();
        mRecorder.setOutputFile(path)
                .setMaxDuration(10)//3s
                .setCallback(new Mp3Recorder.Callback() {
                           /* @Override
                            public void onRecording(double duration) {
                                tvProgress.setText( String.format("%d分%d秒",(int)(duration/1000/60),(int)(duration/1000%60))+"---"+duration );
                            }*/

                    @Override
                    public void onRecording(double duration, double volume) {
                        String str = "";
                        str = String.format("duration:\n" + "%d分%d秒", (int) (duration / 1000 / 60), (int) (duration / 1000 % 60)) + "---" + duration + "\n"
                                + "分贝值:\n" + volume;
                        tvProgress.setText(str);

                    }

                    @Override
                    public void onStart() {
                        toast("开始了....");

                    }

                    @Override
                    public void onPause() {
                        toast("暂停了....");
                    }

                    @Override
                    public void onResume() {
                        toast("恢复....");
                    }

                    @Override
                    public void onStop(int action) {
                        toast("onStop....");
                        tvProgress.setText("onStop");
                    }

                    @Override
                    public void onReset() {
                        toast("onReset....");
                        tvProgress.setText("onReset");
                    }

                    @Override
                    public void onMaxDurationReached() {
                        toast("onMaxDurationReached....");
                        tvProgress.setText("onMaxDurationReached");
                        Log.d("@@@", "onMaxDurationReached() called");
                    }
                });

        //mRecorder.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorder.stop(Mp3Recorder.ACTION_STOP_ONLY);
        EventBus.getDefault().unregister(this);
    }

    @OnClick({R.id.btn_start, R.id.btn_pause, R.id.btn_resume, R.id.btn_stop, R.id.btn_reset, R.id.btn_play, R.id.btn_play_other})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                mRecorder.start();
                break;
            case R.id.btn_pause:
                mRecorder.pause();
                break;
            case R.id.btn_resume:
                mRecorder.resume();
                break;
            case R.id.btn_stop:
                mRecorder.stop(Mp3Recorder.ACTION_STOP_ONLY);
                break;
            case R.id.btn_reset:
                mRecorder.reset();
                break;
            case R.id.btn_play_other:
                startActivity(new Intent(this, PlayerActy.class));
                break;
            case R.id.btn_play:
                /*Intent mIntent = new Intent();
                Uri uri = Uri.fromFile(new File(path));
                mIntent.setAction(android.content.Intent.ACTION_VIEW);
                mIntent.setDataAndType(uri , "audio/mp3");
                startActivity(mIntent);*/
//                播放暂停
// TODO: 2018/9/19  播放暂停
                if (play != null && play.isPlaying()) {
                    play.pause();
                } else {
                    play.play(path);
                }
                break;
        }
    }


}
