package com.naosyth.soundwavelive;

public class WaveData {
    private short[] data;

    public WaveData(int sizeInShorts) {
        data = new short[sizeInShorts];
    }

    public void addData(short[] newData) {
        int index = 0;
        for (index=0; index<data.length - newData.length; index++) {
            data[index] = data[index+newData.length];
        }
        for (int i=0; i<newData.length; i++) {
            data[index] = newData[i];
            index++;
        }
    }

    public short[] getData() {
        return data;
    }
}
