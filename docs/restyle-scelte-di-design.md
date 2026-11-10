# CyberFarm, restyle: scelte di design

Questo documento serve a riprendere il restyle da un'altra chat o da un altro PC senza rifare le stesse discussioni.
Raccoglie cosa è stato deciso, perché e cosa è stato bocciato. L'elenco di ciò che è già stato fatto è in
[restyle-modifiche.md](restyle-modifiche.md).

Stato al 7 novembre 2026, branch `machine-models-prototype`.

## Da leggere prima: cosa non è su git

Da un altro PC, clonando il repository, **questi file non ci sono**, perché sono esclusi da `.gitignore`:

| File | Cosa contiene |
|---|---|
| `DESIGN.md` | Design completo della mod, con i dettagli di ogni macchina |
| `.claude/model-style.md` | Regole di stile per chi disegna i modelli |
| `.claude/agents/` | Gli agenti `model-designer`, `model-critic`, `model-reviewer` |
| `.claude/sketch/` | Strumenti di rendering: `iso.py`, `sheet.py`, `checks.py` |
| `graphify-out/` | Mappa del codice, si rigenera con graphify |

Anche le regole salvate nella memoria di Claude restano su questo PC. Per lavorare altrove conviene copiare a mano
`DESIGN.md` e `.claude/`. Questo documento riporta comunque le regole essenziali.

## Come si lavora

1. **Prima il rendering, poi il codice.** Per ogni modifica estetica si mostra un rendering e si aspetta la scelta dell'utente.
   Le piccole correzioni chieste esplicitamente si fanno subito.
2. **Il giudizio finale è in gioco.** Dopo ogni modifica si apre il client. Il rendering non basta: più volte una proposta
   approvata è stata bocciata una volta in gioco.
3. **Non chiudere mai il client aperto senza chiedere**, ogni volta.
4. **Commit solo su richiesta**, senza push finché non viene chiesto. Nei commit e nelle PR niente `Co-Authored-By` e niente
   "Generated with Claude Code".
5. **Lingua:** risposte in italiano; codice e testi nel gioco in inglese.
6. **Ogni modifica aggiorna la guida** (GuideME, `guides/cyberfarm/handbook/`) e `DESIGN.md` nello stesso lavoro.
7. **Prestazioni:** la mod finisce nei modpack. Modello JSON statico dove basta; un renderer per frame è un'eccezione e se ne
   dichiara il costo.

### Il giro di una proposta

1. **Idee:** 3 o 4 idee davvero diverse su un solo foglio. L'utente sceglie.
2. **Rifinitura:** solo la direzione scelta. L'utente approva o rimanda indietro.
3. **Codice,** poi giudizio in gioco, poi commit su richiesta.

Critico e revisore servono solo per i grandi cambiamenti, come una nuova palette o una nuova famiglia di macchine.

## Palette: "B, pale enamel"

Scelta il 5 novembre 2026 dai colori dell'utente: teal scuro `#132423`, menta `#6DD3C6`, smeraldo `#00B894`.
Ha sostituito il vecchio grigio-blu con rame e ciano, troppo simile a Oritech. Non si aggiungono altre scale di colore.

| Uso | Colori |
|---|---|
| Corpo, teal scuro | `#5B7771` `#395853` `#233E3B` `#132423` `#071314` |
| Corpo di reattore, turbina e tank (più chiaro, richiesto dall'utente) | `#4A6B65`, chiaro `#5C807A` |
| Accento menta | `#A5EAD4` `#5EC2B8` `#488D91` `#496973` |
| Acciaio | `#BDC6C5` `#B1BEBC` `#7C8989` |
| Incavi | `#030909` |
| Luci | alone `#D1FEE7`, nucleo `#FFFFFF`, spente `#055B57` |
| Smeraldo | `#00B894` per marchi e per ciò che ha un significato: stato, GUI, HUD, barra batteria |

- La luce deve essere la cosa più chiara del modello: almeno 1,8:1 sull'accento vicino e 1,5:1 sull'acciaio.
- Smeraldo e menta non devono toccarsi alla stessa luminosità.
- Luci sotto i 3 px: solo il colore dell'alone.
- Eccezioni volute: viola per cristallo e campo del Crystallizer, polvere verde sul Waste Reprocessor ("radioattivo").

## Principi delle macchine

- **Modelli 3D veri,** fatti di box con profondità e sagoma, che non riempiono tutto il cubo.
- **Ogni macchina si anima mentre lavora,** con parti che si muovono davvero e mostrano cosa fa. Niente scintille generiche.
  La parte mobile deve correre almeno 3 px ed essere larga almeno 2,5 px, altrimenti a 8 blocchi non si vede.
- **Ogni presa visibile deve collegarsi davvero.** Un attacco che sembra collegabile e non lo è non va bene.
- **Niente facce grandi e piatte senza dettaglio,** e niente forme troppo tonde per Minecraft.
- **Tubi veri:** nei modelli si usano le texture reali dei tubi, mai inventate.
- **Fluidi veri:** dove si vede un fluido, livello e colore vengono dal tank reale.
- **Striscia di luce alla base:** attorno a tutta la base di ogni macchina, come sullo Steam Engine, sul perimetro intero anche sulle basi a più blocchi. Le macchine su più blocchi hanno una base unica.
- **Simbolo radioattivo** nella nostra palette, non giallo e nero: trifoglio smeraldo su piastra scura con cornice d'acciaio.
  Sta su Waste Reprocessor e Crystallizer, non sulle parti del reattore come regola generale.

## Piccoli multiblocchi

Per le macchine avanzate che hanno bisogno di spazio, come 2x1 o 1x3.

- Si piazza il blocco macchina più i **Cyber Frame**, non blocchi diversi per ogni pezzo.
- A forma completa i frame si nascondono e la macchina **si apre** con un'animazione che muove davvero le parti.
  Rompendo un frame si richiude. Finché non è assemblata non lavora.
- Il **blocco di partenza è sempre un modello 3D,** mai un cubo con una texture. È anche l'icona dell'oggetto e accenna al lato
  dove va il frame.
- I tubi si collegano solo al blocco macchina.
- Le parti che si muovono nell'apertura usano le stesse texture della macchina finita: l'utente ha bocciato i colori piatti che
  cambiano in texture alla fine.
- Limite tecnico: un modello copre da -16 a 32, quindi 2 celle per asse da una cella qualunque, 3 solo col blocco macchina al centro.
- L'idea dell'apertura viene da Oritech; design, palette e forme restano nostri.

## Droni al posto delle indicazioni

Il vecchio sistema per indicare i blocchi mancanti era troppo uguale a Oritech. L'utente voleva qualcosa di originale.

- **Clic destro sul blocco di partenza:** escono tanti droni quanti servono. Ognuno si mette sopra il punto giusto e proietta il
  blocco fantasma da piazzare, in un cono di luce mezzo trasparente.
- **Strutture grandi** (clic sul controller non formato): droni 1,5 volte più grandi, ognuno proietta un gruppo di blocchi, fino a
  8 droni.
- **Reattore e turbina:** si parte dalla taglia con più blocchi già piazzati, così non decide il terreno. Ogni clic successivo
  mostra la taglia dopo.
- Una X ambra segnala un blocco che ostruisce.

## Strutture grandi

- **Reattore e turbina restano blocchi grossi.** Stessa struttura ma a **8 lati**, perché erano "troppo tondi per Minecraft".
  Le facce piatte guardano i quattro lati, dove arrivano i tubi.
- **Pelle "piastre e fasce",** una tessera da 16 pixel per blocco, dentro e fuori: la vecchia immagine stirata perdeva i dettagli.
- **Porte in rilievo** sulle facce. Su reattore e turbina vanno bene così: l'utente ha detto di lasciarle.
- **Mech Hangar: solo palette.** Forma e collisioni originali, console a cubo compresa.
- **Metodo:** sulle strutture grandi "stessa forma, solo palette" è stato quello che ha funzionato meglio.
- **Crystallizer:** resta multiblocco 3x3x4, da solo è un piedistallo modellato.
- **Acid Synthesizer:** stesso design, solo più grande, 3x1x2 con 5 Cyber Frame e la macchina in basso al centro. Scartati il 3x1 basso e la versione col silo.
- **Bio-Digester, Fabricator, Pyrolysis Oven:** ingranditi poco, variante A. Digester con tamburo alto 2 e tank basso; Fabricator lungo 2 con magazzino; forno 2x1. Il forno deve sembrare **una macchina sola**: un tubo, una fessura, un tank lungo, nessun pannello ripetuto ogni blocco.
- **Methane Extractor:** sorella del Crystallizer, con campana di vetro a gradoni. La gabbia quadrata è stata bocciata.

## Tank modulari

Decisi dopo molti tentativi: questa è la sezione da rispettare di più.

**Funzionamento**
- Si parte dal blocco singolo, già funzionante. Blocchi dello stesso tipo che riempiono **qualsiasi parallelepipedo**, fino a 16 per
  lato, diventano un solo tank.
- Ogni blocco aggiunge 32 secchi e tiene la sua parte: romperne uno perde solo quella.
- Fluidi diversi non si fondono. Il tank si riempie dal basso.
- I tubi si attaccano da tutti i lati, anche sopra e sotto. Dove entra un tubo c'è un collare da 7 px.

**Comandi**
- **Tasto destro col secchio:** riempie o svuota.
- **Shift + tasto destro,** con qualsiasi cosa in mano: dice quanto è pieno e quanto spazio ha.
- **Tasto destro con altri oggetti:** niente, così si può appoggiare un blocco sul tank senza shift.
- Se il tank rifiuta il secchio, un messaggio spiega perché.

**Aspetto (variante A)**
- Cornice da 3 px solo sul **perimetro del tank**. Finestra da 10 px su un blocco singolo.
- **Un solo vetro per lato,** che attraversa i blocchi. Si vede attraverso il tank fino al lato opposto.
- Il fluido è un **volume vero** subito dietro il vetro, non una texture sulla barra e non una colonna staccata dalle pareti.
- Coperchio e base sono un pezzo continuo, senza segni tra i blocchi.

**Liquido o gas**
- **Colori:** linea attorno al vetro e collare in acciaio `#B1BEBC` per il liquido, menta `#5EC2B8` per il gas.
- **Segno sul coperchio,** uno solo per tank, centrato, doppio dal 2x2 in su:
  - **onde** sul Liquid Tank;
  - **nuvola** sul Gas Tank, solo contorno, con la sagoma dell'immagine data dall'utente.
- Stile del segno: solco scuro `#2B403C` con pochi pixel chiari, pixel grandi come quelli del gioco.
- Il segno è **dritto per chi ha piazzato il tank** e poi resta fermo. Nei tank fusi comanda il blocco all'angolo basso.
- Con un tubo al centro del coperchio, il segno non compare.

La nuvola, 10x5 pixel (`m` = pixel menta):

```text
....#m#...
.m#.#..##.
#..#.....#
#........#
##########
```

**Vincolo tecnico:** ogni blocco sa solo se è unito ai vicini, non dove si trova nel tank. Per questo la cornice è fissa e il vetro
riempie il resto: una barra sottile centrata su un tank largo non si può fare con i modelli statici.

**Da non rifare sui tank**

| Bocciato | Giudizio o motivo |
|---|---|
| Feritoie multiple, finestre alte, cinture | "non mi piacciono" |
| Fondo nero nella barra | Doveva essere trasparente |
| Texture del fluido in rilievo sulla barra | Non si capiva che si stava riempiendo |
| Una barra per ogni blocco | Si vedevano i cubi |
| Fluido staccato dalle pareti | Sembrava una colonna |
| Linea acciaio contro menta come unica differenza | "il colore varia pochissimo" |
| Cornici smaltate blu e ambra, fasce colorate | "bruttissime" |
| Goccia e nuvola piene | "terribili" |
| Bolle | Ricordano l'acqua |
| Nuvola a ovale con gobbe | "non sembra una nuvola" |
| Nuvola piena a molti colori | Stonava con le onde |
| Segno che gira verso chi guarda | "non ha senso" |

## Bocciature generali

Da non riproporre:

- Macchine "in una cornice" o gabbia con montanti sottili e vetro piatto.
- Cubi piatti con sola texture, compreso il blocco di partenza dei multiblocchi.
- Palette solarpunk o agro-industriali. Aspetto troppo Oritech.
- Strisce gialle e nere. Aspetto da TV o monitor.
- Vasca di vetro con acqua teal (un fluido vero in uno spioncino va bene).
- Attacchi che sembrano collegabili e non lo sono.
- Passaggi da colori piatti a texture durante un'animazione.
- Corpi troppo pallidi. Forme troppo tonde. Texture stirate e povere di dettagli.
- Portali squadrati e pesanti. Rack leggero a piloni per il Mech Hangar ("bruttissimo"). Gabbia quadrata per il Methane Extractor ("pessimo").
- Drone piatto, con pinna dorsale o con un tank alto sopra.
- Scintille generiche sulle macchine ("horrible").

## Lasciati in sospeso

- Bilancio dello Steam Engine e Gas Turbine del villaggio: "vediamo dopo".
- Console del Mech Hangar: da decidere, ora resta a cubo.

## Strumenti

```bash
python tools/machinemodels.py
```

Va lanciato da dentro `tools/`: rigenera modelli e blockstate delle macchine. Le texture sono in `tools/texgen.py`, gli altri JSON
in `tools/assetgen.py`.

I segni dei tank sono PNG in `textures/block/`: `tank_waves`, `tank_cloud`, con le versioni `_x2`, e non sono generati dagli strumenti.

Il codice condiviso è in `client/renderer/ModelParts.java` (macchine animate), `ShellTiles.java` (pelle di reattore e turbina),
`FrameDroneRenderer.java` (droni), `SmallMultiblockBlock.java` (piccoli multiblocchi).
