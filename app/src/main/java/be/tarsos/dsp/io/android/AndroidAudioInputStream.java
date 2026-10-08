package be.tarsos.dsp.io.android;

import android.media.AudioRecord;

import java.io.IOException;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

public class AndroidAudioInputStream implements TarsosDSPAudioInputStream {

    private final AudioRecord audioRecord;
    private final TarsosDSPAudioFormat format;
    private boolean closed = false;
    private boolean started = false;

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

        if (!started) {
            audioRecord.startRecording();
            started = true;
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }

        int totalRead = 0;
        int attempts = 0;
        while (totalRead < len && attempts < 5 && !closed) {
            int read = audioRecord.read(b, off + totalRead, len - totalRead);
            if (read > 0) {
                totalRead += read;
            } else if (read == AudioRecord.ERROR_INVALID_OPERATION
                    || read == AudioRecord.ERROR_BAD_VALUE
                    || read == AudioRecord.ERROR_DEAD_OBJECT) {
                throw new IOException("AudioRecord read error: " + read);
            } else {
                attempts++;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
            }
        }

        return totalRead > 0 ? totalRead : len;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                if (started) {
                    audioRecord.stop();
                }
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
