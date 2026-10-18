# Nuclyus

Foundation mod for a modular Minecraft project (Minecraft Java Edition). Hard-imported
(`type="required"`) by every other mod in the suite -- a shared base for cross-mod classes/
contracts, starting with the cross-mod menu-contribution system.

## Context directory -- read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/nuclyus-context), not
part of this one -- it's listed in `.gitignore` here and must never be committed to this repo. It's
cloned as a subdirectory at `context/` for local convenience. If this directory is missing (e.g. a
fresh clone of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/nuclyus-context.git context
```

- `context/design-document.md` -- the authoritative design spec. Start here for anything about
  intended shape or scope.
- `context/decisions.md` -- dated log of decisions made during implementation that extend or
  override the design document, with rationale.
- `context/classes/` -- one short markdown file per implemented class/system: public surface, key
  state, collaborators. Read the relevant file here before opening the actual source, and before
  editing a class update its file to match.

**Keep this system current as you work** -- same discipline as every other mod in this suite:
update design-document.md/decisions.md when a decision is made, update classes/ when a class's
public surface changes, commit+push context/ separately from this repo's own commits.

## Status

Scaffolded 2026-10-17 (see [context/decisions.md](context/decisions.md)):
- Loader: **NeoForge**, Minecraft **26.1.2**, Java **25** -- same toolchain as every sibling mod
- Group ID: `com.github.cerealklla.nuclyus` / Mod ID: `nuclyus`
- **No sibling-mod dependencies of its own** -- every other mod in the suite depends on *this* one
  (`type="required"`), not the other way around.

**Menu-contribution system built the same day** (see [context/decisions.md](context/decisions.md),
[context/classes/MenuSystem.md](context/classes/MenuSystem.md)) -- `menu.client.NuclyusMenu` (the
render/input contract a tab implements), `MenuContributor` (what a mod implements to contribute
tabs -- "GetAllMenus()"), `NuclyusMenuRegistry` (open registration, no pairwise dependency needed
between contributing mods), `NuclyusMenuScreen` (the host screen, tab bar + delegated content
area), and the "K" keybind (`NuclyusModClient`, moved here from Lyfe's former K=Skills/M=Map).
`./gradlew build` green standalone; whole-suite (12 mods) `sync-mods.sh` rebuild green;
`runServer` boot-smoke-tested clean from Cartographyr. **Not yet live-tested** -- no real
contributor has registered a tab yet (Lyfe/Myssions/Coryspondance's own follow-up work).

**All 8 tabs landed the same day** (see [context/decisions.md](context/decisions.md)) -- Lyfe
(Character Info, Map, Skills, Hunter's Journal, Crafting Guide, Rune Codex), Myssions (Quests),
Coryspondance (Mail); see each mod's own CLAUDE.md for their side. New `menu.client.WidgetHost`
interface (lets a tab add/remove real widgets on whatever host it's attached to, since
`Screen#addRenderableWidget`/`#removeWidget` are `protected`) and a real fix to
`NuclyusMenuScreen#init()` (it was rebuilding the whole tab list -- discarding every tab's live
state -- every time `setScreen` targeted the same host instance again, not just on a genuinely new
open; this happens whenever Quest Log's Abandon-confirm/Report-Bug dialog closes). `./gradlew build`
green, whole-suite (12 mods) rebuild green, `runServer` boot-smoke-tested clean. **Not yet
live-tested in a real client.**

**Three shared utilities extracted from real sibling-mod duplicates, 2026-10-17** (see
[context/decisions.md](context/decisions.md)) -- the first real use of Nuclyus as a foundation
beyond just the menu system, per the user's own framing: "a mod which is meant to be hard-imported
by all the others which contains base classes." All three are pure behavior-preserving extractions
(no call-site-visible change):
1. **`client.TextWrap#wrap(Font, String, int): List<String>`** -- the real duplicate turned out to
   be only 4 call sites, not the larger list first guessed (Lyfe's `CraftingStructureScreen`/
   `CookingStructureScreen` and Myssions'/Lyfe's Hunter's Journal/Crafting Guide tabs never actually
   wrapped text at all) -- Lyfe's `SkillsTab`/`RuneCodexTab`, Myssions' `QuestLogTab`, Coryspondance's
   `MailboxTab`.
2. **`server.PersistedServerStore<K, V>`** -- collapsed Lyfe's `craft.ServerRecipeStore`/
   `cook.ServerFoodRecipeStore`/`socket.ServerRunewordStore` into thin static facades over one
   generic instance each (constructed with a file name, entry `Codec`, and key-extractor function);
   each subclass now only supplies its own `generate()`. `ServerRunewordStore` additionally uses the
   new `onLoaded()` hook to keep recomputing `RuneWorth` on every load/reroll.
3. **`request.ClientRequestSlot<T>`/`request.ClientRequestQueue<T>`** -- the poll-once-handoff
   pattern (a volatile pending value/queue + request/takePending, written from a common payload
   handler and read only from a client-only tick listener) repointed into Lyfe
   (`knowledge.ClientWritingRequest`, part of `recallcinite.client.ClientRecallciniteState`),
   Settlemynts (`founding.ClientFoundingRequests`, `plotsign.ClientPlotSignRequests`,
   `guardhouse.ClientGuardhouseRequests`), Blueprynts (`construction.ClientConstructionRequests`,
   its preview-image queue using `ClientRequestQueue`), and Kyt (`ClientKytRequests`) -- two more
   real call sites (Blueprynts, Kyt) than the three originally flagged. Both classes are themselves
   free of any client-only import, same as every class they're used from.

`./gradlew build`/`test` green in Nuclyus and all six consuming repos (Lyfe, Myssions,
Coryspondance, Settlemynts, Blueprynts, Kyt), whole-suite (12 mods) `sync-mods.sh` rebuild green,
`runServer` boot-smoke-tested clean from Cartographyr (all 12 mods logged common setup, `Done
(0.919s)`, no exceptions). **Not separately live-tested** -- these are refactors with no intended
behavior change, verified by the boot test plus each repo's own green `test` task, not a new
client-visible feature needing its own playtest.

Deliberately not pursuing anything more speculative yet (e.g. a generic registry base class for the
open-contribution-registry shape `MenuContributor`/Cartographyr's `Layer`/Settlemynts'
`ZoneTypeRegistry` all share) -- extracting from a second real duplicate is reasonable, extracting
ahead of one isn't.

**Real live-test bug fixed, 2026-10-17** -- a player screenshot at a wider GUI Scale showed tab
labels overlapping/truncated ("Hunter's JourCrafting Guide"); `NuclyusMenuScreen#tabButtonRect`
used to split the tab bar into equal-width slices regardless of each title's actual text width, so
a longer label overran its fixed slice once the font rendered larger relative to the window. Fixed
with a new `tabRects()` sizing each tab to its own text width (plus padding), the last tab still
absorbing any remaining slack so the bar still spans edge to edge. `./gradlew build` green,
whole-suite (12 mods) rebuild + `runServer` boot-smoke-test clean. **Not yet re-confirmed live** --
this fix was built directly from a reported screenshot, not yet re-tested in game.

**`PersistedServerStore` now self-heals a file that predates a later-added entry, 2026-10-17** (see
[context/decisions.md](context/decisions.md)) -- real report (Lyfe): researching Bow/Crossbow said
"nothing to learn" because the server's persisted recipe file predated those two being added to
`generate()`, and `loadOrGenerate` only ever regenerated on a genuinely *absent* file. New
`mergeMissing` adds any key `generate()` produces that the loaded map doesn't have, leaving every
existing key untouched, and saves back only if anything changed -- fixes all three current
subclasses, and any future one, for free. `./gradlew build` green here and in Lyfe. **Not yet
live-tested** -- only takes effect on a real server restart.

**Player-centric Pinging infrastructure, 2026-10-18** (see
[context/decisions.md](context/decisions.md), [context/classes/Ping.md](context/classes/Ping.md))
-- new `ping` package, built from a design discussion aimed at reducing server load from the
suite's many independent per-mod timer-based scans, shifting toward event-driven dispatch where it
makes sense. `ping.Pingable` is a plain interface any `BlockEntity`/`Entity` in any mod implements
directly, with no registration anywhere in Nuclyus -- `ping.client.ClientPingScanner` finds nearby
implementors via a plain `instanceof` check against structures the client already tracks for
rendering (loaded chunks' own block-entity maps, the level's own loaded entities), and reports only
a small candidate list to the server, which re-validates real distance before dispatching
(`PingDispatcher`) -- the actual point is moving the recurring scan cost onto each player's own
client, not eliminating it ("90% of the goal is moving the cost off the server," not performance in
the abstract). Cooldown/throttling is deliberately NOT part of the `Pingable` contract itself
(`PingCooldown` is an optional convenience helper, not required) -- confirmed explicitly: different
implementations legitimately want different cooldown shapes, so Nuclyus shouldn't force one.
Confirmed out of scope for this: every existing settlement-simulation ticker in Settlemynts
(`NpcAutoFundingTicker` etc.) needs to keep running with no player nearby at all, which this
mechanism can't trigger by design. `./gradlew build` green, whole-suite (12 mods) rebuild +
`runServer` boot-smoke-test clean. **Pure infrastructure, no real consumer exists anywhere in the
suite yet, and the new client-side scanning code hasn't been exercised by a real `runClient`
session** -- ready whenever a future feature wants it.
