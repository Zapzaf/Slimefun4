# Fork Changelog

This file lists everything this fork changes compared to the upstream
[Slimefun4](https://github.com/Slimefun/Slimefun4) `experimental` branch (base commit `5374034c8`).
The upstream history is kept in [CHANGELOG.md](CHANGELOG.md), which this fork leaves untouched.

- [v4.9-fork.6 (17 Nov 2026)](#v49-fork6-17-nov-2026)
- [v4.9-fork.5 (16 Nov 2026)](#v49-fork5-16-nov-2026)
- [v4.9-fork.4 (14 Nov 2026)](#v49-fork4-14-nov-2026)
- [v4.9-fork.3 (14 Nov 2026)](#v49-fork3-14-nov-2026)
- [v4.9-fork.2 (14 Nov 2026)](#v49-fork2-14-nov-2026)
- [v4.9-fork.1 (12 Nov 2026)](#v49-fork1-12-nov-2026)
  - [Minecraft 26.3 support](#minecraft-263-support)
  - [Bug fixes from the upstream issue tracker](#bug-fixes-from-the-upstream-issue-tracker)
  - [Build and test changes](#build-and-test-changes)
  - [Known issues](#known-issues)
  - [Which jar to use](#which-jar-to-use)

## v4.9-fork.6 (17 Nov 2026)

#### Fixes
* Fixed Slimefun blocks turning into vanilla blocks after being broken and placed again in the same spot many times.
  The energy network ticks asynchronously and still wrote charge to a machine the player had just broken. When that
  write ran at the same moment the player placed a new block there, it could replace the new block's data with data
  that had no `id`, so the block acted like a vanilla block. `BlockStorage#addBlockInfo` now gets or creates the
  data in one atomic step, and `BlockInfoConfig` uses a `ConcurrentHashMap` because it is shared between threads
* Fixed a capacitor placed where one was just broken getting the old capacitor's charge. `EnergyNetComponent`
  (`setCharge`, `addCharge`, `removeCharge`) no longer writes charge to a spot that holds no Slimefun block
* Fixed "Attempted to refresh skin cache ... Do not cast to com.destroystokyo.paper.profile.PlayerProfile" on 26.3.
  Contributor skins are now read with the Bukkit `PlayerProfile` API. When Mojang rate-limits the lookup (HTTP 429),
  Slimefun now logs "likely rate-limited (HTTP 429)" and waits before trying again
* Removed a duplicate `dough-api` dependency from `pom.xml`

#### Tests
* `TestBlockStorageRace`: places a block and writes energy to the same spot at the same instant 20,000 times.
  Before the fix 32 blocks lost their id; now none do
* `TestVersionedPlayerHead`: skins of fetched profiles, and profiles without textures (rate-limited)

## v4.9-fork.5 (16 Nov 2026)

#### Changes
* Cargo output nodes can now insert fuel into Programmable Androids. Upstream Slimefun blocks all cargo
  access to androids. Cargo only fills the fuel slot, and only with fuel that android's tier can burn
  (solid fuel for tier 1, liquid for tier 2, nuclear for tier 3). Cargo still cannot take items out of an android

#### Fixes
* Fixed an Android duplication exploit. When an android moved, its old spot was air right away, but its data
  only moved at the end of the ticker cycle. A player who placed a block into that spot in between triggered the
  "ghost block" fix (#2636), which dropped a free android item and deleted the data. The android at its new spot
  then lost its script and fuel and stopped moving. `BlockListener` now ignores spots whose data is about to be
  moved, using the new `TickerTask#isMovedSoon(Location)`

#### Tests
* Added `TestAndroidRefuel`, which checks refuelling from an Android Interface (Fuel), refuelling through the
  "Pull fuel" script instruction, and cargo insertion into the fuel slot. All 6 tests pass on the 1.21 test server
* Added `TestAndroidMove`, which checks that a moved android keeps its data and fuel, and that placing a block
  into the spot it just left does not drop an android item. Both tests pass on the 1.21 test server, and the
  duplication test fails without the fix

## v4.9-fork.4 (14 Nov 2026)

#### Fixes
* Enhanced Furnaces (including the Carbonado Edged Furnace) were very slow on 26.3 and ran at about vanilla speed.
  Their speed is now applied through `FurnaceStartSmeltEvent`: each item's total cook time is divided by the
  furnace's speed, so a Carbonado Edged Furnace smelts an item in 10 ticks instead of 200.
  The old method, which pushed the furnace's progress forward every 10 ticks, is only used on 1.16

#### Additions
* Added `EnhancedFurnaceCookTimeListener`, `EnhancedFurnace#getCookTime(int)` and
  `EnhancedFurnace#isStartSmeltEventSupported()`

#### Known issues
* The new furnace speed has not been tested on a live 26.3 server yet
* Fortune on Enhanced Furnaces was also reported as not working on 26.3. It passes its tests on 1.21,
  and this release does not change it

## v4.9-fork.3 (14 Nov 2026)

#### Tests
* Added `TestEnhancedFurnaceListener`, which checks the speed, fuel efficiency and fortune of Enhanced Furnaces
  using the Carbonado Edged Furnace's values (speed 20, efficiency 10, fortune 3). All 9 tests pass on the
  1.21 test server

#### Known issues
* Carbonado Edged Furnace speed and fortune were reported as not working on a 26.3 server.
  This is not fixed yet: the furnace logic passes its tests on 1.21 and no cause has been found for 26.3

## v4.9-fork.2 (14 Nov 2026)

#### Fixes
* Fixed `[dough: items] Failed to detect items nbt methods ... UnknownServerVersionException` on 26.3.
  The bundled dough library could not parse version strings such as `26.3.build.2658-stable`, so vanilla
  item names showed as `unknown` (machine inventory titles, locked items in the guide, Ancient Pedestal labels)
* This fork now ships its own copy of dough's `MinecraftVersion`, which reads the leading
  `<major>.<minor>[.<patch>]` numbers. Dough's original class is excluded from the shaded jar in `pom.xml`

## v4.9-fork.1 (12 Nov 2026)

### Minecraft 26.3 support

Minecraft switched to year-based version numbers (`26.1`, `26.2`, `26.3`), and Paper 26.x needs Java 25.
Slimefun now runs on Paper 26.3. On 1.16–1.21 servers it behaves exactly as before: every 26.x change
only takes effect when the server runs 26.3 or newer.

#### Additions
* Added `MinecraftVersion.MINECRAFT_26_3`
* Slimefun now reads year-based versions such as `(MC: 26.3)` and `26.3.build.249-stable`
  (`MinecraftVersion#parseVersion` and `MinecraftVersion#of`)
* Added the 26.3 biome list (`dappled_forest`, `pale_garden`, `sulfur_caves` are new)
* Added `VersionedAttribute.MAX_HEALTH`, which works with both `MAX_HEALTH` (26.x) and `GENERIC_MAX_HEALTH` (1.21)
* Added `VersionedPlayerHead`, which creates custom heads through the Bukkit `PlayerProfile` API on 26.3+

#### Changes
* Slimefun no longer uses PaperLib to detect the Minecraft version. PaperLib reads 26.x as "version 0",
  which made Slimefun run in its oldest compatibility mode
* The "wrong version of Minecraft" warning now shows the server's real version string
* Bandages, Splints, Medical Supplies and the Vampire Blade use `VersionedAttribute.MAX_HEALTH`
* Armor stands (holograms, pedestals) now detect the version themselves instead of asking PaperLib
* All custom heads (items, androids, capacitors, the debug fish, contributor heads in the credits menu)
  now go through `VersionedPlayerHead`. Heads keep the same UUID and name as before, so existing heads
  still stack with new ones
* `HeadTexture#getAsSkin()` is now deprecated, because it still crashes on 26.x

#### Fixes
* Fixed Slimefun failing to enable on Paper 26.3 with
  `IncompatibleClassChangeError: CustomGameProfile cannot inherit from final class GameProfile`,
  which made every `/sf` command report "plugin is disabled"
* Fixed Slimefun running in its oldest compatibility mode on 26.x, because PaperLib could not read the version
* Fixed contributor skin lookups ignoring Mojang's rate limit (HTTP 429) on 26.3. Slimefun now waits
  5 minutes, as it does on 1.21, instead of continuing to send requests

### Bug fixes from the upstream issue tracker

* Fixed [#4326](https://github.com/Slimefun/Slimefun4/issues/4326): Piglin barter drops always favoured
  the same items. They are now shuffled on every barter
* Fixed [#4272](https://github.com/Slimefun/Slimefun4/issues/4272): `/sf give` created stacks of 99.
  Items are now given in normal stack sizes (64, 16 or 1)
* Fixed [#4333](https://github.com/Slimefun/Slimefun4/issues/4333): the Auto Drier (and every other
  machine using the default recipe lookup) still crafted items disabled in `Items.yml`. These recipes
  are now skipped and hidden from the guide
* Fixed [#4304](https://github.com/Slimefun/Slimefun4/issues/4304): the Pickaxe of Vein Mining applied
  the Fortune amount rolled for the mined block to every other block in the vein
  (for example, mining lapis next to diamond ore gave stacks of diamonds)
* Fixed [#4268](https://github.com/Slimefun/Slimefun4/issues/4268): breaking a machine while it was
  processing deleted the item inside. It now drops exactly what was taken from the input slots
* Fixed [#4260](https://github.com/Slimefun/Slimefun4/issues/4260): items placed into the GPS Control
  Panel or Geo-Scanner menus (for example with the number keys) were lost. These menus, and the
  Teleporter, Hologram Projector and Elevator editor menus, no longer accept items
* Fixed [#3931](https://github.com/Slimefun/Slimefun4/issues/3931): Rainbow Armor recolored normal or
  custom leather armor that was swapped in right after taking it off

### Build and test changes

* Added the Maven profile `minecraft-26`, which builds against Paper API `26.3.build.249-stable`:
  `mvn clean package -P minecraft-26`. It skips unit tests, since MockBukkit has no 3.x release for 26.3
* Added the property `paper.api.version`, so the build can be compiled against any Paper API version
* Updated Mockito from 5.15.2 to 5.24.0 and JaCoCo from 0.8.12 to 0.8.14, so the tests run on Java 25
* Added tests for:
  * year-based version parsing
  * `VersionedAttribute` and `VersionedPlayerHead`
  * the 26.3 biome map
  * `/sf give` stack splitting
  * the machine changes (consumed inputs, disabled outputs)
  * Rainbow Armor
* Result: 1843 tests, 0 failures, 7 skipped (the 7 skips were already in upstream)

### Known issues

* ~~On 26.3, startup logs `[dough: items] Failed to detect items nbt methods ... UnknownServerVersionException`~~
  (fixed in v4.9-fork.2)
* 26.1 and 26.2 are reported as unsupported, because only the 26.3 API was checked
* The unit tests still run against the 1.21 test server (MockBukkit). The 26.3 build is only checked
  by compiling it and by running it on a real server
* [#3010](https://github.com/Slimefun/Slimefun4/issues/3010) (a disabled Hologram Projector keeps its
  hologram) is not fixed yet
* None of the upstream bug fixes have been tested on a live server yet

### Which jar to use

| Server | Build command | Jar |
|---|---|---|
| Paper 26.3 | `mvn clean package -P minecraft-26` | `target/Slimefun v4.9-UNOFFICIAL.jar` |
| Paper / Spigot 1.16–1.21 | `mvn clean package` | `target/Slimefun v4.9-UNOFFICIAL.jar` |

Both builds write a jar with the same name. Copy it right after running the build for your server.
