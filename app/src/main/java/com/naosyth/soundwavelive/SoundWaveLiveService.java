package com.naosyth.soundwavelive;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;

public class SoundWaveLiveService extends WallpaperService {
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static AudioRecord recorder;
    private static int bufferSize;

    private AudioTrack at;
    long lastTime;

    @Override
    public Engine onCreateEngine() {
        if (recorder == null) {
            bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

            at=new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, 88200 /* 1 second buffer */,
                    AudioTrack.MODE_STREAM);

            lastTime = Calendar.getInstance().getTimeInMillis();

            Log.v("SoundWave", "Buffer Size " + bufferSize);
        }

        return new SoundWaveLiveEngine(this.getApplicationContext());
    }

    class SoundWaveLiveEngine extends Engine {
        private WaveData data;
        private short[] readData;

        private Handler handler = new Handler();
        private Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        private Paint paint = new Paint();
        int backgroundColor;
        int lineColor;
        float maxLineSize = 10.0f;
        float lineSize;
        int maxFrameRate = 30;
        int frameRate;

        SharedPreferences preferences;
        SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("background_color")) {
                    backgroundColor = preferences.getInt("background_color", 0xFF000000);
                } else if (key.equals("line_color")) {
                    lineColor = preferences.getInt("line_color", 0xFFFFFFFF);
                    paint.setColor(lineColor);
                } else if (key.equals("line_size")) {
                    lineSize = maxLineSize * preferences.getFloat("line_size", 0.5f);
                    paint.setStrokeWidth(lineSize);
                } else if (key.equals("frame_rate")) {
                    frameRate = (int)(maxFrameRate * preferences.getFloat("frame_rate", 0.25f));
                    frameRate = Math.max(1, frameRate);
                }
            }
        };

        private boolean visible = true;
        private int width;
        private int height;

        public SoundWaveLiveEngine(Context context) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
            backgroundColor = preferences.getInt("background_color", 0xFF000000);
            lineColor = preferences.getInt("line_color", 0xFFFFFFFF);
            lineSize = maxLineSize * preferences.getFloat("line_size", 0.5f);
            frameRate = (int)(maxFrameRate * preferences.getFloat("frame_rate", 0.25f));
            frameRate = Math.max(1, frameRate);

            paint.setColor(lineColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(lineSize);

            handler.post(drawRunner);

            readData = new short[bufferSize];
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                recorder.startRecording();
                handler.post(drawRunner);
            } else {
                recorder.stop();
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawRunner);
            this.visible = false;
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            this.width = width;
            this.height = height;
            data = new WaveData(width);
            super.onSurfaceChanged(holder, format, width, height);
        }

        private void draw() {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
                recorder.startRecording();

            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    readAudio();
                    drawWaveform(canvas);
                }
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }

            handler.removeCallbacks(drawRunner);
            if (visible)
                handler.postDelayed(drawRunner, 1000/frameRate);
        }

        private void readAudio() {
            short[] testData = new short[bufferSize/2];

            recorder.read(testData, 0, testData.length);
            data.addData(testData);

            if (Calendar.getInstance().getTimeInMillis() - lastTime > 15000) {
                //recorder.stop();
                lastTime = Calendar.getInstance().getTimeInMillis();
                at.write(data.getData(), 0, data.getData().length);
                at.play();
                Log.v("SoundWave", "Playing");
                /*try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                //at.stop();
                //recorder.startRecording();
            }

            /*recorder.read(readData, 0, bufferSize);
            data.addData(readData);*/
        }

        private void drawWaveform(Canvas canvas) {
            short[] wave = data.getData();

            canvas.drawColor(backgroundColor);

            final int halfHeight = height/2;
            final float scale = halfHeight/32768.0f;
            float startY, stopY;
            for (int i = 0; i < width-1; i++) {
                startY = wave[i]*scale+halfHeight;
                stopY = wave[i+1]*scale+halfHeight;
                canvas.drawLine(i, startY, i+1, stopY, paint);
            }
        }
    }
}
