package bg.chorno.slugspeed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Флагче за смяна на езика: български трикольор или британски флаг. */
public class FlagView extends View {

    private boolean bulgarian = true;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FlagView(Context c) { super(c); }

    public void setBulgarian(boolean b) { bulgarian = b; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        p.setStyle(Paint.Style.FILL);
        if (bulgarian) {
            float band = h / 3f;
            p.setColor(0xFFFFFFFF); c.drawRect(0, 0, w, band, p);
            p.setColor(0xFF00966E); c.drawRect(0, band, w, band * 2, p);
            p.setColor(0xFFD62612); c.drawRect(0, band * 2, w, h, p);
        } else {
            p.setColor(0xFF012169); c.drawRect(0, 0, w, h, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xFFFFFFFF); p.setStrokeWidth(h * 0.22f);
            c.drawLine(0, 0, w, h, p); c.drawLine(w, 0, 0, h, p);
            p.setColor(0xFFC8102E); p.setStrokeWidth(h * 0.09f);
            c.drawLine(0, 0, w, h, p); c.drawLine(w, 0, 0, h, p);
            p.setStyle(Paint.Style.FILL);
            float wb = h * 0.34f, rb = h * 0.18f;
            p.setColor(0xFFFFFFFF);
            c.drawRect((w - wb) / 2, 0, (w + wb) / 2, h, p);
            c.drawRect(0, (h - wb) / 2, w, (h + wb) / 2, p);
            p.setColor(0xFFC8102E);
            c.drawRect((w - rb) / 2, 0, (w + rb) / 2, h, p);
            c.drawRect(0, (h - rb) / 2, w, (h + rb) / 2, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(getResources().getDisplayMetrics().density);
        p.setColor(0xFF3A4250);
        c.drawRect(0, 0, w, h, p);
    }
}
