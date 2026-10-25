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
| Energia | **FE**, interoperabile con le altre tech mod. I droni vanno **solo a corrente**; i gas servono solo ai generatori. |
| Mekanism | Compatibile **solo a livello di tubi**: i suoi tubi per fluidi trasportano i nostri gas, che non sostituiscono i suoi. |
| Nucleare, realismo | Come una centrale vera: il reattore scalda il refrigerante, il **vapore** va a una **Steam Turbine** che fa FE. |
| Ricette | Plugin opzionali **JEI** e **REI** (EMI legge quello JEI): categoria Drone Assembly, generatore come stazione dei combustibili, pagine info. In dev si sceglie il viewer con `recipe_viewer` in gradle.properties. |
| Stile | Sci-fi pulito. |

## Contenuti

### Droni
Un drone = **telaio (base a tier)** + **moduli funzione** (decidono il lavoro che fa).

- **Drone Assembler**: slot Frame + 3 slot Modules (sbloccati in base al tier) + output. Costa 2.000 FE.
- **Drone Dock**: base del drone (ricarica 500 FE/t, 9 slot di raccolto, area di lavoro). GUI minimal: "Drone Work" (lavoro in corso) e "Drone Position", cargo 3x3, Filter, barra energia e pulsante **Return/Resume** (richiama il drone e lo tiene in dock finché non si riprende; un taxi finisce prima la corsa). Due schede apribili a sinistra: **Drone Status** (stato, moduli, area solo se un modulo la usa, percorso/taxi) e **Battery** (carica, FE, in ricarica o in volo); zone escluse per JEI e REI.
- **Survey Tool**: 4 modalità, si cambiano con Shift + rotella del mouse o clic destro in aria (come il Configurator di Mekanism; pacchetto `SurveyModePayload` al server): **Dock** (clic su una dock la collega, Shift+clic la azzera), **Area** (due angoli: max 33x33, dislivello max 8, entro 48 blocchi), **Route** (due contenitori per il Transport; anche la dock di un altro drone), **Taxi** (blocco di arrivo, entro 256). Cambiare modalità annulla l'angolo o la sorgente a metà. Guida passo passo nella barra sopra la hotbar: al cambio modalità e dopo ogni clic dice cosa cliccare dopo; il primo clic, in qualsiasi modalità, sceglie la dock. Agisce prima del blocco (`onItemUseFirst`): non apre mai casse o macchine.
- **Telai**: Basic (T1: 20k FE, 80% = 2,4 blocchi/s, 1 modulo, 4 addon) → Advanced (T2: 60k FE, 130%, 2 moduli, 8 addon) → Elite (T3: 150k FE, 160%, 3 moduli, 16 addon).
- **Componenti di crafting**: Drone Core, Rotor, Battery Cell. **Cyber Frame**: blocco strutturale dei multiblocchi; si illumina (texture "formed") quando la struttura è completa.
- **Moduli funzione**: Harvester ✅ · Quarry ✅ (strati fino al bedrock, solo con area marcata) · Transport ✅ (cassa → cassa, percorso col Survey Tool) · Planter ✅ (semi dalla Dock su terra arata) · Logger ✅ (abbatte alberi interi e ripianta) · Taxi ✅ (giocatore appeso all'uncino, destinazione con Shift+clic del Survey Tool, solo andata, rotta in quota) · Rancher ✅ (tosa ogni mob tosabile via `IShearable`, munge mucche e capre con secchi vuoti dalla Dock) · Fertilizer ✅ (qualsiasi fertilizzante: tag `c:fertilizers` + farina d'ossa, usato tramite fake player su colture, steli e arbusti; non su erba e muschio) · Builder ✅ (costruisce il multiblocco collegato alla dock col proiettore: blocchi dalla dock, un tipo per viaggio nella pinza del Transport; uno strato alla volta dal basso, il successivo solo a strato finito, e se mancano i blocchi dello strato corrente aspetta; nello strato prima il più vicino; tutto ciò che sta sopra lo strato corrente è vuoto, quindi il drone sale in verticale fuori dallo strato e va dritto al blocco; solo se la linea è ostruita fa la gru (sale sopra tutta la struttura, attraversa e scende); ogni tratta parte poco prima della fine della precedente, così curva senza fermarsi agli angoli; non resta mai murato dentro; scheda Build nella GUI della dock con strato corrente e blocchi che gli mancano; ogni blocco è prenotato da un drone alla volta, così più droni su più dock si dividono la stessa costruzione; non rompe mai blocchi, la GUI della dock conta quelli d'intralcio. Con una costruzione collegata la dock accetta dall'esterno solo i blocchi che mancano, e il Transport carica solo ciò che la destinazione accetta: una rotta magazzino → dock porta esattamente i materiali) · poi Fluid Siphon.
- **Logica**: ogni modulo è un `DroneTask` che trova il prossimo `DroneJob`; il drone alterna i moduli a ogni giro e concatena i lavori dello stesso tipo.
- **Addon** (4 slot nell'Assembler, ognuno una pila dello stesso addon; totale limitato dal telaio):
  Speed (+10% velocità ciascuno) · Stack (+1 stack per viaggio del Transport; base 16 oggetti, max 9 stack) · Battery (+25% batteria del telaio) ·
  Fortune (max 3) · Silk Touch (max 1, incompatibile con Fortune). Fortune/Silk valgono per Quarry e Harvester.
- **Consumo in volo** = (telaio 2/3/4 FE/t + moduli: Harvester +1, Quarry +2, Transport +1, Planter +1, Logger +2, Taxi +2, Builder +2) × (1 + % addon: Speed 10%, Stack 5%, Battery 5%), arrotondato per eccesso.
- **Consumo per lavoro**: raccolto 40 FE, blocco scavato 20 FE + 10 × durezza, seme piantato 10 FE, tronco 20 FE / foglia 5 FE, viaggio Taxi 50 FE, blocco piazzato dal Builder 20 FE; Fortune +25% per livello, Silk Touch +50%. Tutti i costi sono nei tooltip.
- **Smontaggio**: drone nello slot di uscita dell'Assembler (altri slot vuoti) → pulsante Disassemble; i pezzi tornano negli slot, la carica va nel buffer dell'Assembler.
- **Indistruttibili**: nessun danno da nessuna fonte, giocatori in creativa compresi (il flag vanilla li lascia passare), immuni al fuoco, il clic sinistro li attraversa; solo `/kill` e il vuoto li eliminano e allora cadono come oggetto.
- **Volo**: rettilineo con accelerazione e frenata, pathfinding solo come riserva quando il drone è bloccato.
  Fischio da quadricottero vero, sintetizzato da `tools/soundgen.py` (4 eliche a toni vicini che battono tra loro più
  il soffio dell'aria, loop di 2 s senza giunture); volume e tono seguono velocità e giri delle eliche, addolciti.
- **Ripresa** (Shift + clic destro a mano vuota): il drone atterra (sulla dock se c'è, se no a terra sotto di sé), le
  eliche rallentano fino a fermarsi, i bracci si richiudono e finisce nell'inventario di chi l'ha preso (a terra se è
  andato lontano); il carico va nella dock. Sincronizzato con l'istante dell'atterraggio (`DATA_SHUTDOWN`).
- **Avvio** quando si posa sulla dock (~4 s): appare chiuso coi bracci ripiegati lungo lo scafo, si apre con uno scatto,
  le eliche accelerano da ferme (il ronzio sale con loro), poi decolla di 1,5 blocchi, resta un attimo in volo e va al lavoro
  o riatterra. Sincronizzato solo con l'istante d'inizio (`DATA_STARTUP`), il resto lo calcola il client.
- **Quarry**: scava a serpentina riga per riga, invertendo il verso a ogni strato; torna a scaricare a 192 oggetti. In una cava profonda la linea retta dalla dock taglia il bordo della buca (e il pathfinding arriva solo a 16 blocchi): se la linea è bloccata ma il cielo sopra è libero, il drone esce dalla buca, passa sopra la colonna del blocco e scende in verticale; al ritorno sale prima di puntare alla dock. Uno strato si chiude solo quando non ha più blocchi scavabili raggiungibili da sopra: quelli che il drone non ha raggiunto in questo giro si riprovano dopo il ritorno alla dock, quelli sotto blocchi mai scavati (casse, bedrock) si lasciano. Se lo strato registrato ha sopra terreno ancora intatto (contatore andato avanti rispetto allo scavo, come succedeva prima) la cava riparte dall'alto dell'area.
- **Idee future**: Range Booster, Solar Skin.

### Energia
- **Energy Conduit**: si collega da solo a cavi e blocchi FE di qualsiasi mod; non accumula, distribuisce subito
  a turno tra le macchine della rete (2.000 / 16.000 / 128.000 FE/t per sorgente secondo il tier, reti fino a 1.024 cavi).
- **Tier 1** ✅: Combustion Generator (brucia i combustibili della fornace).
- **Tier 2** ✅: Bio-Digester (compostabili e carne marcia → Biogas, senza corrente), Methane Extractor (multiblocco "doccia" 3x3x4:
  pavimento e tetto di Cyber Frame, pareti con angoli in Cyber Frame e vetro al centro, estrattore al centro del tetto;
  a struttura completa suono + estrattore e frame illuminati, GUI apribile da ogni blocco della camera (Shift per piazzare);
  mob caricato con la Mob Capsule, che cattura qualsiasi mob tranne boss e droni; zombie 25 mB/t, mostri 12, animali 4), Gas Turbine (biogas 3 FE/mB, metano 8 FE/mB,
  20 mB/t), Gas Pipe (1.000 / 4.000 / 16.000 mB/t per sorgente secondo il tier). Gas = fluidi NeoForge standard con tag `c:gaseous`/`c:biogas`/`c:methane`.
  I gas servono solo ai generatori: i droni vanno sempre e solo a corrente. Mekanism: compatibile solo a livello di tubi
  (i suoi tubi per fluidi trasportano biogas e metano), i nostri gas non sostituiscono i suoi.
- **Acido** (da testare in gioco): Nether Sulfur Ore nel netherrack del Nether (2-4 Sulfur, Fortune; tag `c:ores/sulfur`,
  `c:dusts/sulfur`, `c:ores_in_ground/netherrack`), Acid Synthesizer (1 Sulfur + 500 mB acqua → 500 mB Acid, 2 s, 20 FE/t).
  Il Methane Extractor ora vuole acido (1 mB/t, serbatoio 8.000 mB visibile come pozza verde in camera): senza acido non
  produce; ogni 15 s l'acido corrode il mob, che alla fine viene consumato. Pareti della camera in Containment Glass.
  Camera restyle: a struttura completa i blocchi si nascondono e `MethaneChamberRenderer` disegna una vasca cilindrica
  (base e cappello bianchi con fasce ciano, vetro, 4 pilastri con strisce arancioni, acido con bolle; niente
  ologramma, i livelli sono nella GUI). Port: qualsiasi Cyber Frame può essere Acid Inlet (solo acido in)
  o Methane Outlet (il metano esce solo da lì); l'estrattore non espone più fluidi. Intake Gate al posto di un vetro
  della parete bassa: a camera vuota risucchia il primo mostro davanti (niente boss), portello che si arrotola.
  Acid Inlet / Methane Outlet con riga colorata nel tooltip; la GUI dice dove va l'acido quando manca.
  Acid Synthesizer: due finestre di vetro incorniciate (acqua, acido) riempite al livello reale con bolle mentre lavora
  (`AcidSynthesizerRenderer`, sync ogni 5 tick).
  Il livello dell'acido arriva ai client anche ogni 5 s; con il serbatoio del metano pieno l'estrattore si ferma (niente acido sprecato).
  Decomposizione lenta: il mob resta della sua taglia ma la pelle si buca a chiazze e diventa verde malato
  (`DissolvingMobRenderer`: render state modifier + layer con `RenderTypes.entityCutoutDissolve` e maschera di rumore,
  come la morte del drago; il corpo normale è nascosto come per l'invisibilità), in modo continuo tra un punto vita e
  l'altro. Il server manda fumi verdi, brandelli di carne marcia e gocce di slime; si vede anche nella GUI.
  GUI nuova: finestra sulla camera col mob renderizzato, acido che sale, bolle e archi, integrità del mob.
  Riempimento animato: quando entra acido (sincronizzato `filling`) un getto scende dalla testa dell'estrattore, schizzi
  e onde sulla pozza, livello che sale in modo fluido, Acid Inlet che pulsa; nella GUI stato FILLING e getto nella finestra.
- **Steam Engine** (tier 2.5, da testare in gioco): multiblocco in fila, Caldaia a induzione → 1-4 Cilindri → Dinamo.
  15 mB/t di biogas (6 FE/mB) o metano (10 FE/mB) per cilindro, fino a 600 FE/t; ~5 s per andare a regime, buffer
  200.000 FE, l'energia esce dalla dinamo. Restyle come la camera del metano: a fila completa i blocchi (FORMED) si
  nascondono e `SteamEngineRenderer` disegna la macchina intera su un basamento con linea ciano: caldaia bianca con
  bobine arancioni che pulsano con la velocità e sbuffi di vapore, cilindri con pistone luminoso in camicia di vetro,
  linea del vapore, dinamo con avvolgimenti in rame e volano a raggi con bordo ciano. GUI con vista laterale animata.
  Tubi e cavi collegati a caldaia e dinamo ricevono un manicotto con anello ciano fino al bordo del blocco.
- **Tier 3 — Nucleare** (in progettazione, vedi sotto).

### Nucleare (Tier 3) — progetto

**Risorse**
- **Uranium Ore** (+ deepslate) nell'Overworld in profondità (Y -64..16), raro → Raw Uranium → fornace → **Uranium Ingot**
  (tag `c:ores/uranium`, `c:raw_materials/uranium`, `c:ingots/uranium`).
- **Fuel Rod**: lingotti di uranio in un involucro di ferro. Brucia per un tempo fisso, poi diventa **Depleted Fuel Rod** (le scorie).

**Fission Reactor** — multiblocco a torre di raffreddamento, 5x5 di base e 7 di altezza:
```
y6   ■ ■ ■ ■ ■     bordo superiore aperto: da qui esce il vapore
y5     ■ ■ ■
y4     ■ ■ ■       "vita" stretta (anello 3x3)
y3     ■ ■ ■
y2   ■ ■ ■ ■ ■     base: anello 5x5 (Reactor Casing) con Controller e porte
y1   ■ ■ ■ ■ ■     interno 3x3 vuoto = nocciolo
y0   ■ ■ ■ ■ ■     pavimento 5x5
```
- Blocchi: **Reactor Controller** (uno, nella base), **Reactor Casing** e **Reactor Port** (porte per tubi, cavi e tramogge).
  Come l'Extractor: struttura completa = suono, involucro con texture "formed" illuminata, GUI apribile da ogni blocco.
  Da acceso: nuvole di vapore dalla cima.
- GUI: slot per le Fuel Rod, slot di uscita per le scorie, **potenza (boost)** regolabile, barra della **temperatura**,
  serbatoio del refrigerante e del vapore.
- **Calore**: lo producono le barre attive × potenza. Il refrigerante lo assorbe ed evapora in **Steam** (fluido gas,
  tag `c:steam`). Il vapore prodotto è proporzionale al calore assorbito, qualunque sia il refrigerante.
- **Refrigeranti**: più si spinge la potenza, più calore va tolto; oltre un certo boost l'acqua non basta più.
  - **Water**: calore assorbito ×1 per mB (anche da pompe di altre mod);
  - **Coolant**: ×2,5, prodotto da un **Coolant Mixer** (acqua + ghiaccio blu/lapis + FE);
  - **Cryo Coolant**: ×6, Coolant + materiale raro nel Coolant Mixer.
- **Temperatura**: sopra l'80% allarme sonoro e avviso nella GUI; al 100% **meltdown**: esplosione (config esistente
  `meltdownExplosionPower`, `meltdownDestroysBlocks`) e forte contaminazione della zona. Rompere la torre o il controller
  sopra l'80% non lo evita: il meltdown parte lo stesso (dal controller, l'esplosione al tick successivo).

**Taglie dei multiblocchi** — reattore e turbina in 3 taglie; le grandi sono gradualmente migliori:
- Reattore: Small 5×5×7 (2 barre, max 200%, 150 calore/t), Medium 7×7×11 (4 barre, 350%, 250), Large 9×9×15 (8 barre, 500%, 400);
  Coolant ×2,5, Cryo ×6. Il controller riconosce la taglia da solo (prova dalla più grande).
- **Steam Turbine** (multiblocco, sostituisce il vecchio blocco singolo): sala orizzontale di Turbine Casing/Port, controller al centro
  del fronte, interno vuoto per il rotore. Small 3×3×4 (200 mB/t, 4 FE/mB), Medium 5×5×6 (600, 5), Large 7×7×8 (1600, 6).
  Il rotore accelera e rallenta gradualmente; formata diventa un cilindro bianco/azzurro con rotore animato. Accetta `c:steam`.

**Multiblock Projector** — ispirato al Projector di Immersive Engineering: click destro apre la lista delle strutture
(`ProjectorScreen`, con i blocchi che servono; pacchetto `ProjectorSelectPayload`), la proiezione segue lo sguardo e si
blocca con Shift + click destro su un blocco (su un controller si allinea), Shift + click destro in aria la sblocca;
blocchi fantasma per ciò che manca, rossi per ciò che è sbagliato, contatore sopra la hotbar. Click destro su una
Drone Dock con la proiezione bloccata la collega alla dock (`BuildPlan`, salvato per nome del Blueprint); con una
proiezione sbloccata la scollega. In creativa il click destro su un blocco costruisce subito.

**Scorie e Waste Reprocessor**
- Le Depleted Fuel Rod sono radioattive: in inventario, in una cassa o a terra contaminano la zona attorno.
  Se lo slot scorie del reattore è pieno, il reattore si ferma e perde radiazioni.
- **Waste Reprocessor** (macchina a FE): Depleted Fuel Rod → **Vitrified Waste** (blocco inerte e sicuro) + un po' di
  uranio recuperato.

**Radiazioni**
- Contaminazione per chunk, salvata nel mondo, che cala lentamente nel tempo. Fonti: scorie non trattate, reattore che perde, meltdown.
- **Radiation Sickness** (effetto): debolezza, fame, nausea e danno nel tempo, più forte con livelli alti; colpisce giocatori e mob.
- **Effetti sul mondo** nelle zone contaminate: erba → terra, colture → arbusti secchi, foglie che cadono.
- **Hazmat Suit**: armatura in 4 pezzi; il set completo protegge del tutto, i pezzi singoli in parte.
- **Geiger Counter**: in mano ticchetta più veloce dove c'è più radiazione e mostra il livello sopra la hotbar.

**Fasi di sviluppo**
1. ✅ (da testare in gioco) Uranio (minerale, worldgen, lingotti, Fuel Rod), Fission Reactor multiblocco con acqua, Steam e
   Steam Turbine, più una **Water Pump** (2+ sorgenti d'acqua accanto, 500 mB/t, 40 FE/t). Numeri: 50 mB/t d'acqua → 50 mB/t di
   vapore per barra, Fuel Rod da 40 minuti (3 lingotti); Steam Turbine 4 FE/mB fino a 200 mB/t (800 FE/t).
2. ✅ (da testare in gioco) Potenza da Off a 500% (step 50%, parte da Off), calore e temperatura, circuito primario:
   acqua 200 calore/t, Coolant 500, Cryo Coolant 1200 (usura 1 mB ogni 400 calore); l'acqua secondaria diventa vapore
   1:1. Le barre bruciano anche senza raffreddamento: allarme all'80%, meltdown a 200.000 di calore (esplosione da config,
   barre distrutte). Coolant Mixer: acqua + lapis → Coolant, Coolant + ghiaccio blu → Cryo Coolant (500 mB, 20 FE/t).
   Secchi di tutti i fluidi della mod: si riempiono con un secchio vuoto su una macchina/serbatoio con ≥1000 mB e si
   versano con clic destro in macchine, porte e serbatoi. I gas (secchio capovolto) non si piazzano nel mondo; Coolant e
   Cryo Coolant sono liquidi veri, piazzabili, scorrono come l'acqua (texture dell'acqua vanilla colorate). Il movimento dentro è quello dell'acqua
   (`CoolantFluidType.move`: NeoForge lascia il movimento nei fluidi delle mod al loro tipo) al 70% / 50%: chi ci entra rallenta e
   gela piano (danno solo da congelato del tutto; Cryo rallenta e gela di più, il cuoio protegge dal gelo).
3. ✅ (da testare in gioco) Contaminazione per chunk (`RadiationData`, cala del 2% + 0,1 ogni 5 s). Fonti: barre esaurite
   addosso, nei contenitori vicino ai giocatori (anche i magazzini di altre mod, letti tramite la capability degli item;
   le macchine CyberFarm schermano) o a terra (2,5 per barra ogni 5 s), reattore con scorie piene (perde),
   meltdown (400 su 5x5 chunk). Radiation Sickness da 20 / 100 / 300 Rad (danno che ignora l'armatura, fame, debolezza,
   nausea) per giocatori e mob; da 50 Rad il terreno appassisce (config `radiationWorldEffects`). Waste Reprocessor:
   barra esaurita → Vitrified Waste (+ 1/3 lingotto di uranio), 40 FE/t, 10 s.
4. ✅ (da testare in gioco) Hazmat Suit (4 pezzi, 25% ciascuno, protegge anche dal gelo del refrigerante) e Geiger Counter
   (ticchettio e livello in Rad sopra la hotbar, mai silenziato dal Muffler).
5. ✅ (da testare in gioco) Aspetto: a torre formata i casing spariscono e il controller disegna una vera torre di
   raffreddamento (iperboloide liscio, pannelli bianchi futuristici con fasce azzurre luminose). Anche porte e controller
   spariscono: la torre li disegna come portelli luminosi e pannello a filo della superficie, dove entrano tubi e cavi.

### Accumulo di energia (Energy Storage)
- **Resonance Core**: batteria costruibile a FE. Basamento con un cristallo poliedrico (dodecaedro rombico di vetro con un
  nucleo luminoso che cresce con la carica) che fluttua sopra, disegnato dal renderer. Da solo è tier 1; un **guscio** di
  **Resonance Cell** a cubo vuoto subito sopra il basamento (3x3x3, 5x5x5, 7x7x7) lo porta a tier 2-4: a guscio completo le
  celle diventano invisibili (`formed`) e il cristallo cresce fino a riempirlo. Clic su una cella formata apre il core.
  Rompendo una cella il tier scende e l'energia che non ci sta più va persa; rompendo il basamento l'energia si perde.
- **Tier**: capacità 1M / 32M / 256M / 2.000M FE; I/O per pilone 16k / 128k / 512k / 2M FE/t.
- **Stabilizer Pylon**: si impilano in **colonne** (base con piedistallo, segmenti, in cima anello emettitore e punta che
  emettono luce). Colonne sullo stesso piano del basamento, allineate sui 4 lati, una per lato: ne servono quante il tier,
  alte tier+1 blocchi (l'anello in cima sta all'altezza del cristallo, raggio orizzontale), a distanza tier+2 .. 6+2*tier,
  senza ostacoli all'altezza del cristallo. Con meno colonne il core è **Unstable** (cristallo lento, viola, tremolante) e
  non entra né esce niente, ma l'energia resta.
- **I/O dai piloni**: il basamento non ha capability; ogni blocco di una colonna collegata espone l'energia del core
  (`PylonEnergyHandler`, che cerca il core a ogni chiamata). Passiva: si svuota col braccio "pull" dei cavi.
- Il Multiblock Projector mostra i tier 2-4 con le colonne di piloni.
- Grafica dei piloni: dove si collega un Energy Conduit compare una presa (multipart, proprietà north/east/south/west) che
  va dal fusto al bordo del blocco; il raggio parte da una lente luminosa che pulsa sul lato dell'anello rivolto al core.
- **GUI**: si apre da qualsiasi parte (basamento, celle anche a guscio incompleto, piloni allineati; il menu resta aperto entro 24 blocchi). Mostra tier, stato, colonne presenti/necessarie e per ogni lato cosa ha trovato (collegato, nessun pilone, troppo vicino, troppo lontano, troppo corto, raggio bloccato) con altezza e distanza al passaggio del mouse.

### Meka (progetto, branch `mech`)
Un mecha pilotabile di **~3,5 blocchi** (stile Knightmare, nei colori della mod: bianco con accenti azzurro/blu, ali scure), entità `MechEntity`.
- **Ottenerlo**: si crafta il **Mech Frame** (item) e lo si dispiega nell'Hangar; il meka vive come entità nel mondo.
- **Entrare/uscire**: click destro → le piastre del torace si aprono (~1 s), il player viene risucchiato nella cabina,
  le piastre si richiudono e parte il boot dell'HUD; un tasto dedicato (default **V**) fa l'animazione inversa e lo
  fa scendere. Dentro il player è nascosto; visuale in prima persona dalla cabina con HUD, F5 per la terza persona
  (camera più lontana).
- **Base (senza moduli)**: cammina (WASD), **salto alto** (~4 blocchi), **pugno** che colpisce forte e rompe i blocchi
  più in fretta. Batteria interna **500.000 FE**; consuma FE muovendosi (~20 FE/t camminando), da fermo quasi niente.
- **Integrità**: il meka ha la sua vita; a 0 non esplode, si **spegne**, espelle il pilota e va riparato in Hangar.
- **Moduli** (uno per slot, visibili sul modello e montati con animazioni fluide):
  - **Jet** (schiena): le ali escono dalla schiena e si compongono pezzo per pezzo; doppio salto = volo, consuma ~80 FE/t.
  - **Braccio trivella** (braccio destro): l'avambraccio si apre e ne esce la trivella che gira scavando; scava 3x3.
  - **Lama energetica** (braccio sinistro): la lama scorre fuori dall'avambraccio e si illumina; colpo corpo a corpo forte.
  - **Corazza** (busto): piastre extra si agganciano a spalle e petto; riduce i danni.
- **HUD futuristico**: cornici agli angoli, barra energia, integrità, velocità/quota, bussola; boot animato all'ingresso.
  Ogni modulo montato aggiunge il suo pannello (JET: spinta/quota, DRILL: giri/durezza del blocco, BLADE: carica,
  ARMOR: piastre).
- **Mech Hangar** (multiblocco con console, pavimento di ricarica e due bracci a gru): il meka parcheggiato si
  **ricarica** (~2.000 FE/t) e si **ripara**; dalla GUI della console si montano/smontano i moduli (slot per schiena,
  braccio destro, braccio sinistro, busto) e i bracci della gru si muovono per agganciarli.
- **Fase 1 fatta** (da testare in gioco): `MechEntity` (Mob, 1,6x3,6, 200 di integrità, armatura 10, batteria nei dati
  sincronizzati, movimento simulato sul client del pilota come un cavallo), sequenza OPENING → BOARDING → CLOSING →
  PILOTED → OPENING_OUT → UNBOARDING → CLOSING_OUT con il pilota che scivola dentro/fuori dal davanti; Shift bloccato
  (`EntityMountEvent`), uscita col tasto V (`MechExitPayload`); salto 0,95, pugno +10 danni (50 FE), scavo da piccone
  di ferro con i drop giusti, portata +2,5; danni al pilota girati al meka; 100 FE per blocco percorso + 1 FE/t.
  Modello `MechModel` 128x128 (bianco/blu, spalline, pinne a V, schiena scura). Il tronco è un **vano** in cui il
  pilota sta **in piedi** contro un'imbracatura, con gli occhi al visore della testa. Il davanti del tronco sono
  **quattro petali** (due sopra, due sotto) incernierati sui bordi esterni che si aprono uno dopo l'altro come un
  fiore, e la piastra del visore si alza. Imbarco: il meka si accovaccia (il vano scende a un gradino da terra), il
  pilota cammina fin davanti partendo da dove si trovava, si gira e indietreggia nel vano salendo il gradino; i petali
  si chiudono e il meka si raddrizza. Uscita: si accovaccia, si apre (sbuffo di vapore dalle giunture), il pilota si stacca dall'imbracatura, scende il
  gradino con un passo e si allontana; è visibile nel vano da quando i petali iniziano ad aprirsi, e in prima persona
  testa e tronco si nascondono solo finché gli occhi sono dentro. Testa 8x8x8 con visore 8x5. La telecamera in terza persona va dai 4 ai 9 blocchi
  (e ritorno) in modo graduale invece di scattare; il messaggio vanilla "Shift per scendere" è sostituito; se il
  client riceve il pilota a bordo un tick prima dello stato, resta dov'era invece di finire per un attimo nel vano.
  Il cavaliere viene alzato dei 0,6 blocchi che il gioco toglie a chi sta su un veicolo (prima i piedi sprofondavano e
  la vista finiva nella vita del meka). Posizioni, direzione e camminata del pilota in `MechEntity` (`pilotOffset`,
  `pilotFacing`, `isPilotWalking`), pose in `MechModel` (`petals`, `crouch`). Nella stazione (fase 2) potrà restare
  in piedi con una pedana all'altezza del vano. In prima persona non si disegnano tronco, testa e petali: si vede
  dal visore, con braccia e gambe in vista, e il colpo del pilota diventa un pugno del braccio destro. Mentre è pilotato il meka non
  è bersagliabile (il mirino non lo colpisce),
  texture da `texgen.py`, telecamera in terza persona a 9 blocchi (attributo `camera_distance`), in prima persona il
  meka non si disegna. HUD `MechClient`: boot con scanline, angoli, bussola, reticolo, ENERGY/INTEGRITY, SPD/ALT/
  MODULES, avviso di batteria scarica; nasconde cuori, fame, armatura, aria ed esperienza. Mech Frame (ricetta
  provvisoria) che lo dispiega e lo ripiega tenendo la carica (100.000 FE di fabbrica). Pagina "Mech" nella guida.
- **Fase 2 fatta** (da testare in gioco): **Mech Hangar** a **rastrelliera** (stile stazione per armature, futuristica,
  bianco/blu), 5x5 alto 6, aperto davanti e ai lati: console (`MechHangarBlock`, controller) in testa al binario
  sinistro rivolta fuori, due binari a terra di Hangar Frame, due montanti in fondo ai binari, trave in cima e due
  bracci in avanti sopra le spalle del meka, Hangar Pad 3x3 al centro con lo spazio sopra libero. Formato: i frame si
  nascondono e `HangarRenderer` disegna il rack (binari con gradini e linea ciano, montanti con fasce blu e controventi,
  trave, bracci, pistoni idraulici, due morse che scendono sulle spalle del meka parcheggiato, stato sincronizzato).
  GUI ridisegnata: scheda del meka a sinistra (barre etichettate Batteria/Integrità/Hangar, stato, Fold) e sagoma
  con gli slot ai punti di montaggio e linee di richiamo a destra (`Blueprint.MECH_HANGAR`, anche nel
  proiettore). Un meka parcheggiato vuoto sul pad viene caricato (2.000 FE/t dalla
  batteria della console, 1.000.000 FE da cavi), riparato (1 punto ogni 10 tick per 200 FE) e, se spento, si riaccende
  a integrità piena. GUI con schema del meka e **4 slot moduli** (schiena/Jet, braccio destro/Trivella, braccio
  sinistro/Lama, busto/Corazza) che rispecchiano il meka parcheggiato: le modifiche vanno subito al meka (bitmask
  sincronizzata `DATA_MODULES`), senza meka gli slot sono vuoti e non accettano nulla. Il Mech Frame si dispiega **solo sul pad di un hangar completo** (al centro, rivolto
  verso l'uscita) con un'animazione di apertura (UNFOLDING); il pulsante **Fold** della console lo fa richiudere
  (FOLDING: si accovaccia, incrocia le braccia, la testa rientra, le gambe si raccolgono, poi si compatta verso terra)
  e cade come Mech Frame che conserva carica e moduli (componente `mech_modules`). Shift+click destro non lo ripiega
  più. La console si chiama "Mech Hangar Console" e la sua GUI si apre anche a mano vuota
  da qualsiasi frame o dal pad dell'hangar completo. La pagina dell'hangar nella guida ha la scena 3D della struttura col meka sul pad. I 4 moduli esistono come oggetti
  (ricette provvisorie) e l'HUD li elenca; effetti, parti visibili e animazioni nella fase 3. Guida: Mech diventa una
  sezione con Mech Hangar e Mech Modules.
- **Fase 3 fatta** (da testare in gioco): le parti dei moduli sono nel `MechModel` e escono con animazioni quando il
  pilota è dentro (stato PILOTED; uscita graduale lato client, 16 tick): **Jet** due ali a tre segmenti che si aprono
  dal modulo dorsale + due ugelli, volo (salto in aria = spinta fino a 0,5, Shift = picchiata, niente = planata a
  -0,18; velocità in aria 0,035; 60 FE/t in volo); **Trivella** sotto la mano destra, tasto R (`MechTogglePayload`), gira
  quando accesa, scavo a 12 e 3x3 sul piano dello sguardo (20 FE per blocco extra), drop di qualsiasi tier; **Lama**
  d'energia dall'avambraccio sinistro, tasto G, +18 danni e fendente a 8 sui vicini entro 2,5 (80 FE); **Corazza**
  piastre su spalle, petto e cosce che si chiudono sul corpo, -40% danni. Gli strumenti si spengono quando il pilota
  esce. HUD: una riga per modulo con stato e tasto. Base dell'hangar coerente: pad bianco/blu con anello, binari alti
  quanto il pad con i gradini sopra. GUI dell'hangar: percentuali accanto alle barre, niente sovrapposizioni.
  Lasciato sul pad, il meka si raddrizza da solo sullo stand (scivola al centro e ruota verso l'uscita, 15% della
  distanza e 6° per tick); le morse si chiudono solo quando è in posizione.
- **Fase 4 fatta** (da testare in gioco): **Mech Core** da end game. Catena: il Waste Reprocessor recupera un
  **Isotope Shard** una barra su quattro (quarto slot d'uscita); il **Crystallizer** (`CrystallizerBlockEntity`) fa crescere
  un **Fusion Crystal** da 1 Isotope Shard + 4 Amethyst Shard + 2.000 mB di Acido, 1.000 FE/t per 30 s (600.000 FE).
  Il Crystallizer è il controller di una **camera multiblocco 3x3x4** (Blueprint `CRYSTALLIZER`, anche nel proiettore):
  pavimento e tetto di Cyber Frame, angoli di Cyber Frame, pareti di Containment Glass, i due blocchi centrali vuoti,
  il controller al centro del bordo frontale del pavimento. Completa, i blocchi si nascondono e `CrystallizerRenderer`
  la disegna sfaccettata, diversa dalla camera del metano: basamento ottagonale a gradini con la vasca d'acido
  incassata, quattro piloni bianchi che reggono una corona ottagonale col mozzo-emettitore, un campo d'energia viola
  pulsante al posto del vetro, console col progresso; il cristallo (poliedro della Resonance Core, allungato, viola)
  cresce al centro nutrito da due raggi, con archi elettrici dai piloni e scintille che vi spiraleggiano; finito resta
  sospeso finché non lo si prende. Il controller è `noOcclusion` (nascosto non deve bucare il terreno).
  **Crystallizer Port**: sostituisce qualsiasi Cyber Frame della camera (pavimento, angoli, tetto; il proiettore ne
  suggerisce una dietro il pavimento) e, a camera formata, passa acido, energia e oggetti al controller
  (`CrystallizerBlockEntity.ofChamberBlock`); a camera formata si nasconde come gli altri blocchi e il renderer disegna
  la porta come un nodo dei tubi: al centro del suo blocco un giunto scuro grande come quello dei tubi, con una finestra
  viola su ogni lato, appoggiato alla macchina (nel basamento, contro il pilone, sotto la corona; negli angoli del tetto
  un braccetto lo unisce al pilone); per ogni lato con un tubo collegato un braccio con la texture di quel tubo va dal
  giunto alla faccia, allineato al braccio del tubo (bianco per tramogge e tubi di altre mod). Le porte del pavimento
  mostrano anche una presa viola sul basamento. Per l'automazione, sia dal
  controller che dalle porte, entrano solo Isotope Shard e ametista ed esce solo il Fusion Crystal; l'acido non si estrae. Mech Core =
  Fusion Crystal + 2 Elite Frame + diamanti + celle; il Mech Frame lo richiede. Ricette finali: hangar e moduli con
  un Advanced Frame, Crystallizer con Acid Synthesizer e Containment Glass. **Braccio a carroponte** nell'hangar: un ponte
  che scorre sui bracci, un carrello e una pinza telescopica portano il modulo al suo attacco (schiena, avambracci,
  petto) in 60 tick; il modulo si monta a metà corsa con un lampo (`fitModule`/`fitStart` sincronizzati nell'update
  tag). **Suoni** del meka: servo, jet, trivella, lama e boot sintetizzati in `soundgen.py`, il resto da audio vanilla;
  passi a tempo con l'andatura (lato client, ogni mezzo ciclo del passo) e tonfo all'atterraggio; jet e trivella in
  loop (`MechLoopSound`); il Muffler non silenzia il meka (è un veicolo).
  **Bilanciamento** (verificato a fine fase 4): l'Isotope Shard esce una barra esaurita su quattro (prima otto), cioè
  in media ~40 min per uno con un reattore Medium (4 barre da 40 min), ~80 col Small: traguardo da end game senza
  attese assurde. Il Crystallizer (1.000 FE/t, 600k FE a cristallo) chiede uno Steam Turbine Medium o più Steam Engine;
  il meka consuma ~22 FE/t camminando (500k = ~19 min continui, ~7 min di volo col jet a 60 FE/t), l'hangar lo ricarica
  in ~4 min a 2.000 FE/t e lo ripara del tutto con ~40k FE. Il resto (generatori, macchine, consumi) resta come nella 0.3.0.
- **Fasi**: 1) entità + modello base + entrata/uscita + comandi + energia + HUD base; 2) Hangar (ricarica, riparazione,
  slot moduli); 3) i quattro moduli con modelli, animazioni, abilità e pannelli HUD; 4) ricette, guida, bilanciamento.

### Suoni
- Eventi propri (`ModSounds`, `sounds.json` generato da `assetgen.py`) che riusano audio vanilla con pitch/volume propri:
  fischio delle eliche del drone in volo (sintetizzato; più forte e acuto con la velocità), decollo, atterraggio; bolle dell'Extractor mentre produce,
  accensione/spegnimento della camera; scoppiettio del generatore; Assembler.
- **Muffler**: blocco (lana + ferro + note block) con GUI: ogni Muffler sceglie cosa zittire con 5 interruttori
  (Machines = suoni CyberFarm, Drones = `drone.*`, Monsters = HOSTILE, Animals = NEUTRAL, Other machines = lista in config).
  Scelte salvate nella block entity e sincronizzate al client; filtro lato client con `PlaySoundEvent`. I suoni del giocatore restano.
  Config client (`cyberfarm-client.toml`, sezione `muffler`): `range` (1–64, default 10), `extraSounds` (lista della riga
  Other machines: mod id o id del suono; di default i forni vanilla).

### Libro guida
**Drone Engineer's Handbook**. Sezioni: Getting Started, Muffler e Water Pump alla radice · **Generators** (Combustion, Gas Power, Bio-Digester,
Methane Extractor, Sulfur and Acid, Gas Turbine, Steam Engine) ·
**Nuclear** (Uranium and Fuel Rods, Fission Reactor, Coolant Mixer, Steam Turbine, Waste and Radiation, Waste
Reprocessor, Hazmat Suit and Geiger Counter) · **Energy Storage** (pagina indice con le tre parti, poi Resonance Core, Resonance Cell, Stabilizer Pylon) · **Cables** (Energy Conduit, Gas Pipe, Liquid Pipe, Liquid and Gas Tanks) · **Drones** (Assembler, Frames,
Modules, Addons, Dock, Using Drones) · **Mech** (Mech Core, Mech Hangar, Mech Modules).
La guida si aggiorna sempre insieme alle novità.
Libreria: **GuideME** 26.1.15-beta, dipendenza **opzionale** (la mod funziona anche senza). Guida data-driven:
`assets/cyberfarm/guideme_guides/handbook.json` + pagine in `assets/cyberfarm/guides/cyberfarm/handbook/*.md`.
Il libro viene regalato al primo accesso ed è craftabile (libro + lingotto di rame).

## Roadmap

1. **MVP**: Drone Assembler, telai T1–T3, Harvester Module, Drone Dock, Survey Tool, Combustion Generator, libro guida. ✅ testato in gioco
2. ✅ Moduli Quarry, Transport, Planter, Logger e Taxi, ✅ addon e smontaggio, ✅ Energy Conduit, ✅ JEI + REI.
3. ✅ Gas: Bio-Digester, Methane Extractor + Mob Capsule, Gas Turbine, Gas Pipe ✅ testato in gioco.
4. ✅ Endgame: reattore a fissione (torre), scorie e radiazioni, raffreddamento, meltdown.
   ✅ Reattore e Steam Turbine multiblocco in 3 taglie + Multiblock Projector, testati in gioco.
5. Rifinitura: bilanciamento, config, plugin JEI completo, texture e guida.
   ✅ Bilanciamento energia (confrontato con Mekanism, Immersive Engineering, Oritech): porte del reattore 2.500 mB/t di vapore,
   Gas Turbine 20 mB/t (biogas 60, metano 160 FE/t), Water Pump 500 mB/t a 40 FE/t.
   ✅ Ribilanciamento generatori a gas (ott. 2026): Combustion 40 FE/t; Bio-Digester ~6 mB/t di biogas (~19 FE/t in turbina);
   camera del metano zombie 25 mB/t (~200 FE/t in turbina, ~10 Bio-Digester, giustificato da struttura + acido + mob);
   Steam Engine 15 mB/t per cilindro, biogas 6 / metano 10 FE/mB, max 600 FE/t (= ~15 Combustion, 2,4 camere) sotto il
   reattore più piccolo al massimo (800 FE/t). Guida: tabelle di confronto in Generators e Gas Power.
   ✅ Cavi in 3 tier (base / Advanced oro / Elite viola), i tier si uniscono nella stessa rete e la portata è quella del cavo
   toccato dalla sorgente: Energy Conduit 2.000 / 16.000 / 128.000 FE/t, Gas Pipe e Liquid Pipe 1.000 / 4.000 / 16.000 mB/t.
   Gas Pipe solo gas (`FluidType.isLighterThanAir`: biogas, metano, vapore), Liquid Pipe solo liquidi (acqua, lava,
   coolant); i due tipi non si uniscono, così la linea dell'acqua e quella del vapore corrono affiancate.
   ✅ Uscita dai blocchi per ogni cavo (Energy Conduit, Gas Pipe, Liquid Pipe): click destro sul braccio verso un blocco →
   il cavo tira da lì ogni tick fino alla portata del tier (collare arancione); la rete non rimanda mai niente in un
   blocco da cui tira. Serve per batterie, serbatoi e magazzini passivi.
   ✅ Liquid Tank e Gas Tank: 32.000 mB di un solo fluido (liquidi o gas, come i tubi), passivi; secchi su Liquid Tank;
   click destro mostra il contenuto. Rompendoli si svuotano.
   ✅ Restyle: cavi "tubo pulito" (involucro chiaro, fascia colorata per tipo: azzurro energia, ambra gas, blu liquidi;
   anello nel colore del tier dove due cavi si toccano). I tratti dritti sono un tubo continuo: il blockstate mostra il
   giunto centrale solo a incroci, curve ed estremità, altrimenti un segmento `_middle` unisce le due braccia. Serbatoi: l'oblò di ogni lato mostra il fluido vero al suo livello
   (`FluidTankRenderer`, texture e tinta dal modello del fluido, gas semitrasparenti; contenuto sincronizzato al client
   al massimo ogni 5 tick). Multiblock Projector: icona a tablet con il profilo della torre su griglia.
   Niente tubi per item: gli oggetti li sposta il drone col Transport Module (l'Item Pipe è stato tolto).
   ✅ Filtro della Drone Dock (pulsante Filter nella sua GUI): 9 campioni fantasma, whitelist/blacklist, NBT esatto o
   qualsiasi, per item o per mod, tag scelto tra quelli del primo campione. Il Quarry tiene solo i drop che passano (gli
   altri vengono buttati), il Transport sposta solo gli oggetti che passano. Un filtro per dock, condiviso dai due moduli.
   ✅ Il drone col Transport Module mostra l'oggetto che trasporta appeso alla pinza (primo stack del carico, sincronizzato ai client).
   ✅ Pagine JEI e REI per le macchine (`compat/MachineExample`, una categoria generica per viewer): Bio-Digester (organici
   raggruppati per mB), Methane Extractor (uova spawn per gruppo: zombie, mostri, animali), Gas Turbine, Water Pump, Coolant
   Mixer, Fission Reactor (per taglia + refrigeranti), Steam Turbine (per taglia), Waste Reprocessor, Crystallizer; il blocco è la workstation.
6. Energy Storage: Resonance Core costruibile in 4 tier (guscio di Resonance Cell) con colonne di Stabilizer Pylon da cui entra ed esce l'energia (da testare in gioco).
7. ✅ Acido per il Methane Extractor (zolfo nel Nether, Acid Synthesizer, Containment Glass) e Steam Engine multiblocco (da testare in gioco).
8. Meka pilotabile con Mech Hangar e moduli (Jet, Braccio trivella, Lama energetica, Corazza): fasi 1-3 rilasciate in 0.3.0; fase 4 (Mech Core da end game con Crystallizer, ricette finali, braccio a carroponte, suoni) fatta, da testare.

## Asset

- Texture: `python tools/texgen.py` (disegnate in codice con palette condivisa; `--preview DIR` crea un foglio di anteprima).
- JSON (blockstate, modelli, item, loot, tag, ricette): `python tools/assetgen.py`. Le traduzioni (`lang/en_us.json`) si scrivono a mano.
- Suoni sintetizzati (`sounds/*.ogg`: volo del drone, servo, jet, trivella, lama e boot del meka): `uv run --with numpy --with soundfile python tools/soundgen.py`.
- Modello 3D del drone in Java (`DroneModel`), GUI disegnate in codice (`MachineScreen`).
