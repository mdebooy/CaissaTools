Dit project bevat tools voor een schaker.

This project contains tools for a chess player.

The parameter -Duser.language=en is used to force the application into english.
If you do not use it then the default language is used.

~$ java -Duser.language=en -jar caissatools-1.2.0-SNAPSHOT-jar-with-dependencies.jar
+----------+----------+----------+----------+----------+----------+----------+
|          |          |
|   |\__   *   __/|   | Caissa Tools
|   /  .\ *** /.   \  |
|  | ( _ \ * / _ ) |  |
+--|    \_) (_/    |--+----------+----------+----------+----------+----------+
|  |    |     |    |  |
|  /_____\   /_____\  |
| [_______]_[_______] | E-Mail : marco.development@debooy.eu
|       [_____]       | Website: http://www.debooy.eu
+----------+----------+----------+----------+----------+----------+----------+
                                        v1.2.0-SNAPSHOT | 2016-10-26 19:58:44

  ChessTheatre      Convert a file with PGN games into input for ChessTheatre.
  ELOBerekenaar     Calculate the ELO rating for the players in the PGN file.
  PgnToHtml         Convert the PGN file with games from a tournament into 2
                    HTML files. One file with the standing and the other with
                    the result matrix.
  PgnToLatex        Convert the PGN file with games from a tournament into a
                    .tex file.
  StartPgn          Make a PGN file with all games that need to be played
                    between the given users.
  SpelerStatistiek  Makes statistics on all games of a given player.
  VertaalPgn        Translate the piece codes from one language into another.

java -jar CaissaTools.jar ChessTheatre [OPTION...] --bestand=<PGN file>

  --bestand      The file with games in PGN format.
  --charsetin    The characterset of <bestand> if it is different from UTF-8.
  --charsetuit   The characterset of the output if it is different from UTF-8.
  --maxBestanden Maximum number of files to hold the games. Default value is 50.
  --minPartijen  Minimum number of games per file. Default value is the total
                 number of games divided by <maxBestanden>.
  --uitvoerdir   Directory in which the output files are written.
  --zip          Name (can include a directory directory) of the zip file with
                 the games. This zip file is not created by the application.

Only parameter bestand is mandatory.


java -jar CaissaTools.jar ELOBerekenaar [OPTION...] \
    --spelerBestand=<CSV file> --toernooiBestand=<PGN file>

  --charsetin           The characterset of <bestand> if it is different from
                        UTF-8.
  --charsetuit          The characterset of the output if it is different from
                        UTF-8.
  --geschiedenisBestand The file (with .csv extension) with the evolution of
                        the ratings.
  --spelerBestand       A CSV file (with .csv extension) with the latest
                        ratings of the players. This file is updated or created.
  --startDatum          A date after which the games count (yyyy.mm.dd).
  --startELO            The starting ELO points for unrated players. The
                        default value is 1,600.
  --toernooiBestand     The file with games from a tournament in PGN format.
  --uitvoerdir          Directory in which the output files are written.

The parameters spelerBestand and toernooiBestand are mandatory.

The parameters spelerBestand and geschiedenisBestand without directory.


java -jar CaissaTools.jar PgnToHtml [OPTION...] --bestand=<PGN file>

  --bestand    The file with games in PGN format.
  --charsetin  The characterset of <bestand> if it is different from UTF-8.
  --charsetuit The characterset of the output if it is different from UTF-8.
  --enkel      Single round <J|n> (J=Yes, N=No). Any other value gives a Match
               matrix.
  --halve      List of players (separated by a ;) that only plays the first
               half of the tournament. Only needed when <enkel>=N.
  --uitvoerdir Directory in which the output files are written.

Only parameter bestand is mandatory.


java -jar CaissaTools.jar PgnToLatex [OPTION...] --bestand=<PGN file>

  --auteur     The author or club that publishes the games.
  --bestand    The file with games in PGN format.
  --charsetin  The characterset of <bestand> if it is different from UTF-8.
  --charsetuit The characterset of the output if it is different from UTF-8.
  --datum      The date on which the games were played.
  --enkel      Single round <J|n> (J=Yes, N=No). Any other value gives a Match
               matrix.
  --halve      List of players (separated by a ;) that only plays the first
               half of the tournament. Only needed when <enkel>=N.
  --keywords   List of keywords (separated by a ;).
  --logo       Logo on the title page.
  --matrix     Result matrix <J|n> (J=Yes, N=No).
  --template   Een template voor het uitvoer TEX bestand. 
  --titel      The title of the document.

Only parameter bestand is mandatory.


java -jar CaissaTools.jar StartPgn --bestand=<PGN file>
    --date=<date> --event=<event> --site=<site> \
    --spelers=<player1>[;<player2>...] \
    [--uitvoerdir=<output-directory>]

  --bestand    The file with games in PGN format.
  --charsetin  The characterset of <bestand> if it is different from UTF-8.
  --charsetuit The characterset of the output if it is different from UTF-8.
  --date       Date of the tournament.
  --event      Name of the tournament.
  --site       Place where the tournament is played.
  --spelers    List of players (separated by a ;).
  --uitvoerdir Directory in which the output files are written.

All parameters (except uitvoerdir) are mandatory.


java -jar CaissaTools.jar SpelerStatistiek [OPTION...] \
    --bestand=<PGN file> --speler=<Player's name>

  --bestand    The file with games in PGN format.
  --charsetin  The characterset of <bestand> if it is different from UTF-8.
  --charsetuit The characterset of the output if it is different from UTF-8.
  --logo       Logo on the title page.
  --speler     The speler for whom the statistics are made.
  --tag        The PGN tag on which the statistics are based. Without this
               parameter the opponents are taken. With the tag "date" the year
               is used.

The parameters bestand and speler are mandatory.


java -jar CaissaTools.jar VertaalPgn [OPTION...]

  --bestand    The file with games in PGN format.
  --charsetin  The characterset of <bestand> if it is different from UTF-8.
  --charsetuit The characterset of the output if it is different from UTF-8.
  --naartaal   The output language if it is different from: en.
  --pgn        The moves that need to be translated. Put them between ".
  --vantaal    The input language if it is different from: en.

The input can be a PGN file (<bestand>) or a parameter <pgn>. In case of a
<bestand> a file will be created that has the same name but with a postfix of
an _ and the <naartaal>.
~$ 
