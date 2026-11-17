# Obsidium: conținut, progresie și plan de finalizare

Profil ales de utilizator: **dificultate medie, automatizare și explorare, fără grind excesiv**.

## Direcția recomandată

Obsidium are o identitate coerentă când combină **geologie vulcanică, botanică minerală și inginerie inspirată din mitologia greacă**. Nu are nevoie de mai multe mașini pentru a deveni complet. Prioritatea este o progresie lizibilă, roluri diferite pentru mașini și conservarea resurselor la salvare și automatizare.

Păstrăm IDs și salvările existente. Consolidarea se face prin rețete, utilitate, ghid și organizarea interfețelor; eliminarea directă a blocurilor din registry ar afecta lumile utilizatorilor.

Acest document separă observațiile din cod de propunerile de design. Rețetele și ratele de producție descrise mai jos **nu au fost reechilibrate automat** în această etapă.

## Inventarul analizat

- `ObsidiumMod.java` conține 405 apeluri literale `registerItem` și 210 `registerBlock`. Acestea sunt numărători ale declarațiilor, nu o captură a registry-ului runtime; itemele și blocurile nu trebuie adunate ca obiecte independente, deoarece există BlockItems.
- 434 rețete JSON: 305 shaped, 88 shapeless, 25 smelting, 15 blasting și una campfire cooking. Procesele implementate direct în block entities nu sunt incluse în această numărătoare.
- 15 advancements în `odyssey`.
- 28 evenimente sonore proprii, toate cu subtitrări declarate.
- Analiza folosește rețete și coduri de mașini/pacte, fără un playthrough Survival complet. Evaluările de ritm sunt concluzii de design, nu timpi măsurați în joc.

## Ce se potrivește

**Caldera, Volcanite și căldura** sunt punctul de pornire distinctiv. Explorarea descoperă materialul, iar căldura justifică primele procese. Magma Forge poate fi reperul modului, cu funcționare clară și recompense vizibile.

**Arborii minerali și arboristul Demetrian** dau o cale proprie de resurse regenerabile. Se potrivesc dacă primul exemplar cere explorare/investiție, iar producția industrială cere infrastructură. Varianta automată trebuie să fie mai bună decât așteptarea, nu doar o obligație pentru a elimina un timp artificial.

**Poseidon pentru fluide, Hermes pentru iteme și Astrape pentru energie** sunt roluri ușor de explicat. Merită păstrată această separare și în rutare, filtre și documentație. Tierurile trebuie să schimbe debitul și raza utilă, nu doar numele și costul.

**Distilarea, aburul și combustibilii** creează o punte bună între termal și industrial. Un produs intermediar este justificat dacă schimbă o decizie: randament, temperatură, stocare sau alimentarea unei instalații.

**Pactele divine și echipamentul** pot funcționa ca ramură opțională de explorare/combat. Bonusurile ar trebui să susțină stiluri de joc, fără a transforma fiecare unealtă într-un salt obligatoriu peste echipamentul anterior.

**Quantum și colliderul** au loc ca obiective avansate. Quantum este confort logistic după stabilirea unei fabrici; colliderul poate fi o provocare opțională de final. Este nevoie de o explicație în ghid pentru legătura dintre ingineria vulcanică și fizica avansată.

## Ce nu se potrivește încă suficient

### Acces Quantum prea simplu în raport cu puterea oferită

`recipe/quantum_link_module.json` cere Ender Eyes, Ender Pearls, Crying Obsidian și un Volcanite Ingot. Nu cere infrastructură industrială sau un proces avansat. Ender Eyes implică progres Nether, deci nu este literalmente acces de început; rămâne însă accesibil înaintea multor mașini pe care le poate face mai puțin relevante.

**Propunere:** deblocare după prima distilare sau după un component avansat procesat. Un cost de fabricație clar, apoi utilizare comodă; evităm introducerea unui cost RF per item doar pentru a produce grind. Culoarea, canalul și proprietarul trebuie explicate înainte de utilizare.

### Bariere repetate cu Nether Stars

`supreme_volcanic_alloy.json` consumă o Nether Star pentru un aliaj. Catalizatorul astral consumă o Nether Star pentru două bucăți. Numai controllerul Poseidon cere patru aliaje Supreme și doi catalizatori: **cinci Nether Stars prin aceste rețete**, înaintea casingurilor și a altor subcomponente.

Pentru profilul mediu, această barieră pare disproporționată pentru o pompă, mai ales când există deja o pompă simplă și automatizare de fluide. Nu este o concluzie bazată pe un timp de farm măsurat.

**Propunere:** Wither ca reper unic pentru acces la un catalizator reutilizabil sau o familie avansată de procese. Înlocuim plata repetată a bossului cu infrastructură și materiale procesate. Rezervăm repetarea bossului pentru opțiuni de lux, nu pentru fiecare piesă structurală.

### Regenerarea resurselor rare poate scurta prea mult explorarea

`hephaestus_volcanite_sapling.json` folosește stick, ash și un ingot. `tartarean_netherite_sapling.json` produce doi puieți din patru scraps, patru Volcanite Ingots și un puiet Hephaestus. Patru fructe Tartarean și patru gold ingots produc un netherite ingot.

Frunzele au o șansă `1/800` la un apel random tick eligibil pentru fructificare. Aceasta nu înseamnă un fruct la 800 de tickuri de joc; viteza reală depinde de numărul de frunze, random ticks, încărcarea chunkului și automatizare.

**Propunere:** păstrăm recompensa regenerabilă după descoperirea resursei, dar mutăm multiplicarea arborilor rari într-un proces controlat de hibridizare. Măsurăm separat randamentul unei ferme mici și al uneia automatizate înainte de a modifica șansele. Preferăm costuri de energie/fertilizare explicabile în locul așteptării lungi.

### Progresul ghidat nu reflectă complet accesul real

Arborele actual pune `mechanical_pulverization` înainte de `magmatic_smelting`. Grinderul cere trei diamante, în timp ce aliajul vulcanic se poate fabrica fără el. Advancementul Magmatic Smelting urmărește obținerea de materiale, nu efectuarea unui proces de forjare. Părinții din advancement organizează afișarea; nu blochează automat rețetele.

**Propunere:** repere bazate pe acțiuni reale: prima încălzire, primul lot procesat, primul transfer, primul circuit fluid, prima producție regenerabilă controlată. Avansarea trebuie să explice următorul obiectiv, nu să certifice doar că un item a fost ținut în inventar.

### Puține motive clare pentru unele alternative

Există multe familii de generatoare, stocări și upgrade-uri. Numărul lor nu dovedește redundanța, dar crește efortul de învățare. Pentru fiecare trebuie documentate avantajul, dezavantajul și momentul folosirii. O baterie mai mare sau o conductă cu debit superior justifică un tier; un al doilea obiect cu același comportament și aceeași etapă nu este suficient diferențiat.

**Propunere:** organizarea taburilor Creative pe Materiale/Botanică, Mașini/Energie, Logistică/Stocare, Echipament/Pacte și Construcții. Stabilim reguli consecvente pentru Basic/Advanced/Supreme și numele mitologice. Schimbăm textele afișate fără a redenumi IDs persistente.

## Progresia propusă pentru dificultate medie

### Etapa 1 — explorare și termal

Caldera, cenușă, Volcanite, cuptor și prelucrare termală simplă. Obiectiv: prima resursă proprie și primul proces care îmbunătățește viața jucătorului. Nu condiționăm învățarea modului de Wither sau de un stoc mare de diamante.

### Etapa 2 — atelier și automatizare locală

Grinder, prima sursă RF, Hermes/Naiad/Astrape Basic, stocare simplă, primele culturi minerale. Obiectiv: o linie mică funcțională, cu intrare și ieșire observabile. Jucătorul învață fețele, filtrele și diagnosticele.

### Etapa 3 — fabrică regenerabilă

Arborist, distilare, abur/biocombustibili, baterii și conducte Advanced, controller și crafting automat. Obiectiv: resurse regenerabile obținute prin investiție în automatizare. Ramurile botanică și industrială se ajută reciproc, fără să impună toate mașinile într-un singur lanț.

### Etapa 4 — infrastructură avansată

Multiblocuri, upgrade-uri specializate, Poseidon și Quantum. Se cer componente prelucrate și o capacitate de alimentare rezonabilă. Primele legături Quantum trebuie să simplifice fabrica deja construită, nu să elimine nevoia de a învăța transportul local.

### Etapa 5 — obiective opționale

Collider, tieruri Supreme/Singularity și pacte optimizate. Recompensele sunt putere, confort și exprimare; funcțiile de bază nu trebuie să depindă de terminarea acestei etape.

Acestea sunt ținte de design. Nu sunt timpi promiși de terminare și nu sunt gate-uri deja aplicate în cod.

## Dificultate: ce trebuie măsurat și corectat

Grinderul folosește 250 RF/t și 100 unități de progres. În regim electric simplu, fără upgrade și fără boost termal, asta înseamnă 100 de tickuri de lucru și 25.000 RF pentru un proces continuu. În configurația implicită, solarul are 80 RF/t la amiază, generatorul de abur 120 RF/t, iar biomasa 100 RF/t. Un singur generator din acestea nu susține continuu acel grinder; bateria îl poate alimenta în rafale.

Boostul termal poate permite grinderului să funcționeze și fără RF și dublează avansul de bază. Acesta este un avantaj tematic bun, dar schimbă economia: trebuie să fie o alegere explicită, nu o regulă pe care jucătorul o descoperă accidental.

**Recomandare pentru mediu:** primul generator să poată susține o activitate de atelier utilă, chiar dacă nu toate mașinile simultan. Căldura pasivă poate susține un mod mai lent, iar energia o îmbunătățire controlabilă. Valorile finale se aleg după un test de producție, nu după comparația izolată a două constante.

Pactele trebuie evaluate împreună cu armele și modurile de combat. Există deja diferențe între descriere și configurație: Athena promite 35% în descriere, în timp ce valoarea implicită este 25%; Thanatos descrie două inimi, în timp ce heal-ul implicit este 3 puncte, adică 1,5 inimi. Prioritatea este afișarea valorii efective, nu creșterea automată a bonusurilor pentru a potrivi textul.

## Stabilitate înainte de lansare

1. Păstrăm regresiile pentru resurse, componente, rollback și aliasuri Quantum. Extindem la salvare/restart complet, chunk unload/reload și două conexiuni multiplayer.
2. Închidem constatările încă deschise din auditul din 15 noiembrie: identitatea rucsacurilor, telecomanda, componentele fluidelor și sincronizarea vizuală Quantum. Corecțiile din 16 noiembrie nu certifică rezolvarea acestora.
3. Transformăm erorile de încărcare a rețetelor Obsidium în eșecuri de validare. Runnerul serverului face deja acest lucru; merită păstrat ca poartă de release.
4. Externalizăm parametrii importanți de balans și validăm intervalele configurației. Evităm valori de producție importante ascunse în mai multe block entities.
5. Profilăm rețelele și multiblocurile cu chunkuri încărcate/descărcate și cu multe endpointuri. Măsurăm timpul de tick și costul scanărilor, fără a echivala un build reușit cu performanță bună.
6. Testăm upgrade-ul unei lumi vechi, fără eliminarea IDs și fără rescrierea tăcută a inventarelor. Documentăm separat orice migrare care nu poate reconstitui informația deja pierdută.

## Sunete

Cele 28 de evenimente au subtitrări; este o bază bună de accesibilitate. Sistemul existent avea mute prin tasta M și unele interfețe, dar filtrul de redare bloca în principal pornirea unor sunete noi.

**Implementat în această etapă:** mutarea oprește imediat evenimentele active `obsidium:machine.*`/`obsidium:machine/*`; filtrul continuă să blocheze redările noi. Starea este persistentă. Sunetele mașinilor sunt oprite în configurațiile locale folosite pentru testare. Tasta poate fi schimbată în Controls → Obsidium. Avertismentul câmpului `PipeConfigMenu.filterContainer` a fost eliminat fără a afecta slotul.

Pentru finalizare recomand volum reglabil în client, trei grupe — funcționare continuă, finalizare/avertizare, interacțiune — și limitarea suprapunerilor când multe mașini lucrează în același loc. Volumul individual din `machine_volume` nu este încă aplicat de mixin ca un multiplicator complet; nu trebuie prezentat drept slider funcțional. De asemenea, valorile mesajelor de pact și ale setărilor audio trebuie să reflecte configurația efectivă.

## Plan de finalizare cu rezultate verificabile

**Pasul A: stabilitate.** Închidem problemele de salvare și acces restante; rulăm suitele, upgrade-ul lumii și multiplayerul. Rezultat: nicio pierdere/duplicare în scenariile de acceptare.

**Pasul B: progresie.** Alegem rețetele revizuite pentru Quantum, Supreme, Poseidon și arborii rari. Actualizăm ghidul și advancements împreună cu ele. Rezultat: fiecare etapă are un obiectiv, o investiție și o recompensă distinctă.

**Pasul C: prezentare.** Organizăm taburile, corectăm tooltipurile, documentăm fețele de transfer și standardizăm nivelurile. Rezultat: jucătorul poate construi prima linie fără a citi codul.

**Pasul D: audio și client.** Verificăm în joc mute/unmute imediat, restartul, subtitrările, interfețele și aspectul blocurilor. Reglăm suprapunerile înainte de a adăuga sunete noi.

**Pasul E: release candidate.** Înghețăm conținutul, facem un playthrough Survival cu profil mediu și o sesiune cu Tech Reborn. Definim explicit erorile care blochează lansarea. Publicarea și distribuirea sunt pași separați de această analiză.

Modul nu este declarat finalizat doar pe baza acestei evaluări. Modificările de balans rămân propuneri, iar validarea audio în client și playthrough-ul sunt încă necesare.
