package com.github.cerealklla.nuclyus.llm;

/**
 * The one short, hand-authored (not LLM-generated) World Lore document injected into every LLM call
 * across the suite that narrates this world -- moved here from Audyble's own {@code npc.WorldLore},
 * 2026-11-05, alongside {@link OllamaClient} (same move, same reasoning: the LLM isn't a TTS-only
 * concern, so neither is the shared context fed into it). Both Settlemynts' NPC-birth generation and
 * Audyble's conversation replies read this same constant now, guaranteeing a shared baseline setting
 * with zero runtime cross-mod coordination needed. Flagged placeholder/early-draft content, same
 * footing as every other first-pass flavor text in this suite -- worth a real editorial pass later
 * rather than treated as final.
 */
public final class WorldLore {

    public static final String TEXT = """
            This is a world of small, hard-won settlements spread across wild, mostly-unmapped land --
            forests, rivers, mountains, and the occasional ruin of something older that nobody fully
            remembers. Most people live and die within a day's walk of where they were born. Travel
            between settlements is real but not casual: roads are a settlement's own investment, not a
            given, and a trip to the next town is worth talking about afterward.

            Settlements govern themselves -- a Town Planner or two, a handful of tradespeople, hired
            or volunteer guards. There is no overarching kingdom or central authority anyone defers
            to; a settlement's reputation is built locally, one dealing at a time. Money is Gold
            Nuggets, trade is common and expected, and most people are plainly practical about it --
            a fair price matters more than charity.

            Monsters are a real, ordinary hazard, not a myth -- the usual kinds come out at night or
            in the wild places, and most settlements keep some kind of defense against them. Magic
            exists but is uncommon and mostly practical (a staff that mends wounds, a stone that
            remembers a place) rather than spectacular. Most people have never seen real magic
            up close and are a little wary of those who use it.

            People here are generally plainspoken, a little guarded with strangers until they've
            earned trust, and quick to help a neighbor in a real emergency. Humor tends dry rather
            than broad. Nobody here has ever heard of "the internet," modern technology, or any
            real-world country -- keep references grounded in this world only.

            Most people go by a single given name only -- a surname is uncommon, and usually marks
            someone from a noble line, a prominent trading family, or an appointed official; an
            ordinary farmer, guard, laborer, or tradesperson almost never has one. Given names
            themselves are plain but genuinely varied, drawn from many different real-world-sounding
            roots and styles rather than one narrow mold -- nobody expects every stranger they meet
            to sound alike.
            """;

    private WorldLore() {
    }
}
