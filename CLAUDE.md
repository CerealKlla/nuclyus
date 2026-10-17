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

**Next, agreed 2026-10-17**: three real patterns duplicated across sibling mods, now candidates to
pull into Nuclyus as shared base classes/utilities (its stated purpose beyond just the menu system):
1. **Word-wrap text helper** -- `wrapText`/`wrapToWidth`, nearly line-for-line identical in
   `SkillsScreen`-successor code, Myssions' `QuestLogTab`, Coryspondance's `MailboxTab`, Lyfe's
   `CraftingStructureScreen`, and the new menu tabs this session added. Trivial, zero-risk --
   a static `Nuclyus.wrapText(Font, String, int): List<String>` utility, every call site repointed.
2. **`PersistedServerStore<T>`** -- Lyfe's `craft.ServerRecipeStore`/`cook.ServerFoodRecipeStore`/
   `socket.ServerRunewordStore` are three near-identical copies of "generate once on first boot,
   persist to a JSON file in the server's own working directory via Gson+Codec, reuse thereafter."
   A generic version would collapse all three to thin wrappers.
3. **The `Client*Request*` bridge pattern** -- the common-safe static holder + client-tick poller
   that lets a server-sent payload open a screen without a common-sourceset class ever referencing
   `Minecraft`/`Screen` directly (the load-bearing pattern behind the real Production crash
   documented in the root project's own CLAUDE.md). Lyfe (`knowledge.ClientWritingRequest`,
   `recallcinite.client.ClientRecallciniteState`, etc.) and Settlemynts
   (`founding.ClientFoundingRequests`, `plotsign.ClientPlotSignRequests`,
   `guardhouse.ClientGuardhouseRequests`) each reimplement this independently today.

Deliberately not pursuing anything more speculative yet (e.g. a generic registry base class for the
open-contribution-registry shape `MenuContributor`/Cartographyr's `Layer`/Settlemynts'
`ZoneTypeRegistry` all share) -- extracting from a second real duplicate is reasonable, extracting
ahead of one isn't.
