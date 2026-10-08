package be.tarsos.dsp.io.android;

import android.media.AudioRecord;
import java.io.IOException;
import java.io.InputStream;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

public class AndroidAudioInputStream implements TarsosDSPAudioInputStream {

    private final AudioRecord audioRecord;
    private final TarsosDSPAudioFormat format;
    private boolean closed = false;

    public AndroidAudioInputStream(AudioRecord audioRecord, TarsosDSPAudioFormat format) {
        this.audioRecord = audioRecord;
        this.format = format;
    }

    @Override
    public long skip(long bytesToSkip) throws IOException {
        throw new IOException("Skip not supported");
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) return -1;
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.startRecording();
        }
        return audioRecord.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {}
            audioRecord.release();
        }
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return format;
    }

    @Override
    public long getFrameLength() {
        return -1;
    }
}
