# Telecomanda și verificarea modificărilor — 2026-11-16

Am păstrat versiunea 1.1.0 și structura noului CHANGELOG. Am verificat modificările
Astrolabe (traduceri), restricționarea coliziunilor la combinațiile acceptate și
concordanța DATA_COUNT=19 între controllerul Collider și meniul acestuia.
Compilarea include și sursele client. Layouturile manualului, texturile și randarea
Naiad nu sunt validate vizual prin aceste teste.

## Telecomanda

Rezolvarea controllerului folosește dimensiunea stocată în item și configurația
curentă. Nu încarcă chunkuri destinație. Dimensiunile inexistente, coordonatele
incomplete și blocurile cu alt tip de meniu sunt respinse.

Meniul remote păstrează identitatea itemului care l-a deschis. Itemul trebuie să
rămână în inventarul jucătorului (inclusiv offhand); mutarea lui între sloturi este
permisă. Aruncarea, înlocuirea sau relegarea lui invalidează sesiunea.
Meniurile fizice păstrează raza de 8 blocuri. Clickurile, mutările rapide și
butoanele reverifică accesul înainte de modificări pe server.

## Teste efectuate

- Telecomandă: 22 verificări native Fabric, toate trecute:
  `build/reports/runtime-tests/20261116-150041-991613/summary.json`.
- Regresii fluide: 21; Quantum chest: 23, toate trecute:
  `build/reports/runtime-tests/20261116-145958-762216/summary.json`.
- Prima rulare remote a avut 19 verificări trecute; rularea finală include
  suplimentar deschiderea prin item și relegarea în timpul sesiunii.
- Verificările folosesc un ServerPlayer de test care capturează MenuProvider;
  nu validează pachetele de deschidere sau interfața unui client conectat.
- Build Gradle offline: trecut, inclusiv storage/fluid/crafting.
  JAR 1.1.0 verificat, fără clasele fixture; log:
  `build/reports/remote-fix-2026-11-16/build.log`.

Test manual recomandat: controller alimentat într-un chunk încărcat, deschidere
la 200 de blocuri, depășirea razei, aruncarea telecomenzii; apoi acces din Nether cu
opțiunea cross-dimension activată/dezactivată. Lumea destinație nu este încărcată
automat. Acest patch nu introduce autorizare de proprietar pentru rețeaua de
stocare; problema separată de acces din audit rămâne deschisă.
