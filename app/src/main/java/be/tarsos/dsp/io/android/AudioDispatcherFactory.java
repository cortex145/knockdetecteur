package be.tarsos.dsp.io.android;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class AudioDispatcherFactory {

    public static AudioDispatcher fromDefaultMicrophone(
            final int sampleRate, final int audioBufferSize, final int bufferOverlap) {
        int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBufferSize, audioBufferSize);

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(
                sampleRate, 16, 1, true, false);

        AndroidAudioInputStream stream = new AndroidAudioInputStream(audioRecord, format);
        return new AudioDispatcher(stream, audioBufferSize, bufferOverlap);
    }
}
