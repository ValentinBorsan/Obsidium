# Testarea Obsidium

Necesită Java 21, Python 3 (macOS/Linux) și dependențele Gradle descărcate. Runnerul este offline; la prima configurare rulează `./gradlew build` cu acces la rețea. Serverele reutilizează acceptarea EULA din `run/eula.txt`; runnerul nu creează o acceptare în numele utilizatorului.

## Regresii rapide

```sh
./gradlew build --offline --no-daemon
```

Include testele JVM pentru inventare, fluide și crafting. O compilare reușită nu validează interfețele grafice sau multiplayerul.

## Integrare pe server Fabric

```sh
python3 scripts/runtime-tests.py
python3 scripts/runtime-tests.py pastoral
python3 scripts/runtime-tests.py pump
python3 scripts/runtime-tests.py quantum compactor
python3 scripts/runtime-tests.py newcontent
python3 scripts/runtime-tests.py tech --techreborn-jar build/compatibility/techreborn/TechReborn-5.11.19.jar
```

Fiecare suită rulează într-o lume temporară nouă, pe localhost și port ales automat. Nu folosește instanțele Prism sau lumile existente. Fixture-ul este împachetat ca mod temporar numai din clasele de test; nu intră în JAR-ul distribuit. Tickurile sunt controlate pentru verificări repetabile. Testul cufărului așteaptă activarea chunkurilor înaintea verificării entităților căzute.

- `pump`: pompare directă/prin conducte, butoi local/Quantum, simulare, capacitate fracționară, inserare parțială în mB întregi, 20 de repetări, buget comun de 50 mB/tick, vizibilitatea debitării la un peer Quantum și salvare/reîncărcare nativă a datelor Quantum.
- `pastoral`: protecția animalelor numite/îmblânzite și a puilor, backpressure, capturarea loot-ului, rezervă persistentă cu restart, export fără RF, hrană și recipiente, inventare cu commit/rollback, zona 9×4×9 și Aegis/redstone. Tagurile `obsidium:pastoral_harvestable` și `obsidium:pastoral_feed` permit extinderea controlată prin datapack.
- `quantum`: inventar comun, automatizare, sortare, Shift-click parțial și merge, persistență pe disc, protejarea conținutului local și spargere/demontare.
- `fluidpersistence`: salvare completă a variantelor de fluid, migrare, reload nativ pe disc, componente, rollback și refuzul conversiilor fără suport de metadate.
- `followup`: identitatea rucsacurilor Basic/Advanced, main hand/offhand, salvare după înlocuire și energie; încărcarea achievements-urilor, criterii de crafting și cerințe AND/OR.
- `progression`: rețetele early/mid/late, lipsa blocajelor din modelul documentat de surse, limitele regenerării și mutațiilor rare. Rulează și `scripts/audit-progression.py`; vezi `docs/PROGRESIE_EARLY_MID_LATE.md`.
- `newcontent`: corecțiile auditului din 16 noiembrie: tranzacții Quantum, waterlogged, migrarea bufferelor, meniuri, proprietari, izolare, bugete, capacitatea Poseidon, valvă fizică și rețeta Poseidon.
- `tech` (opțional, nu rulează implicit): instanțe reale Tech Reborn — rezervoare, baterii și cuptor electric; transferuri de energie, fluide și iteme, cu commit/rollback. JAR-ul oficial Tech Reborn 5.11.19 include Reborn Core; nu instala o a doua copie.
- `compactor`: rollback când rezultatul încape parțial, componente, repetări, rețete 2×2/3×3, Quantum și excepții.

Raportul este în `build/reports/runtime-tests/<data-ora>/summary.json`, cu logurile Gradle/server, configurația serverului și datele Quantum salvate pentru fiecare suită. Raportul se poate șterge la `gradlew clean`; copiază-l înainte dacă trebuie arhivat. Lumile temporare sunt eliminate la final.

Un timeout, un cod de ieșire nenul, un marker de eșec sau lipsa markerului final de succes produce cod de ieșire 1 al runnerului. Astfel sunt detectate și aserțiunile fixture-ului pentru care Gradle returnează succes. `--timeout 240` controlează limita în secunde pentru fiecare server. Testele de integrare se rulează separat de `build`; CI-ul existent rulează momentan doar buildul și regresiile JVM.

Salvarea/reîncărcarea din fixture citește fișierul NBT nativ într-o instanță nouă a stării Quantum. Nu reprezintă o repornire completă a procesului serverului.

## Verificarea în Prism înainte de distribuire

Duplică instanța într-una dedicată QA și creează o lume nouă. Notează buildul, pașii, cantitățile inițiale/finale și rezultatul fiecărui scenariu.

- Verifică meniurile, tooltipurile, filtrele, modelele și lichidele randate pentru blocuri locale și Quantum.
- Repetă transferurile cu inventarul plin, rezultate aproape pline și obiecte cu nume/componente diferite. Totalurile trebuie păstrate.
- Sparge și demontează blocurile, descarcă/reîncarcă chunkurile, închide și redeschide lumea; verifică din nou obiectele și lichidele.
- Pe un server separat, doi clienți accesează simultan același canal Quantum. Verifică transferurile concurente și accesul public/privat cu proprietari diferiți.
- Lasă instalația de crafting/compactare/pompare să ruleze 30 de minute. Compară totalurile și inspectează logurile și timpii tickurilor.

Aceste verificări vizuale, de acces multiplayer și de durată sunt manuale; suita automată actuală nu certifică încă aceste comportamente și nici toate problemele deschise din audit.

## Laborator Tech Reborn în Prism

Instanța separată `Obsidium + Tech Reborn — QA` folosește Minecraft 1.21 și Tech Reborn 5.11.19, cu Reborn Core inclus în JAR. JAR-ul extern este opțional și nu intră în dependențele de producție sau în JAR-ul Obsidium.

Runnerul poate exporta lumea unui singur test reușit într-un director nou:

```sh
python3 scripts/runtime-tests.py tech --export-world /cale/noua/Compatibility-Lab
```

Laboratorul are platformă la Y=99 și spawn lângă X=40, Z=40. Instalațiile: butoi → rezervor Tech Reborn la (32,100,32); Naiad → rezervor la (48,100,32); rezervor → Naiad → butoi la (64,100,32); Astrape → baterie la (32,100,64); baterie → Astrape → cuptor la (64,100,64); Hermes → cuptor electric la (48,100,80).

Tickurile sunt înghețate pentru aserțiuni. În lumea exportată, folosește `/tick unfreeze` dacă sunt încă înghețate. Creează o copie înainte de experimente destructive. Exportul este o lume nouă; nu importă salvările instanței principale.

Limitări: modulele fizice pierdute de versiuni vechi care nu serializau proprietatea nu pot fi reconstituite automat fără a acorda și module configurate gratuit. Noile salvări păstrează flagurile fizice. Stocarea veche din conducte este evacuabilă și se resalvează; o cantitate Poseidon veche peste 256.000 mB este păstrată și refuză intrări suplimentare până scade sub noua limită.

## Telecomanda de stocare

`python3 scripts/runtime-tests.py remote` pornește un server Fabric izolat.
Verifică raza configurată și limita exactă, separarea meniului local de cel remote,
controllerul din dimensiunea corectă, schimbările de configurație, înlocuirea sau
relegarea telecomenzii, legături invalide și absența încărcării chunkului destinație.
Deschiderea prin metoda itemului este testată cu un jucător de test care capturează
meniul, fără conexiune de client. În joc trebuie verificată separat afișarea GUI.

## Mythic Rails — faza 1

`python3 scripts/runtime-tests.py rails` compilează fixture-ul addonului și
încarcă JAR-ul de dezvoltare pe un server izolat împreună cu Obsidium.
Suita este explicită, nu intră în selecția implicită de teste pentru core.
Verifică 24 de cazuri pentru cartul de marfă; detalii și limitări în
[README addon](addons/mythic_rails/README.md).

## Mythic Rails — faza 2

`python3 scripts/runtime-tests.py railstations` execută 34 de verificări ale
stațiilor. Runnerul oprește și repornește serverul în aceeași lume temporară.
Succesul necesită markerul final din a doua pornire; un eșec înainte de restart
sau lipsa lui împiedică raportarea succesului. Logul primei porniri este păstrat
și ca `server-before-restart.log`. Inventarul blocat și starea de plecare sunt
verificate după reîncărcarea entităților de pe disc.

## Mythic Rails — șine, locomotivă și GUI stații

`python3 scripts/runtime-tests.py railsindustry rails railstations` verifică
șinele noi cu carturi vanilla (inclusiv frânare, accelerație și curbe), motorul,
combustibilul, mutarea din meniu, distanța pentru comenzi, rotația protejată și
eliberarea manuală. Suita `railstations` repornește aceeași lume și verifică
și locomotiva cu rezervă de ardere plus o stație pusă în pauză.
Interfețele folosesc meniuri native; testele server nu validează randarea clientului.

## Pornire de producție cu runtime-ul Prism

`python3 scripts/production-server-smoke.py --addon-jar addons/mythic_rails/build/libs/obsidium-mythic-rails-0.4.1.jar`

Folosește Java 21 din Prism, Fabric Loader 0.16.5 și bibliotecile sale exacte
(verificate cu SHA-256 din manifest), Minecraft 1.21, Fabric API și JAR-urile
remapate Obsidium/addon. Copiază acceptarea EULA existentă în proiect; nu creează
una nouă. Serverul ascultă numai pe localhost, port aleatoriu, într-o lume temporară,
apoi primește comanda `stop` după pornire. Salvează log și summary JSON în
`build/reports/production-smoke/`. Nu accesează conturi sau lumi ale utilizatorului.

`--expect-redirect-crash` este controlul negativ pentru originalul 0.4.0;
succesul său înseamnă reproducerea exactă a erorii, nu că acel JAR funcționează.
`--probe-mod` acceptă un fixture remapat separat, care trebuie să emită markerul
final și să oprească serverul. Remaparea unui fixture trebuie să includă
output-ul main al addonului în classpath, pentru metodele Minecraft moștenite.
Profilul server nu verifică randarea clientului sau toate modurile din instanță.

## Mythic Rails 0.5.0 — inventare, frână și perechi cuplate

`python3 scripts/runtime-tests.py railstrains railsindustry rails railstations`

Suita `railstrains` include cuplare prin callback-ul Chain, cost/refund o singură
dată, urmărire pe traseu și curbe, OFF pentru ambele vehicule, oprire comună la stație,
reluare, inventarul real de 9 sloturi, automatizare și protecția GUI-ului înlocuit.
Runnerul repornește aceeași lume pentru UUID-urile reciproce, traseu, combustibil,
marfă cu componente și bufferul stației. `railstations` are propriul restart.
Modelele curbelor se calibrează cu `java -Djava.awt.headless=true scripts/ExportRailCurve.java`;
verificarea compară îmbinările ambelor margini cu șina dreaptă.
