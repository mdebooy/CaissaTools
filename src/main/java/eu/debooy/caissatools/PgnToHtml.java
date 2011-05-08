/**
 * Copyright 2008 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or � as soon they will be approved by
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

import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.html.Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * @author Marco de Booij
 */
public class PgnToHtml {
  private PgnToHtml() {}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter  output      = null;
    List<PGN>       partijen    = new ArrayList<PGN>();
    HashSet<String> spelers     = new HashSet<String>();
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();

    Banner.printBanner("PGN to HTML");

    Arguments       arguments   = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "charsetin", "charsetuit",
                                          "enkel", "halve", "uitvoerdir"});
    arguments.setVerplicht(new String[] {"bestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String  bestand = arguments.getArgument("bestand");
    int     enkel   = 1;
    if (DoosConstants.ONWAAR.equalsIgnoreCase(arguments.getArgument("enkel"))) {
      enkel = 2;
    }
    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    String[]  halve       =
      DoosUtils.nullToEmpty(arguments.getArgument("halve")).split(";");
    String    uitvoerdir  = arguments.getArgument("uitvoerdir");
    if (null == uitvoerdir) {
      uitvoerdir  = ".";
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    if (bestand.endsWith(".pgn")) {
      bestand = bestand.substring(0, bestand.length() - 4);
    }
    File    indexFile   = new File(uitvoerdir + "/index.html");
    File    matrixFile  = new File(uitvoerdir + "/matrix.html");

    partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn);

    try {
      for (PGN partij: partijen) {
        // Verwerk de spelers
        String  wit   = partij.getTag("White");
        String  zwart = partij.getTag("Black");
        spelers.add(wit);
        spelers.add(zwart);
      }

      // Maak de Matrix
      int           noSpelers = spelers.size();
      int           kolommen  = noSpelers * enkel;
      Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
      String[]      namen     = new String[noSpelers];
      double[][]    matrix    = new double[noSpelers][noSpelers * enkel];
      Iterator<String>
                  speler    = spelers.iterator();
      for (int i = 0; i < noSpelers; i++) {
        namen[i]  = speler.next();
        for (int j = 0; j < kolommen; j++) {
          matrix[i][j] = -1.0;
        }
      }
      Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
      for (int i = 0; i < noSpelers; i++) {
        punten[i] = new Spelerinfo();
        punten[i].setNaam(namen[i]);
      }

      for (PGN partij: partijen) {
        int     ronde   = 1;
        try {
          ronde = Integer.valueOf(partij.getTag("Round")).intValue();
        } catch (NumberFormatException nfe) {
          ronde = 1;
        }
        String  uitslag = partij.getTag("Result");
        String  wit     = partij.getTag("White");
        String  zwart   = partij.getTag("Black");
        if (ronde > noSpelers
            && (Arrays.binarySearch(halve, wit,
                                    String.CASE_INSENSITIVE_ORDER) > -1
                || Arrays.binarySearch(halve, zwart,
                                       String.CASE_INSENSITIVE_ORDER) > -1)) {
          continue;
        }
        int   iWit    = Arrays.binarySearch(namen, wit,
                                            String.CASE_INSENSITIVE_ORDER);
        int   iZwart  = Arrays.binarySearch(namen, zwart,
                                            String.CASE_INSENSITIVE_ORDER);
        if ("1-0".equals(uitslag)) {
          punten[iWit].addPartij();
          punten[iWit].addPunt(1.0);
          matrix[iWit][iZwart * enkel] = 1.0;
          punten[iZwart].addPartij();
          matrix[iZwart][iWit * enkel + enkel - 1] = 0.0;
        } else if ("1/2-1/2".equals(uitslag)) {
          punten[iWit].addPartij();
          punten[iWit].addPunt(0.5);
          matrix[iWit][iZwart * enkel] = 0.5;
          punten[iZwart].addPartij();
          punten[iZwart].addPunt(0.5);
          matrix[iZwart][iWit * enkel + enkel - 1] = 0.5;
        } else if ("0-1".equals(uitslag)) {
          punten[iWit].addPartij();
          matrix[iWit][iZwart * enkel] = 0.0;
          punten[iZwart].addPartij();
          punten[iZwart].addPunt(1.0);
          matrix[iZwart][iWit * enkel + enkel - 1] = 1.0;
        }
      }
      // Bereken Weerstandspunten
      for (int i = 0; i < noSpelers; i++) {
        Double weerstandspunten = 0.0;
        for (int j = 0; j < kolommen; j++) {
          if (matrix[i][j] > 0.0) {
            weerstandspunten += punten[j / enkel].getPunten() * matrix[i][j];
          }
        }
        punten[i].setWeerstandspunten(weerstandspunten);
      }

      // Maak de matrix.html file
      output  = new BufferedWriter(
                  new OutputStreamWriter(
                   new FileOutputStream(matrixFile), charsetUit));
      output.write("<table>");
      output.newLine();
      output.write("  <colgroup>");
      output.newLine();
      output.write("    <col width=\"34\" />");
      output.newLine();
      output.write("    <col width=\"186\" />");
      output.newLine();
      for (int i = 0; i < noSpelers; i++) {
        output.write("    <col width=\"17\" />");
        if (enkel == 2) {
          output.write("<col width=\"17\" />");
        }
        output.newLine();
      }
      output.write("    <col width=\"34\" /><col width=\"10\" />");
      output.newLine();
      output.write("    <col width=\"34\" />");
      output.newLine();
      output.write("    <col width=\"34\" /><col width=\"10\" />");
      output.newLine();
      output.write("  </colgroup>");
      output.newLine();
      output.write("  <tbody>");
      output.newLine();
      output.write("    <tr>");
      output.newLine();
      output.write("      <th colspan=\"2\"></th>");
      output.newLine();
      for (int i = 0; i < noSpelers; i++) {
        output.write("      <th");
        if (enkel == 2) {
          output.write(" colspan=\"2\"");
        }
        output.write(" align=\"center\">" + (i + 1) + "</th>");
        output.newLine();
      }
      output.write("      <th align=\"right\" colspan=\"2\">Punten</th>");
      output.newLine();
      output.write("      <th align=\"right\">Partijen</th>");
      output.newLine();
      output.write("      <th align=\"right\" colspan=\"2\">SB</th>");
      output.newLine();
      output.write("    </tr>");
      output.newLine();
      if (enkel == 2) {
        output.write("    <tr>");
        output.newLine();
        output.write("      <th colspan=\"2\"></th>");
        output.newLine();
        for (int i = 0; i < noSpelers; i++) {
          output.write("      <th align=\"center\">W</th><th align=\"center\">Z</th>");
          output.newLine();
        }
        output.write("      <th colspan=\"2\"></th>");
        output.newLine();
        output.write("      <th></th>");
        output.newLine();
        output.write("      <th colspan=\"2\"></th>");
        output.newLine();
        output.write("    </tr>");
        output.newLine();
      }
      for (int i = 0; i < noSpelers; i++) {
        output.write("    <tr>");
        output.newLine();
        output.write("      <th align=\"center\">" + (i + 1) + "</th>");
        output.newLine();
        output.write("      <th align=\"left\">"
                     + swapNaam(punten[i].getNaam()) + "</th>");
        output.newLine();
        for (int j = 0; j < kolommen; j++) {
          if ((j / enkel) * enkel == j ) {
            output.write("      ");
          }
          if (i == j / enkel) {
            output.write("<td class=\"zelf\"></td>");
          } else {
            output.write("<td align=\"center\"");
            if ((j / enkel) * enkel != j ) {
              output.write(" class=\"zwart\"");
            }
            output.write(">");
            if (matrix[i][j] == 0.0) {
              output.write("0");
            } else if (matrix[i][j] == 0.5) {
              output.write(Utilities.kwart(matrix[i][j]));
            } else if (matrix[i][j] == 1.0) {
              output.write("1");
            }
            output.write("</td>");
          }
          if ((j / enkel) * enkel != j ) {
            output.newLine();
          }
        }
        output.write("      <td align=\"right\">");
        if (punten[i].getPunten() == 0.0
            || punten[i].getPunten() >= 1.0) {
          output.write("" + punten[i].getPunten().intValue());
        }
        output.write("</td><td>" + Utilities.kwart(punten[i].getPunten())
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">" + punten[i].getPartijen()
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">");
        if (punten[i].getWeerstandspunten() == 0.0
            || punten[i].getWeerstandspunten() >= 1.0) {
          output.write("" + punten[i].getWeerstandspunten().intValue());
        }
        output.write("</td><td>"
                     + Utilities.kwart(punten[i].getWeerstandspunten())
                     + "</td>");
        output.newLine();
        output.write("    </tr>");
        output.newLine();
      }
      output.write("  </tbody>");
      output.newLine();
      output.write("</table>");
      output.newLine();
      output.close();

      // Maak de index.html file
      Arrays.sort(punten);
      output  = new BufferedWriter(
                  new OutputStreamWriter(
                    new FileOutputStream(indexFile), charsetUit));
      output.write("<table>");
      output.newLine();
      output.write("  <colgroup>");
      output.newLine();
      output.write("    <col width=\"34\" />");
      output.newLine();
      output.write("    <col width=\"186\" />");
      output.newLine();
      output.write("    <col width=\"78\" /><col width=\"10\" />");
      output.newLine();
      output.write("    <col width=\"88\" />");
      output.newLine();
      output.write("    <col width=\"78\" /><col width=\"10\" />");
      output.newLine();
      output.write("  </colgroup>");
      output.newLine();
      output.write("  <tbody>");
      output.newLine();
      output.write("    <tr>");
      output.newLine();
      output.write("      <th align=\"center\">Nr</th>");
      output.newLine();
      output.write("      <th align=\"left\">Naam</th>");
      output.newLine();
      output.write("      <th align=\"right\" colspan=\"2\">Punten</th>");
      output.newLine();
      output.write("      <th align=\"right\">Partijen</th>");
      output.newLine();
      output.write("      <th align=\"right\" colspan=\"2\">SB</th>");
      output.newLine();
      output.write("    </tr>");
      output.newLine();
      for (int i = 0; i < noSpelers; i++) {
        output.write("    <tr>");
        output.newLine();
        output.write("      <td align=\"center\">" + (i + 1) + "</td>");
        output.newLine();
        output.write("      <td align=\"left\">"
                     + swapNaam(punten[i].getNaam()) + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">");
        if (punten[i].getPunten() == 0.0
            || punten[i].getPunten() >= 1.0) {
          output.write("" + punten[i].getPunten().intValue());
        }
        output.write("</td><td>" + Utilities.kwart(punten[i].getPunten())
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">" + punten[i].getPartijen()
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">");
        if (punten[i].getWeerstandspunten() == 0.0
            || punten[i].getWeerstandspunten() >= 1.0) {
          output.write("" + punten[i].getWeerstandspunten().intValue());
        }
        output.write("</td><td>"
                     + Utilities.kwart(punten[i].getWeerstandspunten())
                     + "</td>");
        output.newLine();
        output.write("    </tr>");
        output.newLine();
      }
      output.write("  </tbody>");
      output.newLine();
      output.write("</table>");
      output.newLine();
      output.close();
    } catch (FileNotFoundException ex) {
      System.out.println(ex.getLocalizedMessage());
    } catch (IOException ex) {
      System.out.println(ex.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        System.out.println(ex.getLocalizedMessage());
      }
    }
    System.out.println("Bestand : " + bestand);
    System.out.println("Partijen: " + partijen.size());
    System.out.println("Uitvoer : " + uitvoerdir);
    System.out.println("Klaar.");
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    System.out.println("java -jar CaissaTools.jar PgnToHtml [OPTIE...] \\");
    System.out.println("  --bestand=<PGN bestand>");
    System.out.println();
    System.out.println("  --bestand    Het bestand met de partijen in PGN formaat.");
    System.out.println("  --charsetin  De characterset van <bestand> als deze niet "
                       + Charset.defaultCharset().name() + " is.");
    System.out.println("  --charsetuit De characterset van de uitvoer als deze niet "
                       + Charset.defaultCharset().name() + " moet zijn.");
    System.out.println("  --enkel      Enkelrondig <J|n>.");
    System.out.println("  --halve      Lijst met spelers (gescheiden door een ;) die enkel eerste helft meespelen.");
    System.out.println("               Enkel nodig bij enkel=N.");
    System.out.println("  --uitvoerdir Directory waar de uitvoer bestanden moeten staan.");
    System.out.println();
    System.out.println("Enkel bestand is verplicht.");
    System.out.println();
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }
    
    return deel[1].trim() + " " + deel[0].trim();
  }
}
