# Resurse sursă pentru aspectul Mythic Rails

`texture-atlas.png` și `freight-cart-icon.png` sunt imagini generate cu imagegen
pentru această actualizare. `texture-preview.png` arată resursele exportate.
Acest folder nu intră în JAR; resursele de runtime sunt sub `src/main/resources`.

Exportați din rădăcina proiectului:

```sh
java -Djava.awt.headless=true scripts/ExportRailsTextures.java
```

Exportul folosește nearest-neighbor, păstrează transparența iconului și produce
texturi de bloc 32×32, un icon 64×64 și textura UV a minecartului 64×32.
Panoul GUI se desenează în `FreightCartScreen`, pentru a păstra alinierea
sloturilor și a textelor la scara interfeței Minecraft.

Actualizarea 0.4.0 adaugă `rail-atlas.png` (normală, curbă, alimentată oprit/pornit)
și `locomotive-icon.png`, generate cu imagegen. Exportul include patru texturi
32×32 de șină și un icon 64×64 pentru locomotivă. Modelele șinelor folosesc
geometria vanilla, iar modelul locomotivei include un cazan și un coș propriu.

0.5.0 adaugă `rail-curve-fixed.png`. `ExportRailCurve.java` calibrează UV-urile
modelului de curbă la intervalele de pixeli ale celor două traverse aurii de pe
șina dreaptă și verifică ambele ieșiri. `curve-model-preview.png` vizualizează
modelul calibrat; nu reprezintă un screenshot din client.
