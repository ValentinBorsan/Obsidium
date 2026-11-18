# Obsidium: Mythic Rails — 0.5.0

Addon pentru Minecraft 1.21 / Fabric, cu Obsidium 1.1.0+ și Fabric API.
Distribuit separat: `build/libs/obsidium-mythic-rails-0.5.0.jar` în acest subproiect.
Instalați aceeași versiune pe client și server. Runtime verificat: Java 21,
Fabric Loader 0.16.5 și MixinExtras 0.4.1, ca în instanțele Prism de test.

## Șine

Obsidian Rail este șina normală, inclusiv curbe și pante; Ember Powered Rail are
accelerația și frânarea vanilla. Șinele vanilla nu sunt înlocuite.
Curbele au un model cu UV calibrat după îmbinările șinei drepte.

- 8 Rails + 1 Volcanite Ingot → 8 Obsidian Rails.
- 4 Powered Rails + 1 Volcanite Ingot + 1 Redstone → 4 Ember Powered Rails.
- Alimentați separat segmentele care schimbă tipul de powered rail.

## Loading și Unloading: inventar propriu

Ambele stații au **9 sloturi salvate**, accesibile în GUI și prin hopper/Fabric
Transfer API. Se pot folosi fără cufăr extern.

1. Puneți stația ca suport sub o șină orizontală dreaptă.
2. Orientați sensul plecării de-a lungul șinei; săgeata de pe stație îl indică.
3. Click dreapta pe stație deschide GUI-ul: stare, marfă, semnal, controale,
   bufferul stației și inventarul jucătorului. Shift-click mută obiectele.
4. Loading transferă din buffer în vagon; Unloading transferă din vagon în buffer.
   Când bufferul de descărcare este plin, vagonul așteaptă fără să piardă marfa.

Un inventar lateral este opțional. Se conectează în stânga sensului de plecare,
la înălțimea blocului stației. Loading îl folosește ca sursă pentru buffer,
Unloading ca destinație. Sunt respectate regulile de acces pe față ale inventarului.
Puntea laterală funcționează și fără cart la stație. Transferul de marfă este de
până la 8 obiecte la fiecare 4 tickuri, cu tranzacții și componente păstrate.

Plecarea automată are o întârziere de aproximativ **2 secunde** după terminarea
lucrului: sursă goală/vagon plin la Loading, vagon gol la Unloading.
Redstone permite transferul, dar blochează plecarea automată.

- **Pause** oprește transferul și ține cartul. **Resume** reactivează stația.
- `<` / `>` rotesc stația. Cu un cart prezent, puneți întâi stația pe Pause.
- **Release cart** eliberează manual, inclusiv cu marfă rămasă și în pauză;
  ignoră blocarea redstone. Șina trebuie să fie dreaptă și aliniată.
- **Rail misaligned**: cartul este oprit; aliniați stația cu șina sau înlocuiți
  curba/panta de deasupra cu o șină dreaptă.

La spargere, conținutul bufferului este aruncat separat de bloc. Schimbarea
setărilor nu golește inventarul. Stațiile existente primesc buffer gol la upgrade.

## Locomotivă și frână

Hephaestean Locomotive are un slot de combustibil solid și GUI propriu.
Rețetă: `III / IFI / R `, I = Volcanite Ingot, F = Furnace Minecart,
R = Redstone Block.

- Click dreapta fără lanț pentru GUI; puneți Coal, Charcoal sau alt combustibil
  solid acceptat de furnal. Recipientele cu rest, precum Lava Bucket, sunt refuzate.
- **Motor on** pornește propulsia. **Motor off** aplică acum frâna: ține locomotiva
  pe loc pe șină, inclusiv pe powered rails. Rezerva de ardere nu se consumă în pauză.
- `<` / `>` aleg direcția când motorul este oprit și locomotiva nu este cuplată.
  Direcția se actualizează după deplasarea prin curbe, pentru reluare corectă.
- O locomotivă necuplată se oprește și ea la stație; folosiți Release cart în GUI-ul
  stației, apoi Motor on dacă motorul era oprit. Nu este un vagon de marfă.

## Cuplare: o locomotivă + un vagon Hermes

1. Opriți motorul și puneți ambele vehicule pe același segment drept de șină.
2. Alegeți direcția locomotivei în GUI. Vagonul trebuie să fie la **1,2–3,5 blocuri
   în spatele ei**, pe partea opusă direcției afișate.
3. Cu un **Chain** în mână, click dreapta pe locomotivă, apoi pe vagon.
   Se consumă un singur lanț în Survival; în Creative nu se consumă.
4. Porniți motorul. Legătura vizuală apare între vehicule. Vagonul urmărește
   traseul parcurs de locomotivă, inclusiv prin curbe.

Când vagonul ajunge la o stație, locomotiva așteaptă fără să ardă combustibil.
După transfer și eliberare, convoiul își continuă traseul. Motor off oprește
ambele vehicule. Cuplarea, traseul recent, marfa și combustibilul se salvează.

Pentru decuplare, opriți motorul și folosiți **Unlink** în GUI sau Shift + click
cu lanțul pe unul dintre vehicule. Lanțul plătit este returnat o singură dată.
Direcția inițială nu poate fi schimbată manual cât timp vehiculele sunt cuplate.
Dacă partenerul nu este încărcat, convoiul așteaptă; nu îl încarcă forțat.
Legătura este pentru o singură pereche, în aceeași dimensiune.

## Build și teste

```sh
./gradlew build --offline --no-daemon
python3 scripts/runtime-tests.py railstrains railsindustry rails railstations
python3 scripts/production-server-smoke.py --addon-jar addons/mythic_rails/build/libs/obsidium-mythic-rails-0.5.0.jar
java -Djava.awt.headless=true scripts/ExportRailsTextures.java
```

Exportul texturilor include calibrarea modelului curbei. Sursele imaginilor sunt
în `artwork/`, în afara JAR-ului. `curve-model-preview.png` este o previzualizare
tehnică a modelului, nu o captură din joc.

Suitele verifică bufferul și automatizarea, componentele, frâna, plata/decuplarea,
curbele și traseul încărcare → transport → descărcare, plus restartul real al lumii.
174 verificări funcționale au trecut; încă 45 au fost rerulate pe JAR-ul de
producție cu runtime-ul Prism. Rezultatele și limitele sunt în CHANGELOG.
Randarea pe un client conectat necesită în continuare verificare vizuală.
Dispenserul aruncă itemul cartului; nu îl plasează automat pe șină.
