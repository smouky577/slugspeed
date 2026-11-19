package bg.chorno.slugspeed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Малки линейни икони, нарисувани с Canvas (без библиотека с икони). */
public class IconView extends View {

    public enum Glyph { GEAR, INFO, TRASH, SHARE, UNDO, LIST }

    private final Glyph glyph;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float sizeDp;

    public IconView(Context c, Glyph g, int color, float sizeDp) {
        super(c);
        glyph = g;
        this.sizeDp = sizeDp;
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setColor(int color) { p.setColor(color); invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float w = sizeDp * d;
        c.save();
        c.translate((getWidth() - w) / 2f, (getHeight() - w) / 2f);
        p.setStrokeWidth(w * 0.09f);
        p.setStyle(Paint.Style.STROKE);
        switch (glyph) {
            case GEAR: {
                float cx = w / 2, cy = w / 2;
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x0 = cx + (float) Math.cos(a) * w * 0.30f, y0 = cy + (float) Math.sin(a) * w * 0.30f;
                    float x1 = cx + (float) Math.cos(a) * w * 0.44f, y1 = cy + (float) Math.sin(a) * w * 0.44f;
                    p.setStrokeWidth(w * 0.13f);
                    c.drawLine(x0, y0, x1, y1, p);
                }
                p.setStrokeWidth(w * 0.09f);
                c.drawCircle(cx, cy, w * 0.29f, p);
                c.drawCircle(cx, cy, w * 0.11f, p);
                break;
            }
            case INFO: {
                c.drawCircle(w / 2, w / 2, w * 0.42f, p);
                c.drawLine(w / 2, w * 0.45f, w / 2, w * 0.72f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(w / 2, w * 0.30f, w * 0.06f, p);
                break;
            }
            case TRASH: {
                c.drawLine(w * 0.15f, w * 0.25f, w * 0.85f, w * 0.25f, p);
                c.drawLine(w * 0.4f, w * 0.12f, w * 0.6f, w * 0.12f, p);
                Path t = new Path();
                t.moveTo(w * 0.25f, w * 0.25f); t.lineTo(w * 0.3f, w * 0.88f);
                t.lineTo(w * 0.7f, w * 0.88f); t.lineTo(w * 0.75f, w * 0.25f);
                c.drawPath(t, p);
                break;
            }
            case SHARE: {
                float r = w * 0.11f;
                float ax = w * 0.74f, ay = w * 0.2f, bx = w * 0.26f, by = w * 0.5f, cx = w * 0.74f, cy = w * 0.8f;
                c.drawLine(bx, by, ax, ay, p);
                c.drawLine(bx, by, cx, cy, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(ax, ay, r, p); c.drawCircle(bx, by, r, p); c.drawCircle(cx, cy, r, p);
                break;
            }
            case UNDO: {
                RectF o = new RectF(w * 0.2f, w * 0.22f, w * 0.84f, w * 0.86f);
                c.drawArc(o, 180, 250, false, p);
                Path a = new Path();
                a.moveTo(w * 0.06f, w * 0.42f); a.lineTo(w * 0.2f, w * 0.56f); a.lineTo(w * 0.34f, w * 0.42f);
                c.drawPath(a, p);
                break;
            }
            case LIST: {
                for (int i = 0; i < 3; i++) {
                    float y = w * (0.25f + 0.25f * i);
                    c.drawLine(w * 0.32f, y, w * 0.88f, y, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(w * 0.14f, y, w * 0.06f, p);
                    p.setStyle(Paint.Style.STROKE);
                }
                break;
            }
        }
        c.restore();
    }
}
