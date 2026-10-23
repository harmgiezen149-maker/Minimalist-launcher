# Wat je zelf nog moet doen

Alles uit de audit is gebouwd (zie de sectie "Opvolging" in [AUDIT.md](AUDIT.md)). Een paar stappen kan alleen jij doen: ze vragen jouw GitHub-account, jouw telefoon of jouw Vercel-account. Doe ze in deze volgorde. Stap 1 t/m 4 horen bij elkaar; reken op ongeveer 20 minuten.

Waarom dit nodig is: de oude ondertekeningssleutel stond openbaar op GitHub. Iemand anders kon daarmee een "update" van je launcher maken. Daarom krijgt de app een nieuwe, geheime sleutel. Android accepteert een app met een andere sleutel alleen als nieuwe installatie, dus je installeert één keer opnieuw.

---

## 1. Instellingen veiligstellen (telefoon, 3 min)

1. Open op je telefoon de vertrouwde oude link en installeer de update over de huidige app heen:
   <https://github.com/harmgiezen149-maker/PW2/releases/latest/download/minimalist-launcher-apk.zip>
   (zip openen → `minimalist-launcher.apk` installeren). Dit is de laatste versie met de oude sleutel. Hij heeft één nieuwe functie: back-up.
2. Open de launcher → **Settings** → helemaal onderaan **Backup** → **Export settings to a file**.
3. Bewaar het bestand `minimalist-launcher-settings.json`, bijvoorbeeld in Downloads of in Google Drive.

## 2. Een geheim wachtwoord voor de sleutel instellen (computer of browser, 5 min)

Dit kan niet in de GitHub-app; gebruik een browser (op de telefoon: "Desktopsite").

1. Maak een lang, willekeurig wachtwoord van **minstens 32 tekens** met de generator van je wachtwoordmanager (bijvoorbeeld Bitwarden, 1Password of Samsung Pass).
2. Sla het daar op onder een duidelijke naam, bijvoorbeeld "Minimalist Launcher – sleutelwachtwoord". **Raak het niet kwijt.** Zonder dit wachtwoord kun je nooit meer een update maken die over je geïnstalleerde app heen gaat; dan moet je opnieuw installeren.
3. Ga naar <https://github.com/harmgiezen149-maker/Minimalist-launcher/settings/secrets/actions/new>
4. Vul in:
   - **Name:** `SIGNING_PASSPHRASE`
   - **Secret:** het wachtwoord uit stap 1
5. Klik **Add secret**.

## 3. De sleutel laten maken (browser, 2 min + 5 min wachten)

1. Ga naar <https://github.com/harmgiezen149-maker/Minimalist-launcher/actions/workflows/create-signing-key.yml>
2. Klik **Run workflow**. Laat de branch op **main** staan en klik nogmaals op het groene **Run workflow**.
3. Na ongeveer een halve minuut staat er een groen vinkje. De workflow maakt de sleutel op de server van GitHub, versleutelt hem met jouw wachtwoord, zet hem (versleuteld) in de repo en start meteen een build.
4. Wacht tot ook de run **Android build** op main groen is (±5 minuten). Daarna staat de nieuwe APK klaar.

Zie je een rode run? Open hem; de melding zegt wat er mis is. Meestal is het een te kort wachtwoord (minder dan 20 tekens) of een tikfout in de naam `SIGNING_PASSPHRASE`. Pas het secret aan en start stap 3 opnieuw.

## 4. Overstappen op de nieuwe app (telefoon, 10 min)

1. Download de nieuwe versie:
   <https://github.com/harmgiezen149-maker/Minimalist-launcher/releases/latest/download/minimalist-launcher.apk>
   (blijft de download op 99% hangen, neem dan de zip: <https://github.com/harmgiezen149-maker/Minimalist-launcher/releases/latest/download/minimalist-launcher-apk.zip>)
2. Zet tijdelijk **One UI Home** als standaard-startscherm: Instellingen › Apps › Standaardapps › Startscherm-app.
3. **Verwijder** de oude Minimalist Launcher (Instellingen › Apps › Minimalist Launcher › Verwijderen).
4. Installeer de nieuwe APK.
5. Open de launcher. De checklist met rechten verschijnt; geef ze opnieuw, inclusief **standaard-startscherm**, **gebruikstoegang**, **toegankelijkheidsdienst** en **meldingstoegang**.
6. **Settings** › **Backup** › **Import settings from a file** → kies het bestand uit stap 1 → **OK**.
7. Controleer onderaan in Settings het versienummer (bijvoorbeeld `1.12 (abc1234)`).

Voortaan komen updates alleen van de nieuwe link, en alleen nadat ze alle controles hebben doorstaan. Een update installeer je gewoon over de app heen.

## 5. Dependabot-meldingen aanzetten (browser, 1 min)

Dit mocht ik niet vanuit mijn sessie doen.

1. Ga naar <https://github.com/harmgiezen149-maker/Minimalist-launcher/settings/security_analysis>
2. Zet **Dependabot alerts** en **Dependabot security updates** op **Enable**.

Je krijgt dan een melding (en vaak meteen een voorstel) als een gebruikte bibliotheek een bekend lek heeft. Wekelijkse update-voorstellen staan al aan via `.github/dependabot.yml`.

## 6. Vercel nakijken (browser, 5 min)

De oude branch in PW2 bevatte een kopie van je webapp met een debug-endpoint dat de KV-database zonder wachtwoord teruggaf. Ik heb dat endpoint verwijderd en previews voor die branch uitgezet (`vercel.json`). Oude previews kunnen nog bestaan.

1. Vercel › project van Planken Wambuis › **Settings** › **Deployment Protection**: zorg dat **Vercel Authentication** aan staat voor previews.
2. Vercel › **Deployments**: verwijder de deployments van branch `claude/dumb-phone-launcher-blocker-dh6ag9`.

## 7. Testen op je telefoon (15 min, wanneer het uitkomt)

Dit kan alleen op een echt toestel:

- **App-lade + toetsenbord (B08):** open de lade, tik op het zoekveld en typ één letter. De resultaten moeten zichtbaar blijven boven het toetsenbord.
- **Blokkeerscherm één keer (B14):** blokkeer een app, open hem, tik op "Back to focus". Het blokkeerscherm mag niet nog een keer verschijnen.
- **Kleur direct (B15):** Settings › Text color → green, ga terug. Het beginscherm is meteen groen, zowel bij het zwarte als bij het lichte thema.
- **Snelkoppeling rechtsonder:** staat daar een **+**, dan is de ingestelde app (standaard Gmail) niet geïnstalleerd. Tik erop en kies een app.
- **Overlay-recht (B24):** zet "Weergeven over andere apps" voor de launcher uit (Instellingen › Apps › Minimalist Launcher). Verschijnt het blokkeerscherm nog even snel, dan is die permissie overbodig en kan hij er in een volgende ronde uit (vraag het in een nieuwe sessie). Zo niet, zet hem dan weer aan.
- **TalkBack:** zet TalkBack aan; de knoppen op het beginscherm moeten als "knop" voorgelezen worden. Draai daarnaast de app *Accessibility Scanner* (Google) op beginscherm, lade en instellingen.
- **Open-apps-scherm:** wissel tussen een paar apps; de lijst moet blijven staan.
- **Strenge modus (nieuw, optioneel):** Settings › Strict mode aan, start een focussessie van 15 minuten en probeer een app te deblokkeren. Je krijgt dan een melding tot hoe laat dat niet kan.

## 8. Opruimen (optioneel, na stap 4)

- In PW2 kun je de branch `claude/dumb-phone-launcher-blocker-dh6ag9`, de bijbehorende pull request en de release `latest` verwijderen. Ze worden niet meer gebruikt.
- Wil je die release liever laten staan? Zet dan een verwijzing in de beschrijving. Ik mocht releases niet bewerken. Ga naar <https://github.com/harmgiezen149-maker/PW2/releases/tag/latest>, klik op het potloodje (**Edit**) en zet bovenaan:
  `Nieuwe versies: https://github.com/harmgiezen149-maker/Minimalist-launcher/releases/latest`
  Klik daarna **Update release**.
- Verwijder het bestand `minimalist-launcher-settings.json` als je het niet meer nodig hebt.
