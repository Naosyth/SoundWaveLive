package com.naosyth.soundwavelive;

import android.os.SystemClock;

public class WaveData {
    private short[] data;

    private long lastFrameTime = SystemClock.elapsedRealtime();

    public WaveData(int sizeInShorts) {
        data = new short[sizeInShorts];
    }

    public void addData(short[] newData) {
        long currentTime = SystemClock.elapsedRealtime();
        long dt = currentTime - lastFrameTime;
        lastFrameTime = currentTime;

        int index = 0;
        float factor = 44100.0f/data.length;
        int numNew = (int)(newData.length/factor);

        numNew = Math.min((int)(newData.length*(dt/1000.0f)), newData.length)/5;
        factor = newData.length/numNew;

        for (index = 0; index < data.length-numNew; index++) {
            data[index] = data[index+numNew];
        }
        for (int i = 0; i < numNew; i++) {
            data[index] = newData[(int)(i*factor)];
            index++;
        }
    }

    public short[] getData() {
        return data;
    }
}
