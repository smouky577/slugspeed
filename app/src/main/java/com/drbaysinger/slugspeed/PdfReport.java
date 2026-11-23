package com.drbaysinger.slugspeed;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** PDF отчет за серията: статистика, графика на скоростта и таблица с изстрелите (A4). */
final class PdfReport {

    private static final int W = 595, H = 842;
    private static final float L = 40f, R = 555f;
    private static final int ACCENT = 0xFF9BE15D;

    private PdfReport() { }

    static void write(OutputStream out, List<Double> shots, Analyzer.Params p, double massG, L10n tr) throws IOException {
        PdfDocument doc = new PdfDocument();
        try {
            int pageNo = 1;
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
            Canvas c = page.getCanvas();

            Paint title = text(24, Color.BLACK, true);
            Paint header = text(14, Color.DKGRAY, true);
            Paint body = text(12, Color.BLACK, false);
            Paint label = text(10, Color.GRAY, false);
            Paint accent = new Paint();
            accent.setColor(ACCENT);

            float y = 50f;
            c.drawRect(L, y, L + 20f, y + 30f, accent);
            c.drawText(tr.pdfTitle, L + 30f, y + 24f, title);

            y += 65f;
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
            c.drawText(tr.pdfDate + ": " + date, L, y, body);
            y += 20f;
            c.drawText(tr.pdfShots + ": " + shots.size(), L, y, body);
            y += 20f;
            StringBuilder setup = new StringBuilder();
            setup.append(tr.pdfRange).append(": ").append(f2(p.dist)).append(' ').append(tr.uM)
                    .append("   ").append(tr.pdfTemp).append(": ").append(f1(p.tempC)).append(" °C");
            if (massG > 0) setup.append("   ").append(tr.pdfMass).append(": ").append(f2(massG)).append(' ').append(tr.uG);
            c.drawText(setup.toString(), L, y, body);

            // --- статистика ---
            int n = shots.size();
            double sum = 0, mn = Double.MAX_VALUE, mx = 0;
            for (double v : shots) { sum += v; mn = Math.min(mn, v); mx = Math.max(mx, v); }
            double avg = sum / n;
            double var = 0;
            for (double v : shots) var += (v - avg) * (v - avg);
            double sd = Math.sqrt(var / n);  // като ChronoMate: дели на n

            y += 35f;
            c.drawRect(L, y, R, y + 2f, accent);
            y += 25f;
            c.drawText(tr.pdfStats, L, y, header);
            float c1 = L, c2 = 230f, c3 = 420f;
            y += 28f;
            c.drawText(tr.avg + ": " + f1(avg) + " " + tr.uMs, c1, y, body);
            c.drawText(tr.max + ": " + f1(mx) + " " + tr.uMs, c2, y, body);
            c.drawText(tr.min + ": " + f1(mn) + " " + tr.uMs, c3, y, body);
            y += 22f;
            c.drawText("ES: " + f1(mx - mn) + " " + tr.uMs, c1, y, body);
            c.drawText("SD: " + f2(sd) + " " + tr.uMs, c2, y, body);
            c.drawText(tr.speedFps + ": " + String.format(Locale.US, "%.0f", avg * 3.28084), c3, y, body);
            if (massG > 0) {
                double e = 0;
                for (double v : shots) e += energy(massG, v);
                y += 22f;
                c.drawText(tr.energy + ": " + f2(e / n) + " J", c1, y, body);
            }

            // --- графика ---
            y += 45f;
            c.drawText(tr.pdfTrend + " (" + tr.uMs + ")", L, y, header);
            y += 22f;
            float gl = 80f, gw = R - gl, gh = 150f, gt = y;
            Paint bg = new Paint();
            bg.setColor(0xFFF7F7F7);
            c.drawRect(gl, gt, gl + gw, gt + gh, bg);

            double range = Math.max(mx - mn, 1);
            double dMin = Math.max(0, mn - range * 0.2), dMax = mx + range * 0.2;
            double dRange = Math.max(dMax - dMin, 0.1);

            label.setTextAlign(Paint.Align.RIGHT);
            c.drawText(tr.uMs, gl - 5f, gt - 5f, label);
            c.drawText(f1(dMax), gl - 5f, gt + 10f, label);
            c.drawText(f1(dMin), gl - 5f, gt + gh, label);
            label.setTextAlign(Paint.Align.CENTER);
            c.drawText(tr.pdfShotAxis, gl + gw / 2, gt + gh + 25f, label);

            float avgY = (float) (gt + gh - (avg - dMin) / dRange * gh);
            Paint avgLine = new Paint(Paint.ANTI_ALIAS_FLAG);
            avgLine.setColor(0xFFE55A16);
            avgLine.setStrokeWidth(1f);
            avgLine.setStyle(Paint.Style.STROKE);
            avgLine.setPathEffect(new DashPathEffect(new float[]{5f, 5f}, 0f));
            c.drawLine(gl, avgY, gl + gw, avgY, avgLine);

            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setColor(0xFF4CAF50);
            line.setStrokeWidth(2f);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeJoin(Paint.Join.ROUND);
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(0xFF2E7D32);
            Path path = new Path();
            for (int i = 0; i < n; i++) {
                float x = n == 1 ? gl + gw / 2 : gl + (float) i / (n - 1) * gw;
                float py = (float) (gt + gh - (shots.get(i) - dMin) / dRange * gh);
                if (i == 0) path.moveTo(x, py); else path.lineTo(x, py);
            }
            c.drawPath(path, line);
            for (int i = 0; i < n; i++) {
                float x = n == 1 ? gl + gw / 2 : gl + (float) i / (n - 1) * gw;
                float py = (float) (gt + gh - (shots.get(i) - dMin) / dRange * gh);
                c.drawCircle(x, py, 3f, dot);
            }

            // --- таблица ---
            y = gt + gh + 45f;
            tableHeader(c, y, header, massG > 0, tr);
            y += 35f;
            for (int i = 0; i < n; i++) {
                if (y > 800f) {
                    doc.finishPage(page);
                    page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNo).create());
                    c = page.getCanvas();
                    y = 50f;
                    tableHeader(c, y, header, massG > 0, tr);
                    y += 35f;
                }
                double v = shots.get(i);
                c.drawText(String.format(Locale.US, "%02d", i + 1), 50f, y, body);
                c.drawText(f1(v), 120f, y, body);
                c.drawText(String.format(Locale.US, "%.0f", v * 3.28084), 270f, y, body);
                if (massG > 0) c.drawText(f2(energy(massG, v)), 420f, y, body);
                y += 20f;
            }
            doc.finishPage(page);
            doc.writeTo(out);
        } finally {
            doc.close();
        }
    }

    private static void tableHeader(Canvas c, float y, Paint header, boolean withEnergy, L10n tr) {
        Paint bg = new Paint();
        bg.setColor(0xFFEEEEEE);
        c.drawRect(L, y, R, y + 20f, bg);
        c.drawText("#", 50f, y + 15f, header);
        c.drawText(tr.pdfVelocity + " (" + tr.uMs + ")", 120f, y + 15f, header);
        c.drawText("fps", 270f, y + 15f, header);
        if (withEnergy) c.drawText(tr.energy + " (J)", 420f, y + 15f, header);
    }

    private static Paint text(float size, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private static double energy(double massG, double v) { return 0.5 * massG * 0.001 * v * v; }

    private static String f1(double v) { return String.format(Locale.US, "%.1f", v); }

    private static String f2(double v) { return String.format(Locale.US, "%.2f", v); }
}
