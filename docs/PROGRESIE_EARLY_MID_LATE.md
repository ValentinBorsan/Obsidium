# Progresie early / mid / late game — 16 noiembrie 2026

Implementare pentru început puțin mai greu, dificultate medie în partea centrală și nivel ridicat în late game. Nu sunt eliminate obiecte din registry și nu sunt șterse blocuri/inventare din lumi existente.

## Early game: explorare și atelier termal

- Volcanite Furnace cere acum un **Blast Furnace vanilla**, păstrând celelalte materiale. Prima mașină presupune fier și prelucrare vanilla, fără boss obligatoriu.
- Grinderul cere **Volcanite Furnace**, astfel încât atelierul are o ordine explicită. Costul de trei diamante al grinderului rămâne.
- Aliajul vulcanic de bază și generarea naturală nu au fost mutate după Wither. Sunt păstrate rutele de pornire pentru o lume nouă.
- Arborii comuni găsiți prin explorare rămân recompense naturale. Rețetele de fabricație repetabilă au fost mutate în mid game; schimbarea nu șterge arborii găsiți deja în lume.

## Mid game: prelucrare, resurse regenerabile și infrastructură

- Rețetele de pornire pentru arborii minerali comuni și semințele de resurse comune cer **Volcanite Flux Mechanism**. Acesta folosește gold dust, deci introduce prelucrarea mecanică în lanț. Rețetele care multiplică semințe din petale rămân utile după obținerea primei culturi.
- Transportul **Advanced** Hermes, Naiad și Astrape cere Flux Mechanism. Rețetele păstrează loturile de opt conducte, pentru a nu transforma infrastructura într-un grind individual pe fiecare segment.
- **Quantum Link** cere Advanced Casing, Energy Cell și Flux Mechanism, alături de materiale Ender. Nu mai este o rețetă cu un singur lingou Volcanite în centru. Este accesibil după infrastructură Nether/mid game, fără a cere terminarea colliderului.
- Flux Mechanism nu cere la rândul lui semințe, arbori sau conducte Advanced: dependența nu este circulară.

## Late game: resurse rare și tieruri puternice

- **Diamond Seed** cere catalizatori astrali, diamante și mecanism de atelier.
- **Netherite Seed** cere netherite ingots, Supreme Alloy, catalizatori astrali și Diamond Seed. Scraps singure nu mai deschid această cultură.
- Prima **Tartarean Netherite Sapling** cere componente Supreme, catalizatori și netherite ingots și produce **un singur puiet**.
- Altoirea din Volcanite + Iron/Lead nu mai produce direct Netherite. Propagarea Netherite cere un părinte Tartarean deja obținut. Diamond rămâne accesibil prin Netherite + Emerald.
- Mutațiile rare provocate de albină/perie cer un părinte rar. Arboristul automat cere frunziș Tartarean + Hephaestus pentru ramura Netherite; simpla alăturare a arborilor comuni nu mai sare peste late game.
- Pentru a nu bloca lanțul comun de altoire, **Oak + Hephaestus → Hades Coal** este acum o rută de pornire explicită și testată.
- Tierurile **Supreme** Hermes/Naiad cer Supreme Alloy, la fel ca ramura de energie. Supreme Alloy păstrează cerința Nether Star; Singularity păstrează produsul colliderului. Rețetele Poseidon/collider și bonusul de forjare existent nu au fost ieftinite.

## Protecții pentru modpackuri

Schimbările controlează în primul rând accesul și producția resurselor rare, fără a modifica global rețetele altor moduri sau a redenumi metalele lor.

Noua opțiune server din `config/obsidium.json`:

```json
"rareResourceGrowthMultiplier": 4
```

- Valoarea efectivă este limitată la 1–64. Absența câmpului într-o configurație veche folosește valoarea implicită 4.
- Bonsai rare — Netherite, Diamond, Pyretic Titan — cer implicit 4.800 tickuri de creștere de bază, față de 1.200 pentru arborii comuni. Ash păstrează bonusul său; acestea sunt cantități de progres/tick în implementare, nu garanții de timp când chunkul nu este procesat.
- Fructificarea frunzelor rare și creșterea naturală a culturilor Diamond/Netherite sunt încetinite cu același factor.
- Bone meal și ash avansează culturile rare cu o etapă. Mierea nu mai maturizează instant un bonsai rar; adaugă 600 de tickuri de progres. Arborii/culturile comune își păstrează comportamentul de creștere anterior.

Alte moduri pot avea fertilizatori sau mecanisme care forțează creșterea; nicio valoare locală nu poate certifica balansul tuturor modpackurilor. Autorii pot modifica parametrul și rețetele prin datapack. Compatibilitatea API cu Tech Reborn se verifică separat de economia completă a unui pack.

## Obținerea obiectelor

`python3 scripts/audit-progression.py` produce `build/reports/progression/obtainability.json`.

Modelul acoperă **405 înregistrări literale de iteme**, comparate cu registry-ul nativ într-un test pe server. **398** au o cale declarată prin rețete, lume, loot, procese sau botanică. Cele șapte excepții sunt cele trei dispozitive Creative, două spawn eggs și două blocuri tehnice — Grafted Sapling și Stygian Cenotaph. Acestea nu sunt tratate ca recompense Survival de fabricat.

Auditul verifică dependențele prin punct fix, nu consideră automat un item disponibil doar pentru că are un loot de tip self-drop. Rutele necraftabile sunt declarate în script, cu sursa și precondițiile lor. Materialele vanilla Survival sunt presupuse accesibile; cantitățile, probabilitățile și timpul de obținere nu sunt simulate. Testul nativ verifică încărcarea rețetelor și ingredientelor; un playthrough complet rămâne necesar pentru a valida experiența și toate interacțiunile.

## Ghid și verificare

- Ordinea advancements pentru primele etape a fost corectată: materialele termale preced atelierul mecanic.
- Sunt adăugate repere pentru atelierul automatizat, Quantum, industria late și regenerarea rară.
- Ghidul botanic și cerințele de mutație ale arboristului reflectă noii părinți. Advancements sunt orientare; ingredientele sunt cele care implementează accesul, nu doar părinții din arbore.

Comenzi:

```sh
./gradlew build --offline --no-daemon
python3 scripts/audit-progression.py
python3 scripts/runtime-tests.py progression
python3 scripts/runtime-tests.py
python3 scripts/runtime-tests.py tech
```

Suita `progression` verifică nativ limitele early/mid/late, ingredientele rețetelor, acoperirea registry-ului, altoirea fără ocolirea costurilor, mutațiile automate și creșterea rară/configurația. Este inclusă în runnerul standard, separat de testele JVM ale buildului.

Validare finală standard: **123 verificări pe server trecute**, dintre care **41 de progresie**. Dovezi: `build/reports/runtime-tests/20261116-072912-595609/summary.json`.

Compatibilitate Tech Reborn după schimbări: **23 de verificări trecute** (`build/reports/runtime-tests/20261116-073152-756435/summary.json`). Buildul final și regresiile JVM au trecut.
