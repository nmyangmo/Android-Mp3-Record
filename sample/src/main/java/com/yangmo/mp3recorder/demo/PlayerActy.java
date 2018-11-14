package com.yangmo.mp3recorder.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author: nmyangmo@126.com
 * Description:
 */

public class PlayerActy extends Activity {
    @Bind(R.id.btn_start)
    Button btnStart;
    @Bind(R.id.btn_pause)
    Button btnPause;
    @Bind(R.id.btn_resume)
    Button btnResume;
    @Bind(R.id.btn_stop)
    Button btnStop;
    @Bind(R.id.btn_release)
    Button btnRelease;
    @Bind(R.id.tv_progress)
    TextView tvProgress;
    @Bind(R.id.seekbar)
    SeekBar seekbar;
    @Bind(R.id.tv_size)
    TextView tvSize;
    @Bind(R.id.btn_play_url1)
    Button btnPlayUrl1;
    @Bind(R.id.btn_play_url2)
    Button btnPlayUrl2;
    @Bind(R.id.btn_play_file)
    Button btnPlayFile;

    StatedMediaPlay manager;
    String url1 = "http://static.qxinli.com/srv/voice/201702281645261757.mp3";
    String url2 = "http://static.qxinli.com/srv/voice/201702051645337133.mp3";
    String file = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        ButterKnife.bind(this);

        initPlayer();



        initEvent();
    }

    private void initPlayer() {
        // TODO: 2018/9/19 初始化播放器
        manager=new StatedMediaPlay();
    }

    private void initEvent() {
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                manager.seekTo(seekBar.getProgress());
            }
        });

    }

    @OnClick({R.id.btn_start, R.id.btn_pause, R.id.btn_resume, R.id.btn_stop, R.id.btn_release,
            R.id.tv_progress, R.id.seekbar, R.id.tv_size, R.id.btn_play_url1, R.id.btn_play_url2, R.id.btn_play_file})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                manager.play(url1);
                break;
            case R.id.btn_pause:
                manager.pause();
                break;
            case R.id.btn_resume:
                manager.resume();
                break;
            case R.id.btn_stop:
                manager.stop();
                break;
            case R.id.btn_release:
                manager.resume();
                break;
            case R.id.btn_play_url1:
                manager.play(url1);
                break;
            case R.id.btn_play_url2:
                manager.play(url2);
                break;
            case R.id.btn_play_file:
                manager.play(file);
                break;
        }
    }
}
