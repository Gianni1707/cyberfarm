# CyberFarm — Design Document

> Mod tech per **Minecraft 26.1.2 / NeoForge 26.1.2.122** (Java 25).
> Mod ID: `cyberfarm` · Package: `com.cyberfarm` · Lingua in-game: **inglese**.

## Pilastri

1. **Droni modulari** che lavorano le farm al posto del giocatore, in stile *Steve's Carts*.
2. **Energia a tier** che sale di complessità: combustione → biogas/metano → fissione nucleare.
3. **Massima compatibilità**: energia **FE** (capability standard NeoForge), fluidi e item tramite capability NeoForge, ricette visibili in **JEI** e **REI**, tag comuni (`c:`) per i materiali.
4. **Stile visivo sci-fi pulito**: pannelli grigio antracite, bordi chiari, accenti ciano luminosi.
5. **Tutto documentato** nel libro guida *Drone Engineer's Handbook*.

## Decisioni prese

| Tema | Decisione |
|---|---|
| Logica droni | **Area + Dock**: l'area si marca con un tool e il drone parte dal dock, lavora e torna a scaricare e ricaricarsi. Niente programmazione vera (forse in futuro). |
| Nucleare | **Pericoloso ma non troppo**: meltdown con esplosione moderata (potenza 6.0 di default), configurabile, con opzione per non distruggere blocchi. |
| Energia | **FE**, interoperabile con le altre tech mod. |
| Ricette | Plugin opzionali **JEI** e **REI** (EMI legge quello JEI): categoria Drone Assembly, generatore come stazione dei combustibili, pagine info. In dev si sceglie il viewer con `recipe_viewer` in gradle.properties. |
| Stile | Sci-fi pulito. |

## Contenuti

### Droni
Un drone = **telaio (base a tier)** + **moduli funzione** (decidono il lavoro che fa).

- **Drone Assembler**: slot Frame + 3 slot Modules (sbloccati in base al tier) + output. Costa 2.000 FE.
- **Drone Dock**: base del drone (ricarica 500 FE/t, 9 slot di raccolto, area di lavoro).
- **Survey Tool**: collega un dock, marca due angoli dell'area (max 33x33, dislivello max 8, entro 48 blocchi) o due contenitori (percorso Transport). Shift+clic sulla dock la azzera.
- **Telai**: Basic (T1: 20k FE, 80% = 2,4 blocchi/s, 1 modulo, 4 addon) → Advanced (T2: 60k FE, 130%, 2 moduli, 8 addon) → Elite (T3: 150k FE, 160%, 3 moduli, 16 addon).
- **Componenti di crafting**: Drone Core, Rotor, Battery Cell.
- **Moduli funzione**: Harvester ✅ · Quarry ✅ (strati fino al bedrock, solo con area marcata) · Transport ✅ (cassa → cassa, percorso col Survey Tool) · poi Planter, Logger, Fluid Siphon, Shearer/Milker.
- **Logica**: ogni modulo è un `DroneTask` che trova il prossimo `DroneJob`; il drone alterna i moduli a ogni giro e concatena i lavori dello stesso tipo.
- **Addon** (4 slot nell'Assembler, ognuno una pila dello stesso addon; totale limitato dal telaio):
  Speed (+10% velocità ciascuno) · Stack (+1 stack per viaggio del Transport; base 16 oggetti, max 9 stack) · Battery (+25% batteria del telaio) ·
  Fortune (max 3) · Silk Touch (max 1, incompatibile con Fortune). Fortune/Silk valgono per Quarry e Harvester.
- **Consumo in volo** = (telaio 2/3/4 FE/t + moduli: Harvester +1, Quarry +2, Transport +1) × (1 + % addon: Speed 10%, Stack 5%, Battery 5%), arrotondato per eccesso.
- **Consumo per lavoro**: raccolto 40 FE, blocco scavato 20 FE + 10 × durezza; Fortune +25% per livello, Silk Touch +50%. Tutti i costi sono nei tooltip.
- **Smontaggio**: drone nello slot di uscita dell'Assembler (altri slot vuoti) → pulsante Disassemble; i pezzi tornano negli slot, la carica va nel buffer dell'Assembler.
- **Volo**: rettilineo con accelerazione e frenata, pathfinding solo come riserva quando il drone è bloccato.
- **Quarry**: scava a serpentina riga per riga, invertendo il verso a ogni strato; torna a scaricare a 192 oggetti.
- **Idee future**: Range Booster, Solar Skin, filtri per Quarry/Transport.

### Energia
- **Energy Conduit**: si collega da solo a cavi e blocchi FE di qualsiasi mod; non accumula, distribuisce subito
  a turno tra le macchine della rete (1.000 FE/t per sorgente, reti fino a 1.024 cavi).
- **Tier 1**: Combustion Generator, Bio-Digester (scarti organici → Biogas).
- **Tier 2**: Methane Extractor (sotto un mob contenuto in una Containment Cage), Gas Turbine, Methane Canister.
- **Tier 3**: Fission Reactor (multiblocco), Cooling Tower (multiblocco a cono), Steam Turbine. Risorse: Uranium Ore → Yellowcake → Fuel Rod → Depleted Rod.

### Libro guida
**Drone Engineer's Handbook**. Capitoli: Getting Started · Drones · Modules · Energy · Nuclear.
Libreria: **GuideME** 26.1.15-beta, dipendenza **opzionale** (la mod funziona anche senza). Guida data-driven:
`assets/cyberfarm/guideme_guides/handbook.json` + pagine in `assets/cyberfarm/guides/cyberfarm/handbook/*.md`.
Il libro viene regalato al primo accesso ed è craftabile (libro + lingotto di rame).

## Roadmap

1. **MVP**: Drone Assembler, telai T1–T3, Harvester Module, Drone Dock, Survey Tool, Combustion Generator, libro guida. ✅ testato in gioco
2. ✅ Moduli Quarry e Transport, ✅ addon, ✅ Energy Conduit, ✅ JEI + REI. Prossimi: Planter/Logger.
3. Fluidi: Bio-Digester, Methane Extractor, Gas Turbine, droni a metano.
4. Endgame: Fission Reactor, Cooling Tower, meltdown.
5. Rifinitura: bilanciamento, config, plugin JEI completo, texture e guida.

## Asset

- Texture: `python tools/texgen.py` (disegnate in codice con palette condivisa; `--preview DIR` crea un foglio di anteprima).
- JSON (blockstate, modelli, item, loot, tag, ricette): `python tools/assetgen.py`. Le traduzioni (`lang/en_us.json`) si scrivono a mano.
- Modello 3D del drone in Java (`DroneModel`), GUI disegnate in codice (`MachineScreen`).
