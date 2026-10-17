package com.github.cerealklla.nuclyus.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;

/** Shared greedy word-wrap, pulled out of several near-identical copies across sibling mods'
 * Nuclyus menu tabs (Lyfe's Skills/Rune Codex, Myssions' Quest Log, Coryspondance's Mail). */
public final class TextWrap {

    private TextWrap() {
    }

    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }
}
