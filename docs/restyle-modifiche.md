# CyberFarm, restyle: cosa è stato modificato

Elenco di tutto ciò che è cambiato finora. I motivi delle scelte sono in
[restyle-scelte-di-design.md](restyle-scelte-di-design.md).

Stato all'8 novembre 2026: il restyle è su `machine-models-prototype` (19 commit da `ce455a6`) e, per minerali, icone, attrezzi del drone, proiettore, villaggio, batteria, suoni, Biomass Press e correzioni dalle segnalazioni dei giocatori, sul branch `restyle-icons-drones` che parte da lì. Tutto su GitHub, nulla ancora unito a `master`.

## Riepilogo per macchina

| Macchina | Cosa è adesso | Parte animata |
|---|---|---|
| Combustion Generator | Modello 3D con ventola sul camino | Ventola a 6 palette, fuoco acceso, fumo e crepitio |
| Biomass Press | Multiblocco alto 2, camera aperta, imbuto e ugello | Apertura, piastra su asta che schiaccia un mucchio alto secondo il contenuto |
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
- Ce l'hanno: Combustion Generator, Biomass Press, Bio-Digester, Drone Dock e Swarm Dock, Drone Assembler, Fabricator, Acid Synthesizer, Water Pump, Electrolyzer,
  Pyrolysis Oven, Coolant Mixer, Gas Turbine, Waste Reprocessor, Supply Terminal, Crystallizer da solo.
- Lo Steam Engine ha la sua, più alta, che ora chiude il giro anche davanti e in fondo alla fila.
- Non ce l'hanno: il Methane Extractor da solo (non ha base) e le strutture grandi.
- Il Bio-Digester ha una base unica sotto tamburo e tank.

**Renderer**
- Nuovi: `ModelParts`, `ShellTiles`, `FrameDroneRenderer`, `FabricatorRenderer`, `WaterPumpRenderer`, `ElectrolyzerRenderer`, `PyrolysisOvenRenderer`,
  `CoolantMixerRenderer`, `GasTurbineRenderer`, `WasteReprocessorRenderer`, `SupplyTerminalRenderer`, `BioDigesterRenderer`, `BiomassPressRenderer`, `CombustionGeneratorRenderer`, `MufflerRenderer`.
- Riscritti: `AcidSynthesizerRenderer`, `FluidTankRenderer`.
- Tolto: `MovingPartsRenderer`, sostituito da `BioDigesterRenderer`.

**Suoni**
- Tutti da audio vanilla, definiti in `tools/assetgen.py` (`sounds()`):

| Macchina | Suono | Quando |
|---|---|---|
| Biomass Press | Pistone e slime, a volume basso | A ogni oggetto pressato |
| Bio-Digester | Bolle | A ogni Biomass digerita |
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
- Ogni colonna collegata manda un drone che orbita il cristallo salendo e scendendo, al posto del raggio del pilone (`ResonanceCoreRenderer`).
- Il raggio del drone diventa un treno di 3 anelli che volano dal ventre al cristallo (`entity/stabilizer_ring`, generata da `texgen.py`). Il cristallo respira come pezzo unico e si chiude quando gli anelli arrivano. Costo: al massimo 24 quad di anelli con 4 droni, al posto dei 16 del raggio; il respiro non aggiunge quad.
- `assetgen.py` riportava la bauxite ai valori di prima della 0.9.5: corretto, ora rigenera il JSON della release.

**Proiettore**
- Da bloccata, la struttura la proiettano mini droni che partono dal giocatore (`FrameDroneRenderer.project`). I piccoli multiblocchi restano fuori.

**Bilancio**
- Numeri invariati, verificati. Gas Turbine e Bio-Digester del villaggio avevano perso i Cyber Frame: rimessi, con tubi e tramoggia spostati. Una schematica accetta il frame nascosto.

**Suono di apertura**
- Suono di montaggio per tutti i piccoli multiblocchi, da `SmallMultiblockBlockEntity`: scatto allo sblocco, colpi meccanici durante l'apertura, colpo secco alla fine.

**Biomass Press e Biomass** (`bfcfaec`)
- Nuovo oggetto `biomass`, nuova macchina con `BiomassPressBlock`, `BiomassPressBlockEntity`, `BiomassPressMenu`, `BiomassPressScreen`, `BiomassPressRenderer`; parti mobili `PRESS_TOWER`, `PRESS_HEAD`, `PRESS_MASH`.
- Scarti organici (tutto ciò che accetta il compostatore e la carne marcia) → Biomass con la probabilità del compostatore, 2 s, 4 FE/t (Press e Digester vanno a corrente, chiesto dall'utente dopo la prima versione). La carne marcia ha l'80%.
- L'ugello spinge la Biomass nel contenitore accanto. Dall'esterno entrano solo gli scarti ed esce solo la Biomass.
- Una tramoggia sopra il frame nascosto alimenta la pressa: il frame le passa gli oggetti, solo per questa macchina (`CyberFarm`).
- Il mucchio cresce con scarti e Biomass presenti, pieno a 64. Il contenuto arriva ai client ogni 5 tick se è cambiato. La piastra si ferma sul mucchio e lo schiaccia di 0,7 px.
- Bio-Digester: accetta solo Biomass, 250 mB in 2 s. Stesso gas di prima in media, a parità di oggetti.
- `BiomassItem`, derivata da `BoneMealItem`: fertilizzante a mano e col Fertilizer Module. Si consuma sempre, fa crescere una volta su tre.
- Villaggio, `biogas_shed`: baule, tramoggia, pressa col frame sopra, ugello nel digester; poiché Press e Digester ora vanno a corrente, un Combustion Generator con tramoggia e baule del combustibile e due cavi, da rifornire a mano. JEI e REI, pagine info, guida (`biomass_press.md` nuova).

**Correzioni dalle segnalazioni dei giocatori** (`bfcfaec`)
- **Shift+click:** un oggetto rifiutato non compare più per un istante nello slot; il client non anticipa più lo spostamento (`MachineMenu`).
- **Transport e volo:** nei tragitti lunghi il drone sale prima a quota di attraversamento e poi va dritto (`DroneFlight.flyOver`). Il ritorno al dock veniva dall'abbandono dopo 5 s bloccato. Durante la salita quel conteggio non corre.
- **Raggio:** il limite è di 48 blocchi dal dock, in chunk caricati. Non è cambiato: ora è scritto nella guida.
- **Harvester:** le piante mature che hanno `age` al massimo e si raccolgono col clic vengono raccolte senza spaccarle (`HarvestJob.pick`, con un `FakePlayer`). Le `CropBlock` non si ripiantano più: ci pensa il Planter.
- **Supply Terminal:** tolto il timer in alto a destra.

## Pannelli, droni e liquidi

- **Pannello di stato (`StatusPanel`):** al posto dell'overlay accanto al mirino, un pannello sopra ciò che guardi, disegnato sull'interfaccia: macchine, Fluid Tank, Dock Tank e droni. Su macchine e tank sparisce con Jade, WTHIT o WAILA; sui droni resta.
- **Hologram Table 2x2 e 3x3:** tavoli affiancati si fondono da soli, raggio fino a 48 e 64, nomi dei droni. Anello di luce con texture animata.
- **Survey Tool guidato:** legge i moduli del drone e chiede solo area, rotta o destinazione. Niente più modalità da scegliere.
- **Mini droni per l'area (`DockAreaView`):** col pulsante Area della dock volano sugli angoli e illuminano i blocchi.
- **Sponge Module:** aspira qualsiasi liquido in un'area, 3x3 alla volta. Cappa aspirante sul modello.
- **Tank Module:** lattina da 32 secchi sul fianco. Conserva il liquido della spugna e sposta liquidi fra tank col Transport.
- **Dock Tank:** blocco accanto alla dock, solo per conservare; barra nella GUI della dock.
- **Pinza del Transport:** oggetto piccolo stretto fra le ganasce.

## Modifiche che cambiano il gioco

- **Harvester:** non ripianta più, serve il Planter. Corretto anche il bug per cui non raccoglieva le colture.
- **Compenetrazioni dei droni:** spostati il laser e ristretto il serbatoio dei semi.
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
- **Bio-Digester e scarti organici:** accetta solo Biomass. Le fattorie automatiche vanno rifatte inserendo una Biomass Press col suo Cyber Frame. Gli scarti già nello slot di un digester vecchio restano lì finché non si tolgono a mano.
- **Biogas Shed del villaggio:** ha blocchi nuovi, quindi quello già costruito risulta non finito.
- **Ribilanciamento dell'energia (9 novembre 2026):** i generatori producono di più (Combustion 60, Gas Turbine 100/240, Steam Engine 405/675, turbine 2.000/9.600/40.000 FE/t) e le macchine consumano di più (circa il doppio, Crystallizer 4.000 FE/t). Droni e mech consumano il doppio con batterie doppie. Macchine, droni e mech già esistenti prendono i nuovi valori da soli: la batteria massima di un drone si calcola dal telaio, quindi raddoppia anche per quelli già costruiti, che ripartono con la carica che avevano.
- **Trasmissione a parabole:** due blocchi nuovi, Transmitter Dish (piccolo multiblocco 2x2 con tre Cyber Frame) e Receiver Dish (alto 2, con un Cyber Frame sopra), con reti private o pubbliche e quattro Range Module. Modelli e GUI come approvati (vedi le scelte di design).
- **Water Pump:** la ricetta vuole un Basic Circuit invece dell'Advanced, così nel tier 1 l'acqua per l'Acid Synthesizer non arriva solo a secchi.
- **Addon nelle macchine:** 10 macchine hanno 3 slot addon su una linguetta a destra della GUI (Speed, Efficiency, Battery). Nuovo oggetto: Efficiency Addon.

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
| `c69404f` | Tank modulari; Acid Synthesizer, Bio-Digester, Fabricator e forno più grandi; striscia di luce; suoni; documenti |
| `7f80a74` | Ventola del Combustion Generator; documenti aggiornati |
| `235fc20` | Muffler a molla; Electrolyzer 2x1; Waste Reprocessor alto 2; correzione delle texture per le facce lunghe |
| `a97708b` | Minerali, icone, attrezzi del drone, proiettore a droni, villaggio, batteria e torri, suono di apertura (`restyle-icons-drones`) |
| `bfcfaec` | Biomass Press, Digester solo Biomass, fertilizzante debole; correzioni dalle segnalazioni dei giocatori |

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
- L'Harvester non è provato con Pam's HarvestCraft, la mod della segnalazione: non è installata qui. Vale per le piante con `age` e `BonemealableBlock`; una pianta fatta diversamente non viene riconosciuta.
- Il valore di sincronizzazione del timer del Terminal (`GLOBAL+1`) resta nella block entity, inutilizzato.

**Errore da ricordare**
- La prima striscia sul contorno usciva di 0,1 px dal limite dei modelli (-16..32) e il forno e il Coolant Mixer diventavano cubi neri e viola.
  Ora la base cede quel tanto alla striscia.

## GUI delle macchine

Tutte le macchine con una GUI usano lo sfondo da `tools/guigen.py`; restano a targa smaltata solo le schermate che non sono di una macchina: Item Filter, Projector e Mechanist. Generator e Gas Turbine hanno il disegno di lato (ventola e fuoco, rotore); Steam Engine, Methane Extractor, Reactor, Hangar e Dock tengono le loro viste e il loro layout sul nuovo fondo; Supply Terminal, Resonance Core e Muffler hanno testi e righe su pannelli scuri. Le barre di velocità di turbina e Steam Engine sono sparite: il disegno gira alla velocità vera.

Due stili. Le **nove macchine di processo** (Biomass Press, Bio-Digester, Pyrolysis Oven, Electrolyzer, Coolant Mixer, Acid Synthesizer, Fabricator, Waste Reprocessor, Crystallizer) hanno uno sfondo PNG in `textures/gui/`, generato da `tools/guigen.py`. Tutte le altre schermate tengono la "targa smaltata" qui sotto, a riempimenti.

Macchine di processo:
- **Fondo:** la texture vera del corpo macchina (`oven_body`), fascia del titolo e striscia di luce alla base.
- **In primo piano al centro:** slot con cornice d'acciaio chiara, senza scritte. Da vuoti mostrano sbiadito l'oggetto che entra o esce (`drawGhost`); Fabricator senza, perché dipende dalla ricetta. Una freccia incisa dà la direzione e non si riempie.
- **Di lato, colonna x 5..41:** il disegno del processo a colori pieni; l'avanzamento si legge da lì. Parti fisse nel PNG, parti mobili in Java: piastra e mucchio della Press, pastone e bolle del Digester, filo e fiamme del forno, alluminio, grani e arco dell'Electrolyzer, liquido e pale del Mixer, liquido e bolle dell'Acid Synthesizer, testina e tracciato del Fabricator, barra, disco e polvere del Waste Reprocessor, cristalli del Crystallizer.
- **Tank:** con le tacche; corrente: vetro d'acciaio col fulmine sopra.
- **Posizioni:** slot e tank spostati verso il centro, menu lato server aggiornati (`menu/*Menu.java`).
- **Scritte tolte dal pannello:** "Organic Waste" e "Biomass" sono tooltip sullo slot vuoto, la ricetta dell'Acid Synthesizer è un tooltip sulla freccia.
- Gli avvisi come "No power!" stanno su una targhetta.

Targa smaltata:

- **Piastra:** smalto teal con rivetti d'acciaio, fascia scura del titolo e riga menta. Il corpo è `#1B312F`.
- **Slot:** quelli della macchina hanno la cornice d'acciaio; quelli dell'inventario sono discreti, con un solo gradino scuro in basso a destra.
- **Tank:** spioncini d'acciaio. Il fluido sta a 1 px dalla cornice, così il bordo si vede anche su vapore e cryo; lo stesso vale per le barre. Le tacche diventano scure sotto il fluido.
- **Movimento:** accensione all'apertura, scintilla sulla riga del titolo, onda di luce sul vetro, riflesso sull'energia, impulso sulle barre, luce sulle barre del nucleo del Reactor. Solo riempimenti.
- **Helper nuovi:** `drawWell`, `drawLockedSlot`; `drawPlate` è usato anche da Mechanist e Projector.
- **Reactor:** fascia scura sotto lo stato, per leggere rosso e ambra; "Waste" 1 px a sinistra.
- **Drone Assembler:** il bottone vanilla è diventato un `drawButton`, 2 px più largo a sinistra perché "Disassemble" ci stia. Stesse azioni.
- **Colori tolti:** arancio e verdi di Fabricator, Pyrolysis, Steam Engine, Hangar, Methane Extractor e Terminal. Restano i colori del processo: acido, biogas, viola del Crystallizer, verde del Waste Reprocessor.
- Slot, aree di clic e menu lato server non cambiano.
- **La guida non cambia:** non descrive l'aspetto delle schermate.
- Il Mechanist tiene i bottoni vanilla delle risposte: non sono stati ridisegnati.

## Tubi e cavi

- **Una famiglia sola:** Energy Conduit, Liquid Pipe e Gas Pipe hanno la stessa forma e si distinguono per il colore delle luci e per il contenuto. Il tubo di vetro da 4,9 px, teal scuro come il resto della mod, arriva fino al bordo del blocco, quindi entra nel collare delle macchine e nel tubo accanto. A 1 px da ogni lato collegato c'è un manicotto: due piastrine sopra e sotto il vetro nel colore del tier (acciaio, ottone chiaro, viola) e una striscia di luce su tutti e quattro i lati (ciano, blu, verde, che respira), così si vede da ogni lato in qualunque direzione corra il tubo. Dove la linea si piega, si dirama o finisce c'è un cubo dello stesso vetro con una cornice d'acciaio scuro su ogni parete (`pipe_node_glass`); le pareti stanno solo dove non arriva un braccio. Il tubo da solo ha anche le strisce di luce sui fianchi (`{nome}_lone`). Nessun tappo, telaio né simbolo.
- **Solo mentre passa qualcosa:** il contenuto si vede finché c'è un trasferimento e il tubo torna vuoto 2 s dopo che si ferma. Il tubo che riceve il trasferimento lo mostra su tutta la rete e si programma un tick; il tick svuota la rete se nessun tubo ha trasferito da 40 tick, altrimenti si riprogramma. Lo stato non si salva: un mondo caricato parte con i tubi vuoti.
- **Liquidi:** il fluido vero riempie il vetro, senza animazioni sopra (l'onda e le bolle non sono piaciute).
- **Gas:** il gas vero (alpha 0xA0) e sopra delle nuvolette che scorrono lungo il tubo.
- **Energia:** mentre passa energia, due piani incrociati nel vetro con un arco ciano sottile a sinusoide (1 px, ±1 px) che corre, sfasati, e una scintilla ogni due fotogrammi. Li disegna `PipeFluidModel`; l'icona dell'oggetto ha lo stesso arco fisso nel modello.
- **Fluido vero:** `NetworkPipeBlockEntity` sa cosa passa (un fluido o l'energia) e lo manda al client. `client/model/PipeFluidModel.java` aggiunge alla mesh del chunk il fluido, le nuvolette del gas o l'arco, al massimo 48 quad. Nessun costo per frame: la mesh si ricostruisce solo quando un flusso parte, si ferma o cambia fluido, al massimo una volta ogni 2 s per rete. Il trasporto non cambia.
- **Icone:** l'oggetto mostra il contenuto del tipo, acceso sul vetro scuro come l'arco dell'energia: un tubo pieno a metà di liquido blu, con la superficie chiara e due bolle (`pipe_item_liquid`), nuvolette verdi brillanti con spazi vuoti (`pipe_item_gas`). Al centro dell'icona una fascia larga nel colore del tier, perché le piastrine sono troppo sottili per distinguere i tier nello slot.
- **Hitbox:** uguale per tutti, bracci fino al manicotto (5,4x5,4) e nodo 5x5 attorno al cubo di vetro. Il collare d'estrazione non è più arancione: è un manicotto scuro al bordo con una fascia di luce tutta attorno, nel colore del tipo (`{nome}_extract`).
- **Texture:** `pipe_atlas()`, `pipe_glass()`, `pipe_strip()`, `pipe_frames()` (nuvole e arco, 4 fotogrammi ciascuna) in `texgen.py`. Modelli e blockstate da `pipe_models()` e `pipe_glass_models()` in `assetgen.py`, con il vetro in modelli condivisi `pipe_glass_straight`, `pipe_glass_arm` e `pipe_glass_cap`. Le righe 6..9 dell'atlante restano la manica dei monconi delle macchine. Via i vecchi modelli `_straight`/`_arm` e `*_glass_*`.

## Cosa manca

**Restyle**
- Niente: il restyle è finito.

**Da decidere o rimandati**
- Rotazione e specchiatura delle prese.
- Linee dei generatori non più usate.
- Uniformare l'altezza della striscia dello Steam Engine con le altre, se stona.
- Biomass: dispenser e tag `c:fertilizers` (motivi in [restyle-scelte-di-design.md](restyle-scelte-di-design.md)).
- Icona della pressa: mostra la camera senza mucchio.

**Prima di pubblicare**
- Prova su server dedicato.
- Merge su `master`: i branch sono già su GitHub.
- Porting sulle altre versioni di Minecraft.
