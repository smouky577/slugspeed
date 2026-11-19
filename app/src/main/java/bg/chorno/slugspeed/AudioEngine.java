package bg.chorno.slugspeed;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Непрекъснат запис от микрофона в кръгов буфер.
 * При звук над прага (изстрел) записва още postMs милисекунди и връща целия откъс.
 */
public class AudioEngine {

    public interface Listener {
        void onLevel(float peak);                       // 0..1
        void onCapture(short[] data, int triggerIndex, int sampleRate);
        void onError(String msg);
        void onStarted(int sampleRate, String source);
    }

    private final Listener listener;
    private final boolean unprocessedSupported;
    private volatile boolean running;
    private volatile boolean armed;
    private volatile float triggerLevel = 0.3f;
    private volatile int postMs = 800;
    private Thread thread;

    public AudioEngine(Listener l, boolean unprocessedSupported) {
        this.listener = l;
        this.unprocessedSupported = unprocessedSupported;
    }

    public void setTriggerLevel(float f) { triggerLevel = f; }
    public void setPostMs(int ms) { postMs = Math.max(200, Math.min(3400, ms)); }
    public void rearm() { armed = true; }
    public boolean isRunning() { return running; }

    public void start() {
        if (running) return;
        running = true;
        armed = true;
        thread = new Thread(this::loop, "audio");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void stop() {
        running = false;
        Thread t = thread;
        if (t != null) {
            try { t.join(1500); } catch (InterruptedException ignored) { }
            thread = null;
        }
    }

    private static AudioRecord open(int sr, int src) {
        int min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return null;
        int size = Math.max(min * 2, sr / 5 * 2);
        try {
            AudioRecord r = new AudioRecord(src, sr, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size);
            if (r.getState() == AudioRecord.STATE_INITIALIZED) return r;
            r.release();
        } catch (Exception ignored) { }
        return null;
    }

    private void loop() {
        AudioRecord rec = null;
        int sampleRate = 0;
        String srcName = "";
        int[] rates = {48000, 44100};
        int[] sources;
        String[] names;
        if (unprocessedSupported) {
            sources = new int[]{MediaRecorder.AudioSource.UNPROCESSED,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
            names = new String[]{"UNPROCESSED", "VOICE_RECOGNITION", "MIC"};
        } else {
            sources = new int[]{MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC};
            names = new String[]{"VOICE_RECOGNITION", "MIC"};
        }
        outer:
        for (int s = 0; s < sources.length; s++) {
            for (int sr : rates) {
                rec = open(sr, sources[s]);
                if (rec != null) { sampleRate = sr; srcName = names[s]; break outer; }
            }
        }
        if (rec == null) {
            running = false;
            listener.onError("Микрофонът не може да бъде отворен");
            return;
        }

        try {
            rec.startRecording();
        } catch (Exception e) {
            rec.release();
            running = false;
            listener.onError("Грешка при старт на записа: " + e.getMessage());
            return;
        }
        listener.onStarted(sampleRate, srcName);

        final int ringLen = sampleRate * 4;
        final short[] ring = new short[ringLen];
        final short[] buf = new short[sampleRate / 100];   // 10 ms
        final int pre = sampleRate / 10;                    // 100 ms преди изстрела
        long total = 0;
        long trigAbs = -1, endAbs = -1;
        long armStart = 0;
        boolean lastArmed = false;

        while (running) {
            int n = rec.read(buf, 0, buf.length);
            if (n < 0) { listener.onError("Грешка при четене от микрофона (" + n + ")"); break; }
            if (n == 0) continue;

            boolean nowArmed = armed;
            if (nowArmed && !lastArmed) armStart = total;
            lastArmed = nowArmed;

            int thr = (int) (triggerLevel * 32767f);
            int peak = 0;
            for (int i = 0; i < n; i++) {
                short s = buf[i];
                int a = s < 0 ? -s : s;
                if (a > peak) peak = a;
                ring[(int) (total % ringLen)] = s;
                if (nowArmed && trigAbs < 0 && a >= thr && total - armStart > sampleRate / 4) {
                    trigAbs = total;
                    endAbs = total + (long) postMs * sampleRate / 1000;
                }
                total++;
            }
            listener.onLevel(peak / 32768f);

            if (trigAbs >= 0 && total >= endAbs) {
                long from = Math.max(trigAbs - pre, Math.max(0, total - ringLen));
                int len = (int) (endAbs - from);
                short[] out = new short[len];
                for (int j = 0; j < len; j++) out[j] = ring[(int) ((from + j) % ringLen)];
                int trigIdx = (int) (trigAbs - from);
                trigAbs = -1;
                armed = false;
                lastArmed = false;
                listener.onCapture(out, trigIdx, sampleRate);
            }
        }
        try { rec.stop(); } catch (Exception ignored) { }
        rec.release();
        running = false;
    }
}
