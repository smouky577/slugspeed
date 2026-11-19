package bg.chorno.slugspeed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Показва записа с маркери за изстрела и попадението. Маркерите се местят с пръст. */
public class WaveformView extends View {

    public interface OnMarkerMoved {
        void onMoved(boolean isShot, int index, boolean finished);
    }

    private short[] data;
    private int sr = 48000;
    private int viewStart, viewEnd;
    private int shot = -1, impact = -1, winA = -1, winB = -1;
    private final boolean zoom;
    private OnMarkerMoved listener;
    private boolean draggingShot;

    private final Paint wave = new Paint();
    private final Paint shotP = new Paint();
    private final Paint impP = new Paint();
    private final Paint winP = new Paint();
    private final Paint text = new Paint();
    private final Paint axis = new Paint();
    private final float dp;
    private String shotLbl = "shot", impLbl = "impact", emptyText = "";

    public WaveformView(Context c, boolean zoom) {
        super(c);
        this.zoom = zoom;
        dp = c.getResources().getDisplayMetrics().density;
        wave.setColor(MainActivity.BLUE);
        wave.setStrokeWidth(Math.max(1f, dp));
        shotP.setColor(MainActivity.OK);
        shotP.setStrokeWidth(2 * dp);
        impP.setColor(MainActivity.DANGER);
        impP.setStrokeWidth(2 * dp);
        winP.setColor(0x1F9BE15D);
        text.setColor(MainActivity.MUTED);
        text.setTextSize(11 * dp);
        text.setAntiAlias(true);
        axis.setColor(MainActivity.LINE);
        axis.setStrokeWidth(1);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF0A0D09);
        bg.setCornerRadius(10 * dp);
        bg.setStroke(Math.max(1, (int) dp), MainActivity.LINE);
        setBackground(bg);
        setClipToOutline(true);
    }

    public void setListener(OnMarkerMoved l) { listener = l; }

    public void setLabels(String shot, String impact, String empty) {
        shotLbl = shot;
        impLbl = impact;
        emptyText = empty;
        invalidate();
    }

    public void setData(short[] d, int sampleRate) {
        data = d;
        sr = sampleRate;
        invalidate();
    }

    public void setMarkers(int shot, int impact, int winA, int winB) {
        this.shot = shot;
        this.impact = impact;
        this.winA = winA;
        this.winB = winB;
        invalidate();
    }

    public void setRange(int a, int b) {
        if (data == null) return;
        viewStart = Math.max(0, a);
        viewEnd = Math.min(data.length, Math.max(viewStart + 2, b));
        invalidate();
    }

    public void clear() {
        data = null;
        invalidate();
    }

    private float xOf(int idx) {
        return (idx - viewStart) / (float) (viewEnd - viewStart) * getWidth();
    }

    private int idxOf(float x) {
        int i = viewStart + Math.round(x / getWidth() * (viewEnd - viewStart));
        return Math.max(viewStart, Math.min(viewEnd - 1, i));
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (data == null || viewEnd <= viewStart) {
            c.drawText(emptyText, (w - text.measureText(emptyText)) / 2f, h / 2f + 4 * dp, text);
            return;
        }
        float mid = h / 2f;
        c.drawLine(0, mid, w, mid, axis);

        if (winA >= 0 && winB > winA) {
            float xa = Math.max(0, xOf(winA)), xb = Math.min(w, xOf(winB));
            if (xb > xa) c.drawRect(xa, 0, xb, h, winP);
        }

        int peak = 1;
        for (int i = viewStart; i < viewEnd; i++) {
            int v = Math.abs((int) data[i]);
            if (v > peak) peak = v;
        }
        // корен квадратен - за да се вижда и по-тихото попадение до силния изстрел
        double norm = 1.0 / Math.sqrt(peak);
        int n = viewEnd - viewStart;
        if (n <= w) {
            float px = 0, py = mid;
            for (int i = 0; i < n; i++) {
                int v = data[viewStart + i];
                float x = i / (float) (n - 1) * w;
                float y = (float) (mid - Math.signum(v) * Math.sqrt(Math.abs(v)) * norm * (mid - 2));
                if (i > 0) c.drawLine(px, py, x, y, wave);
                px = x; py = y;
            }
        } else {
            for (int x = 0; x < w; x++) {
                int s0 = viewStart + (int) ((long) x * n / w);
                int s1 = viewStart + (int) ((long) (x + 1) * n / w);
                int mn = 0, mx = 0;
                for (int i = s0; i < s1 && i < viewEnd; i++) {
                    int v = data[i];
                    if (v < mn) mn = v;
                    if (v > mx) mx = v;
                }
                float y0 = (float) (mid - Math.sqrt(mx) * norm * (mid - 2));
                float y1 = (float) (mid + Math.sqrt(-mn) * norm * (mid - 2));
                c.drawLine(x, y0, x, Math.max(y1, y0 + 1), wave);
            }
        }

        if (shot >= viewStart && shot < viewEnd) {
            float x = xOf(shot);
            c.drawLine(x, 0, x, h, shotP);
            c.drawText(shotLbl, x + 4 * dp, 14 * dp, text);
        }
        if (impact >= viewStart && impact < viewEnd) {
            float x = xOf(impact);
            c.drawLine(x, 0, x, h, impP);
            c.drawText(impLbl, Math.min(x + 4 * dp, w - text.measureText(impLbl) - 4 * dp), 28 * dp, text);
        }
        double spanMs = n * 1000.0 / sr;
        String lbl = zoom ? String.format(java.util.Locale.US, "%.1f ms", spanMs)
                : String.format(java.util.Locale.US, "%.0f ms", spanMs);
        c.drawText(lbl, w - text.measureText(lbl) - 6 * dp, h - 6 * dp, text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (data == null || viewEnd <= viewStart) return false;
        int idx = idxOf(e.getX());
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                boolean shotVisible = shot >= viewStart && shot < viewEnd;
                boolean impVisible = impact >= viewStart && impact < viewEnd;
                if (zoom) {
                    draggingShot = shotVisible && !impVisible;
                } else if (shotVisible && impact >= 0) {
                    draggingShot = Math.abs(e.getX() - xOf(shot)) < Math.abs(e.getX() - xOf(impact));
                } else {
                    draggingShot = false;
                }
                move(idx, false);
                return true;
            case MotionEvent.ACTION_MOVE:
                move(idx, false);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                move(idx, true);
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void move(int idx, boolean finished) {
        if (draggingShot) shot = idx; else impact = idx;
        invalidate();
        if (listener != null) listener.onMoved(draggingShot, idx, finished);
    }
}
