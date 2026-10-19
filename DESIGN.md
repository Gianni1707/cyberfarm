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
- **Drone Dock**: base del drone (ricarica 500 FE/t, 9 slot di raccolto, area di lavoro).
- **Survey Tool**: collega un dock, marca due angoli dell'area (max 33x33, dislivello max 8, entro 48 blocchi) o due contenitori (percorso Transport). Shift+clic sulla dock la azzera.
- **Telai**: Basic (T1: 20k FE, 80% = 2,4 blocchi/s, 1 modulo, 4 addon) → Advanced (T2: 60k FE, 130%, 2 moduli, 8 addon) → Elite (T3: 150k FE, 160%, 3 moduli, 16 addon).
- **Componenti di crafting**: Drone Core, Rotor, Battery Cell. **Cyber Frame**: blocco strutturale dei multiblocchi; si illumina (texture "formed") quando la struttura è completa.
- **Moduli funzione**: Harvester ✅ · Quarry ✅ (strati fino al bedrock, solo con area marcata) · Transport ✅ (cassa → cassa, percorso col Survey Tool) · Planter ✅ (semi dalla Dock su terra arata) · Logger ✅ (abbatte alberi interi e ripianta) · Taxi ✅ (giocatore appeso all'uncino, destinazione con Shift+clic del Survey Tool, solo andata, rotta in quota) · poi Fluid Siphon, Shearer/Milker.
- **Logica**: ogni modulo è un `DroneTask` che trova il prossimo `DroneJob`; il drone alterna i moduli a ogni giro e concatena i lavori dello stesso tipo.
- **Addon** (4 slot nell'Assembler, ognuno una pila dello stesso addon; totale limitato dal telaio):
  Speed (+10% velocità ciascuno) · Stack (+1 stack per viaggio del Transport; base 16 oggetti, max 9 stack) · Battery (+25% batteria del telaio) ·
  Fortune (max 3) · Silk Touch (max 1, incompatibile con Fortune). Fortune/Silk valgono per Quarry e Harvester.
- **Consumo in volo** = (telaio 2/3/4 FE/t + moduli: Harvester +1, Quarry +2, Transport +1, Planter +1, Logger +2, Taxi +2) × (1 + % addon: Speed 10%, Stack 5%, Battery 5%), arrotondato per eccesso.
- **Consumo per lavoro**: raccolto 40 FE, blocco scavato 20 FE + 10 × durezza, seme piantato 10 FE, tronco 20 FE / foglia 5 FE, viaggio Taxi 50 FE; Fortune +25% per livello, Silk Touch +50%. Tutti i costi sono nei tooltip.
- **Smontaggio**: drone nello slot di uscita dell'Assembler (altri slot vuoti) → pulsante Disassemble; i pezzi tornano negli slot, la carica va nel buffer dell'Assembler.
- **Volo**: rettilineo con accelerazione e frenata, pathfinding solo come riserva quando il drone è bloccato.
- **Quarry**: scava a serpentina riga per riga, invertendo il verso a ogni strato; torna a scaricare a 192 oggetti.
- **Idee future**: Range Booster, Solar Skin, filtri per Quarry/Transport.

### Energia
- **Energy Conduit**: si collega da solo a cavi e blocchi FE di qualsiasi mod; non accumula, distribuisce subito
  a turno tra le macchine della rete (1.000 FE/t per sorgente, reti fino a 1.024 cavi).
- **Tier 1** ✅: Combustion Generator (brucia i combustibili della fornace).
- **Tier 2** ✅: Bio-Digester (compostabili e carne marcia → Biogas, senza corrente), Methane Extractor (multiblocco "doccia" 3x3x4:
  pavimento e tetto di Cyber Frame, pareti con angoli in Cyber Frame e vetro al centro, estrattore al centro del tetto;
  a struttura completa suono + estrattore e frame illuminati, GUI apribile da ogni blocco della camera (Shift per piazzare);
  mob caricato con la Mob Capsule, che cattura qualsiasi mob tranne boss e droni; zombie 10 mB/t, mostri 5, animali 2), Gas Turbine (biogas 3 FE/mB, metano 8 FE/mB,
  10 mB/t), Gas Pipe. Gas = fluidi NeoForge standard con tag `c:gaseous`/`c:biogas`/`c:methane`.
  I gas servono solo ai generatori: i droni vanno sempre e solo a corrente. Mekanism: compatibile solo a livello di tubi
  (i suoi tubi per fluidi trasportano biogas e metano), i nostri gas non sostituiscono i suoi.
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
  `meltdownExplosionPower`, `meltdownDestroysBlocks`) e forte contaminazione della zona.

**Steam Turbine** — blocco singolo come la Gas Turbine: vapore → FE. Accetta anche il vapore di altre mod (`c:steam`).

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
   Steam Turbine, più una **Water Pump** (2+ sorgenti d'acqua accanto, 250 mB/t, 20 FE/t). Numeri: 50 mB/t d'acqua → 50 mB/t di
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
   addosso, nei contenitori vicino ai giocatori o a terra (2,5 per barra ogni 5 s), reattore con scorie piene (perde),
   meltdown (400 su 5x5 chunk). Radiation Sickness da 20 / 100 / 300 Rad (danno che ignora l'armatura, fame, debolezza,
   nausea) per giocatori e mob; da 50 Rad il terreno appassisce (config `radiationWorldEffects`). Waste Reprocessor:
   barra esaurita → Vitrified Waste (+ 1/3 lingotto di uranio), 40 FE/t, 10 s.
4. ✅ (da testare in gioco) Hazmat Suit (4 pezzi, 25% ciascuno, protegge anche dal gelo del refrigerante) e Geiger Counter
   (ticchettio e livello in Rad sopra la hotbar, mai silenziato dal Muffler).
5. ✅ (da testare in gioco) Aspetto: a torre formata i casing spariscono e il controller disegna una vera torre di
   raffreddamento (iperboloide liscio, pannelli bianchi futuristici con fasce azzurre luminose). Anche porte e controller
   spariscono: la torre li disegna come portelli luminosi e pannello a filo della superficie, dove entrano tubi e cavi.

### Suoni
- Eventi propri (`ModSounds`, `sounds.json` generato da `assetgen.py`) che riusano audio vanilla con pitch/volume propri:
  ronzio del drone in volo (più forte e acuto con la velocità), decollo, atterraggio; bolle dell'Extractor mentre produce,
  accensione/spegnimento della camera; scoppiettio del generatore; Assembler.
- **Muffler**: blocco (lana + ferro + note block) con GUI: ogni Muffler sceglie cosa zittire con 5 interruttori
  (Machines = suoni CyberFarm, Drones = `drone.*`, Monsters = HOSTILE, Animals = NEUTRAL, Other machines = lista in config).
  Scelte salvate nella block entity e sincronizzate al client; filtro lato client con `PlaySoundEvent`. I suoni del giocatore restano.
  Config client (`cyberfarm-client.toml`, sezione `muffler`): `range` (1–64, default 10), `extraSounds` (lista della riga
  Other machines: mod id o id del suono; di default i forni vanilla).

### Libro guida
**Drone Engineer's Handbook**. Sezioni: Getting Started, Muffler e Water Pump alla radice · **Generators** (Combustion, Gas Power, Bio-Digester,
Methane Extractor, Gas Turbine) ·
**Nuclear** (Uranium and Fuel Rods, Fission Reactor, Coolant Mixer, Steam Turbine, Waste and Radiation, Waste
Reprocessor, Hazmat Suit and Geiger Counter) · **Cables** (Energy Conduit, Gas Pipe) · **Drones** (Assembler, Frames,
Modules, Addons, Dock, Using Drones).
La guida si aggiorna sempre insieme alle novità.
Libreria: **GuideME** 26.1.15-beta, dipendenza **opzionale** (la mod funziona anche senza). Guida data-driven:
`assets/cyberfarm/guideme_guides/handbook.json` + pagine in `assets/cyberfarm/guides/cyberfarm/handbook/*.md`.
Il libro viene regalato al primo accesso ed è craftabile (libro + lingotto di rame).

## Roadmap

1. **MVP**: Drone Assembler, telai T1–T3, Harvester Module, Drone Dock, Survey Tool, Combustion Generator, libro guida. ✅ testato in gioco
2. ✅ Moduli Quarry, Transport, Planter, Logger e Taxi, ✅ addon e smontaggio, ✅ Energy Conduit, ✅ JEI + REI.
3. ✅ Gas: Bio-Digester, Methane Extractor + Mob Capsule, Gas Turbine, Gas Pipe ✅ testato in gioco.
4. Endgame: reattore a fissione (torre), scorie e radiazioni, raffreddamento, meltdown.
5. Rifinitura: bilanciamento, config, plugin JEI completo, texture e guida.

## Asset

- Texture: `python tools/texgen.py` (disegnate in codice con palette condivisa; `--preview DIR` crea un foglio di anteprima).
- JSON (blockstate, modelli, item, loot, tag, ricette): `python tools/assetgen.py`. Le traduzioni (`lang/en_us.json`) si scrivono a mano.
- Modello 3D del drone in Java (`DroneModel`), GUI disegnate in codice (`MachineScreen`).
