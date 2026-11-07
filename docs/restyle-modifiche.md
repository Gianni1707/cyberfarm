# CyberFarm, restyle: cosa è stato modificato

Elenco di tutto ciò che è cambiato finora. I motivi delle scelte sono in
[restyle-scelte-di-design.md](restyle-scelte-di-design.md).

Stato al 7 novembre 2026, branch `machine-models-prototype`: 17 commit locali da `ce455a6` in poi, non pushati.

## Riepilogo per macchina

| Macchina | Cosa è adesso | Parte animata | Stato |
|---|---|---|---|
| Combustion Generator | Modello 3D | Modello | Commit |
| Bio-Digester | Multiblocco: tamburo alto 2 e tank | Apertura, pala dietro l'oblò, gas nel vetro | Commit |
| Drone Dock | Modello 3D | Luci sul pad, archi di ricarica | Commit |
| Swarm Dock | Modello proprio, con 4 segni di atterraggio | Come il Dock | Commit |
| Drone Assembler | Base, spina dorsale, testina a sbalzo | Ologramma del drone | Commit |
| Fabricator | Multiblocco lungo 2, con magazzino | Apertura, scheda dal magazzino al vassoio, rulli | Commit |
| Acid Synthesizer | Multiblocco 3x1x2, torre tra due serbatoi alti | Apertura, collare e bolle, fluidi a livello reale | Commit |
| Water Pump | Cavallo di pompaggio | Manovella, bilanciere, asta | Commit |
| Electrolyzer | Vasca con punzone | Punzone, 4 colpi per lingotto | Commit |
| Pyrolysis Oven | Multiblocco 2x1, una sola macchina | Apertura, navetta che tira la fibra | Commit |
| Coolant Mixer | Multiblocco 2x1 con due serbatoi | Apertura, carrello | Commit |
| Gas Turbine | Multiblocco 1x3 | Apertura, rotore | Commit |
| Waste Reprocessor | Carosello sotto un simbolo radioattivo | Disco a 4 tasche | Commit |
| Crystallizer | Piedistallo; camera nella palette | Cristallo e campo viola | Commit |
| Supply Terminal | Bacheca su un palo | Schede con i contratti veri | Commit |
| Steam Engine | Multiblocco 1x5 con 3 cilindri | Apertura "a nastro", pistoni, volano | Commit |
| Methane Extractor | Testa con anello; camera a gradoni | Anello spruzzatore | Commit |
| Mech Hangar | Stessa forma, solo palette | Come prima | Commit |
| Reattore | Torre a 8 lati, pelle a piastre | Come prima | Commit |
| Turbina | Scafo a 8 lati, pelle a piastre | Come prima | Commit |
| Liquid Tank, Gas Tank | Modulari, con vetro unico | Livello del fluido | Commit |

## Modifiche trasversali

**Palette**
- Palette "pale enamel" su modelli, drone, icona e tutte le texture di `texgen` (`e30aa1e`).
- In Java: le luci nel mondo prendono l'alone chiaro, i colori con un significato (stato, GUI, HUD, batteria) lo smeraldo.
- Reattore, turbina e tank: corpo più chiaro, `#4A6B65`.

**Drone**
- Quadricottero agricolo più piccolo (`ce455a6`), con nuova icona; moduli centrati sotto lo scafo (`39cc4f7`).
- Decolla e atterra rivolto verso il fronte del dock.

**Sistema dei modelli**
- `tools/machinemodels.py`: genera modelli a box, atlante, smussi e gradienti.
- Prese dei tubi visibili dove si collega qualcosa, tramite `MachineBlock.hasPipePorts()`.
- Correzione dello sfarfallio tra facce coincidenti (`8555828`).
- Le macchine non fanno più scintille generiche.

**Piccoli multiblocchi** (`e36ec96`, `b8ddb06`)
- Nuove classi `SmallMultiblockBlock`, `SmallMultiblockBlockEntity`, `HiddenFrameBlock`, `MachinePartBlock`.
- Si richiudono rompendo un frame. I frame nascosti tornano normali, così la macchina si può sempre riassemblare.

**Droni proiettori** (`b8ddb06`, `ebc427b`, `b3b00c5`)
- Nuovo `FrameDroneRenderer`: mini droni per i piccoli multiblocchi, al posto delle scintille.
- Droni più grandi per reattore, turbina, Crystallizer, camera del metano e Mech Hangar.
- Reattore e turbina: si parte dalla taglia con più blocchi piazzati e ogni clic passa alla successiva.

**Renderer nuovi**
`ModelParts`, `ShellTiles`, `FrameDroneRenderer`, `FabricatorRenderer`, `WaterPumpRenderer`, `ElectrolyzerRenderer`,
`PyrolysisOvenRenderer`, `CoolantMixerRenderer`, `GasTurbineRenderer`, `WasteReprocessorRenderer`, `SupplyTerminalRenderer`.

**Suoni**
- Nuovi suoni per Bio-Digester, Water Pump, Gas Turbine e Resonance Core, tutti da audio vanilla.

## Modifiche che cambiano il gioco

Non solo estetica, da tenere presenti per i mondi esistenti:

- **Water Pump:** ora vuole una fonte d'acqua proprio sotto. Le pompe con l'acqua solo ai lati si fermano.
- **Gas Turbine:** non lavora finché non è assemblata, quindi quelle già piazzate aspettano i due Cyber Frame.
- **Coolant Mixer:** non lavora finché non è assemblato col suo frame.
- **Acid Synthesizer:** non lavora finché non ha i suoi 5 Cyber Frame. I tubi si collegano solo davanti e dietro, quindi quelli di lato e sopra sono da spostare.
- **Steam Engine:** i blocchi separati di cilindro e dinamo non esistono più. Gas ed energia passano dalla caldaia, con 4 Cyber Frame.
- **Crystallizer:** a camera formata prende i tubi al centro dei bordi del pavimento e al centro del tetto, senza Crystallizer Port.
- **Bio-Digester, Fabricator, Pyrolysis Oven:** non lavorano senza frame: 2 per il digester, 1 per gli altri due. Il digester perde il tubo in cima e la presa dal lato del tank, il Fabricator la presa dietro, il forno quella a sinistra.
- **Tank:** ora si fondono e rispondono ai click in modo diverso (vedi sotto).

## Commit, dal più vecchio

| Commit | Contenuto |
|---|---|
| `ce455a6` | Prototipi 3D di generatore, digester e dock; nuovo drone |
| `39cc4f7` | Icona del drone, modello dello Swarm Dock |
| `e30aa1e` | Palette nuova; Drone Assembler 3D |
| `85a2c95` | Fabricator 3D |
| `8555828` | Correzione dello sfarfallio e di tre difetti nei modelli |
| `783ecc6` | Acid Synthesizer 3D |
| `ff2fb22` | Water Pump 3D |
| `68bbb65` | Electrolyzer 3D |
| `311d95e` | Pyrolysis Oven 3D |
| `e36ec96` | Coolant Mixer 2x1, primo piccolo multiblocco |
| `e2f4704` | Gas Turbine 1x3 |
| `366faa3` | Waste Reprocessor 3D |
| `9a86b1c` | Crystallizer e Supply Terminal 3D |
| `b8ddb06` | Steam Engine e Methane Extractor rifatti; mini droni |
| `ebc427b` | Droni grandi; Mech Hangar nella palette |
| `b3b00c5` | Reattore e turbina a 8 lati; i droni scorrono le taglie |
| Ultimo | Tank modulari; Acid Synthesizer, Bio-Digester, Fabricator e forno più grandi; striscia di luce; suoni; documenti |

## Ultimo commit: tank, Acid Synthesizer, Bio-Digester, Fabricator, Pyrolysis Oven

**Bio-Digester, Fabricator, Pyrolysis Oven**
- Passano a piccolo multiblocco con modelli `_lone`, `_parts` e parti mobili `BIO_*`, `FAB_*`, `OVEN_*`.
- `BioDigesterRenderer` nuovo sostituisce `MovingPartsRenderer`; `FabricatorRenderer` e `PyrolysisOvenRenderer` estesi.

**Acid Synthesizer**
- Modelli `acid_synthesizer_parts()` e `acid_synthesizer_lone()` in `tools/machinemodels.py`; parti mobili `ACID_*` in `MachinePartBlock`.
- `AcidSynthesizerBlock` ed entità passano a piccolo multiblocco; `AcidSynthesizerRenderer` riscritto.
- `SmallMultiblockBlock`: frame anche fuori dai bracci. `HiddenFrameBlock`: `TO_MACHINE` vale anche verso il basso.

### Tank

**Codice**
- `FluidTankBlock`: stato per ogni lato (faccia, presa, unito); click col secchio, messaggio di rifiuto, shift + destro.
- `FluidTankBlockEntity`: fusione in parallelepipedi fino a 16 per lato, fluido condiviso.
- `FluidTankRenderer`: vetro, fluido a volume, segno sul coperchio.
- `CyberFarm`, `ModBlocks`: capacità del fluido, evento del click, `noOcclusion`.

**Risorse**
- `tools/machinemodels.py`: pezzi di cornice, piastre, prese; blockstate multipart.
- `tools/assetgen.py`, `tools/texgen.py`: tolti i vecchi modelli e texture dei tank.
- Texture nuove: `tank_glass`, `tank_waves`, `tank_waves_x2`, `tank_cloud`, `tank_cloud_x2`.
- Tolte: `liquid_tank_side`, `gas_tank_side`, `tank_top`.
- Nuovi messaggi in `lang/en_us.json`; guida `fluid_tanks.md` riscritta.

**Limiti noti**
- Il segno sul coperchio sparisce con un tubo al centro, invece di spostarsi.
- Un tank a cavallo di due chunk può ignorare i blocchi non caricati finché non cambia o non si ricarica.
- Non provato in partita da Claude: i giudizi in gioco sono dell'utente.
- Il messaggio di rifiuto del secchio non è stato visto all'opera. Resta da chiarire la segnalazione sui fluidi nel tank sbagliato:
  il salvataggio delle 14:27 mostrava tutti i tank col fluido giusto, ma le prove successive non sono state controllate.

## Cosa manca

**Restyle**
- Muffler.
- Console del Mech Hangar (da decidere).
- Controllo della palette su Laser Turret, Resonance Core e Hologram Table.
- Scintille di default rimaste su Assembler e dock, da verificare.
- Mech Repair Bay.

**Da decidere o rimandati**
- Bilancio dello Steam Engine e Gas Turbine del villaggio.
- Piccoli multiblocchi nel proiettore.
- Rotazione e specchiatura delle prese.
- Linee dei generatori non più usate.

**Prima di pubblicare**
- Prova su server dedicato.
- Push e merge su `master`.
- Porting sulle altre versioni di Minecraft.
