package com.Graphic.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.*;

public class DialogueManager {

    private static I18NBundle bundle;
    private static final Random random = new Random();


    private static final Set<String> finishedInitial = new HashSet<>();

    public static void load() {
        reload();

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



    public static String[] getInitialLines(String npcId) {
        int count = Integer.parseInt(bundle.get(npcId + ".initial.count"));
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            lines[i] = bundle.get(npcId + ".initial." + (i + 1));
        }
        return lines;
    }



    public static String getRandomPrecept(String npcId) {
        int count = Integer.parseInt(bundle.get(npcId + ".precept.count"));
        int pick  = random.nextInt(count) + 1;
        return bundle.get(npcId + ".precept." + pick);
    }



    public static boolean hasFinishedInitial(String npcId) {
        return finishedInitial.contains(npcId);
    }

    public static void markInitialDone(String npcId) {
        finishedInitial.add(npcId);
    }


    public static Set<String> getFinishedInitials() {
        return Collections.unmodifiableSet(finishedInitial);
    }

    public static void restoreFinishedInitials(Set<String> saved) {
        finishedInitial.clear();
        finishedInitial.addAll(saved);
    }
}
