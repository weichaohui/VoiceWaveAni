package com.example.weichaohui.voicewaveani;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.animation.PathInterpolator;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by weichaohui on 17-11-23.
 */

public class VoiceWaveView extends View {
    protected static long sFrameDelay = 10;
    private final static int MAX_DB = 21;

    private LinkedList<Wave> mWaveList = new LinkedList<>();
    private int mWaveAreaLength = 22;//绘制的总长度

    private float mWaveColumnWidth = 5;//px
    private float[] mWaveArea = null;

    private int mColumnColor = Color.BLUE;

    private Random mRandom = new Random(System.currentTimeMillis());
    private Paint mPaint = new Paint();

    private Choreographer mChoreographer = Choreographer.getInstance();
    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            long currentTime = System.currentTimeMillis();
            Log.e("weichaohui", "" + currentTime / 16);
            while (!mWaveList.isEmpty()) {
                Wave first = mWaveList.getFirst();
                if (first.dead(currentTime)) {
                    mWaveList.removeFirst();
                } else {
                    break;
                }
            }

            for (int i = 0; i < mWaveArea.length; i++) {
                mWaveArea[i] = 5;
            }

            if (!mWaveList.isEmpty()) {
                for (Wave wave : mWaveList) {
                    wave.apply(mWaveArea, currentTime);
                }

                VoiceWaveView.this.invalidate();
            }
            mChoreographer.postFrameCallbackDelayed(mFrameCallback, sFrameDelay);
        }
    };

    public VoiceWaveView(Context context) {
        super(context);
        init();
    }

    public VoiceWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VoiceWaveView);
        mColumnColor = typedArray.getInteger(R.styleable.VoiceWaveView_mzColumnColor, context.getResources().getColor(R.color.mz_voice_wave_column_color));
        mWaveColumnWidth = typedArray.getFloat(R.styleable.VoiceWaveView_mzColumnWidth, context.getResources().getDimension(R.dimen.mz_voice_wave_column_width));
        init();
    }

    private void init() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColumnColor);
        mWaveArea = new float[mWaveAreaLength];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int centerH = getPaddingTop() + (height - getPaddingTop() - getPaddingBottom()) / 2;
        float pxPerDB = (height - getPaddingTop() - getPaddingBottom()) / (float) MAX_DB / 2;

        float space = ((width - getPaddingLeft() - getPaddingRight()) - mWaveColumnWidth * mWaveAreaLength) / (mWaveAreaLength - 1);

        int left = getPaddingLeft();
        for (int i = 0; i < mWaveAreaLength; i++) {
            canvas.drawRect(left, centerH + mWaveArea[i] * pxPerDB, left + mWaveColumnWidth, centerH - mWaveArea[i] * pxPerDB, mPaint);
            left += mWaveColumnWidth + space;
        }
    }

    public void waveOccurd(int db) {
        //有可能一个声波会有两个
        int waveNum = mRandom.nextInt(2) + 1;
        while (waveNum-- > 0) {
            Wave newWave = new Wave(db, System.currentTimeMillis(), mWaveAreaLength);
            mWaveList.add(newWave);
            if (mWaveList.size() == 1) {
                mFrameCallback.doFrame(0);
            }
        }
    }

    public static class Wave {
        final PathInterpolator INTERPOLATOR = new PathInterpolator(0, 0.67f, 0.67f, 1);
        final int TIME_RANGE = 200;//时间差异在100毫秒中
        final int RISE_DURATION = 200; //声波升起持续时间, 会有随机差异，　差异范围为TIME_RANGE
        final int DECLINE_DURATION = 400; //声波下降持续时间

        Random random = new Random(System.currentTimeMillis());


        private int mRangeL; // 声波在view中展现的区域左边界
        private int mRangeR; // 声波在view中展现的区域右边界

        private Column[] mColumnChart = null;//顶峰值

        //每一个的时间略有差异
        static class Column {
            float peakDB;//顶峰值
            long startTime;//开始时间
            private int riseDuration; //声波升起持续时间
            private int declineDuration; //声波下降持续时间
        }

        public Wave(int db, long startTime, int waveAreaLength) {
            int maxRange = db / 2;
            int minRange = db / 5;

            mRangeL = random.nextInt(waveAreaLength - 1);
            mRangeR = random.nextInt(waveAreaLength - 1 - mRangeL) + mRangeL;

            if (mRangeR - mRangeL > maxRange) {
                mRangeR = mRangeL + maxRange;
            }

            if (mRangeR - mRangeL < minRange) {
                mRangeR = mRangeL + minRange;
            }

            mColumnChart = new Column[mRangeR - mRangeL + 1];
            int centerIndex = mColumnChart.length / 2;
            float increase = db / (float) (centerIndex + 1);
            for (int i = 0; i < mColumnChart.length; i++) {
                mColumnChart[i] = new Column();
                if (i < mColumnChart.length / 2) {
                    mColumnChart[i].peakDB = increase * (i + 1);
                } else {
                    mColumnChart[i].peakDB = db - increase * (i - centerIndex);
                }
                mColumnChart[i].startTime = startTime + random.nextInt(TIME_RANGE) * (random.nextInt(2) == 0 ? 1 : -1);
                mColumnChart[i].riseDuration = RISE_DURATION + random.nextInt(TIME_RANGE) * (random.nextInt(2) == 0 ? 1 : -1);
                mColumnChart[i].declineDuration = DECLINE_DURATION + random.nextInt(TIME_RANGE) * (random.nextInt(2) == 0 ? 1 : -1);
            }
        }

        private float countFraction(Column column, long currentTime) {
            float fraction;
            if (currentTime - column.startTime <= column.riseDuration) {
                fraction = (currentTime - column.startTime) / (float) column.riseDuration;
            } else {
                fraction = (currentTime - column.startTime - column.riseDuration) / (float) column.declineDuration;
                fraction = 1 - fraction;//fraction是反方向的
            }
            return INTERPOLATOR.getInterpolation(fraction);
        }

        private float[] countCurentWaveChart(long currentTime) {
            float[] result = new float[mColumnChart.length];
            for (int i = 0; i < result.length; i++) {
                float fraction = countFraction(mColumnChart[i], currentTime);
                result[i] = mColumnChart[i].peakDB * fraction;
            }
            return result;
        }

        private boolean dead(long currentTime) {
            for (Column column : mColumnChart) {
                if (currentTime < column.startTime + column.riseDuration + column.declineDuration) {
                    return false;
                }
            }
            return true;
        }

        public float[] apply(float[] waveArea, long currentTime) {
            //将本wave使用到整体的waveArea中
            float[] waveChart = countCurentWaveChart(currentTime);
            for (int i = mRangeL; i <= mRangeR && i < waveArea.length; i++) {
                waveArea[i] += waveChart[i - mRangeL];
            }
            return waveArea;
        }
    }
}
