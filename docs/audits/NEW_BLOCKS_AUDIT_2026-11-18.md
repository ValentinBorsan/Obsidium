# Audit blocuri noi — 18 noiembrie 2026

Au fost analizate Aegis Sanctuary Ward, Ambrosia Pastoral Feeder și Pan's Pastoral Harvester, inclusiv block entities, meniuri, GUI, mixin de spawn, înregistrări Fabric/energie/inventar, rețete, loot, modele și traduceri. Poseidon și celelalte modificări mai vechi din working tree nu sunt incluse în acest audit.

Nu am modificat implementarea de producție. Am adăugat fixture-ul de audit `src/test/java/net/valentin/obsidium/PastoralAuditRuntimeChecks.java` și acest raport. Lumea de test a fost temporară; nu au fost deschise salvările personale.

## Stadiu după remediere

Problemele de mai jos descriu implementarea inițială. Au fost remediate: protecția animalelor (tag de specii + excluderi explicite), colectare sincronă a drop-urilor standard cu rezervă persistentă, backpressure, export independent de RF, inserare atomică pe mai multe sloturi, hrană pe taguri cu returnarea recipientelor, aria 9×4×9, traduceri și catalogul de 408 iteme.

Fixture-ul istoric `PastoralAuditRuntimeChecks` a fost înlocuit cu `PastoralRuntimeChecks`, care verifică așteptările corecte și restartul aceleiași lumi. Rulare finală: `build/reports/runtime-tests/20261118-072023-543988/summary.json` — **36 verificări trecute**. Comandă: `python3 scripts/runtime-tests.py pastoral --timeout 180`.

Capturarea loot-ului acoperă calea standard `Entity.spawnAtLocation` a animalului selectat. Modurile care ocolesc complet această cale sau generează drop-uri asincron necesită probe de compatibilitate distincte. Build-ul complet offline și regresiile storage/fluid/crafting au trecut. JAR-ul remapat a trecut pornirea cu runtime-ul Prism (Loader 0.16.5): `build/reports/production-smoke/20261118-072202-761431/summary.json`. Catalogul are acoperire 408/408. Nu am validat vizual interfețele în client.

## Concluzie inițială

Baza de integrare este funcțională, dar recoltarea animalelor necesită două corecții prioritare înaintea utilizării lângă animale valoroase. Sunt două probleme P1, patru P2 și trei P3. P1 înseamnă risc ridicat de pierdere de animale/produse; P2 afectează funcționalitatea; P3 privește claritatea și întreținerea.

## P1-01 — Harvester-ul omoară cai îmblânziți, inclusiv animale numite

**Loc:** `src/main/java/net/valentin/obsidium/block/PanHarvesterBlockEntity.java:100–110`.

Filtrul protejează numai `TamableAnimal.isTame()`. Caii folosesc ierarhia `AbstractHorse`, astfel încât nu intră în această excludere. Nu există nici filtru pentru numele personalizat.

**Reproducere pe server:** trei cai adulți îmblânziți și numiți, minimum păstrat 2, 1.200 RF disponibili. După un ciclu, un cal este mort și rămân doi în viață. Logul conține și mesajul de deces al animalului numit.

**Impact:** afirmația din tooltip „Never harms ... tamed pets” nu este respectată; animalele valoroase pot fi pierdute ireversibil în joc.

**Remediere recomandată:** exclude explicit caii îmblânziți și animalele cu nume; definește lista de specii recoltate și politica pentru animale deținute. Adaugă probe pozitive pentru animale domestice obișnuite și negative pentru cai, animale numite, pui și animale îmblânzite.

## P1-02 — Recoltarea continuă când bufferul este plin

**Loc:** `PanHarvesterBlockEntity.java:85–110`.

Colectarea itemelor și omorârea următorului animal sunt operații independente. Când toate cele nouă sloturi sunt pline, inserarea loot-ului eșuează, dar selecția animalului și `hurt(...)` continuă. Loot-ul nou este colectat abia la un ciclu ulterior, dacă există loc.

**Reproducere pe server:** nouă stive complete de cobblestone, trei vaci adulte, minimum 2, 1.200 RF. Ciclul consumă energia și recoltează un animal în ciuda bufferului plin.

**Impact:** produsele rămân în lume, unde pot dispărea, cădea în lavă sau fi luate de alt sistem. La umplerea continuă a bufferului se acumulează și entități de iteme.

**Remediere recomandată:** aplică backpressure înainte de omorâre. Soluția robustă trebuie să asigure colectarea/păstrarea întregului loot, inclusiv cantități variabile și drop-uri introduse de alte moduri; simpla verificare a unui slot gol nu garantează spațiu suficient.

## P2-01 — Auto-exportul Pan depinde accidental de rezerva pentru sacrificare

**Loc:** `PanHarvesterBlockEntity.java:93–95`, exportul fiind la `116`.

Dacă energia este sub 1.200 RF, metoda revine înainte de `autoExportOutputs`. Astfel se oprește și evacuarea itemelor deja produse, deși energia este verificată drept cost al recoltării, nu al exportului.

**Reproducere:** opt bucăți de piele în buffer și un cufăr adiacent. Cu 0 RF nu se transferă nimic. La 1.200 RF transferul funcționează chiar fără alte animale de recoltat.

**Remediere:** separă colectarea/exportul de condiția pentru o nouă recoltare; dacă exportul trebuie să coste RF, definește și afișează explicit propriul cost.

## P2-02 — Bufferul refuză spațiul disponibil împărțit între sloturi

**Loc:** `AmbrosiaFeederBlockEntity.java:165–177`; aceeași logică există în `PanHarvesterBlockEntity.java:145–157`.

`insertOutput` cere ca întreaga stivă să încapă într-un singur slot. Nu distribuie cantitatea între stive compatibile sau între mai multe sloturi.

**Reproducere:** cele trei ieșiri Ambrosia conțin fiecare 63 White Wool. Inserarea a trei White Wool este respinsă, deși capacitatea liberă totală este exact trei.

**Impact:** tunderea sau colectarea par blocate chiar dacă inventarul are loc suficient; poate agrava acumularea drop-urilor lângă Pan.

**Remediere:** simulare de inserare completă urmată de commit, folosind toate sloturile permise. Un rezultat parțial nu trebuie urmat de ștergerea integrală a ItemEntity-ului din lume.

## P2-03 — Ambrosia respinge hrană validă pentru unele animale vanilla

**Loc:** `AmbrosiaFeederBlockEntity.java:201–213`, plus filtrul sloturilor din `AmbrosiaFeederMenu`.

Intrările sunt limitate la o listă manuală de iteme, dar consumul ulterior folosește `animal.isFood(stack)`. Lista nu acoperă toate animalele căutate de bloc.

**Reproducere:** o țestoasă acceptă Seagrass ca hrană, însă `AmbrosiaFeederBlockEntity.isBreedingFood(Seagrass)` returnează false. Hrana nu poate fi introdusă prin sloturile GUI sau prin fețele inventarului.

**Remediere:** aliniază inserarea cu speciile acceptate și cu tagurile de hrană; menționează explicit speciile suportate dacă blocul este destinat doar unei liste restrânse. Evită presupunerea că lista manuală acoperă automat modpack-urile.

## P2-04 — Zona de lucru este mai mare decât cea afișată

**Loc:** `AmbrosiaFeederBlockEntity.java:67,73`, `PanHarvesterBlockEntity.java:81`; tooltip Ambrosia la `AmbrosiaFeederBlock.java:78`.

`new AABB(pos)` are deja dimensiunea 1×1×1. După `inflate(4.5, 2.0, 4.5)`, zona este **10×5×10**, nu 9×4×9. Interogarea poate selecta și animale ale căror bounding box intersectează această zonă. Nu se verifică gardurile sau apartenența la un țarc.

**Confirmare:** dimensiunile AABB au fost verificate în fixture-ul Minecraft.

**Impact:** blocul poate hrăni sau recolta animale dintr-un țarc vecin mai departe decât sugerează tooltip-ul.

**Remediere:** alege o dimensiune unică, folosește-o în logică, GUI și tooltip și precizează că zona este geometrică, fără detectarea gardurilor. Adaugă hologramă sau previzualizare a ariei înaintea activării Pan.

## P3-01 — Descrierea Aegis sugerează o protecție mai largă decât hook-ul efectiv

**Loc:** `src/main/java/net/valentin/obsidium/mixin/NaturalSpawnerMixin.java:18–29` și tooltip-ul Ward.

Hook-ul interceptează doar `NaturalSpawner.isValidSpawnPostitionForType` pentru categoria `MONSTER`. Nu este un veto general pentru spawn din spawners, ouă, comenzi sau alte mecanisme independente de acest flux. Nu elimină monștrii existenți.

**Remediere:** precizează „natural hostile spawning”, tipul zonei și limitele. Doar dacă designul cere toate sursele de spawn, extinde implementarea și testează separat acele surse.

Aegis verifică limite pe axe (±48 X/Z, ±24 Y), deci o zonă dreptunghiulară, nu o rază circulară. Descrierea trebuie să fie consecventă cu această geometrie.

## P3-02 — GUI și tooltip-uri hardcodate în engleză

**Loc:** `src/client/java/net/valentin/obsidium/client/AmbrosiaFeederScreen.java:34–38`, `PanHarvesterScreen`, `AegisSanctuaryWardScreen` și `appendHoverText` ale blocurilor.

Numele din en_us/ro_ro există, dar majoritatea etichetelor, stărilor, butoanelor și tooltip-urilor sunt `literal(...)` sau șiruri directe. De exemplu, Ambrosia ignoră titlul tradus al meniului și desenează „Ambrosia Feeder”.

**Remediere:** chei de traducere pentru titlu, hrană, ieșiri, energie, minimum de adulți, stare, rază și tooltip-uri. În această etapă nu s-a făcut verificare vizuală în client.

## P3-03 — Catalogul și documentația nu includ noile blocuri

Niciunul dintre cele trei ID-uri noi nu apare în exporturile actuale ale catalogului. Afirmația de acoperire de 405 iteme este anterioară acestor adăugări.

**Remediere:** include noile blocuri în categoriile potrivite, regenerează catalogul și raportul de acoperire, apoi actualizează numărul de iteme afișat pe landing page/descriere dacă s-a schimbat. Regenerarea singură nu stabilește categoria corectă fără actualizarea metadatelor.

## Ce a fost verificat și funcționează

- `testClasses` a compilat, iar serverul Fabric dedicat de dezvoltare a pornit și s-a oprit normal.
- Înregistrările block/block entity/menu și screen sunt prezente pentru cele trei blocuri.
- API-urile Fabric pentru energie și inventar sunt înregistrate. Inventarele sided disting intrările de ieșiri.
- Rețetele, blockstate-urile, modelele itemelor, loot-table-urile și numele en_us/ro_ro există. Loot-table-urile verificate indică propriul bloc.
- Inventarul Ambrosia a păstrat stiva la un round-trip NBT.
- Pan exportă spre cufărul adiacent când trece de pragul energetic.
- Aegis consumă exact 800 RF la activare, protejează limita (48,24,48) și nu protejează poziția (49,0,0).
- Dezactivarea manuală și redstone opresc Aegis; pauza nu consumă energia verificată.

## Dovezi și limite

Rularea finală: `build/reports/runtime-tests/20261118-070800-591001/summary.json`.
Log server: `build/reports/runtime-tests/20261118-070800-591001/pastoralaudit/server.log`.
Fixture: `src/test/java/net/valentin/obsidium/PastoralAuditRuntimeChecks.java`.
Runner folosit: `build/reports/pastoral-audit-2026-11-18/runner.py`.

Comandă de reproducere în mediul local:

```sh
python3 build/reports/pastoral-audit-2026-11-18/runner.py pastoralaudit --timeout 180
```

**12 probe au reușit:** șase confirmă intenționat comportamente defectuoase existente, iar șase sunt controale sau verificări de structură/persistență. „PASS” în acest fixture înseamnă că observațiile auditului au fost reproduse, nu că implementarea este lipsită de probleme. După remedieri, aceste probe trebuie convertite în așteptări pentru comportamentul corect.

Prima rulare a produs aceleași patru reproduceri inițiale și două controale, dar runner-ul a raportat FAIL din cauza markerului final al fixture-ului (`ALL_OK` în loc de `ALL_PASSED`). Rularea finală corectează doar markerul și extinde verificările; nu schimbă implementarea blocurilor.

Nu s-au verificat interactiv GUI-urile în joc, fiecare sursă reală de spawn, toate modurile de hrană/specii, restartul aceleiași lumi, un server de producție remapat ori modpack-uri externe. Nu s-a făcut profilare de performanță. `dataAccess().get(1)` din Ambrosia recalculează toate animalele la sincronizarea GUI; merită cache/polling mai rar dacă profilarea cu multe blocuri și spectatori indică un cost relevant.

## Ordinea recomandată de remediere

1. Protecția animalelor și backpressure pentru recoltare (P1-01/P1-02).
2. Separarea exportului de costul recoltării și inserare completă pe mai multe sloturi (P2-01/P2-02).
3. Alinierea hranei acceptate și ariei de lucru (P2-03/P2-04).
4. Descrieri Aegis, traduceri și actualizare catalog (P3).
5. Probe suplimentare pentru încărcare/salvare/restart, bloc distrus cu GUI deschis, două ferme suprapuse, buffer plin, redstone și alimente/specii introduse de alte moduri.
