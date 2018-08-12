Dit project bevat tools voor een schaker.

De parameter -Duser.language=nl wordt gebruikt om de uitvoer in het Nederlands
te krijgen. Zonder deze parameter wordt de standaard taal gebruikt. Om alle
opties te zien voer je het volgende commando uit:

    ~$ java -Duser.language=nl -jar caissatools-X.Y.Z-jar-with-dependencies.jar

This project contains tools for a chess player.

The parameter -Duser.language=en is used to force the application into english.
If you do not use it then the default language is used. For all options run:

    ~$ java -Duser.language=en -jar caissatools-X.Y.Z-jar-with-dependencies.jar

----
Upgrade naar 1.4.0

Bij de ELOBerekenaar is er een extra kolom toegevoegd aan het spelerBestand. Om
een spelerBestand uit een voorgaande versie te kunnen gebruiken moet men zelf
een lege kolom met de naam "groei" toevoegen achter de kolom "elo". Men kan ook
het bestand opnieuw aanmaken.

Upgrade to 1.4.0

The ELOBerekenaar uses an extra column in the spelerBestand. To use a
spelerBestand from a previous version you need to add an empty column with the
name "groei" aftre the "elo" column. You can also generate the file again.