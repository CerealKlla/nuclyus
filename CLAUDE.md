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
