# Persistența componentelor de fluid — 16 noiembrie 2026

Remediază punctul 5 din auditul din 15 noiembrie: fluidele cu componente Fabric erau acceptate, dar salvate doar ca tip și cantitate. După reload, de exemplu, apa cu un nume propriu devenea apă simplă.

## Corecție

Quantum SavedData și block entities bazate pe LiquidTank salvează acum varianta completă prin `FluidVariant.CODEC`, cu registry-ul Minecraft. Câmpul nou `FluidVariant` păstrează componentele persistente ale variantei. ID-ul vechi `Fluid` este menținut pentru compatibilitatea formatului, iar loaderul preferă întotdeauna varianta completă.

Salvările fără câmpul nou sunt încărcate prin ID-ul vechi. ID-urile invalide nu mai ajung ca valori null în lookup. Dacă un payload nou este invalid, loaderul nu îl transformă într-un fluid simplu prin fallback; un astfel de payload nu este o recuperare validă de fluid.

Inventarele nu amestecă variante cu componente diferite. Snapshoturile și transferul pe conducte păstrează varianta. Salvarea locală, payloadul local de update și împachetarea butoiului în item includ datele complete.

## Recipiente

Gălețile vanilla, sticlele de apă/flacoanele simple și actuala sticlă Volcanite nu reprezintă componente arbitrare de fluid. Conversia este refuzată când ar pierde aceste date, înaintea consumului de fluid sau recipient. Sunt protejate atât umplerea automată, cât și găleata/sticla folosită manual pe butoi. Adaptorul Fabric al sticlei Volcanite refuză variantele cu componente.

Apa/lava și celelalte fluide fără componente își păstrează traseele normale. Fluidele cu componente pot fi mutate între stocările compatibile și prin API-ul tranzacțional; această etapă nu adaugă un nou format de sticlă cu metadate.

## Verificări

Rularea finală a trecut **95 de verificări pe server Fabric**:

- `fluidpersistence`: 21 — componente și cantități după salvare nativă pe disc, rollback, salvări vechi, payloaduri invalide, NBT local, update local, itemul împachetat, transfer și refuzul amestecării variantelor;
- aceeași suită verifică refuzul conversiilor automate/manuale și al inserării în sticla Volcanite, plus umplerea obișnuită a unei găleți cu apă;
- `pump`: 28;
- `quantum`: 23;
- `tech`: 23, cu Tech Reborn real.

Dovezi: `build/reports/runtime-tests/20261116-082639-394114/summary.json`. Buildul și regresiile JVM au trecut. Fixture-ul permanent este `FluidPersistenceRuntimeChecks.java`, inclus în runnerul standard, nu în JAR-ul distribuit.

```sh
python3 scripts/runtime-tests.py fluidpersistence
```

## Limite și continuare

Componentele deja pierdute de o versiune veche nu pot fi reconstruite automat. Au fost testate variante reale cu componenta persistentă CUSTOM_NAME; nu sunt certificate toate modurile sau componentele tranzitorii neserializabile.

Reloadul Quantum citește fișierul NBT nativ într-o nouă instanță SavedData. Nu este un restart complet de proces. Interacțiunile manuale sunt apelate pe server cu un ServerPlayer de test; nu reprezintă o sesiune grafică sau multiplayer real.

Payloadul local al butoiului păstrează acum componentele; **sincronizarea vizuală a cantității comune Quantum rămâne distinctă**. Nu sunt declarate rezolvate telecomanda, autorizarea tuturor endpointurilor vechi sau toate adaptoarele istorice care folosesc callbackuri tranzacționale.
