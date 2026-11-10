# CyberFarm, restyle: scelte di design

Questo documento serve a riprendere il restyle da un'altra chat o da un altro PC senza rifare le stesse discussioni.
Raccoglie cosa è stato deciso, perché e cosa è stato bocciato. L'elenco di ciò che è già stato fatto è in
[restyle-modifiche.md](restyle-modifiche.md).

Stato all'8 novembre 2026, branch `restyle-icons-drones` (parte da `machine-models-prototype`).

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
8. **Se l'utente dà l'immagine o la forma, si usa quella.** Quando indica esattamente cosa vuole non servono altri fogli di idee: va
   applicato e fatto vedere in gioco.
9. **Dopo ogni avvio si controlla il log** cercando `Failed to load model`, `Missing block model`, `Missing textures in model` e
   `Using missing texture`. Un modello o una texture rotti si vedono neri e viola, e "Missing model" da solo non li trova.

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
- Eccezioni volute: viola per cristallo e campo del Crystallizer, polvere verde sul Waste Reprocessor ("radioattivo"), verde e marrone della poltiglia nella Biomass Press.

## Principi delle macchine

- **Modelli 3D veri,** fatti di box con profondità e sagoma, che non riempiono tutto il cubo.
- **Ogni macchina si anima mentre lavora,** con parti che si muovono davvero e mostrano cosa fa. Niente scintille generiche.
  La parte mobile deve correre almeno 3 px ed essere larga almeno 2,5 px, altrimenti a 8 blocchi non si vede.
- **Ogni presa visibile deve collegarsi davvero.** Un attacco che sembra collegabile e non lo è non va bene.
- **Niente facce grandi e piatte senza dettaglio,** e niente forme troppo tonde per Minecraft.
- **Tubi veri:** nei modelli si usano le texture reali dei tubi, mai inventate.
- **Muffler:** blocco singolo, un peso su una bobina che rimbalza secondo quanti suoni silenzia. Scartati la torre a cassetti e il tamburo a ventola.
- **Combustion Generator:** la parte mobile è una ventola a 6 palette sul camino. Scartati il bilanciere sullo sportello e le alette davanti al fuoco.
- **Fluidi veri:** dove si vede un fluido, livello e colore vengono dal tank reale.
- **Stessa forma, solo più grande.** Quando una macchina piace, si ingrandisce senza ridisegnarla: "modificalo leggermente". È il metodo che ha funzionato meglio.
- **Una macchina su più blocchi deve sembrare una sola:** base unica, nessuna fascia o pannello ripetuto a ogni blocco. Il forno diviso è stato bocciato:
  "sembrano due macchinari uno di fianco all'altro".
- **Striscia di luce alla base:** su tutto il contorno, non solo davanti, e continua anche sulle basi a più blocchi. Obbligatoria sui macchinari componibili; le strutture grandi
  non la hanno, "ovviamente". È sottile (0,7 px); lo Steam Engine tiene la sua, più alta.
- **Ogni macchina che lavora ha un suono,** da audio vanilla adattato e a volume basso. I blocchi passivi (tank, Muffler) restano muti, e i dock lasciano suonare il drone.
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

| Macchina | Forma | Cyber Frame | Tubi sul blocco macchina |
|---|---|---|---|
| Coolant Mixer | 2x1 | 1 a destra | Sui lati liberi |
| Gas Turbine | 1x3, macchina al centro | 2, davanti e dietro | Sui due lati e sopra |
| Steam Engine | 1x5 con 3 cilindri | 4 in fila dietro | Sui lati liberi, sopra e sotto |
| Acid Synthesizer | 3x1x2, macchina in basso al centro | 5, uno per lato e tre sopra | Davanti e dietro |
| Biomass Press | Alta 2, camera aperta davanti | 1 sopra | Nessun tubo: ugello a destra, tramoggia sull'imbuto o ai lati del blocco basso |
| Bio-Digester | Tamburo alto 2 e tank accanto | 2, sopra e a sinistra | Davanti, dietro e a destra |
| Fabricator | Lungo 2, con magazzino delle schede | 1 dietro | Davanti, sui lati e sopra |
| Pyrolysis Oven | 2x1, un tubo e un tank lungo | 1 a sinistra | Davanti, dietro e a destra |
| Electrolyzer | 2x1, una vasca, catodo e anodo | 1 a sinistra | Davanti, dietro e a destra |
| Waste Reprocessor | Alto 2, con caricatore | 1 sopra | Su tutti i lati |

Destra e sinistra valgono guardando la macchina dal davanti. Scartati: Acid Synthesizer 3x1 basso e con silo; Bio-Digester col tank alto 2; Fabricator lungo 3; forno lungo 3; Electrolyzer alto 2; Waste Reprocessor 2x2. La Water Pump resta a blocco singolo.

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
- **Mech Hangar: solo palette.** Forma e collisioni originali. La console resta a cubo (deciso dall'utente).
- **Strutture del Mechanist nella palette:** corpo in terracotta ciano, legno warped per finiture, porte e tetti in legno, accenti in cemento ciano, vetro ciano, luci in sea lantern. Via le strisce gialle e nere e le fasce arancio e lime. Solo blocchi ottenibili in sopravvivenza, perché li piazzano i droni: niente prismarine, che pure ha il colore giusto. Forme invariate.
- **Suono di montaggio:** per tutti i piccoli multiblocchi, scatto, colpi meccanici durante l'apertura e un colpo secco alla fine. Bocciato il ronzio del beacon: non si sentiva e non sapeva di montaggio.
- **Metodo:** sulle strutture grandi "stessa forma, solo palette" è stato quello che ha funzionato meglio.
- **Crystallizer:** resta multiblocco 3x3x4, da solo è un piedistallo modellato.
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

## Attrezzi sul drone

Stesso attrezzo e stesso posto, disegno e movimento migliori. Approvati Harvester, Transport, Logger, Taxi, Bomb, e i dettagli di Builder (livella) e Fisher (galleggiante).

- **Planter e Fertilizer:** il serbatoio piace e resta com'è. Sotto hanno una pompetta che **spruzza** il contenuto: bocciati il piolo seminatore e la bombola a fasce con leva e ugello.
- **Rancher:** cesoie sotto lo scafo, non di lato.
- **Breeder:** rastrelliera del fieno a listelli sotto i pattini, con il mangime che cade sull'animale. Bocciate la bocchetta dietro il serbatoio (non si vedeva) e la mangiatoia piccola tra i pattini ("non si capisce").
- **Taxi e Fisher:** l'uncino deve restare visibile anche da fermi.
- **Più moduli insieme:** "fai dei mix": si dividono lo spazio sotto lo scafo da davanti a dietro, quelli lunghi si accorciano. Uno solo resta al centro, identico.
- **Compenetrazioni:** niente deve attraversare gambe, pattini o la fascia del serbatoio centrale. Sono state segnalate in gioco, non si vedevano sul foglio.
- **Guard:** resta come era; bocciato il rifacimento con le alette.
- **Quarry:** già revisionato, non si tocca.

## Minerali

Scelta dell'utente: **simili a ferro e rame vanilla**. Texture piatte su roccia vanilla, 12 facce come prima; forme e colori nostri, nessun pixel copiato.

- **Uranio e zolfo come il ferro:** grumi larghi e storti, bordo smorzato che sfuma nella roccia, centro chiaro. Verde giallo per non confondersi con lo smeraldo, giallo limone per non confondersi con l'oro del Nether.
- **Bauxite come il rame:** due minerali affiancati, ruggine e ocra pallida.
- Il deepslate usa lo stesso overlay della pietra. L'uranio tiene la luce di blocco 3.

| Bocciato | Motivo o giudizio |
|---|---|
| Noduli grossi, vene, cristalli sporgenti (57 facce) | "non mi piacciono" |
| Incavati, con buchi nella roccia | "non mi piacciono" |
| Stile vanilla con grumi piccoli o a filamenti, bauxite rossa | "non mi piacciono" |
| Grumi tondi e simmetrici | Scartati prima di mostrarli: sembravano sfere |

## Icone

Da rifare, scelte dall'utente: i moduli Planter, Logger, Rancher, Fertilizer, Fisher, Breeder e Bomb; telai; circuiti; Mech Core, Mech Frame e moduli del meka; allumina e zolfo; Fuel Rod, barra esausta, Geiger, Hazmat; Survey Tool, proiettore, Handbook. Il Blueprint Disc cambia solo colore (fatto). Le altre icone restano.

Stile scelto, "indurito", partito da una barra che piaceva all'utente ma sapeva "troppo di Faithful": nativo 16x16, pixel messi a mano, forme dure e squadrate, contorno scuro tinto del colore che tocca, materiali che si leggono come acciaio, vetro o combustibile. I moduli Logger, Fisher, Breeder e Bomb hanno lo stesso gancio di quelli tenuti. Hazmat e Geiger restano gialli. I moduli del meka sono **schede orizzontali** con i contatti sul lato e il simbolo della funzione (scelta dell'utente, bocciate quelle verticali). Si giudica in gioco.

**Tuta, pantaloni e stivali Hazmat, telai, Rancher, Fertilizer e Planter tornano al concept originale** ("avevano un ottimo concept, solo troppo semplici e brutte"): stesso oggetto e stessa composizione, ridisegnati con più toni, luce coerente e piccoli dettagli. Dopo tanti tentativi questa è la regola: non cambiare oggetto a un'icona che ha già un'idea chiara.

Bocciate per questi gruppi: tuta gonfia, giacca snella, armatura vanilla, tuta col cappuccio, gilet; telai a cornice, a chassis dall'alto e di lato, a piastra, ad H; Rancher con forbici e lana, secchio, matassa, marchio all'orecchio; Fertilizer con granuli, sacco, ugello, scatola; Planter con germoglio, semi nella terra, bustina, seme, piolo.

Bocciate: "sagoma grande", cartuccia scura, 32x32 ("troppo Faithful": pixel più fini del vanilla, troppo morbido), tessera chiara; "tecnico scuro", "smalto chiaro", "senza contorno" ("non mi piacciono"). Sia qui sia sui minerali ha funzionato partire da un riferimento dato dall'utente.

## Batteria e torri

Direzione "sentinelle in orbita", scelta fra tre (bocciate le stazioni di relè ferme e la corona che gira): un drone per colonna esce dalla culla in cima e orbita il cristallo, grande 1,5 volte i mini droni perché a 8 blocchi si veda. Il raggio parte dal drone, non più dal pilone.

Fusto delle colonne: a croce con quattro spigoli di luce continui e un impulso che sale, perché la colonna si legga come una cosa sola. Bocciati la scatola scura con due fasce menta ripetute a ogni blocco; scartati il traliccio e il dissipatore ad alette, che ripetono un motivo per blocco.

## Biomass Press e Biomass

Parte da una segnalazione degli amici che hanno provato la mod: il Bio-Digester deve produrre qualcosa di fisico, perché la biomassa che diventa subito liquida non ha senso.

- **Due macchine:** "la macchina a parte deve creare la biomass e poi nel digester diventerà liquida". Scarti → Biomass Press → Biomass, oggetto vero → Bio-Digester → Biogas. Il Digester resta com'era, modello compreso.
- **Nessuna corrente:** come il Digester, la catena del gas di base non chiede energia.
- **Forma:** idea "B, pressa", scelta fra quattro. Nata a blocco singolo, l'utente ha poi detto che poteva essere a più blocchi: con un Cyber Frame sopra è alta 2, la piastra corre per tutta l'altezza e il movimento si legge da lontano.
- **Il mucchio mostra il contenuto:** richiesta dell'utente in gioco. Sale con scarti e Biomass presenti e la piastra si ferma sopra, invece di attraversarlo.
- **Ugello e tramoggia:** l'ugello è a destra guardando la macchina dal davanti e si scarica da solo, anche direttamente nel Digester. Una tramoggia sull'imbuto deve funzionare, perché è la prima cosa che un giocatore prova.
- **Fertilizzante debole:** proposta accettata dall'utente, "una su tre". La Biomass si consuma sempre e fa crescere la pianta una volta su tre.
  Con il compostatore servono circa 11 grani per una farina d'ossa, con la pressa circa 1,5 per una Biomass. A piena forza il compostatore non servirebbe più. Così resta circa il doppio più conveniente e un campo non si mantiene da solo.

| Bocciato o scartato | Motivo o giudizio |
|---|---|
| Biomass come stadio intermedio dentro il Digester, in uno slot visibile | "la biomassa che poi diventa subito liquida non ha senso" |
| Trituratore a rulli (A) | I rulli si vedono solo dall'alto |
| Macina con braccio (C) | L'idea più confusa |
| Estrusore a vite 2x1 (D) | La bocca degli scarti si vede poco |

## Correzioni dalle segnalazioni dei giocatori

- **Limite di raggio del Transport:** 48 blocchi dal dock e chunk caricati restano, ma ora la guida li scrive. La segnalazione era sul limite non descritto.
- **Timer del Supply Terminal:** via, perché era superfluo. Il tooltip degli eventi tiene il suo orologio.
- **Harvester:** le piante che si raccolgono col clic non si spaccano. Le colture normali non vengono più ripiantate: serve il Planter, sullo stesso drone (telaio più potente) o su un altro.

## Pannelli, droni e liquidi

Scelte dell'utente, tutte provate in gioco col setup `/function cyberfarm:test_setup`.

- **Overlay:** sopra la macchina, non in alto al centro né accanto al mirino. Disegnato nel mondo non si leggeva: ora è sull'interfaccia, in un punto calcolato dalla macchina. Se c'è Jade, WTHIT o WAILA sparisce, senza plugin.
- **Hologram Table:** idea A, stessa forma più grande. Scartati gradino con lente, piedistallo e paletti che salgono.
- **Sponge Module:** idea A, cappa aspirante, per tutti i liquidi. Scartati soffietto, spugna vera e siringa. Da sola butta il liquido sul posto.
- **Tank Module:** idea A, lattina sul fianco. Scartati pod di coda, fiasca sotto la pancia e pontoni. Solo col modulo il liquido torna alla dock. A modulo pieno il drone si blocca.
- **Dock Tank:** idea A, fusto sulla sella, serve solo a conservare. Scartati silo alto, vasca con asta e cisterna con cappa.
- **Vista dell'area:** mini droni sugli angoli, idea dell'utente. Bocciate le particelle. Partono solo dal pulsante della GUI, non dal Survey Tool.
- **Rotte:** un clic sulla dock collegata ripete la richiesta, due la scelgono come partenza.
- **Compenetrazioni:** ogni parte nuova del drone va controllata contro tutti i moduli, in ogni posa.

## Scartati

- **Piccoli multiblocchi nel proiettore:** non ci vanno, hanno già i loro droni.
- **Mech Repair Bay:** non si fa. Il meka si ripara già nell'Hangar, che ricarica e monta anche i moduli; chi arriva al Drone Workshop III ne ha già uno. Nell'edificio resta il piedistallo di andesite.

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

- **Biomass nel dispenser:** non funziona, il dispenser riconosce solo la farina d'ossa vera. Da aggiungere se serve.
- **Biomass col tag `c:fertilizers`:** non messo di proposito, così le altre mod non la trattano come un fertilizzante pieno.
- **Icona della Biomass Press:** il mucchio lo disegna il renderer, quindi l'icona e la pressa da sola mostrano il fondo marrone. Se sembra spoglia, le si rimette un mucchietto.

## Strumenti

```bash
python tools/machinemodels.py
```

Va lanciato da dentro `tools/`: rigenera modelli e blockstate delle macchine. Le texture sono in `tools/texgen.py`, gli altri JSON
in `tools/assetgen.py`.

I segni dei tank sono PNG in `textures/block/`: `tank_waves`, `tank_cloud`, con le versioni `_x2`, e non sono generati dagli strumenti.

Per rigenerare solo i suoni, sempre da dentro `tools/`:

```bash
python -c "import assetgen; assetgen.sounds()"
```

Un modello non può uscire da -16..32: il gioco lo scarta senza fermarsi. `foot_light` tiene conto di questo limite, e `checks.py` non lo controlla.

Il codice condiviso è in `client/renderer/ModelParts.java` (macchine animate), `ShellTiles.java` (pelle di reattore e turbina),
`FrameDroneRenderer.java` (droni), `SmallMultiblockBlock.java` (piccoli multiblocchi).
