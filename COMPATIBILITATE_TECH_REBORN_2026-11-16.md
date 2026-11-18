# Obsidium + Tech Reborn — 16 noiembrie 2026

Instanța Prism **Obsidium + Tech Reborn — QA** este instalată separat, pentru Minecraft 1.21/Fabric, cu [Tech Reborn 5.11.19](https://modrinth.com/mod/techreborn/version/CNwZZ4FD). JAR-ul oficial include Reborn Core 5.11.19 și Team Reborn Energy 4.1.0; nu s-a instalat o a doua copie a bibliotecii. Descărcarea a fost verificată cu SHA-512 din metadatele Modrinth.

Instanța conține și Fabric API, REI, Architectury, Cloth Config, Jade și Mod Menu. Lumile și modurile instanței principale nu au fost copiate peste această instanță. Obsidium a fost actualizat în ambele instanțe, cu backup pentru fiecare JAR precedent.

## Rezultate automate

**23 de verificări trecute cu modurile reale încărcate într-un server Fabric:**

- Butoi Obsidium → rezervor Tech Reborn, cu conservarea cantității.
- Naiad → rezervor Tech Reborn, inclusiv abort, commit și limita comună pe tick.
- Rezervor Tech Reborn → Naiad → butoi Obsidium.
- Astrape → baterie Tech Reborn, cu rollback și cantitate identică între acceptare și stocare.
- Baterie Tech Reborn → Astrape → cuptor Obsidium.
- Hermes → cuptor electric Tech Reborn, cu refuz pe latura dezactivată și transfer după configurarea acesteia ca INPUT.

Blocurile externe sunt inițializate prin tickerul lor normal înainte de testele sided. Configurarea INPUT din fixture folosește API-ul SlotConfiguration al Reborn Core; nu ocolește regulile de inserare ale mașinii. Nu s-a adăugat o dependență obligatorie Tech Reborn în Obsidium.

Au trecut și **82 de verificări standard pe server**, plus buildul și regresiile JVM. Cele 19 verificări pentru conținutul nou acoperă remedierea cauzelor din audit, inclusiv rețeta Poseidon.

Rapoarte:

- `build/reports/runtime-tests/20261116-065558-736623/summary.json` — compatibilitate Tech Reborn.
- `build/reports/runtime-tests/20261116-065705-573988/summary.json` — cele patru suite standard.
- `build/reports/audit-2026-11-16/final-build.log` și `fixed-build.json` — build și hashul JAR-ului instalat.

## Testarea ta în Prism

1. Selectează **Obsidium + Tech Reborn — QA** și pornește jocul.
2. Deschide lumea nouă **Compatibility-Lab**. Spawnul este lângă **X=40, Y=101, Z=40**, pe o platformă de test.
3. Instalațiile de fluide sunt în jurul **X=32/48/64, Y=100, Z=32**; cele de energie în jurul **X=32/64, Y=100, Z=64**; itemele la **X=48, Y=100, Z=80**.
4. Repetă testele cu rezervoare/baterii aproape pline, laturi dezactivate, redstone și două surse. Compară cantitățile înainte și după salvare/reîncărcare.

Pentru cuptorul Tech Reborn, latura conectată trebuie configurată ca **INPUT**. Laboratorul configurează WEST pentru slotul de intrare. O latură dezactivată care refuză itemele este comportament corect. Dacă lumea exportată păstrează tickurile înghețate, folosește `/tick unfreeze` cu comenzile activate.

## Remedieri și limite

Au fost corectate cele 10 constatări din [audit](AUDIT_OBSIDIUM_2026-11-16.md): tranzacții Quantum, blocuri waterlogged, migrarea bufferelor, validarea/proprietarul meniului, izolarea rețelelor, bugetele pe tick, capacitatea Poseidon, persistența modulelor de valvă și rețeta Poseidon.

Noile salvări păstrează modulele fizice. Itemele pierdute în versiuni care nu salvau flagul fizic nu pot fi reconstituite automat fără a putea acorda iteme gratuite. Bufferele vechi de energie/fluid se păstrează pentru evacuare. Rezervorul Poseidon deja supraîncărcat păstrează conținutul și refuză intrări suplimentare până scade sub limita corectă.

Prism afișează instanța QA, iar fișierele și lumea exportată au fost verificate. **Clientul Minecraft nu a fost lansat în această sesiune.** Rezultatul acoperă API-urile și scenariile automate enumerate, nu toate mașinile Tech Reborn, randarea, performanța de durată sau multiplayerul cu două conexiuni. Problemele mai vechi din auditul din 15 noiembrie rămân în afara acestei etape.
