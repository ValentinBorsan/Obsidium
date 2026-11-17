# Obsidium: Mythic Rails Addon (`v0.5.0`)

**Mythic Rails** adds a **self-propelled modular rail network** built specifically to integrate with Obsidium's items, fluids, and RF energy.

---

## 1. Hermes Modular Cart
A single 58-slot modular rail vehicle (`54` Cargo slots + `Motor`, `Payload Module`, `Fuel`, and `Aux` slots).

### Drive Motors (Slot: Motor)
- **Coal Drive Motor** — Burns furnace fuels in the `Fuel` slot (`0.28` speed).
- **Electric RF Drive Motor** — Consumes internal RF (`0.34` speed; charges from `Aux` Redstone/Coal or Energy Loading Stations).

### Payload Modules (Slot: Module)
- **Cargo Crate Module** — Dedicated 54-slot freight transport.
- **Liquid Barrel Module** — `32,000 mB` internal tank (Water/Lava; supports buckets in `Aux` slot).
- **Energy Cell Module** — Expands cart RF capacity to `2,000,000 RF`.
- **Geothermal Arborist Module** — Harvests nearby trees along the track and replants saplings.
- **Volcanic Smelter Module** — Automatically smelts ores/items inside cargo while powered.
- **Obsidian Grinder Module** — Crushes ores, cobblestone, gravel, bones, and blaze rods inside cargo.

---

## 2. Stations (Place Directly Below a Straight Rail)
All stations face the **departure direction** and support redstone hold + GUI control.

- **Hermes Loading / Unloading Station (Items)**
  - Auto-imports from or auto-exports to adjacent chests, barrels, and machines on **all 5 non-top sides** (`North`, `South`, `East`, `West`, `Down`).
  - Strictly accesses only the `54` cargo slots (upgrade/module slots are protected).
- **Hermes Fluid Loading / Unloading Station**
  - `32,000 mB` buffer tank; connects to **Naiad Fluid Pipes** and buckets in the Aux slot.
- **Hermes Energy Loading / Unloading Station**
  - `2,000,000 RF` buffer; connects to **Astrape Energy Wires**, **Volcanite Energy Cells**, and charges/discharges `EnergyStorage.ITEM` items in the Aux slot.

---

## 3. Rails
- **Obsidian Rail** — Blast-resistant (`1200.0` explosion resistance) and faster than vanilla rails (`0.52` max speed).
- **Ember Powered Rail** — Reinforced powered booster/brake rail.
- **Helios Volcanite Hyper-Rail** — High-speed rail (`0.78` straight max speed, auto-capped to `0.42` in curves to prevent derailing).
- **Oracle Scanner Rail** — Emits redstone signal + comparator analog output (`0–15`) proportional to cart fill (items, fluid, or RF).
- **Labyrinth Switch Rail** — Redstone-controlled junction rail (toggles straight/curved on redstone signal, or cycle manually with right-click).

---

## 4. Signaling, Buffer Stops & Remote Control
- **Atlas Buffer Stop** — End-of-line stop that halts approaching carts cleanly; when powered by redstone or remote command, reverses and launches the cart back along the track.
- **Apollo Signal Semaphore** — Scans up to `16` blocks ahead (or obeys redstone/remote signal). Holds carts on **RED** without turning off their motor, and automatically lets them resume on **GREEN**.
- **Iris Signal Relay** — Compact transceiver placed next to a Semaphore, Buffer Stop, or Switch Rail.
- **Olympus Dispatch Console** — Central GUI console controlling up to `8` linked **Iris Signal Relays** individually or via Master Controls (`ALL GO`, `ALL STOP`, `LAUNCH`, `SWITCH`).
- **Caduceus Signal Linker** — Right-click the **Olympus Dispatch Console** to bind, then right-click **Iris Signal Relays** to link them.
