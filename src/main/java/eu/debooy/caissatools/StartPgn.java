/**
 * Copyright 2009 Marco de Booy
 *
 * Licensed under the EUPL, Version 1.0 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/7330l5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package eu.debooy.caissatools;

import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;


/**
 * @author Marco de Booy
 */
public class StartPgn {
  public static void execute(String[] args) {
    BufferedWriter  output      = null;

    Banner.printBanner("Start PGN");

    Arguments       arguments   = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "date", "event", "site",
                                          "spelers", "uitvoerdir"});
    arguments.setVerplicht(new String[] {"bestand", "date", "event", "site",
                                         "spelers"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand     = arguments.getArgument("bestand");
    String    date        = arguments.getArgument("date");
    String    event       = arguments.getArgument("event");
    String    site        = arguments.getArgument("site");
    String[]  speler      = arguments.getArgument("spelers").split(";");
    String    uitvoerdir  = arguments.getArgument("uitvoerdir");
    if (null == uitvoerdir) {
      uitvoerdir  = ".";
    }

    Arrays.sort(speler, String.CASE_INSENSITIVE_ORDER);
    int noSpelers = speler.length;

    if (bestand.endsWith(".pgn"))
      bestand = bestand.substring(0, bestand.length() - 4);
    if (uitvoerdir.endsWith("\\"))
      uitvoerdir  = uitvoerdir.substring(0, uitvoerdir.length() - 1);
    File    pgnFile     = new File(uitvoerdir + "\\" + bestand + ".pgn");
    try {
      output  = new BufferedWriter(new FileWriter(pgnFile));
      for (int i = 0; i < (noSpelers -1); i++) {
        for (int j = i + 1; j < noSpelers; j++) {
          output.write("[Event \"" + event + "\"]");
          output.newLine();
          output.write("[Site \"" + site + "\"]");
          output.newLine();
          output.write("[Date \"" + date + "\"]");
          output.newLine();
          output.write("[Round \"-\"]");
          output.newLine();
          output.write("[White \"" + speler[i] + "\"]");
          output.newLine();
          output.write("[Black \"" + speler[j] + "\"]");
          output.newLine();
          output.write("[Result \"*\"]");
          output.newLine();
          output.newLine();
          output.write("*");
          output.newLine();
          output.newLine();
          output.write("[Event \"" + event + "\"]");
          output.newLine();
          output.write("[Site \"" + site + "\"]");
          output.newLine();
          output.write("[Date \"" + date + "\"]");
          output.newLine();
          output.write("[Round \"-\"]");
          output.newLine();
          output.write("[White \"" + speler[j] + "\"]");
          output.newLine();
          output.write("[Black \"" + speler[i] + "\"]");
          output.newLine();
          output.write("[Result \"*\"]");
          output.newLine();
          output.newLine();
          output.write("*");
          output.newLine();
          output.newLine();
        }
      }
      output.close();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    System.out.println("Bestand : " + bestand);
    System.out.println("Uitvoer : " + uitvoerdir);
    System.out.println("Klaar.");
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    System.out.println("java -jar CaissaTools.jar StartPgn --bestand=<PGN bestand> \\");
    System.out.println("  --date=<datum> --event=<event> --site=<site> \\");
    System.out.println("  --spelers=<speler1>[;<speler2>...] \\");
    System.out.println("  [--uitvoerdir=<uitvoer-directory>]");
    System.out.println();
    System.out.println("  --bestand    Het bestand met de partijen in PGN formaat.");
    System.out.println("  --date       Datum van het toernooi.");
    System.out.println("  --event      Naam van het toernooi.");
    System.out.println("  --site       Plaats waar het toernooi gespeeld wordt.");
    System.out.println("  --spelers    Lijst met spelers (gescheiden door een ;).");
    System.out.println("  --uitvoerdir Directory waar de uitvoer bestanden moeten staan.");
    System.out.println();
    System.out.println("Alle parameters, behalve uitvoerdir, zijn verplicht.");
    System.out.println();
  }
}
