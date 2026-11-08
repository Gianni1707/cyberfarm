# CyberFarm, restyle: cosa è stato modificato

Elenco di tutto ciò che è cambiato finora. I motivi delle scelte sono in
[restyle-scelte-di-design.md](restyle-scelte-di-design.md).

Stato all'8 novembre 2026: il restyle è su `machine-models-prototype` (19 commit da `ce455a6`) e, per minerali, icone, attrezzi del drone, proiettore, villaggio, batteria e suoni, sul branch `restyle-icons-drones` che parte da lì. Tutto su GitHub, nulla ancora unito a `master`.

## Riepilogo per macchina

| Macchina | Cosa è adesso | Parte animata |
|---|---|---|
| Combustion Generator | Modello 3D con ventola sul camino | Ventola a 6 palette, fuoco acceso, fumo e crepitio |
| Bio-Digester | Multiblocco: tamburo alto 2 e tank | Apertura, pala dietro l'oblò, gas nel vetro |
| Drone Dock | Modello 3D | Luci sul pad, archi di ricarica |
| Swarm Dock | Modello proprio, con 4 segni di atterraggio | Come il Dock |
| Drone Assembler | Base, spina dorsale, testina a sbalzo | Ologramma del drone |
| Fabricator | Multiblocco lungo 2, con magazzino | Apertura, scheda dal magazzino al vassoio, rulli |
| Acid Synthesizer | Multiblocco 3x1x2, torre tra due serbatoi alti | Apertura, collare e bolle, fluidi a livello reale |
| Water Pump | Cavallo di pompaggio | Manovella, bilanciere, asta |
| Electrolyzer | Multiblocco 2x1, una sola macchina | Apertura, punzone, 4 colpi per lingotto |
| Pyrolysis Oven | Multiblocco 2x1, una sola macchina | Apertura, navetta che tira la fibra |
| Coolant Mixer | Multiblocco 2x1 con due serbatoi | Apertura, carrello |
| Gas Turbine | Multiblocco 1x3 | Apertura, rotore |
| Muffler | Base a gradini, peso su una bobina | Il peso rimbalza secondo i suoni silenziati |
| Waste Reprocessor | Multiblocco alto 2, carosello sotto il caricatore | Apertura, disco a 4 tasche |
| Crystallizer | Piedistallo; camera nella palette | Cristallo e campo viola |
| Supply Terminal | Bacheca su un palo | Schede con i contratti veri |
| Steam Engine | Multiblocco 1x5 con 3 cilindri | Apertura "a nastro", pistoni, volano |
| Methane Extractor | Testa con anello; camera a gradoni | Anello spruzzatore |
| Mech Hangar | Stessa forma, solo palette | Come prima |
| Reattore | Torre a 8 lati, pelle a piastre | Come prima |
| Turbina | Scafo a 8 lati, pelle a piastre | Come prima |
| Liquid Tank, Gas Tank | Modulari, con vetro unico | Livello del fluido |

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
- Le macchine non fanno più scintille generiche: anche Assembler, dock, Resonance Core e Supply Terminal, e l'effetto di base è stato tolto da `MachineBlock`.

**Piccoli multiblocchi** (`e36ec96`, `b8ddb06`)
- Nuove classi `SmallMultiblockBlock`, `SmallMultiblockBlockEntity`, `HiddenFrameBlock`, `MachinePartBlock`.
- Si richiudono rompendo un frame. I frame nascosti tornano normali, così la macchina si può sempre riassemblare.

**Droni proiettori** (`b8ddb06`, `ebc427b`, `b3b00c5`)
- Nuovo `FrameDroneRenderer`: mini droni per i piccoli multiblocchi, al posto delle scintille.
- Droni più grandi per reattore, turbina, Crystallizer, camera del metano e Mech Hangar.
- Reattore e turbina: si parte dalla taglia con più blocchi piazzati e ogni clic passa alla successiva.

**Striscia di luce alla base** (`c69404f`)
- `foot_light` in `tools/machinemodels.py`: una striscia su tutto il contorno della base, che si accende quando la macchina lavora.
- Ce l'hanno: Combustion Generator, Bio-Digester, Drone Dock e Swarm Dock, Drone Assembler, Fabricator, Acid Synthesizer, Water Pump, Electrolyzer,
  Pyrolysis Oven, Coolant Mixer, Gas Turbine, Waste Reprocessor, Supply Terminal, Crystallizer da solo.
- Lo Steam Engine ha la sua, più alta, che ora chiude il giro anche davanti e in fondo alla fila.
- Non ce l'hanno: il Methane Extractor da solo (non ha base) e le strutture grandi.
- Il Bio-Digester ha una base unica sotto tamburo e tank.

**Renderer**
- Nuovi: `ModelParts`, `ShellTiles`, `FrameDroneRenderer`, `FabricatorRenderer`, `WaterPumpRenderer`, `ElectrolyzerRenderer`, `PyrolysisOvenRenderer`,
  `CoolantMixerRenderer`, `GasTurbineRenderer`, `WasteReprocessorRenderer`, `SupplyTerminalRenderer`, `BioDigesterRenderer`, `CombustionGeneratorRenderer`, `MufflerRenderer`.
- Riscritti: `AcidSynthesizerRenderer`, `FluidTankRenderer`.
- Tolto: `MovingPartsRenderer`, sostituito da `BioDigesterRenderer`.

**Suoni**
- Tutti da audio vanilla, definiti in `tools/assetgen.py` (`sounds()`):

| Macchina | Suono | Quando |
|---|---|---|
| Bio-Digester | Bolle | A ogni oggetto digerito |
| Water Pump | Pistone | Ogni 30 tick mentre pompa |
| Gas Turbine | Rombo di fiamma | Ogni 60 tick mentre brucia |
| Resonance Core | Ronzio | Ogni 80 tick mentre è online |

- Restano muti tank e Muffler, passivi, e i dock, dove suona il drone.

**Minerali**
- Nuovi overlay di bauxite, uranio e zolfo, simili a ferro e rame vanilla, con la funzione `pixel_art()` in `tools/texgen.py`.
- `texgen.py` si fermava dal commit `b3b00c5` perché mancavano `HAZMAT` e `HAZMAT_DARK`: rimesse.

**Attrezzi sul drone**
- Nuovi Harvester, Transport, Logger, Taxi, Rancher e Bomb, con movimento; dettagli propri per Builder, Breeder e Fisher. Texture del drone a 128x64.
- Planter e Fertilizer tengono il serbatoio e hanno sotto una pompetta che spruzza sul blocco (`Effects.spray`). Guard e Quarry invariati.
- Con più moduli sotto lo scafo, gli attrezzi si dividono lo spazio da davanti a dietro.
- Quando è posato davvero (un blocco subito sotto i piedi) ciò che scende sotto i pattini rientra e non entra più nel pad. In aria resta tutto fuori, anche da inattivo.

**Icone**
- 32 icone ridisegnate nello stile "indurito", in `tools/item_icons.py`; `build_all()` le scrive sopra le vecchie.
- Blueprint Disc nella palette.
- Il circuito avanzato passa da blu ad ambra: i tier sono menta, ambra, viola.
- Tuta, pantaloni e stivali Hazmat, telai, Rancher, Fertilizer e Planter tengono il concept originale, ridisegnato. I moduli del meka sono schede orizzontali.
- Tolto da `texgen.py` il vecchio codice che le disegnava (circa 500 righe); le 182 texture generate restano identiche.

**Strutture del Mechanist**
- Outpost e tutti gli edifici del villaggio nella palette (`tools/schemgen.py`, `tools/village_buildings.py`), 21 strutture rigenerate.

**Batteria e torri**
- Resonance Core, Resonance Cell e Stabilizer Pylon nella palette, modelli da `machinemodels.py`; cella non formata a gabbia aperta.
- Fusto dei piloni a croce con quattro spigoli di luce continui e un impulso animato; le prese sono quelle delle macchine.
- Ogni colonna collegata manda un drone che orbita il cristallo con un raggio, al posto del raggio del pilone (`ResonanceCoreRenderer`).
- `assetgen.py` riportava la bauxite ai valori di prima della 0.9.5: corretto, ora rigenera il JSON della release.

**Proiettore**
- Da bloccata, la struttura la proiettano mini droni che partono dal giocatore (`FrameDroneRenderer.project`). I piccoli multiblocchi restano fuori.

**Bilancio**
- Numeri invariati, verificati. Gas Turbine e Bio-Digester del villaggio avevano perso i Cyber Frame: rimessi, con tubi e tramoggia spostati. Una schematica accetta il frame nascosto.

**Suono di apertura**
- Suono di montaggio per tutti i piccoli multiblocchi, da `SmallMultiblockBlockEntity`: scatto allo sblocco, colpi meccanici durante l'apertura, colpo secco alla fine.

## Modifiche che cambiano il gioco

- **Edifici del villaggio già costruiti:** con i blocchi nuovi risultano non finiti, il livello del villaggio può fermarsi e i Builder collegati rompono i blocchi vecchi per rimetterli. Gli Outpost già generati restano com'erano.
- **Legno warped:** ora gli edifici chiedono legno del Nether.

Non solo estetica, da tenere presenti per i mondi esistenti:

- **Water Pump:** ora vuole una fonte d'acqua proprio sotto. Le pompe con l'acqua solo ai lati si fermano.
- **Gas Turbine:** non lavora finché non è assemblata, quindi quelle già piazzate aspettano i due Cyber Frame.
- **Coolant Mixer:** non lavora finché non è assemblato col suo frame.
- **Acid Synthesizer:** non lavora finché non ha i suoi 5 Cyber Frame. I tubi si collegano solo davanti e dietro, quindi quelli di lato e sopra sono da spostare.
- **Steam Engine:** i blocchi separati di cilindro e dinamo non esistono più. Gas ed energia passano dalla caldaia, con 4 Cyber Frame.
- **Crystallizer:** a camera formata prende i tubi al centro dei bordi del pavimento e al centro del tetto, senza Crystallizer Port.
- **Bio-Digester, Fabricator, Pyrolysis Oven:** non lavorano senza frame: 2 per il digester, 1 per gli altri due. Il digester perde il tubo in cima e la presa dal lato del tank, il Fabricator la presa dietro, il forno quella a sinistra.
- **Electrolyzer, Waste Reprocessor:** non lavorano senza il loro Cyber Frame. L'Electrolyzer perde la presa a sinistra, il Reprocessor nessuna.
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
| `7f80a74` | Ventola del Combustion Generator; documenti aggiornati |
| Ultimo | Muffler a molla; Electrolyzer 2x1; Waste Reprocessor alto 2; correzione delle texture per le facce lunghe |
| `c69404f` | Tank modulari; Acid Synthesizer, Bio-Digester, Fabricator e forno più grandi; striscia di luce; suoni; documenti |

## Dettagli di `c69404f`

**Bio-Digester, Fabricator, Pyrolysis Oven**
- Passano a piccolo multiblocco con modelli `_lone`, `_parts` e parti mobili `BIO_*`, `FAB_*`, `OVEN_*`.
- `BioDigesterRenderer` nuovo sostituisce `MovingPartsRenderer`; `FabricatorRenderer` e `PyrolysisOvenRenderer` estesi.

**Acid Synthesizer**
- Modelli `acid_synthesizer_parts()` e `acid_synthesizer_lone()` in `tools/machinemodels.py`; parti mobili `ACID_*` in `MachinePartBlock`.
- `AcidSynthesizerBlock` ed entità passano a piccolo multiblocco; `AcidSynthesizerRenderer` riscritto.
- `SmallMultiblockBlock`: frame anche fuori dai bracci. `HiddenFrameBlock`: `TO_MACHINE` vale anche verso il basso.

**Tank: codice**
- `FluidTankBlock`: stato per ogni lato (faccia, presa, unito); click col secchio, messaggio di rifiuto, shift + destro.
- `FluidTankBlockEntity`: fusione in parallelepipedi fino a 16 per lato, fluido condiviso.
- `FluidTankRenderer`: vetro, fluido a volume, segno sul coperchio.
- `CyberFarm`, `ModBlocks`: capacità del fluido, evento del click, `noOcclusion`.

**Tank: risorse**
- `tools/machinemodels.py`: pezzi di cornice, piastre, prese; blockstate multipart.
- `tools/assetgen.py`, `tools/texgen.py`: tolti i vecchi modelli e texture dei tank.
- Texture nuove: `tank_glass`, `tank_waves`, `tank_waves_x2`, `tank_cloud`, `tank_cloud_x2`.
- Tolte: `liquid_tank_side`, `gas_tank_side`, `tank_top`.
- Nuovi messaggi in `lang/en_us.json`; guida `fluid_tanks.md` riscritta.

**Limiti noti**
- Il segno sul coperchio sparisce con un tubo al centro, invece di spostarsi.
- Un tank a cavallo di due chunk può ignorare i blocchi non caricati finché non cambia o non si ricarica.
- Claude non può giocare: i giudizi in gioco sono dell'utente, che ha approvato il risultato prima del commit.
- La segnalazione sui fluidi nel tank sbagliato è stata lasciata cadere senza una prova: il salvataggio controllato aveva ogni tank col fluido giusto, e il dubbio
  dell'utente su quale segno indicasse cosa fa pensare a un equivoco. Il guasto non è escluso del tutto.
- Il messaggio di rifiuto del secchio non è stato confermato in partita.
- Il Muffler non è stato provato coi suoni nuovi.
- Dock e Supply Terminal non hanno un modello acceso: non è verificato se lì la striscia resta accesa o spenta.
- Nel Bio-Digester le fasce d'acciaio del tamburo e la seconda luce compaiono solo a fine apertura.

**Errore da ricordare**
- La prima striscia sul contorno usciva di 0,1 px dal limite dei modelli (-16..32) e il forno e il Coolant Mixer diventavano cubi neri e viola.
  Ora la base cede quel tanto alla striscia.

## Cosa manca

**Restyle**
- Niente: il restyle è finito.

**Da decidere o rimandati**
- Rotazione e specchiatura delle prese.
- Linee dei generatori non più usate.
- Uniformare l'altezza della striscia dello Steam Engine con le altre, se stona.

**Prima di pubblicare**
- Prova su server dedicato.
- Push e merge su `master`.
- Porting sulle altre versioni di Minecraft.
