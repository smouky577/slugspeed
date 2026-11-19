package bg.chorno.slugspeed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Тънък индикатор за нивото на звука с маркер за прага на изстрела. */
public class LevelView extends View {

    private float level, shown, trigger = 0.3f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public LevelView(Context c) { super(c); }

    public void setLevel(float l) {
        // бързо нагоре, плавно надолу
        shown = l > shown ? l : shown * 0.8f + l * 0.2f;
        level = l;
        invalidate();
    }

    public void setTrigger(float t) { trigger = t; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), H = getHeight(), h = H * 0.5f, top = H * 0.25f, rad = h / 2f;
        p.setColor(MainActivity.LINE);
        r.set(0, top, w, top + h);
        c.drawRoundRect(r, rad, rad, p);
        float f = Math.min(1f, shown);
        if (f > 0.002f) {
            p.setColor(f >= trigger ? MainActivity.DANGER : MainActivity.ACCENT);
            r.set(0, top, Math.max(h, w * f), top + h);
            c.drawRoundRect(r, rad, rad, p);
        }
        float x = w * trigger;
        p.setColor(MainActivity.TEXT);
        p.setStrokeWidth(Math.max(2f, H / 5f));
        c.drawLine(x, 0, x, H, p);
    }
}
