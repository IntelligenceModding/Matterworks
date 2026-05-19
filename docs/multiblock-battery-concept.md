# Multiblock Battery Concept

## Goal

Add a large-form energy storage multiblock that fits Matterworks instead of copying Mekanism directly.

The closest reference pattern is:

- Mekanism `Induction Matrix`: shell + ports + internal capacity blocks + internal transfer blocks.
- Draconic Evolution `Energy Core`: a clearly identifiable controller/core with optional structure-specific support parts.

## Recommended Matterworks Direction

Use a matrix-style battery built around a powered shell and crystal-backed internals.

Recommended name:

- `Matter Battery Matrix`

Recommended player fantasy:

- The outer shell contains unstable energy.
- Internal blocks define what the battery can do.
- `Verdant Power Crystals` bias the structure toward storage capacity.
- `Crimson Power Crystals` bias the structure toward transfer throughput.

## V1 Shape

The current multiblock system only supports fixed patterns, not variable-size prisms like Mekanism.

Because of that, the first implementation should be a fixed `5x5x5` structure:

- Outer dimensions: `5x5x5`
- Hollow shell: `3x3x3` internal volume
- One controller on a face center
- At least `1` storage cell inside
- At least `1` relay/provider inside
- Remaining internal spaces can be filled with more internals or left as air

This keeps the first battery compatible with the current `MultiblockPattern` API.

## Construction Rules

Suggested rules for the first pass:

1. Edges and corners must be `Matter Battery Frame`.
2. Face blocks can be any mix of:
   - `Matter Battery Casing`
   - `Matter Battery Port`
   - `Crystal Viewport`
   - `Matter Battery Core` at the controller position only
3. The internal volume can contain:
   - `Matter Capacitor Cell`
   - `Matter Induction Relay`
   - air
4. The structure only forms when at least one internal cell and one relay are present.
5. Ports are configured as input/output the same way other Matterworks machines expose sided IO.

## Blocks To Add

### Core multiblock blocks

1. `Matter Battery Core`
   - Controller block entity
   - Forms/disassembles the structure
   - Stores aggregated FE, capacity, and transfer stats
   - Owns the GUI

2. `Matter Battery Frame`
   - Required on edges and corners
   - Strong visual anchor for the structure

3. `Matter Battery Casing`
   - Standard face filler block
   - Used for most of the shell

4. `Matter Battery Port`
   - Energy IO block
   - Same block can be switched between input/output/both
   - Optional later extension: redstone control mode

5. `Crystal Viewport`
   - Glass-like face block
   - Optional shell block so players can see the internals

### Internal function blocks

6. `Matter Capacitor Cell`
   - Adds storage capacity
   - Main scaling block for the battery
   - Can later have tiers

7. `Matter Induction Relay`
   - Adds receive/extract throughput
   - Equivalent to Mekanism's provider role
   - Should stack additively with multiple relays

## Items / Components To Add

These do not all need gameplay logic immediately, but they give the battery its own crafting identity.

1. `Stabilized Matter Plate`
   - Base structural ingredient for frame/casing

2. `Conductive Matter Coil`
   - Used in ports and relays

3. `Verdant Crystal Lattice`
   - Capacity ingredient for capacitor cells

4. `Crimson Flux Coil`
   - Throughput ingredient for induction relays

5. `Crystal Viewport Pane`
   - Ingredient for the viewport block

6. `Battery Core Assembly`
   - Expensive controller ingredient
   - Good place to require existing `Matter Energy Cell` and `Matter Power Bank`

## Good Recipe Anchors From Existing Content

To keep this aligned with the current mod:

- Reuse `Matter Energy Cell` in the controller recipe.
- Reuse `Matter Power Bank` in relay or controller recipes.
- Reuse `Verdant Power Crystal` for storage-oriented internals.
- Reuse `Crimson Power Crystal` for transfer-oriented internals.
- Reuse copper, iron, gold, and redstone as the mundane base layer.

## Multiblock System Work Needed

Before this becomes a solid gameplay feature, the multiblock engine needs a few additions.

### Required for V1

1. Add the first real `MultiblockDefinition` for the battery.
2. Add block entities for the core and any part blocks that need formed-state behavior.
3. Make participating block entities implement `MultiblockPartEntity`.
4. Add assembly/disassembly triggers on block placement, removal, and neighbor changes.
5. Aggregate capacity and transfer from internal matched parts into the controller.

### Strongly recommended before shipping

1. Persist formed multiblocks across reloads with `SavedData` or reconstruct them from chunk-loaded part states.
   - The current registry is in-memory only.
2. Add helper predicates for tags or block sets so shell parts are easier to define.
3. Add validation helpers for "at least N matching internal blocks".

### Useful after V1

1. Support variable-size rectangular multiblocks instead of only fixed patterns.
2. Support multiple battery tiers by allowing larger shells or better internal parts.
3. Render internal energy fill or an active-core effect.

## Suggested Implementation Order

1. Add the battery shell and internal blocks.
2. Implement a fixed `5x5x5` battery definition.
3. Get assembly, disassembly, and FE IO working.
4. Add GUI and structure status text.
5. Add tiered capacitor cells and relays.
6. Extend the engine for variable-size prisms if we still want a Mekanism-scale endgame battery.

## Why This Shape Works

This design borrows the best parts of the reference mods without forcing Matterworks into a direct clone:

- From Mekanism: internal blocks determine storage and throughput.
- From Draconic Evolution: the structure has a memorable core and a stronger visual identity.
- From Matterworks itself: power crystals become part of the machine's purpose, not just generic recipe filler.
