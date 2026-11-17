# Achievements și remedierea rucsacurilor — 16 noiembrie 2026

## Remedierea auditului anterior

Meniurile Basic și Advanced păstrează acum obiectul exact cu care au fost deschise. Înainte, salvarea sau închiderea putea alege rucsacul aflat ulterior în mână, ceea ce risca suprascrierea altui exemplar.

- Încărcarea, modificarea inventarului și închiderea salvează în aceeași referință de ItemStack.
- Schimbarea slotului selectat, înlocuirea obiectului sau înlocuirea cu o copie identică invalidează meniul.
- Transferurile unui meniu invalid sunt refuzate pe server.
- Energia și încărcarea pasivă ale meniului Advanced nu se aplică unui alt rucsac după invalidare.
- Componentele obiectelor și formatul salvării rămân compatibile cu itemele existente.

Nu există o migrare care să reconstituie inventare deja suprascrise de o versiune veche. Corecția previne repetarea defectului.

## Achievements noi

Sunt adăugate **11 achievements** în arborele existent Olympian Odyssey, pentru un total de **30 de advancements**. Se deschid din meniul vanilla Advancements, implicit tasta L. Nu este necesar un mod separat de achievements.

Early game:

- **A Proper Workshop:** fabrică Volcanite Furnace — 25 XP.
- **Take It With You:** fabrică Basic Backpack — 25 XP.
- **Close the Circuit:** fabrică generatorul și firele de energie Basic — 50 XP.
- **A Taste of the Caldera:** bea Volcanic Honey — 15 XP, obiectiv secundar de explorare.

Mid game:

- **Fluid Engineer:** fabrică Lava Pump și conducte Naiad — 50 XP.
- **Traffic Control:** fabrică oricare dintre cele trei familii de transport Advanced — 75 XP.
- **Quantum Engineer:** fabrică Quantum Link Module — 100 XP.

Late game:

- **A Garden Worth a Kingdom:** obține Diamond Seed, Netherite Seed și Tartarean Netherite Sapling — toate trei necesare; 150 XP.
- **Forged for an Odyssey:** colecționează toate cele patru piese Hephaestean — 150 XP.
- **Collider Architect:** fabrică controllerul colliderului și o synchrotron coil — 200 XP. Acest obiectiv nu pretinde că multiblocul este deja format sau funcțional.
- **At the Edge of Matter:** obține Singularity Hadron Alloy — 250 XP.

Obiectivele de crafting folosesc triggerul nativ `minecraft:recipe_crafted`; simpla primire a itemului nu le finalizează. Obiectivul pentru miere folosește `minecraft:consume_item`. Colecțiile folosesc inventarul și cer explicit toate obiectele indicate. Transportul Advanced este o alternativă OR: este suficientă oricare familie.

Recompensele sunt numai XP moderate, acordate prin mecanismul vanilla de finalizare, nu materiale rare sau generatoare Creative. IDs ale obiectivelor vechi nu sunt schimbate. Obiectivele noi de crafting urmăresc acțiunile efectuate după instalarea actualizării; istoricul vechi al craftingului nu poate fi dedus automat.

## Validare

`python3 scripts/runtime-tests.py followup` a trecut **74 de verificări pe server Fabric**:

- Basic și Advanced în main hand/offhand;
- înlocuire cu alt exemplar și cu o copie identică;
- callback întârziat/închidere fără suprascrierea rucsacului nou;
- păstrarea componentelor, refuzul transferurilor invalide și izolarea energiei;
- încărcarea nativă a celor 11 achievements și a părinților;
- existența rețetelor referite, potrivirea criteriilor cu rețeta corectă și refuzul unei alte rețete;
- OR pentru transport și AND pentru colecția de armură.

Dovezi: `build/reports/runtime-tests/20261116-074345-575458/summary.json` și `build/reports/audit-followup-2026-11-16/summary.json`. Sursa fixture-ului: `src/test/java/net/valentin/obsidium/AuditFollowupRuntimeChecks.java`. Suitele native rulează separat de buildul JVM și nu sunt incluse în JAR-ul distribuit.

Validarea criteriilor folosește parserul și matcherul Minecraft; nu reprezintă un playthrough, capturi ale toasturilor sau o sesiune multiplayer cu conexiuni reale. Afișarea grafică a ramurii trebuie verificată în client.

## Ce rămâne din auditul anterior

Componentele fluidelor la persistență, telecomanda, autorizarea endpointurilor Quantum mai vechi și sincronizarea vizuală Quantum rămân lucrări separate. Această etapă rezolvă identitatea rucsacurilor și adaugă obiectivele; nu marchează întregul audit anterior ca închis.
