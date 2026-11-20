package com.drbaysinger.slugspeed;

/** Текстове на български и английски. */
public final class L10n {

    public final boolean bg;

    private String t(String b, String e) { return bg ? b : e; }

    public final String start, stop, statusOff, statusStarting, statusReady, statusNext, micDenied, micError;
    public final String pressStart, notFound, dtTooSmall, recording, dragHint, emptyWave, emptyZoom, shotLbl, impLbl;
    public final String series, avg, sd, es, min, max, energy, noShots, allShots, clearQ, yes, no;
    public final String settings, info, done, dist, temp, mass, advanced, phoneMuzzle, phoneTarget, vmin, vmax;
    public final String copied, shareTitle, help, uM, uMs, uG, needMass, speedFps;

    public L10n(boolean bulgarian) {
        bg = bulgarian;
        start = t("Старт", "Start");
        stop = t("■  Стоп", "■  Stop");
        statusOff = t("Изключен", "Off");
        statusStarting = t("Стартиране…", "Starting…");
        statusReady = t("Готов – стреляй", "Ready – fire");
        statusNext = t("Готов за следващия", "Ready for next shot");
        micDenied = t("Няма достъп до микрофона", "No microphone access");
        micError = t("Грешка с микрофона", "Microphone error");
        pressStart = t("Натисни „Старт“ и стреляй", "Press Start and fire");
        notFound = t("Ударът не е открит – докосни графиката", "Impact not found – tap the chart");
        dtTooSmall = t("Δt е твърде малко за това разстояние", "Δt is too short for this range");
        recording = t("ЗАПИС", "RECORDING");
        dragHint = t("плъзни маркерите за корекция", "drag markers to correct");
        emptyWave = t("Тук ще се покаже звукът от изстрела", "The shot waveform appears here");
        emptyZoom = t("Увеличение около удара", "Zoom around impact");
        shotLbl = t("изстрел", "shot");
        impLbl = t("удар", "impact");
        series = t("СЕРИЯ", "SERIES");
        avg = t("Средна", "Average");
        sd = "SD";
        es = "ES";
        min = t("Мин", "Min");
        max = t("Макс", "Max");
        energy = t("Енергия", "Energy");
        noShots = t("Още няма изстрели", "No shots yet");
        allShots = t("Всички изстрели", "All shots");
        clearQ = t("Да изчистя ли всички изстрели?", "Clear all shots?");
        yes = t("Да", "Yes");
        no = t("Не", "No");
        settings = t("Настройки", "Settings");
        info = t("Как се използва", "How to use");
        done = t("Готово", "Done");
        dist = t("Дуло → мишена (м)", "Muzzle → target (m)");
        temp = t("Температура (°C)", "Temperature (°C)");
        mass = t("Тегло на слъга (гр)", "Slug weight (g)");
        advanced = t("Разширени", "Advanced");
        phoneMuzzle = t("Телефон → дуло (м)", "Phone → muzzle (m)");
        phoneTarget = t("Телефон → мишена (м)", "Phone → target (m)");
        vmin = t("Мин. скорост (м/с)", "Min speed (m/s)");
        vmax = t("Макс. скорост (м/с)", "Max speed (m/s)");
        copied = t("Копирано", "Copied");
        uM = t("м", "m");
        uMs = t("м/с", "m/s");
        uG = t("гр", "g");
        needMass = t("Въведете теглото на слъга в Настройки", "Enter the slug weight in Settings");
        speedFps = t("Средна fps", "Average fps");
        shareTitle = t("Сподели резултатите", "Share results");
        help = bg
                ? "1. Измерете с ролетка разстоянието от дулото до мишената. 1% грешка в него дава ~1% грешка в скоростта.\n\n"
                + "2. Сложете телефона до себе си, на 0.3–1 м встрани от дулото, с микрофона към мишената.\n\n"
                + "3. В Настройки въведете разстоянието и температурата – от нея зависи скоростта на звука.\n\n"
                + "4. Въведете теглото на слъга в грамове (1 grain = 0.0648 гр), за да виждате енергията в джаули.\n\n"
                + "5. Мишената трябва да звучи рязко при удар: метална плочка, гонг, консерва.\n\n"
                + "6. Натиснете „Старт“ и стреляйте. Изстрелът трябва да надхвърли бялата чертичка на индикатора.\n\n"
                + "7. Зеленият маркер е изстрелът, червеният – ударът. Ако е сбъркан, плъзнете го по графиката.\n\n"
                + "Най-точно е при 15–40 м. При ехо от стени променете „Чувствителност за удара“."
                : "1. Measure the muzzle-to-target distance with a tape. A 1% error in it gives ~1% speed error.\n\n"
                + "2. Put the phone next to you, 0.3–1 m to the side of the muzzle, mic towards the target.\n\n"
                + "3. In Settings enter the distance and temperature – the speed of sound depends on it.\n\n"
                + "4. Enter the slug weight in grams (1 grain = 0.0648 g) to see the energy in joules.\n\n"
                + "5. The target must sound sharp on impact: steel plate, gong, can.\n\n"
                + "6. Press Start and fire. The shot must exceed the white tick on the meter.\n\n"
                + "7. Green marker = shot, red = impact. If one is wrong, drag it on the chart.\n\n"
                + "Most accurate at 15–40 m. With echoes from walls change \"Impact sensitivity\".";
    }

    public String soundSpeed(double c) {
        return String.format(java.util.Locale.US, bg ? "Скорост на звука: %.1f м/с" : "Speed of sound: %.1f m/s", c);
    }

    public String trigger(int pct) {
        return bg ? "Праг за изстрела: " + pct + "%" : "Shot trigger level: " + pct + "%";
    }

    public String sens(int pct) {
        return bg ? "Чувствителност за удара: " + pct + "% (по-малко = по-чувствително)"
                : "Impact sensitivity: " + pct + "% (lower = more sensitive)";
    }

    public String targetHint() { return bg ? "празно = дуло → мишена" : "empty = muzzle → target"; }

    public String shareHeader(double dist, double temp) {
        return String.format(java.util.Locale.US, bg ? "SlugSpeed – разстояние %.2f м, %.1f °C" : "SlugSpeed – range %.2f m, %.1f °C", dist, temp);
    }
}
