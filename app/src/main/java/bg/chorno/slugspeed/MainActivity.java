package bg.chorno.slugspeed;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements AudioEngine.Listener {

    // Палитра: тактическо зелено
    public static final int BG = 0xFF0E120D;
    public static final int CARD = 0xFF171D15;
    public static final int CARD2 = 0xFF20281D;
    public static final int LINE = 0xFF2B3527;
    public static final int ACCENT = 0xFF9BE15D;
    public static final int ON_ACCENT = 0xFF142106;
    public static final int TEXT = 0xFFE7EDE2;
    public static final int MUTED = 0xFF9AA893;
    public static final int GREY = 0xFF6B7566;
    public static final int DANGER = 0xFFE5484D;
    public static final int OK = 0xFF3DDC97;
    public static final int BLUE = 0xFFB4C4AA;

    private static final int REQ_MIC = 1;
    private static final double G_KG = 0.001;

    private static final String ABOUT_EN = "Donate to Dr. Baysinger. He promises to spend it on hookers and beer.";
    private static final String ABOUT_BG = "Дарете на Dr. Baysinger. Той обещава да ги изхарчи за курви и бело.";
    private static final String REVOLUT = "revolut.me/georgi09qc";
    private static final String[] BTC = {
            "bc1qr84akpfry7sjs6759wwq722fyert9apcarpst6",
            "bc1p6hp5q20u9t9upsjt7425wgqdrs0cr8wnnlf9atu24z92pzsnq85q62uc8e",
    };

    private enum Status { OFF, STARTING, READY, NEXT, ERROR }

    private SharedPreferences prefs;
    private AudioEngine engine;
    private L10n tr;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile float levelMax;
    private float dp;

    // изгледи
    private TextView tvSpeed, tvUnit, tvSub, tvSecond, tvSecondUnit, tileLbl5, tvStatus, tvSetup, tvSeries, tvChips, tvStart;
    private View statusDot, startDot;
    private LinearLayout startPill;
    private final TextView[] tileVal = new TextView[6];
    private LevelView level;
    private WaveformView wvAll, wvZoom;

    // състояние
    private Status status = Status.OFF;
    private String statusMsg = "";
    private short[] lastData;
    private int lastSr, lastTrig;
    private Analyzer.Result lastRes;
    private int lastListIndex = -1;
    private double lastV = Double.NaN, lastDt;
    private final ArrayList<Double> shots = new ArrayList<>();

    private final Runnable levelTick = new Runnable() {
        @Override public void run() {
            float l = levelMax;
            levelMax = 0;
            level.setLevel(l);
            ui.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        tr = new L10n(prefs.getBoolean("bg", true));
        boolean unproc = false;
        if (Build.VERSION.SDK_INT >= 24) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            unproc = "true".equals(am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        }
        engine = new AudioEngine(this, unproc);
        buildUi();
    }

    // ================= помощни за UI =================

    private int px(float v) { return Math.round(v * dp); }

    /** Текст с размер в dp – не зависи от системния размер на шрифта, за да се събира всичко без скрол. */
    private TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(px(radius));
        return g;
    }

    private Drawable ripple(Drawable content, float radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, round(0xFFFFFFFF, radius));
    }

    private void clickable(View v, float radius, View.OnClickListener l) {
        Drawable bg = v.getBackground();
        v.setBackground(ripple(bg != null ? bg : new ColorDrawable(0), radius));
        v.setClickable(true);
        v.setOnClickListener(l);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout col() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = col();
        GradientDrawable g = round(CARD, 18);
        g.setStroke(Math.max(1, px(1)), LINE);
        c.setBackground(g);
        c.setPadding(px(14), px(12), px(14), px(12));
        return c;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int top) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(w, h, weight);
        l.topMargin = px(top);
        return l;
    }

    private View iconButton(IconView.Glyph g, int color, float sizeDp, float boxDp, int bgColor, View.OnClickListener l) {
        IconView v = new IconView(this, g, color, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(px(boxDp), px(boxDp)));
        if (bgColor != 0) {
            GradientDrawable b = round(bgColor, boxDp / 2);
            b.setStroke(Math.max(1, px(1)), LINE);
            v.setBackground(b);
        }
        clickable(v, boxDp / 2, l);
        return v;
    }

    // ================= главен екран =================

    @SuppressWarnings("deprecation")
    private void buildUi() {
        LinearLayout root = col();
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            v.setPadding(ins.getSystemWindowInsetLeft(), ins.getSystemWindowInsetTop(),
                    ins.getSystemWindowInsetRight(), ins.getSystemWindowInsetBottom());
            return ins;
        });

        // --- горна лента: име | флаг + by Dr. Baysinger ---
        LinearLayout top = row();
        top.setPadding(px(16), px(2), px(12), px(2));
        TextView name = tv("SlugSpeed", 24, ACCENT, true);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        FlagView flag = new FlagView(this);
        flag.setBulgarian(tr.bg);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(px(30), px(18));
        top.addView(flag, flp);
        flag.setOnClickListener(v -> toggleLanguage());
        TextView by = tv("by Dr. Baysinger", 11, MUTED, false);
        by.setPadding(px(8), px(14), px(4), px(14));
        top.addView(by);
        by.setOnClickListener(v -> showAbout());
        root.addView(top);

        LinearLayout content = col();
        content.setPadding(px(12), 0, px(12), 0);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        content.addView(buildHero(), lp(-1, -2, 0, 2));
        content.addView(buildWave(), lp(-1, 0, 1f, 10));
        content.addView(buildSeries(), lp(-1, -2, 0, 10));

        root.addView(buildBottomBar());
        setContentView(root);

        updateDerived();
        renderAll();
    }

    private View buildHero() {
        LinearLayout c = card();

        LinearLayout r1 = row();
        tvSetup = tv("", 12, TEXT, false);
        tvSetup.setBackground(round(CARD2, 14));
        tvSetup.setPadding(px(10), px(6), px(10), px(6));
        tvSetup.setSingleLine(true);
        tvSetup.setEllipsize(TextUtils.TruncateAt.END);
        clickable(tvSetup, 14, v -> showSettings());
        r1.addView(tvSetup, new LinearLayout.LayoutParams(0, -2, 1f));
        statusDot = new View(this);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(px(8), px(8));
        dl.leftMargin = px(10);
        r1.addView(statusDot, dl);
        tvStatus = tv("", 12, MUTED, false);
        tvStatus.setPadding(px(6), 0, 0, 0);
        tvStatus.setSingleLine(true);
        r1.addView(tvStatus);
        c.addView(r1);

        // Две колони: скорост (м/с) | енергия (J) или fps – докосване на дясната сменя единицата
        LinearLayout r2 = row();
        r2.setPadding(0, px(10), 0, 0);
        Typeface big = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        // Число и единица на един ред, подравнени по основната линия на текста (gravity TOP + baselineAligned)
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.HORIZONTAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        left.setPadding(0, px(6), 0, px(6));
        tvSpeed = tv("0.0", 54, TEXT, true);
        tvSpeed.setTypeface(big);
        tvSpeed.setSingleLine(true);
        left.addView(tvSpeed);
        tvUnit = tv(tr.uMs, 17, MUTED, true);
        tvUnit.setPadding(px(5), 0, 0, 0);
        left.addView(tvUnit);
        r2.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        r2.addView(divider, new LinearLayout.LayoutParams(Math.max(1, px(1)), px(52)));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        right.setPadding(0, px(6), 0, px(6));
        tvSecond = tv("0.0", 54, ACCENT, true);
        tvSecond.setTypeface(big);
        tvSecond.setSingleLine(true);
        right.addView(tvSecond);
        tvSecondUnit = tv("J", 17, MUTED, true);
        tvSecondUnit.setPadding(px(5), 0, 0, 0);
        right.addView(tvSecondUnit);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, -2, 1f);
        rl.leftMargin = px(6);
        r2.addView(right, rl);
        clickable(right, 14, v -> toggleUnit());
        c.addView(r2, new LinearLayout.LayoutParams(-1, -2));

        tvSub = tv("", 14, MUTED, false);
        tvSub.setGravity(Gravity.CENTER_HORIZONTAL);
        tvSub.setPadding(0, px(4), 0, 0);
        tvSub.setSingleLine(true);
        tvSub.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(tvSub, new LinearLayout.LayoutParams(-1, -2));

        level = new LevelView(this);
        c.addView(level, lp(-1, px(12), 0, 12));
        return c;
    }

    private View buildWave() {
        LinearLayout c = card();
        c.setPadding(px(12), px(10), px(12), px(12));
        LinearLayout h = row();
        TextView t = tv(tr.recording, 12, MUTED, true);
        t.setLetterSpacing(0.08f);
        h.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        h.addView(tv(tr.dragHint, 11, GREY, false));
        c.addView(h);

        wvAll = new WaveformView(this, false);
        wvAll.setLabels(tr.shotLbl, tr.impLbl, tr.emptyWave);
        c.addView(wvAll, lp(-1, 0, 3f, 8));
        wvZoom = new WaveformView(this, true);
        wvZoom.setLabels(tr.shotLbl, tr.impLbl, tr.emptyZoom);
        c.addView(wvZoom, lp(-1, 0, 2f, 6));
        wvAll.setListener((isShot, idx, fin) -> onMarker(isShot, idx, fin, true));
        wvZoom.setListener((isShot, idx, fin) -> onMarker(isShot, idx, fin, false));
        return c;
    }

    private View buildSeries() {
        LinearLayout c = card();
        c.setPadding(px(14), px(6), px(6), px(12));
        LinearLayout h = row();
        tvSeries = tv("", 12, MUTED, true);
        tvSeries.setLetterSpacing(0.08f);
        h.addView(tvSeries, new LinearLayout.LayoutParams(0, -2, 1f));
        h.addView(iconButton(IconView.Glyph.LIST, MUTED, 18, 38, 0, v -> showAllShots()));
        h.addView(iconButton(IconView.Glyph.UNDO, MUTED, 18, 38, 0, v -> deleteLast()));
        h.addView(iconButton(IconView.Glyph.TRASH, MUTED, 18, 38, 0, v -> confirmClear()));
        h.addView(iconButton(IconView.Glyph.SHARE, MUTED, 18, 38, 0, v -> share()));
        c.addView(h);

        String[] labels = {tr.avg, tr.sd, tr.es, tr.min, tr.max, tr.energy};
        for (int r = 0; r < 2; r++) {
            LinearLayout tr2 = row();
            tr2.setPadding(0, 0, px(8), 0);
            for (int k = 0; k < 3; k++) {
                int i = r * 3 + k;
                LinearLayout tile = col();
                tile.setBackground(round(CARD2, 12));
                tile.setPadding(px(10), px(7), px(8), px(7));
                TextView l = tv(labels[i], 11, MUTED, false);
                tile.addView(l);
                if (i == 5) {
                    tileLbl5 = l;
                    clickable(tile, 12, v -> toggleUnit());
                    tile.setBackground(ripple(round(CARD2, 12), 12));
                }
                tileVal[i] = tv("—", 17, i == 0 ? ACCENT : TEXT, true);
                tileVal[i].setPadding(0, px(3), 0, 0);
                tileVal[i].setSingleLine(true);
                tile.addView(tileVal[i]);
                LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1f);
                if (k > 0) tl.leftMargin = px(6);
                tr2.addView(tile, tl);
            }
            c.addView(tr2, lp(-1, -2, 0, r == 0 ? 2 : 6));
        }

        tvChips = tv("", 12, MUTED, false);
        tvChips.setTypeface(Typeface.MONOSPACE);
        tvChips.setSingleLine(true);
        tvChips.setEllipsize(TextUtils.TruncateAt.END);
        tvChips.setPadding(px(2), px(8), px(8), 0);
        tvChips.setOnClickListener(v -> showAllShots());
        c.addView(tvChips, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    private View buildBottomBar() {
        LinearLayout b = row();
        b.setPadding(px(12), px(10), px(12), px(12));
        b.addView(iconButton(IconView.Glyph.GEAR, TEXT, 22, 54, CARD2, v -> showSettings()));

        startPill = row();
        startPill.setGravity(Gravity.CENTER);
        startDot = new View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFD32F2F);
        startDot.setBackground(dot);
        startPill.addView(startDot, new LinearLayout.LayoutParams(px(12), px(12)));
        tvStart = tv("", 17, ON_ACCENT, true);
        tvStart.setPadding(px(10), 0, 0, 0);
        startPill.addView(tvStart);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(0, px(54), 1f);
        sl.leftMargin = px(12);
        sl.rightMargin = px(12);
        b.addView(startPill, sl);
        startPill.setOnClickListener(v -> toggle());

        b.addView(iconButton(IconView.Glyph.INFO, TEXT, 22, 54, CARD2, v -> showInfo()));
        return b;
    }

    // ================= изобразяване на състоянието =================

    private void renderAll() {
        renderStart();
        renderStatus();
        renderSetup();
        updateStats();
        if (lastData != null && lastRes != null) {
            wvAll.setData(lastData, lastSr);
            wvZoom.setData(lastData, lastSr);
            int end = Math.min(lastData.length, lastRes.winB + lastSr / 50);
            wvAll.setRange(lastRes.shot - lastSr / 100, end);
            if (lastRes.impact >= 0) {
                centerZoom();
                applyResult(false);
            } else {
                int m = (lastRes.winA + lastRes.winB) / 2;
                wvZoom.setRange(m - lastSr / 200, m + lastSr / 200);
                wvAll.setMarkers(lastRes.shot, -1, lastRes.winA, lastRes.winB);
                wvZoom.setMarkers(lastRes.shot, -1, lastRes.winA, lastRes.winB);
                showSpeed(null, tr.notFound);
            }
        } else {
            showSpeed(null, tr.pressStart);
        }
    }

    private boolean useJ() { return prefs.getBoolean("unitJ", true) && mass() > 0; }

    private String energyText(double v) { return String.format(Locale.US, "%.1f J", 0.5 * mass() * G_KG * v * v); }

    private String fpsText(double v) { return String.format(Locale.US, "%.0f fps", v * 3.28084); }

    private void toggleUnit() {
        if (mass() <= 0) {
            Toast.makeText(this, tr.needMass, Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putBoolean("unitJ", !prefs.getBoolean("unitJ", true)).apply();
        if (!Double.isNaN(lastV)) showHero(lastV, lastDt);
        else showSpeed(null, tvSub.getText().toString());
        updateStats();
    }

    /** Голямото число в м/с, под него J или fps (докосване сменя), отдолу другата единица и Δt. */
    private void showHero(double v, double dt) {
        lastV = v;
        lastDt = dt;
        tvSpeed.setText(String.format(Locale.US, "%.1f", v));
        tvSpeed.setTextColor(TEXT);
        boolean j = useJ();
        double e = 0.5 * mass() * G_KG * v * v;
        tvSecond.setText(j ? String.format(Locale.US, "%.1f", e) : String.format(Locale.US, "%.0f", v * 3.28084));
        tvSecondUnit.setText(j ? "J" : "fps");
        tvSecond.setTextColor(ACCENT);
        StringBuilder sb = new StringBuilder();
        if (j) sb.append(fpsText(v)).append("  ·  ");
        else if (mass() > 0) sb.append(energyText(v)).append("  ·  ");
        sb.append(String.format(Locale.US, "Δt %.2f ms", dt));
        tvSub.setText(sb.toString());
    }

    private void showSpeed(String speed, String sub) {
        if (speed == null) {
            lastV = Double.NaN;
            tvSecond.setText(useJ() ? "0.0" : "0");
            tvSecondUnit.setText(useJ() ? "J" : "fps");
            tvSecond.setTextColor(GREY);
        }
        tvSpeed.setText(speed == null ? "0.0" : speed);
        tvSpeed.setTextColor(speed == null ? GREY : TEXT);
        tvSub.setText(sub);
    }

    private void renderStart() {
        boolean on = engine.isRunning();
        startPill.setBackground(ripple(round(on ? DANGER : ACCENT, 27), 27));
        tvStart.setText(on ? tr.stop : tr.start);
        tvStart.setTextColor(on ? 0xFFFFFFFF : ON_ACCENT);
        startDot.setVisibility(on ? View.GONE : View.VISIBLE);
        tvStart.setPadding(on ? 0 : px(10), 0, 0, 0);
    }

    private void renderStatus() {
        String s;
        int color;
        switch (status) {
            case STARTING: s = tr.statusStarting; color = ACCENT; break;
            case READY: s = tr.statusReady; color = OK; break;
            case NEXT: s = tr.statusNext; color = OK; break;
            case ERROR: s = statusMsg; color = DANGER; break;
            default: s = tr.statusOff; color = GREY;
        }
        tvStatus.setText(s);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        statusDot.setBackground(d);
    }

    private void setStatus(Status s, String msg) {
        status = s;
        statusMsg = msg == null ? "" : msg;
        renderStatus();
    }

    private void renderSetup() {
        Analyzer.Params p = params();
        StringBuilder sb = new StringBuilder();
        sb.append(fmt(p.dist)).append(" ").append(tr.uM).append("  ·  ").append(fmt(p.tempC)).append(" °C");
        double mass = mass();
        if (mass > 0) sb.append("  ·  ").append(fmt(mass)).append(" ").append(tr.uG);
        sb.append("  ✎");
        tvSetup.setText(sb.toString());
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf(Math.round(v));
        return String.format(Locale.US, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void toggleLanguage() {
        prefs.edit().putBoolean("bg", !tr.bg).apply();
        tr = new L10n(!tr.bg);
        buildUi();
    }

    // ================= настройки (в SharedPreferences) =================

    private double pref(String key, double def) {
        String s = prefs.getString(key, "").trim().replace(',', '.');
        if (s.isEmpty()) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    private double mass() { return pref("mass_g", 0); }
    private int trigPct() { return prefs.getInt("trig", 30); }
    private int sensPct() { return prefs.getInt("sens", 25); }

    private Analyzer.Params params() {
        Analyzer.Params p = new Analyzer.Params();
        p.dist = Math.max(0.5, pref("dist", 20));
        p.tempC = pref("temp", 20);
        p.phoneMuzzle = Math.max(0, pref("pm", 0.5));
        p.phoneTarget = Math.max(0, pref("pt", p.dist));
        p.vMin = Math.max(10, pref("vmin", 80));
        p.vMax = Math.max(p.vMin + 1, pref("vmax", 450));
        p.sens = sensPct() / 100.0;
        return p;
    }

    private void updateDerived() {
        Analyzer.Params p = params();
        double c = Analyzer.soundSpeed(p.tempC);
        engine.setTriggerLevel(trigPct() / 100f);
        level.setTrigger(trigPct() / 100f);
        double maxDelay = p.dist / p.vMin + Math.max(p.phoneTarget - p.phoneMuzzle, 0) / c;
        engine.setPostMs((int) (maxDelay * 1000) + 120);
    }

    // ================= диалози =================

    /** Долен панел със заоблени горни ъгли. */
    @SuppressWarnings("deprecation")
    private Dialog sheet(String title, View body, String button) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = col();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        float r = px(22);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        box.setBackground(bg);
        int pad = px(18);
        box.setPadding(pad, px(10), pad, pad);
        box.setOnApplyWindowInsetsListener((v, ins) -> {
            v.setPadding(pad, px(10), pad, pad + ins.getSystemWindowInsetBottom());
            return ins;
        });

        View handle = new View(this);
        handle.setBackground(round(LINE, 3));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(px(40), px(5));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(handle, hl);

        LinearLayout head = row();
        head.setPadding(0, px(12), 0, px(10));
        head.addView(tv(title, 20, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1f));
        box.addView(head);

        // weight 1 + wrap: ако съдържанието е дълго, превърта се само то, а бутонът остава долу
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        box.addView(sv, new LinearLayout.LayoutParams(-1, -2, 1f));

        TextView ok = tv(button, 17, ON_ACCENT, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackground(round(ACCENT, 25));
        clickable(ok, 25, v -> d.dismiss());
        box.addView(ok, lp(-1, px(50), 0, 16));

        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
        }
        return d;
    }

    /** Полета, между които Enter прескача – само докато следващото е празно (първоначално попълване). */
    private final ArrayList<EditText> chain = new ArrayList<>();

    private void hideKeyboard(View v) {
        android.view.inputmethod.InputMethodManager im =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private EditText field(LinearLayout parent, String name, String hint, String key, String def, boolean signed,
                           boolean inChain, Runnable onChange) {
        LinearLayout r = row();
        r.setPadding(0, px(5), 0, px(5));
        LinearLayout labels = col();
        labels.addView(tv(name, 14, TEXT, false));
        if (hint != null) {
            TextView h = tv(hint, 11, GREY, false);
            h.setPadding(0, px(3), 0, 0);
            labels.addView(h);
        }
        r.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        EditText e = new EditText(this);
        int type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        if (signed) type |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        e.setInputType(type);
        e.setText(prefs.getString(key, def));
        e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        e.setTextColor(TEXT);
        e.setHintTextColor(GREY);
        e.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        e.setGravity(Gravity.CENTER);
        e.setSelectAllOnFocus(true);
        e.setSingleLine(true);
        e.setBackground(round(CARD2, 12));
        e.setPadding(px(10), px(10), px(10), px(10));
        r.addView(e, new LinearLayout.LayoutParams(px(104), -2));
        parent.addView(r);
        if (inChain) chain.add(e);
        e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        e.setOnEditorActionListener((v, action, ev) -> {
            // Към следващото поле само ако е празно; иначе – край: скрива клавиатурата и курсора
            int i = chain.indexOf(e);
            if (i >= 0) {
                for (int k = i + 1; k < chain.size(); k++) {
                    if (chain.get(k).getText().toString().trim().isEmpty()) {
                        chain.get(k).requestFocus();
                        return true;
                    }
                }
            }
            hideKeyboard(e);
            e.clearFocus();
            parent.requestFocus();
            return true;
        });
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                prefs.edit().putString(key, s.toString()).apply();
                if (onChange != null) onChange.run();
            }
        });
        return e;
    }

    private SeekBar slider(LinearLayout parent, int min, int max, int value) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(value - min);
        sb.setProgressTintList(ColorStateList.valueOf(ACCENT));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        sb.setPadding(px(4), px(10), px(4), px(10));
        parent.addView(sb, new LinearLayout.LayoutParams(-1, -2));
        return sb;
    }

    private TextView sectionTitle(LinearLayout parent, String s, int top) {
        TextView t = tv(s.toUpperCase(tr.bg ? new Locale("bg") : Locale.US), 12, ACCENT, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(0, px(top), 0, px(4));
        parent.addView(t);
        return t;
    }

    private void showSettings() {
        LinearLayout body = col();
        // Самият панел поема фокуса, за да не мига курсор в никое поле
        body.setFocusable(true);
        body.setFocusableInTouchMode(true);
        body.setOnClickListener(v -> { hideKeyboard(v); v.requestFocus(); });
        chain.clear();
        TextView sound = tv("", 12, GREY, false);
        Runnable onChange = () -> {
            sound.setText(tr.soundSpeed(Analyzer.soundSpeed(params().tempC)));
            updateDerived();
            renderSetup();
        };
        field(body, tr.dist, null, "dist", "20", false, true, onChange);
        field(body, tr.temp, null, "temp", "20", true, true, onChange);
        sound.setPadding(0, 0, 0, px(4));
        body.addView(sound);
        field(body, tr.mass, null, "mass_g", "", false, true, onChange);

        sectionTitle(body, tr.bg ? "Засичане" : "Detection", 14);
        TextView tTrig = tv(tr.trigger(trigPct()), 13, MUTED, false);
        body.addView(tTrig);
        SeekBar sTrig = slider(body, 5, 100, trigPct());
        TextView tSens = tv(tr.sens(sensPct()), 13, MUTED, false);
        tSens.setPadding(0, px(6), 0, 0);
        body.addView(tSens);
        SeekBar sSens = slider(body, 5, 90, sensPct());
        SeekBar.OnSeekBarChangeListener sl = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.edit().putInt("trig", sTrig.getProgress() + 5).putInt("sens", sSens.getProgress() + 5).apply();
                tTrig.setText(tr.trigger(trigPct()));
                tSens.setText(tr.sens(sensPct()));
                updateDerived();
            }
            public void onStartTrackingTouch(SeekBar sb) { }
            public void onStopTrackingTouch(SeekBar sb) { }
        };
        sTrig.setOnSeekBarChangeListener(sl);
        sSens.setOnSeekBarChangeListener(sl);

        sectionTitle(body, tr.advanced, 14);
        field(body, tr.phoneMuzzle, null, "pm", "0.5", false, false, onChange);
        field(body, tr.phoneTarget, tr.targetHint(), "pt", "", false, false, onChange);
        field(body, tr.vmin, null, "vmin", "80", false, false, onChange);
        field(body, tr.vmax, null, "vmax", "450", false, false, onChange);
        onChange.run();

        Dialog d = sheet(tr.settings, body, tr.done);
        body.post(body::requestFocus);
        d.setOnDismissListener(x -> {
            updateDerived();
            renderSetup();
            if (lastData != null) reanalyze();
            else showSpeed(null, tr.pressStart);
            updateStats();
        });
        d.show();
    }

    private void showInfo() {
        TextView t = tv(tr.help, 14, TEXT, false);
        t.setLineSpacing(px(3), 1f);
        sheet(tr.info, t, "OK").show();
    }

    private void showAbout() {
        LinearLayout body = col();
        TextView en = tv(ABOUT_EN, 14, TEXT, false);
        en.setLineSpacing(px(2), 1f);
        body.addView(en);
        TextView bgT = tv(ABOUT_BG, 14, TEXT, false);
        bgT.setLineSpacing(px(2), 1f);
        bgT.setPadding(0, px(12), 0, 0);
        body.addView(bgT);
        sectionTitle(body, "Revolut", 20);
        copyLine(body, REVOLUT);
        sectionTitle(body, "BTC address", 16);
        for (String a : BTC) copyLine(body, a);
        sheet("About", body, "OK").show();
    }

    private void copyLine(LinearLayout parent, String value) {
        TextView t = tv(value, 13, ACCENT, false);
        t.setBackground(round(CARD2, 10));
        t.setPadding(px(12), px(10), px(12), px(10));
        clickable(t, 10, v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("address", value));
            Toast.makeText(this, tr.copied, Toast.LENGTH_SHORT).show();
        });
        parent.addView(t, lp(-1, -2, 0, 6));
    }

    private void showAllShots() {
        if (shots.isEmpty()) return;
        TextView t = tv(shotList(shots.size()), 14, TEXT, false);
        t.setTypeface(Typeface.MONOSPACE);
        t.setLineSpacing(px(4), 1f);
        sheet(tr.allShots + " · " + shots.size(), t, "OK").show();
    }

    private void confirmClear() {
        if (shots.isEmpty()) return;
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage(tr.clearQ)
                .setPositiveButton(tr.yes, (d, w) -> clearAll())
                .setNegativeButton(tr.no, null)
                .show();
    }

    // ================= микрофон =================

    private void toggle() {
        if (engine.isRunning()) {
            stopListening();
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        } else {
            startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_MIC) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) startListening();
            else setStatus(Status.ERROR, tr.micDenied);
        }
    }

    private void startListening() {
        updateDerived();
        engine.start();
        setStatus(Status.STARTING, null);
        renderStart();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ui.removeCallbacks(levelTick);
        ui.post(levelTick);
    }

    private void stopListening() {
        engine.stop();
        setStatus(Status.OFF, null);
        renderStart();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ui.removeCallbacks(levelTick);
        level.setLevel(0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (engine.isRunning()) stopListening();
    }

    @Override
    public void onLevel(float peak) {
        if (peak > levelMax) levelMax = peak;
    }

    @Override
    public void onStarted(int sampleRate, String source) {
        ui.post(() -> setStatus(Status.READY, null));
    }

    @Override
    public void onError(String msg) {
        ui.post(() -> {
            setStatus(Status.ERROR, tr.micError);
            renderStart();
            ui.removeCallbacks(levelTick);
        });
    }

    @Override
    public void onCapture(short[] data, int triggerIndex, int sampleRate) {
        ui.post(() -> {
            lastData = data;
            lastSr = sampleRate;
            lastTrig = triggerIndex;
            lastListIndex = -1;
            reanalyze();
            engine.rearm();
            if (engine.isRunning()) setStatus(Status.NEXT, null);
        });
    }

    // ================= анализ =================

    private void reanalyze() {
        Analyzer.Params p = params();
        lastRes = Analyzer.analyze(lastData, lastTrig, lastSr, p);
        wvAll.setData(lastData, lastSr);
        wvZoom.setData(lastData, lastSr);
        int end = Math.min(lastData.length, lastRes.winB + lastSr / 50);
        wvAll.setRange(lastRes.shot - lastSr / 100, end);
        if (!lastRes.found) {
            lastRes.impact = -1;
            showSpeed(null, tr.notFound);
            wvAll.setMarkers(lastRes.shot, -1, lastRes.winA, lastRes.winB);
            int m = (lastRes.winA + lastRes.winB) / 2;
            wvZoom.setRange(m - lastSr / 200, m + lastSr / 200);
            wvZoom.setMarkers(lastRes.shot, -1, lastRes.winA, lastRes.winB);
            if (lastListIndex >= 0) {
                shots.remove(lastListIndex);
                lastListIndex = -1;
                updateStats();
            }
            return;
        }
        centerZoom();
        applyResult(true);
    }

    private void centerZoom() {
        int half = lastSr * 3 / 1000;  // ±3 ms
        wvZoom.setRange(lastRes.impact - half, lastRes.impact + half);
    }

    private void onMarker(boolean isShot, int idx, boolean finished, boolean fromOverview) {
        if (lastRes == null) return;
        if (isShot) lastRes.shot = idx; else lastRes.impact = idx;
        if (lastRes.impact < 0) return;
        if (fromOverview && !isShot) centerZoom();
        applyResult(finished);
    }

    private void applyResult(boolean commit) {
        Analyzer.Params p = params();
        wvAll.setMarkers(lastRes.shot, lastRes.impact, lastRes.winA, lastRes.winB);
        wvZoom.setMarkers(lastRes.shot, lastRes.impact, lastRes.winA, lastRes.winB);
        double v = Analyzer.speed(lastRes.shot, lastRes.impact, lastSr, p);
        double dt = Analyzer.dt(lastRes.shot, lastRes.impact, lastSr) * 1000;
        if (Double.isNaN(v) || v > 2000) {
            showSpeed(null, tr.dtTooSmall);
            return;
        }
        showHero(v, dt);
        if (commit) {
            if (lastListIndex >= 0 && lastListIndex < shots.size()) shots.set(lastListIndex, v);
            else { shots.add(v); lastListIndex = shots.size() - 1; }
            updateStats();
        }
    }

    // ================= серия =================

    private void updateStats() {
        int n = shots.size();
        tvSeries.setText(tr.series + (n > 0 ? "  ·  " + n : ""));
        tileLbl5.setText(useJ() ? tr.energy : tr.speedFps);
        if (n == 0) {
            for (TextView t : tileVal) t.setText("—");
            tvChips.setTypeface(Typeface.DEFAULT);
            tvChips.setText(tr.noShots);
            return;
        }
        double sum = 0, mn = Double.MAX_VALUE, mx = 0;
        for (double v : shots) { sum += v; mn = Math.min(mn, v); mx = Math.max(mx, v); }
        double avg = sum / n;
        double var = 0;
        for (double v : shots) var += (v - avg) * (v - avg);
        double sd = n > 1 ? Math.sqrt(var / (n - 1)) : 0;
        tileVal[0].setText(String.format(Locale.US, "%.1f", avg));
        tileVal[1].setText(String.format(Locale.US, "%.1f", sd));
        tileVal[2].setText(String.format(Locale.US, "%.1f", mx - mn));
        tileVal[3].setText(String.format(Locale.US, "%.1f", mn));
        tileVal[4].setText(String.format(Locale.US, "%.1f", mx));
        if (useJ()) {
            double e = 0;
            for (double v : shots) e += 0.5 * mass() * G_KG * v * v;
            tileVal[5].setText(String.format(Locale.US, "%.1f J", e / n));
        } else {
            tileVal[5].setText(fpsText(avg));
        }
        tvChips.setTypeface(Typeface.MONOSPACE);
        StringBuilder l = new StringBuilder();
        for (int i = n - 1; i >= 0 && i >= n - 8; i--) {
            if (l.length() > 0) l.append("   ");
            l.append(String.format(Locale.US, "#%d %.1f", i + 1, shots.get(i)));
        }
        tvChips.setText(l.toString());
    }

    private String shotList(int count) {
        int n = shots.size();
        double mass = mass();
        StringBuilder l = new StringBuilder();
        for (int i = n - 1; i >= 0 && i >= n - count; i--) {
            double v = shots.get(i);
            l.append(String.format(Locale.US, "#%-3d %6.1f %s  %4.0f fps", i + 1, v, tr.uMs, v * 3.28084));
            if (mass > 0) l.append(String.format(Locale.US, "  %5.1f J", 0.5 * mass * G_KG * v * v));
            l.append('\n');
        }
        return l.toString().trim();
    }

    private void deleteLast() {
        if (shots.isEmpty()) return;
        shots.remove(shots.size() - 1);
        lastListIndex = -1;
        updateStats();
    }

    private void clearAll() {
        shots.clear();
        lastListIndex = -1;
        updateStats();
    }

    private void share() {
        if (shots.isEmpty()) return;
        Analyzer.Params p = params();
        int n = shots.size();
        StringBuilder sb = new StringBuilder(tr.shareHeader(p.dist, p.tempC)).append('\n');
        sb.append(tr.series).append(": ").append(n).append('\n');
        sb.append(tr.avg).append(": ").append(tileVal[0].getText()).append(" ").append(tr.uMs).append(", SD ").append(tileVal[1].getText())
                .append(", ES ").append(tileVal[2].getText()).append('\n');
        if (mass() > 0) sb.append(tr.energy).append(": ").append(tileVal[5].getText()).append('\n');
        sb.append('\n').append(shotList(n));
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(i, tr.shareTitle));
    }
}
