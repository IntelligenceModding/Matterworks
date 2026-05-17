# Matterworks Pylon Transport Concept

## Goal

Build a transport system that is usable in a real modpack, not just in a controlled demo.

That means the pylon system must:

- move `energy`, `items`, and `fluids` reliably
- scale to large bases without turning into a routing mess
- interoperate cleanly with other mods through standard capabilities
- expose enough control to solve real automation problems
- stay readable in-world, even when the link itself is visually minimal

The right target is not "copy Ender IO" or "copy Mekanism". The right target is:

`Ender IO control + Mekanism clarity + Thermal/Pipez operational simplicity`

## Research Summary

### Ender IO

Useful ideas to adopt:

- conduit bundles: one physical path can carry multiple transport types
- per-connection insert/extract configuration
- channels to split one physical network into separate logical networks
- integer priorities on destinations
- round-robin on equal-priority destinations
- per-endpoint filters
- redstone-controlled extraction
- copy/paste and probe tooling for configuration and debugging
- facades/hidden transport for cleaner builds

Useful ideas to avoid copying blindly:

- too much per-connection micro-config becomes slow to build and hard to debug
- if every endpoint has a dense GUI from day one, your implementation cost explodes

### Mekanism

Useful ideas to adopt:

- very clear connection states: `normal`, `push`, `pull`, `none`
- separate transport families for different resource types
- color/channel-based separation for routing
- explicit overflow behavior through lower-priority paths
- network-level throughput and buffering concepts

Useful ideas to avoid copying blindly:

- nearest-valid-target behavior is too limited for a pylon network
- too much behavior hidden behind subtle side states is easy to misread in-world

### Thermal Dynamics

Useful ideas to adopt:

- route choice should be deliberate, not accidental
- equal destinations need deterministic distribution rules
- network should avoid ejecting or voiding in-flight contents if a target changes
- debugability matters: shortest path and overflow logic must be understandable

Useful ideas to avoid copying blindly:

- path weighting through special pipe variants is clever but too opaque for pylons

### Pipez

Useful ideas to adopt:

- simple mental model
- configuration that can be copied quickly
- performance as a first-class design constraint

Useful ideas to avoid copying blindly:

- basic usability should not be locked behind too many upgrades

## Core Design Decision

Each pylon is both:

- an endpoint for an attached machine/inventory/tank on its facing side
- a relay node for the rest of the network

There is no separate relay mode.

That matches the physical fantasy of the block and avoids the current problem where a line node becomes useless unless it is in the exact right mode.

## Network Model

The pylon system should use **one physical link graph** and **multiple logical transport layers** on top of it.

Physical graph:

- pylons are manually linked
- links have a max span
- links are persistent and invisible at rest
- links are shown with a faint particle cable

Logical layers:

- `energy`
- `items`
- `fluids`
- optional later: `chemicals`, `heat`, `redstone`, `data`

One linked pylon line should be able to carry multiple layers at the same time, similar to Ender IO conduit bundles. That is the only way this remains competitive in a modpack where build density matters.

## Required Pylon Data Model

The current single ID is not enough.

For a usable system, split it into these concepts:

### 1. Network Channel

Purpose:

- separates logical subnetworks on the same physical pylon graph

Rules:

- channel is per transport layer
- default is `1`
- pylons only connect traffic within the same layer and same channel

Example:

- `energy channel 1` runs machine power
- `items channel 2` runs ore output
- same pylons, same physical links, different logical traffic

This is the Ender IO style solution to "shared path, separate jobs".

### 2. Endpoint Priority

Purpose:

- decides which valid destinations win first

Rules:

- priority is per layer, per import endpoint
- higher number wins
- equal priority destinations share transport by the selected distribution mode

This is what your current ID is trying to do. It should stay, but as `priority`, not as the network identity itself.

### 3. Distribution Mode

Needed at import endpoints:

- `nearest`
- `round_robin`
- `even_split`
- `fill_one_then_next`

Minimum required for v1:

- `nearest`
- `even_split`

`even_split` is the one you already want.

### 4. Endpoint Direction Mode

Per layer, each pylon face needs:

- `disabled`
- `import`
- `export`
- `import_export`

Because the pylon itself is always relay-capable, this only controls interaction with the attached block, not whether the pylon participates in routing.

## Transport Semantics By Resource Type

### Energy

Energy is the easiest layer and should define the network kernel.

Required behavior:

- exporters simulate extract before sending
- importers simulate receive before claiming energy
- higher-priority imports receive first
- equal-priority imports split fairly
- transfer is tick-based and deterministic
- no bouncing between equal buffers
- optional local buffer in the pylon for smoothing

Do not route energy purely by nearest endpoint. That fails immediately in larger networks.

### Items

Items need more than FE.

Required behavior:

- real item packet or reservation model
- no item loss if destination changes mid-route
- no item dupes on chunk unload or path invalidation
- stack-aware transfer
- whitelist/blacklist filters
- tag and NBT matching later
- sided inventory respect
- extraction limits per tick

Modpack-usable item transport also needs:

- overflow handling
- "do not send back to source unless allowed"
- optional minimum stock / keep-in-inventory rules later

If you do items without reservation or backstuff buffering, it will break under automation stress.

### Fluids

Required behavior:

- fluid type filters
- exact-capability simulation before send
- no forced mixing unless explicitly allowed
- per-tank awareness where the target exposes it
- no fluid loss on re-route

Recommended modes:

- `single-fluid lane` on a channel
- optional `multi-fluid` later

For v1, keep fluids conservative:

- one fluid type per channel path at a time
- no automatic mixing logic

That is less flashy, but much safer.

## Interop Requirements

For the system to be usable in a modpack, it must integrate through capabilities first.

Required:

- `IEnergyStorage`
- `IItemHandler`
- `IFluidHandler`

Recommended later:

- Mekanism chemical capability when Mekanism is present

Do not hardcode support machine-by-machine. Capability-first is the only maintainable path.

## Routing Rules

Routing must be explicit and predictable.

For every export attempt:

1. Discover all reachable import endpoints on the same layer and channel
2. Filter to endpoints that can actually accept right now
3. Sort by:
   - highest priority
   - then lowest path cost
   - then stable tie-breaker such as block position
4. Apply the endpoint distribution mode
5. Reserve or transfer

Path cost should default to:

- number of links traversed

Later, you can add modifiers:

- upgraded pylons reduce path cost
- long-range pylons increase throughput but maybe add loss or cost

But that is phase-two work. Base system should stay simple.

## Throughput Model

You need a throughput model per layer, otherwise the network never feels physical.

Recommended base model:

- each pylon has a per-layer transfer budget per tick
- each link has a per-layer transfer budget per tick
- route capacity is limited by the weakest segment on that path

That gives you:

- meaningful upgrades
- visible bottlenecks
- a reason to build better pylons later

Suggested starting numbers:

- Energy: per-link FE/t budget
- Items: items or stacks per tick
- Fluids: mB/t

## Configuration UX

This is where transport mods usually win or lose.

### Required tools

1. **Linker Tool**
   - create and remove links
   - preview link validity and range
   - highlight whole connected graph

2. **Pylon Config GUI**
   - tabs or sections for `energy`, `items`, `fluids`
   - channel field
   - priority field
   - import/export/import_export mode
   - distribution mode
   - filter controls where relevant

3. **Config Copy Tool**
   - copy full pylon config
   - paste to another pylon
   - optionally paste only one layer

4. **Debug Overlay**
   - on/off command
   - show channel, priority, live transfer, and bottleneck state

Your current debug number overlay is the right direction. It should stay.

### Required GUI constraints

- do not hide network channel and endpoint priority under one field
- do not make the player configure every connection from every side
- one attached face per pylon is enough
- keep per-layer config on one screen

## Visual Rules

At rest:

- light gray particle cable

While active:

- energy: red pulse
- items: amber or white packet pulse
- fluids: color sampled from fluid if possible, otherwise blue

Debug overlay:

- source/export local amount: green `+value`
- import local amount: red `-value`
- relay-through amount: gray `value`

Also add:

- a subtle invalid-state effect if a pylon has no valid attached handler on its facing side
- a selected-network preview when using the linker/config tool

## Redstone Control

This is required for modpack usability.

Per layer, export behavior should support:

- `always active`
- `active with signal`
- `active without signal`
- `disabled`

That covers most automation control cases without implementing full redstone logic conduits.

## Filters

Minimum filter set:

### Items

- whitelist / blacklist
- exact item
- tag match
- NBT match later

### Fluids

- whitelist / blacklist
- exact fluid

### Energy

No content filter needed, but you may want:

- minimum retained energy on exporter
- stop exporting below X%

That last one is very useful for batteries and shared power networks.

## Buffers And Safety

This is non-optional.

The network must never:

- void items or fluids because a destination changed during transfer
- duplicate items or fluids on unload
- oscillate energy forever between equal buffers

Required safeguards:

- reservation before final commit for items/fluids
- export-side retry queue or small local pylon buffer
- graph invalidation on topology change
- safe pause when chunks unload

For chunk behavior:

- if the full route is not loaded, transfer does not start
- in-flight data stays in the source reservation/buffer until completion is possible

## Performance Requirements

This matters more than most feature lists.

Do not run a fresh full-graph BFS from every exporter every tick once the system grows.

Required performance architecture:

- maintain cached connected components per physical graph
- maintain cached endpoint lists per layer/channel
- mark graph dirty only on:
  - link change
  - pylon placement/removal
  - config change that affects routing
- distribute network processing over ticks if needed
- cap particle rendering separately from transfer logic

The Pipez performance angle is worth taking seriously here. Sparse pylon networks should perform better than dense cable spam, but only if the routing cache is built properly.

## Tiers And Upgrades

Do not make the base tier annoying.

Base pylon should already support:

- energy, items, fluids
- channel
- priority
- import/export/import_export
- equal split
- basic filters
- redstone export control

Upgrades should improve scale, not unlock basic competence.

Good upgrade axes:

- range
- throughput
- filter complexity
- local buffer size
- number of simultaneous active transfers

Bad upgrade axes:

- "can export at all"
- "can use filters at all"
- "can use redstone at all"

## Block Set

Recommended block set for a complete system:

### 1. Matter Pylon

General-purpose transport node.

Responsibilities:

- attached face IO
- relay
- GUI config
- channel/priority/filter state

### 2. Pylon Linker

Tool item.

Responsibilities:

- create/remove links
- preview routes
- copy/paste basic settings optionally

### 3. Pylon Controller (optional, later)

Only add this if the network becomes too hard to inspect.

Responsibilities:

- show network members
- throughput summary
- bottleneck summary
- rename channels

Do not build this first.

### 4. Advanced Pylon (later)

Optional higher-tier version:

- longer span
- higher throughput
- maybe chunk-stable relay behavior if that ever becomes a feature

## Recommended Build Order

### Phase 1: Fix The Model

- separate `channel` from `priority`
- keep every pylon relay-capable
- keep one attached face only
- formalize per-layer config data

### Phase 2: Energy Production-Ready

- cached graph routing
- full channel/priority/split logic
- redstone control
- stable debug overlay
- attached-block validity feedback

### Phase 3: Item Transport

- reservation model
- filters
- overflow behavior
- no-loss safety guarantees

### Phase 4: Fluid Transport

- exact simulation
- fluid filters
- conservative single-fluid-per-channel behavior first

### Phase 5: Tooling And Polish

- linker previews
- copy/paste config
- route debug
- better particles

### Phase 6: Cross-Mod Extensions

- Mekanism chemicals
- redstone/data layer
- advanced tiers

## Final Recommendation

If you want this to be usable in a real modpack, the pylon system needs these as **non-negotiable core features**:

- manual links
- one physical graph carrying multiple resource layers
- separate `channel` and `priority`
- per-layer import/export/import_export modes
- fair splitting for equal-priority targets
- filters for items and fluids
- redstone-controlled exporting
- safe item/fluid transfer with no loss or dupe
- cached routing for performance
- copy/paste and debug tooling

If any of those are missing, the system will be interesting, but it will not replace the established transport mods in actual pack play.

## Sources

- Ender IO modern docs: https://new.enderio.com/docs/technical/datapacks/custom_conduits/
- Ender IO legacy item conduit wiki: https://github.com/SleepyTrousers/EnderIO-1.5-1.12/wiki/Item-Conduit
- Ender IO legacy fluid conduit wiki: https://github.com/SleepyTrousers/EnderIO-1.5-1.12/wiki/Fluid-Conduits
- Ender IO legacy Yeta Wrench wiki: https://github.com/SleepyTrousers/EnderIO-1.5-1.12/wiki/Yeta-Wrench
- Ender IO legacy Conduit Probe wiki: https://github.com/SleepyTrousers/EnderIO-1.5-1.12/wiki/Conduit-Probe
- Mekanism Logistical Transporter wiki: https://wiki.aidancbrady.com/wiki/Logistical_Transporter
- Mekanism Mechanical Pipe wiki: https://wiki.aidancbrady.com/wiki/Mechanical_Pipe
- Mekanism Universal Cable wiki: https://wiki.aidancbrady.com/wiki/Universal_Cable
- Mekanism Throughput wiki: https://wiki.aidancbrady.com/wiki/Throughput
- Mekanism Configurator wiki: https://wiki.aidancbrady.com/wiki/Configurator
- Mekanism Diversion Transporter wiki: https://wiki.aidancbrady.com/wiki/Diversion_Transporter
- Mekanism Restrictive Transporter wiki: https://wiki.aidancbrady.com/wiki/Restrictive_Transporter
- Thermal Dynamics Itemducts docs: https://oldcofh.github.io/docs/thermal-dynamics/ducts/itemducts/
- Pipez page: https://www.curseforge.com/minecraft/mc-mods/pipez
