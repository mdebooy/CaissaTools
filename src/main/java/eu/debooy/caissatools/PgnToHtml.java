/**
 * Copyright 2008 Marco de Booij
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

import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.html.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;


/**
 * @author Marco de Booij
 */
public class PgnToHtml {
  public static void execute(String[] args) {
    BufferedReader  input       = null;
    BufferedWriter  output      = null;
    Collection<PGN> partijen    = new Vector<PGN>();
    HashSet<String> spelers     = new HashSet<String>();

    Banner.printBanner("PGN to HTML");

    Arguments       arguments   = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "enkel", "halve",
                                          "seizoen", "uitvoerdir"});
    arguments.setVerplicht(new String[] {"bestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String  bestand = arguments.getArgument("bestand");
    int             enkel       = 1;
    if (DoosConstants.ONWAAR.equalsIgnoreCase(arguments.getArgument("enkel"))) {
      enkel = 2;
    }
    String[]  halve       =
      DoosUtils.nullToEmpty(arguments.getArgument("halve")).split(";");
    String    seizoen     = arguments.getArgument("seizoen");
    String    uitvoerdir  = arguments.getArgument("uitvoerdir");
    if (null == uitvoerdir) {
      uitvoerdir  = ".";
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    if (bestand.endsWith(".pgn"))
      bestand = bestand.substring(0, bestand.length() - 4);
    File    indexFile   = new File(uitvoerdir + "/index.html");
    File    matrixFile  = new File(uitvoerdir + "/matrix.html");
    File    pgnFile     = new File(bestand + ".pgn");
    try {
      input   = new BufferedReader(new FileReader(pgnFile));
      String line = input.readLine().trim();
      // Zoek naar de eerste TAG
      while (line != null && !line.startsWith("[")) {
        line = input.readLine();
      }
      while (line != null) {
        PGN partij = new PGN();
        // Verwerk de TAGs
        while (line != null && line.startsWith("[")) {
          String tag = line.substring(1, line.indexOf(' '));
          String value = line.substring(line.indexOf('"') + 1,
              line.length() - 2);
          try {
            partij.addTag(tag, value);
          } catch (PgnException e) {
            System.out.println(e.getMessage());
          }
          line = input.readLine();
        }

        // Verwerk de zetten
        String uitslag = partij.getTag("Result");
        String zetten = "";
        while (line != null && !line.endsWith(uitslag)) {
          zetten += line.trim();
          if (!zetten.endsWith("."))
            zetten += " ";
          line = input.readLine();
        }
        zetten += line.substring(0, line.indexOf(uitslag));
        partij.setZetten(zetten.trim());

        partijen.add(partij);

        // Verwerk de spelers
        uitslag = partij.getTag("Result");
        String  wit   = partij.getTag("White");
        String  zwart = partij.getTag("Black");
        spelers.add(wit);
        spelers.add(zwart);

        // Zoek naar de eerste TAG
        while (line != null && !line.startsWith("[")) {
          line = input.readLine();
        }
      }
      input.close();

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
        for (int j = 0; j < kolommen; j++)
          matrix[i][j] = -1.0;
      }
      Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
      for (int i = 0; i < noSpelers; i++) {
        punten[i] = new Spelerinfo();
        punten[i].setNaam(namen[i]);
      }

      Iterator<PGN> iter = partijen.iterator();
      while (iter.hasNext()) {
        PGN     partij  = iter.next();
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
                                       String.CASE_INSENSITIVE_ORDER) > -1))
          continue;
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
          if (matrix[i][j] > 0.0)
            weerstandspunten += punten[j / enkel].getPunten() * matrix[i][j];
        }
        punten[i].setWeerstandspunten(weerstandspunten);
      }

      // Maak de matrix.html file
      output  = new BufferedWriter(new FileWriter(matrixFile));
      output.write("<include>declaration.html</include>");
      output.newLine();
      output.write("<head>");
      output.newLine();
      output.write("  <title>Schaakvereniging \"De Brug\"</title>");
      output.newLine();
      output.write("  <meta http-equiv=\"Content-Type\"  content=\"text/html; charset=iso-8859-1\" />");
      output.newLine();
      output.write("  <meta name=\"AbsoluteURL\"         content=\"\" />");
      output.newLine();
      output.write("  <meta name=\"Author\"              content=\"M.N. de Booij\" />");
      output.newLine();
      output.write("  <meta name=\"Content\"             content=\"Schaakvereniging 'De Brug'\" />");
      output.newLine();
      output.write("  <meta name=\"Keywords\"            content=\"chess, zwevegem\" />");
      output.newLine();
      output.write("  <meta name=\"Rating\"              content=\""
                   + seizoen + "\" />");
      output.newLine();
      output.write("  <meta name=\"Robots\"              content=\"index, follow\" />");
      output.newLine();
      output.write("  <meta name=\"Title\"               content=\"Schaakvereniging 'De Brug'\" />");
      output.newLine();
      output.write("  <link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"../common/css/DeBrug.css\" />");
      output.newLine();
      output.write("  <link rel=\"stylesheet\" type=\"text/css\" media=\"print\"  href=\"../common/css/DeBrug_print.css\" />");
      output.newLine();
      output.write("  <link rel=\"shortcut icon\" href=\"../common/favicon.ico\" />");
      output.newLine();
      output.write("</head>");
      output.newLine();
      output.write("<body class=\"matrix\">");
      output.newLine();
      output.write("<div id=\"header\">");
      output.newLine();
      output.write("<h1>Schaakvereniging \"De Brug\"</h1>");
      output.newLine();
      output.write("<h2>Seizoen " + seizoen + "</h2>");
      output.newLine();
      output.write("</div>");
      output.newLine();
      output.newLine();
      output.write("<include>DeBrugCompetitieAktief.html</include>");
      output.newLine();
      output.newLine();
      output.write("<include>DeBrugMenu.html</include>");
      output.newLine();
      output.newLine();
      output.write("<div id=\"content\">");
      output.newLine();
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
        if (enkel == 2)
          output.write("<col width=\"17\" />");
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
        if (enkel == 2)
          output.write(" colspan=\"2\"");
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
          if ((j / enkel) * enkel == j )
            output.write("      ");
          if (i == j / enkel) {
            output.write("<td class=\"zelf\"></td>");
          } else {
            output.write("<td align=\"center\"");
            if ((j / enkel) * enkel != j )
              output.write(" class=\"zwart\"");
            output.write(">");
            if (matrix[i][j] == 0.0)
              output.write("0");
            else if (matrix[i][j] == 0.5)
              output.write(Utilities.kwart(matrix[i][j]));
            else if (matrix[i][j] == 1.0)
              output.write("1");
            output.write("</td>");
          }
          if ((j / enkel) * enkel != j )
            output.newLine();
        }
        output.write("      <td align=\"right\">");
        if (punten[i].getPunten() == 0.0
            || punten[i].getPunten() >= 1.0)
          output.write("" + punten[i].getPunten().intValue());
        output.write("</td><td>" + Utilities.kwart(punten[i].getPunten())
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">" + punten[i].getPartijen()
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">");
        if (punten[i].getWeerstandspunten() == 0.0
            || punten[i].getWeerstandspunten() >= 1.0)
          output.write("" + punten[i].getWeerstandspunten().intValue());
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
      output.write("</div>");
      output.newLine();
      output.write("</body>");
      output.newLine();
      output.write("</html>");
      output.newLine();
      output.close();

      // Maak de index.html file
      Arrays.sort(punten);
      output  = new BufferedWriter(new FileWriter(indexFile));
      output.write("<include>declaration.html</include>");
      output.newLine();
      output.write("<head>");
      output.newLine();
      output.write("  <title>Schaakvereniging \"De Brug\"</title>");
      output.newLine();
      output.write("  <meta http-equiv=\"Content-Type\"  content=\"text/html; charset=iso-8859-1\" />");
      output.newLine();
      output.write("  <meta name=\"AbsoluteURL\"         content=\"\" />");
      output.newLine();
      output.write("  <meta name=\"Author\"              content=\"M.N. de Booij\" />");
      output.newLine();
      output.write("  <meta name=\"Content\"             content=\"Schaakvereniging 'De Brug'\" />");
      output.newLine();
      output.write("  <meta name=\"Keywords\"            content=\"chess, zwevegem\" />");
      output.newLine();
      output.write("  <meta name=\"Rating\"              content=\""
                   + seizoen + "\" />");
      output.newLine();
      output.write("  <meta name=\"Robots\"              content=\"index, follow\" />");
      output.newLine();
      output.write("  <meta name=\"Title\"               content=\"Schaakvereniging 'De Brug'\" />");
      output.newLine();
      output.write("  <link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"../common/css/DeBrug.css\" />");
      output.newLine();
      output.write("  <link rel=\"stylesheet\" type=\"text/css\" media=\"print\"  href=\"../common/css/DeBrug_print.css\" />");
      output.newLine();
      output.write("  <link rel=\"shortcut icon\" href=\"../common/favicon.ico\" />");
      output.newLine();
      output.write("</head>");
      output.newLine();
      output.write("<body class=\"stand\">");
      output.newLine();
      output.write("<div id=\"header\">");
      output.newLine();
      output.write("<h1>Schaakvereniging \"De Brug\"</h1>");
      output.newLine();
      output.write("<h2>Seizoen " + seizoen + "</h2>");
      output.newLine();
      output.write("</div>");
      output.newLine();
      output.newLine();
      output.write("<include>DeBrugCompetitieAktief.html</include>");
      output.newLine();
      output.newLine();
      output.write("<include>DeBrugMenu.html</include>");
      output.newLine();
      output.newLine();
      output.write("<div id=\"content\">");
      output.newLine();
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
      output.write("      <th align=\"right\" colspan=\"2\">Punten</th");
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
            || punten[i].getPunten() >= 1.0)
          output.write("" + punten[i].getPunten().intValue());
        output.write("</td><td>" + Utilities.kwart(punten[i].getPunten())
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">" + punten[i].getPartijen()
                     + "</td>");
        output.newLine();
        output.write("      <td align=\"right\">");
        if (punten[i].getWeerstandspunten() == 0.0
            || punten[i].getWeerstandspunten() >= 1.0)
          output.write("" + punten[i].getWeerstandspunten().intValue());
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
      output.write("</div>");
      output.newLine();
      output.write("</body>");
      output.newLine();
      output.write("</html>");
      output.newLine();
      output.close();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (input != null) {
          input.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
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
    System.out.println("  --enkel      Enkelrondig <J|n>");
    System.out.println("  --halve      Lijst met spelers (gescheiden door een ;) die enkel eerste helft meespelen.");
    System.out.println("               Enkel nodig bij enkel=N.");
    System.out.println("  --seizoen    Seizoen.");
    System.out.println("  --uitvoerdir Directory waar de uitvoer bestanden moeten staan.");
    System.out.println();
    System.out.println("Enkel bestand is verplicht.");
    System.out.println();
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1)
      return naam;
    
    return deel[1].trim() + " " + deel[0].trim();
  }
}
