package com.example.dominic.shakedemo;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int START_SHAKE = 1;
    private static final int AGAIN_SHAKE = 2;
    private static final int END_SHAKE = 3;
    @BindView(R.id.main_shake_top)
    ImageView mMainShakeTop;
    @BindView(R.id.main_shake_top_line)
    ImageView mMainShakeTopLine;
    @BindView(R.id.main_linear_top)
    LinearLayout mMainLinearTop;
    @BindView(R.id.main_shake_bottom_line)
    ImageView mMainShakeBottomLine;
    @BindView(R.id.main_shake_bottom)
    ImageView mMainShakeBottom;
    @BindView(R.id.main_linear_bottom)
    LinearLayout mMainLinearBottom;
    @BindView(R.id.activity_main)
    LinearLayout mActivityMain;

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private SoundPool mSoundPool;
    private Vibrator mVibrator;
    private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        //初始化SoundPool
        mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
      //  mSoundPool.load(this, R.raw.weichat_audio, 1);

        //获取Vibrator震动服
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


        mHandler = new MyHandler(this);

        mMainShakeTopLine.setVisibility(View.GONE);
        mMainShakeBottomLine.setVisibility(View.GONE);

    }


    @Override
    protected void onStart() {
        super.onStart();
        //获取SensorManager 传感器管理器
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager != null) {
            //获取加速器管理器
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mAccelerometerSensor != null) {

                mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    /*  至于为什么我们要在onStart和onPause中执行SensorManager的注册和注销,
    *   就是因为, 防止在界面退出(包括按Home键)时, 摇一摇依旧生效(代码中有注释)
    * */
    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
        }
    }


    private boolean isShake = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_ACCELEROMETER) {
            //获取三个方向值
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            if ((Math.abs(x) > 17 || Math.abs(y) > 17 || Math
                    .abs(z) > 17) && !isShake) {
                isShake = true;
                // TODO: 2016/10/19 实现摇动逻辑, 摇动后进行震动
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Log.d(TAG, "onSensorChanged: 摇动");

                            //开始震动 发出提示音 展示动画效果
                            mHandler.obtainMessage(START_SHAKE).sendToTarget();
                            Thread.sleep(500);
                            //再来一次震动提示
                            mHandler.obtainMessage(AGAIN_SHAKE).sendToTarget();
                            Thread.sleep(500);
                            mHandler.obtainMessage(END_SHAKE).sendToTarget();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> mReference;
        private MainActivity mActivity;

        public MyHandler(MainActivity activity) {
            mReference = new WeakReference<MainActivity>(activity);
            if (mReference != null) {
                mActivity = mReference.get();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_SHAKE:
                    //This method requires the caller to hold the permission VIBRATE.
                    mActivity.mVibrator.vibrate(300);
                    //发出提示音
                  //  mActivity.mSoundPool.play(mActivity.mWeiChatAudio, 1, 1, 0, 0, 1);
                    mActivity.mMainShakeTopLine.setVisibility(View.VISIBLE);
                    mActivity.mMainShakeBottomLine.setVisibility(View.VISIBLE);
                    mActivity.startAnimation(false);//参数含义: (不是回来) 也就是说两张图片分散开的动画
                    break;
                case AGAIN_SHAKE:
                    mActivity.mVibrator.vibrate(300);
                    break;
                case END_SHAKE:
                    //整体效果结束, 将震动设置为false
                    mActivity.isShake = false;
                    // 展示上下两种图片回来的效果
                    mActivity.startAnimation(true);
                    break;
            }
        }
    }

    private void startAnimation(boolean isBack) {
        //动画坐标移动的位置的类型是相对自己的
        int type = Animation.RELATIVE_TO_SELF;

        float topFromY;
        float topToY;
        float bottomFromY;
        float bottomToY;
        if (isBack) {
            topFromY = -0.5f;
            topToY = 0;
            bottomFromY = 0.5f;
            bottomToY = 0;
        } else {
            topFromY = 0;
            topToY = -0.5f;
            bottomFromY = 0;
            bottomToY = 0.5f;
        }

        //上面图片的动画效果
        TranslateAnimation topAnim = new TranslateAnimation(
                type, 0, type, 0, type, topFromY, type, topToY
        );
        topAnim.setDuration(200);
        //动画终止时停留在最后一帧~不然会回到没有执行之前的状态
        topAnim.setFillAfter(true);

        //底部的动画效果
        TranslateAnimation bottomAnim = new TranslateAnimation(
                type, 0, type, 0, type, bottomFromY, type, bottomToY
        );
        bottomAnim.setDuration(200);
        bottomAnim.setFillAfter(true);

        //大家一定不要忘记, 当要回来时, 我们中间的两根线需要GONE掉
        if (isBack) {
            bottomAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //当动画结束后 , 将中间两条线GONE掉, 不让其占位
                    mMainShakeTopLine.setVisibility(View.GONE);
                    mMainShakeBottomLine.setVisibility(View.GONE);
                }
            });
        }
        //设置动画
        mMainLinearTop.startAnimation(topAnim);
        mMainLinearBottom.startAnimation(bottomAnim);

    }
}
