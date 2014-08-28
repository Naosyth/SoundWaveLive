package com.naosyth.soundwavelive;

public class WaveData {
    private short[] data;

    public WaveData(int sizeInShorts) {
        data = new short[sizeInShorts];
    }

    public void addData(short[] newData) {
        float seconds = 5f;

        int halfLength = newData.length/2;

        int numSamples = (int)(11025*seconds)/halfLength;
        int numShorts = (halfLength) * numSamples;
        float factor = numShorts/data.length;
        int numNew = (int)((halfLength)/factor);

        int offset = data.length - numNew;
        for (int i = 0; i < offset; i++) {
            data[i] = data[i+numNew];
        }

        for (int i = 0; i < numNew; i++) {
            data[offset + i] = newData[halfLength + (int)(i*factor)];
        }
    }

    public short[] getData() {
        return data;
    }
}
