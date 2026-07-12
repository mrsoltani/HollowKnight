package com.Graphic.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.*;

public class DialogueManager {

    private static I18NBundle bundle;
    private static final Random random = new Random();

    // Tracks which NPCs have finished their initial dialogue
    private static final Set<String> finishedInitial = new HashSet<>();

    public static void load() {
        reload();
        // Auto-reload when language changes
        LocalizationManager.addListener(DialogueManager::reload);
    }

    private static void reload() {
        Locale locale = LocalizationManager.getCurrent() == LocalizationManager.Language.EN
            ? Locale.ENGLISH
            : Locale.FRENCH;
        bundle = I18NBundle.createBundle(
            Gdx.files.internal("i18n/dialogues"), locale, "UTF-8"
        );
    }

    // ── Initial dialogue (ordered lines, shown once) ─────────────────────

    public static String[] getInitialLines(String npcId) {
        int count = Integer.parseInt(bundle.get(npcId + ".initial.count"));
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            lines[i] = bundle.get(npcId + ".initial." + (i + 1));
        }
        return lines;
    }

    // ── Precepts (random single line, shown on repeat visits) ────────────

    public static String getRandomPrecept(String npcId) {
        int count = Integer.parseInt(bundle.get(npcId + ".precept.count"));
        int pick  = random.nextInt(count) + 1;
        return bundle.get(npcId + ".precept." + pick);
    }

    // ── State tracking ────────────────────────────────────────────────────

    public static boolean hasFinishedInitial(String npcId) {
        return finishedInitial.contains(npcId);
    }

    public static void markInitialDone(String npcId) {
        finishedInitial.add(npcId);
    }

    // Called by SaveManager to persist/restore state
    public static Set<String> getFinishedInitials() {
        return Collections.unmodifiableSet(finishedInitial);
    }

    public static void restoreFinishedInitials(Set<String> saved) {
        finishedInitial.clear();
        finishedInitial.addAll(saved);
    }
}
