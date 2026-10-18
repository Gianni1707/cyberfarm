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
Un drone = **telaio (base a tier)** + **moduli funzione** (decidono il lavoro che fa).

- **Drone Assembler**: slot Frame + 3 slot Modules (sbloccati in base al tier) + output. Costa 2.000 FE.
- **Drone Dock**: base del drone (ricarica 500 FE/t, 9 slot di raccolto, area di lavoro).
- **Survey Tool**: collega un dock e marca due angoli dell'area (max 33x33, entro 48 blocchi).
- **Telai**: Basic (T1: 20k FE, 80%, 1 slot) → Advanced (T2: 60k FE, 130%, 2 slot) → Elite (T3: 150k FE, 160%, 3 slot).
- **Componenti di crafting**: Drone Core, Rotor, Battery Cell.
- **Moduli funzione**: Harvester ✅ · Quarry (prossimo) · Transport (prossimo) · poi Planter, Logger, Fluid Siphon, Shearer/Milker.
- **Upgrade** (idea per il futuro): Cargo Pod, Range Booster, Silk Touch/Fortune Chip, Solar Skin.

### Energia
- **Tier 1**: Combustion Generator, Bio-Digester (scarti organici → Biogas).
- **Tier 2**: Methane Extractor (sotto un mob contenuto in una Containment Cage), Gas Turbine, Methane Canister.
- **Tier 3**: Fission Reactor (multiblocco), Cooling Tower (multiblocco a cono), Steam Turbine. Risorse: Uranium Ore → Yellowcake → Fuel Rod → Depleted Rod.

### Libro guida
**Drone Engineer's Handbook**. Capitoli: Getting Started · Drones · Modules · Energy · Nuclear.
Libreria: **GuideME** 26.1.15-beta, dipendenza **opzionale** (la mod funziona anche senza). Guida data-driven:
`assets/cyberfarm/guideme_guides/handbook.json` + pagine in `assets/cyberfarm/guides/cyberfarm/handbook/*.md`.
Il libro viene regalato al primo accesso ed è craftabile (libro + lingotto di rame).

## Roadmap

1. **MVP**: Drone Assembler, telai T1–T3, Harvester Module, Drone Dock, Survey Tool, Combustion Generator, libro guida. ← *implementato, da testare in gioco*
2. Moduli Quarry e Transport, poi Planter/Logger, upgrade, cavi energetici.
3. Fluidi: Bio-Digester, Methane Extractor, Gas Turbine, droni a metano.
4. Endgame: Fission Reactor, Cooling Tower, meltdown.
5. Rifinitura: bilanciamento, config, plugin JEI completo, texture e guida.

## Asset

- Texture: `python tools/texgen.py` (disegnate in codice con palette condivisa; `--preview DIR` crea un foglio di anteprima).
- JSON (blockstate, modelli, item, loot, tag, ricette): `python tools/assetgen.py`. Le traduzioni (`lang/en_us.json`) si scrivono a mano.
- Modello 3D del drone in Java (`DroneModel`), GUI disegnate in codice (`MachineScreen`).
