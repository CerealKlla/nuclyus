package com.github.cerealklla.nuclyus.welcome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.fml.ModList;

/**
 * Builds the "Kyngdoms, A History" written book -- exposed via {@code api.Nuclyus#createWelcomeBook}.
 * Lives here, not in any one feature mod, because it only ever checks {@code
 * ModList.isLoaded(modId)} per mod and assembles plain vanilla {@code WrittenBookContent} -- it has
 * no real tie to any single mod's own classes/state, and Nuclyus is already hard-imported by every
 * mod that could plausibly want it.
 *
 * <p>Moved here 2026-11-11, its second move the same day. Originally the sole contents of a
 * mod-managed "Welcome" Kyt (Kyt's own {@code welcome.WelcomeListener}), auto-granted once on a
 * player's true first login -- removed entirely per explicit user instruction after a real
 * incident: "move the book into the tutorial hallway chest instead of a kyt and turn that off
 * altogether." A first pass moved it to Arryval (the only remaining caller), but that was still the
 * wrong home -- explicit follow-up: "move the welcome book creation to nuclyus first off, then call
 * it from there." Arryval's {@code onboarding.TutorialRecipeBootstrap} now seeds the built book
 * straight into the tutorial WyrldRecipe's own common-loot pool, so a new player finds it in the
 * tutorial room's loot chest.
 *
 * <p>Called fresh each time it's needed (at {@code ServerStartingEvent}, via Arryval's bootstrap)
 * so it always reflects whichever mods are actually loaded right now -- each mod's section only
 * appears if {@code ModList.get().isLoaded(modId)} is true, in a fixed order, with Kyt's own
 * section always last.
 *
 * <p>Built against this exact Minecraft version's real decompiled {@code WrittenBookContent}
 * signature (confirmed, not guessed) -- {@code pages} is {@code List<Filterable<Component>>}, not
 * {@code List<Filterable<String>>} (that shape belongs to the different, player-editable {@code
 * WritableBookContent}). No per-page length enforcement is needed at the engine level ({@code
 * PAGE_LENGTH} is 32767) -- {@link #PAGE_CHAR_BUDGET} is purely for in-game readability, since
 * vanilla's own book UI already word-wraps within a page.
 */
public final class WelcomeBookContent {

    private static final String TITLE = "Kyngdoms, A History";
    private static final String AUTHOR = "Server";
    private static final int PAGE_CHAR_BUDGET = 400;

    private WelcomeBookContent() {
    }

    public static ItemStack build() {
        List<String> pages = new ArrayList<>();
        pages.addAll(wrapIntoPages(INTRO));

        for (Map.Entry<String, String> section : modSections().entrySet()) {
            if (ModList.get().isLoaded(section.getKey())) {
                pages.addAll(wrapIntoPages(section.getValue()));
            }
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> bookPages = pages.stream()
                .map(page -> Filterable.passThrough((Component) Component.literal(page)))
                .toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(TITLE), AUTHOR, 0, bookPages, true));
        return book;
    }

    private static final String INTRO = "Welcome to Kyngdoms! This book is a quick guide to what's "
            + "actually running on this server. Each section below only shows up if that system is "
            + "installed, so what you see here matches what you can actually do.";

    /** Mod id -> plain, factual "how to play" copy. Order here is the order sections appear in. */
    private static Map<String, String> modSections() {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("cartographyr", "Cartographyr: This server tracks and names real places in "
                + "the world -- settlements, regions, points of interest. As you explore, you build "
                + "up knowledge of what's around you, which other systems use to show you distances "
                + "and directions.");
        sections.put("lyfe", "Lyfe: You have skills that level up as you play -- things like "
                + "Lumberjack, Miner, and Cook -- each tracked separately with its own XP and perks. "
                + "Press your Skills keybind to check your progress at any time.");
        sections.put("settlemynts", "Settlemynts: You can found your own settlement, mark out "
                + "plots of land within it, and build a Town Hall to manage who owns what. "
                + "Settlements can also post guards to defend their grounds.");
        sections.put("yconomics", "Yconomics: Villagers run a real trade economy here -- prices "
                + "and trades change based on supply and demand, not fixed vanilla tables. Pay "
                + "attention to what a village actually needs before you trade.");
        sections.put("protectyons", "Protectyons: Claimed and protected areas on this server "
                + "actually stop unwanted building, breaking, and griefing -- if you own or are a "
                + "guest somewhere, you're covered; if not, don't expect to touch the terrain.");
        sections.put("blueprynts", "Blueprynts: You can design a building as a reusable Blueprint, "
                + "then place a Construction Site to have it actually built block-by-block over "
                + "time, rather than placing every block by hand.");
        sections.put("kyt", "Kyt: A Kyt is a saved loadout -- a named set of armor, an offhand "
                + "item, and a hotbar you can equip in one action instead of digging through chests. "
                + "Ask an admin about available Kyts, or build your own in the Kyt Editor.");
        return sections;
    }

    /** Greedy word-wrap into page-sized chunks (budgeted for readability, not an engine limit). */
    private static List<String> wrapIntoPages(String text) {
        List<String> pages = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > PAGE_CHAR_BUDGET) {
                pages.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            pages.add(current.toString());
        }
        return pages;
    }
}
