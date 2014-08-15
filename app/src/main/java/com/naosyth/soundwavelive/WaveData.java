package com.naosyth.soundwavelive;

public class WaveData {
    private short[] data;

    public WaveData(int sizeInShorts) {
        data = new short[sizeInShorts];
    }

    public void addData(short[] newData) {
        int index = 0;
        float factor = 44100.0f/data.length;
        int numNew = (int)(newData.length/factor);

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
