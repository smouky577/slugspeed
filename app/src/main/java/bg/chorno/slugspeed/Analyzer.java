package bg.chorno.slugspeed;

/**
 * Намира началото на звука от изстрела и от попадението и смята скоростта.
 *
 * Измерено време (телефонът записва и двата звука):
 *   dt = D/v + Dpt/c - Dpm/c
 * където D = дулото→мишената, Dpt = телефон→мишена, Dpm = телефон→дуло, c = скорост на звука.
 *   v = D / (dt - (Dpt - Dpm)/c)
 */
public final class Analyzer {

    public static final class Params {
        public double dist = 20;          // m, дуло -> мишена
        public double tempC = 20;         // °C
        public double phoneMuzzle = 0;    // m
        public double phoneTarget = 20;   // m
        public double vMin = 80;          // m/s
        public double vMax = 450;         // m/s
        public double sens = 0.25;        // праг за попадението като част от най-силния звук в прозореца
    }

    public static final class Result {
        public int shot = -1, impact = -1, winA = -1, winB = -1;
        public boolean found;
    }

    private Analyzer() { }

    public static double soundSpeed(double tC) {
        return 331.3 * Math.sqrt(1.0 + tC / 273.15);
    }

    public static double offset(Params p) {
        return (p.phoneTarget - p.phoneMuzzle) / soundSpeed(p.tempC);
    }

    public static double dt(int shot, int impact, int sr) {
        return (impact - shot) / (double) sr;
    }

    /** Скорост в m/s или NaN, ако е невъзможна. */
    public static double speed(int shot, int impact, int sr, Params p) {
        double denom = dt(shot, impact, sr) - offset(p);
        if (denom <= 0) return Double.NaN;
        return p.dist / denom;
    }

    private static int abs(short s) { return s < 0 ? -s : s; }

    private static int maxAbs(short[] d, int a, int b) {
        a = Math.max(0, a);
        b = Math.min(d.length, b);
        int m = 0;
        for (int i = a; i < b; i++) { int v = abs(d[i]); if (v > m) m = v; }
        return m;
    }

    /**
     * Прецизира началото: праг = 30% от локалния пик (1 ms напред), после връщане назад
     * докато има непрекъснати пресичания на прага (прекъсване > 0.5 ms спира).
     */
    private static int refineOnset(short[] d, int idx, int sr) {
        int w = Math.max(1, sr / 1000);
        int peak = maxAbs(d, idx, idx + w);
        if (peak == 0) return idx;
        int thr = (int) (0.3 * peak);
        int first = idx;
        for (int i = idx; i < Math.min(d.length, idx + w); i++) {
            if (abs(d[i]) >= thr) { first = i; break; }
        }
        int onset = first;
        int limit = Math.max(0, first - sr / 100);
        for (int i = first; i >= limit; i--) {
            if (abs(d[i]) >= thr) onset = i;
            else if (onset - i > w / 2) break;
        }
        return onset;
    }

    public static int[] window(int shot, int len, int sr, Params p) {
        double off = offset(p);
        double tMin = p.dist / p.vMax + off;
        double tMax = p.dist / p.vMin + off;
        int a = shot + (int) (tMin * sr) - sr / 500;
        int b = shot + (int) (tMax * sr) + sr / 500;
        a = Math.max(a, shot + sr / 200);
        b = Math.min(b, len - 1);
        if (b <= a) b = Math.min(len - 1, a + sr / 100);
        return new int[]{a, b};
    }

    public static Result analyze(short[] d, int trig, int sr, Params p) {
        Result r = new Result();
        r.shot = refineOnset(d, trig, sr);
        // Прагът е пресечен при trig, затова началото не може да е след него
        if (r.shot > trig) r.shot = trig;

        int[] w = window(r.shot, d.length, sr, p);
        r.winA = w[0];
        r.winB = w[1];
        if (r.winB - r.winA < 4) return r;

        int blk = Math.max(1, sr / 4000);          // 0.25 ms блокове
        int hist = 40;                             // 10 ms история
        int base = Math.max(0, r.winA - hist * blk);
        int nb = (r.winB - base) / blk;
        if (nb <= hist) return r;
        double[] rms = new double[nb];
        for (int k = 0; k < nb; k++) {
            double s = 0;
            int o = base + k * blk;
            for (int i = 0; i < blk; i++) { double v = d[o + i]; s += v * v; }
            rms[k] = Math.sqrt(s / blk);
        }
        int kStart = (r.winA - base) / blk;
        double maxR = 0;
        for (int k = kStart; k < nb; k++) if (rms[k] > maxR) maxR = rms[k];
        if (maxR < 1) return r;

        // Кандидатите трябва да са достатъчно силни; печели най-рязкото нарастване спрямо предходните 10 ms
        double thr = p.sens * maxR;
        double runSum = 0;
        int runN = 0;
        for (int k = Math.max(0, kStart - hist); k < kStart; k++) { runSum += rms[k]; runN++; }
        int best = -1;
        double bestScore = 0;
        for (int k = kStart; k < nb; k++) {
            double prevMean = runN > 0 ? runSum / runN : 0;
            if (rms[k] >= thr) {
                double score = rms[k] / (prevMean + 1.0);
                if (score > bestScore) { bestScore = score; best = k; }
            }
            runSum += rms[k]; runN++;
            if (runN > hist) { runSum -= rms[k - hist]; runN--; }
        }
        if (best < 0) return r;
        int idx = base + best * blk;
        r.impact = refineOnset(d, Math.max(r.winA, idx - blk), sr);
        if (r.impact < r.winA) r.impact = r.winA;
        r.found = true;
        return r;
    }
}
