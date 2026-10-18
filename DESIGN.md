# CyberFarm — Design Document

> Mod tech per **Minecraft 26.1.2 / NeoForge 26.1.2.122** (Java 25).
> Mod ID: `cyberfarm` · Package: `com.cyberfarm` · Lingua in-game: **inglese**.

## Pilastri

1. **Droni modulari** che lavorano le farm al posto del giocatore, in stile *Steve's Carts*.
2. **Energia a tier** che sale di complessità: combustione → biogas/metano → fissione nucleare.
3. **Massima compatibilità**: energia **FE** (capability standard NeoForge), fluidi e item tramite capability NeoForge, ricette visibili in **JEI**, tag comuni (`c:`) per i materiali.
4. **Stile visivo sci-fi pulito**: pannelli grigio antracite, bordi chiari, accenti ciano luminosi.
5. **Tutto documentato** nel libro guida *Drone Engineer's Handbook*.

## Decisioni prese

| Tema | Decisione |
|---|---|
| Logica droni | **Area + Dock**: l'area si marca con un tool e il drone parte dal dock, lavora e torna a scaricare e ricaricarsi. Niente programmazione vera (forse in futuro). |
| Nucleare | **Pericoloso ma non troppo**: meltdown con esplosione moderata (potenza 6.0 di default), configurabile, con opzione per non distruggere blocchi. |
| Energia | **FE**, interoperabile con le altre tech mod. |
| Ricette | Integrazione **JEI** (plugin opzionale, la mod funziona anche senza). |
| Stile | Sci-fi pulito. |

## Contenuti

### Droni
- **Drone Assembler**: l'officina dove si monta il drone. Ha slot tipizzati e un'anteprima 3D.
- **Drone Dock**: base del drone (ricarica, inventario, punto di partenza).
- **Survey Tool**: marca l'area di lavoro collegata a un dock.
- Slot dei moduli:
  - **Chassis** (tier e numero di slot): Light Frame → Reinforced Frame → Titanium Frame
  - **Propulsion**: Basic Rotors, Quad Rotors, Ion Thrusters
  - **Power Cell**: Battery (FE), Methane Tank, Micro-RTG (endgame)
  - **Tool**: Harvester, Planter, Logger, Miner, Sprinkler, Shearer/Milker, Fluid Siphon
  - **Upgrade**: Cargo Pod, Range Booster, Silk Touch Chip, Fortune Chip, Solar Skin
- **Peso**: ogni modulo pesa, e più peso significa consumo maggiore e velocità minore.

### Energia
- **Tier 1**: Combustion Generator, Bio-Digester (scarti organici → Biogas).
- **Tier 2**: Methane Extractor (sotto un mob contenuto in una Containment Cage), Gas Turbine, Methane Canister.
- **Tier 3**: Fission Reactor (multiblocco), Cooling Tower (multiblocco a cono), Steam Turbine. Risorse: Uranium Ore → Yellowcake → Fuel Rod → Depleted Rod.

### Libro guida
**Drone Engineer's Handbook**. Capitoli: Getting Started · Drones · Modules · Energy · Nuclear.
Candidati come libreria: **GuideME** (pagine in Markdown, anteprime 3D delle strutture, già usata da AE2) oppure **Patchouli** (JSON). Per 26.1.2 entrambe sono in beta. *Da decidere.*

## Roadmap

1. **MVP**: Drone Assembler, Light Frame, Harvester, Battery, Drone Dock, Survey Tool, Combustion Generator, libro con 3–4 pagine. ← *siamo qui (scheletro pronto)*
2. Altri moduli (Planter, Logger, Miner, upgrade) e sistema di peso.
3. Fluidi: Bio-Digester, Methane Extractor, Gas Turbine, droni a metano.
4. Endgame: Fission Reactor, Cooling Tower, meltdown.
5. Rifinitura: bilanciamento, config, plugin JEI completo, texture e guida.

## Asset

- Texture generate da `tools/texgen.py` (pixel art ASCII → PNG, palette condivisa). Comando: `python tools/texgen.py`.
- Modelli dei blocchi in JSON (formato Blockbench), modello del drone in Java.
