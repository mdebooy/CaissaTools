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

import eu.debooy.caissa.CaissaConstants;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.latex.Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * @author Marco de Booij
 */
public class PgnToLatex {
  private PgnToLatex() {}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter  output      = null;
    List<PGN>       partijen    = new ArrayList<PGN>();
    HashSet<String> spelers     = new HashSet<String>();
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();
    String          eindDatum   = "0000.00.00";
    String          hulpDatum   = "";
    String          startDatum  = "9999.99.99";

    Banner.printBanner("PGN to LaTeX");

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"auteur", "bestand", "charsetin",
                                          "charsetuit", "datum", "enkel",
                                          "halve", "logo", "matrix", "titel"});
    arguments.setVerplicht(new String[] {"bestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String  auteur  = arguments.getArgument("auteur");
    String  bestand = arguments.getArgument("bestand");
    if (bestand.endsWith(".pgn")) {
      bestand   = bestand.substring(0, bestand.length() - 4);
    }
    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    String    datum     = arguments.getArgument("datum");
    if (DoosUtils.isBlankOrNull(datum)) {
      try {
        datum = Datum.fromDate(new Date(), "dd/MM/yyyy HH:mm:ss");
      } catch (ParseException e) {
        System.out.println(e.getLocalizedMessage());
      }
    }
    int       enkel     = 1;
    if (DoosConstants.ONWAAR.equalsIgnoreCase(arguments.getArgument("enkel"))) {
      enkel   = 2;
    }
    String[]  halve     =
      DoosUtils.nullToEmpty(arguments.getArgument("halve")).split(";");
    String    logo      = arguments.getArgument("logo");
    String    metMatrix = arguments.getArgument("matrix");
    if (DoosUtils.isBlankOrNull(metMatrix)) {
      metMatrix = DoosConstants.WAAR;
    }
    String    titel     = arguments.getArgument("titel");

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    File    latexFile = new File(bestand + ".tex");

    partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn);

    for (PGN partij: partijen) {
      // Verwerk de spelers
      String  wit   = partij.getTag("White");
      String  zwart = partij.getTag("Black");
      spelers.add(wit);
      spelers.add(zwart);
  
      // Verwerk de 'datums'
      hulpDatum = partij.getTag("EventDate");
      if (DoosUtils.isNotBlankOrNull(hulpDatum)
          && hulpDatum.indexOf('?') < 0) {
        if (hulpDatum.compareTo(startDatum) < 0 ) {
          startDatum  = hulpDatum;
        }
        if (hulpDatum.compareTo(eindDatum) > 0 ) {
          eindDatum   = hulpDatum;
        }
      }
      hulpDatum = partij.getTag("Date");
      if (DoosUtils.isNotBlankOrNull(hulpDatum)
          && hulpDatum.indexOf('?') < 0) {
        if (hulpDatum.compareTo(startDatum) < 0 ) {
          startDatum  = hulpDatum;
        }
        if (hulpDatum.compareTo(eindDatum) > 0 ) {
          eindDatum   = hulpDatum;
        }
      }
      if (DoosUtils.isBlankOrNull(auteur)) {
        auteur  = partij.getTag("Site");
      }
      if (DoosUtils.isBlankOrNull(titel)) {
        titel   = partij.getTag("Event");
      }
    }

    try {
      output  = new BufferedWriter(
                  new OutputStreamWriter(
                    new FileOutputStream(latexFile), charsetUit));

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
          matrix[i][j]  = -1.0;
        }
        punten[i].setWeerstandspunten(weerstandspunten);
      }
      Arrays.sort(punten);
      int[] stand = new int[noSpelers];
      for (int i = 0; i < noSpelers; i++) {
        stand[Arrays.binarySearch(namen, punten[i].getNaam(),
                                  String.CASE_INSENSITIVE_ORDER)] = i;
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
        int   iWit    =
          stand[Arrays.binarySearch(namen, wit,
                                    String.CASE_INSENSITIVE_ORDER)];
        int   iZwart  =
          stand[Arrays.binarySearch(namen, zwart,
                                    String.CASE_INSENSITIVE_ORDER)];
        if ("1-0".equals(uitslag)) {
          matrix[iWit][iZwart * enkel] = 1.0;
          matrix[iZwart][iWit * enkel + enkel - 1] = 0.0;
        } else if ("1/2-1/2".equals(uitslag)) {
          matrix[iWit][iZwart * enkel] = 0.5;
          matrix[iZwart][iWit * enkel + enkel - 1] = 0.5;
        } else if ("0-1".equals(uitslag)) {
          matrix[iWit][iZwart * enkel] = 0.0;
          matrix[iZwart][iWit * enkel + enkel - 1] = 1.0;
        }
      }

      // Maak de .tex file
      output.write("\\documentclass[dutch,twocolumn,a4,10pt]{report}");
      output.newLine();
      output.newLine();
      output.write("\\usepackage{skak}");
      output.newLine();
      output.write("\\usepackage{babel}");
      output.newLine();
      output.write("\\usepackage{color}");
      output.newLine();
      output.write("\\usepackage{colortbl}");
      output.newLine();
      output.write("\\usepackage[T1]{fontenc}");
      output.newLine();
      output.write("\\usepackage[pdftex]{graphicx}");
      output.newLine();
      output.write("\\usepackage{pdflscape}");
      output.newLine();
      output.newLine();
      output.write("\\topmargin =0.mm");
      output.newLine();
      output.write("\\oddsidemargin =0.mm");
      output.newLine();
      output.write("\\evensidemargin =0.mm");
      output.newLine();
      output.write("\\headheight =0.mm");
      output.newLine();
      output.write("\\headsep =0.mm");
      output.newLine();
      output.write("\\textheight =250.mm");
      output.newLine();
      output.write("\\textwidth =165.mm");
      output.newLine();
      output.write("\\parindent =0.mm");
      output.newLine();
      output.newLine();
      output.write("\\newcommand{\\chessgame}[7]{");
      output.newLine();
      output.write("  $\\circ$ \\textbf{#1} \\hfill Ronde {#4}\\\\");
      output.newLine();
      output.write("  $\\bullet$ \\textbf{#2}\\\\");
      output.newLine();
      output.write("  {#3} \\hfill {#6} \\hfill {#5}\\\\");
      output.newLine();
      output.write("  \\styleB");
      output.newLine();
      output.write("  \\newgame");
      output.newLine();
      output.write("  \\mainline{#7} {\\bf #5}");
      output.newLine();
      output.write("  \\[\\showboard\\]");
      output.newLine();
      output.write("  \\begin{center} \\hrule \\end{center}");
      output.newLine();
      output.write("}");
      output.newLine();
      output.newLine();
      output.write("\\newcommand{\\chessempty}[6]{");
      output.newLine();
      output.write("  $\\circ$ \\textbf{#1} \\hfill Ronde {#4}\\\\");
      output.newLine();
      output.write("  $\\bullet$ \\textbf{#2}\\\\");
      output.newLine();
      output.write("  {#3} \\hfill {#6} \\hfill {#5}\\\\");
      output.newLine();
      output.write("  \\begin{center} \\hrule \\end{center}");
      output.newLine();
      output.write("}");
      output.newLine();
      output.newLine();
      output.write("% Be sure that a game is not split on two columns or "
                   + "pages.");
      output.newLine();
      output.write("\\raggedbottom \\topskip 1\\topskip plus1000pt % like "
                   + "\\raggedbottom; moreso \\def\\need#1{\\vskip #1"
                   + "\\penalty0 \\vskip-#1\\relax}");
      output.newLine();
      output.newLine();
      output.write("\\title{" + titel + "}");
      output.newLine();
      output.write("\\author{" + auteur + "}");
      output.newLine();
      output.write("\\date{" + datum + "}");
      output.newLine();
      output.newLine();
      output.write("\\begin{document}");
      output.newLine();
      if (DoosUtils.isNotBlankOrNull(logo)) {
        output.write("\\DeclareGraphicsExtensions{.pdf,.png,.gif,.jpg}");
        output.newLine();
      }
      output.write("\\begin{titlepage}");
      output.newLine();
      output.write("  \\begin{center}");
      output.newLine();
      output.write("    \\huge " + titel + " \\\\");
      output.newLine();
      output.write("    \\vspace{1in}");
      output.newLine();
      output.write("    \\large " + auteur + " \\\\");
      output.newLine();
      if (DoosUtils.isNotBlankOrNull(logo)) {
        output.write("    \\vspace{2in}");
        output.newLine();
        output.write("    \\includegraphics[width=6cm]{"+ logo + "} \\\\");
        output.newLine();
      }
      output.write("    \\vspace{1in}");
      output.newLine();
      output.write("    \\large " + datumInTitel(startDatum, eindDatum)
                   + " \\\\");
      output.newLine();
      output.write("  \\end{center}");
      output.newLine();
      output.write("\\end{titlepage}");
      output.newLine();
      if (DoosConstants.WAAR.equalsIgnoreCase(metMatrix)) {
        output.write("\\begin{landscape}");
        output.newLine();
        output.write("  \\begin{center}");
        output.newLine();
        output.write("    \\begin{tabular} { | c | l | ");
        for (int i = 0; i < kolommen; i++) {
          output.write(" c | ");
        }
        output.write("r | r | r | }");
        output.newLine();
        output.write("    \\hline");
        output.newLine();
        output.write("    \\multicolumn{2}{|c|}{} ");
        for (int i = 0; i < noSpelers; i++) {
          if (enkel == 1) {
            output.write(" & " + (i + 1));
          } else {
            output.write(" & \\multicolumn{2}{c|}{" + (i + 1) + "} ");
          }
        }
        output.write("& Punten & Partijen & SB \\\\");
        output.newLine();
        output.write("    \\cline{3-" + (2 + kolommen) + "}");
        output.newLine();
        if (enkel == 2) {
          output.write("    \\multicolumn{2}{|c|}{} & ");
          for (int i = 0; i < noSpelers; i++) {
            output.write("W & Z & ");
          }
          output.write("& & \\\\");
          output.newLine();
        }
        output.write("    \\hline");
        output.newLine();
        for (int i = 0; i < noSpelers; i++) {
          output.write((i + 1) + " & " + punten[i].getNaam() + " & ");
          for (int j = 0; j < kolommen; j++) {
            if (i == j / enkel) {
              output.write("\\multicolumn{1}"
                           + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ");
            } else {
              if ((j / enkel) * enkel != j ) {
                output.write("\\multicolumn{1}"
                             + "{>{\\columncolor[rgb]{0.8,0.8,0.8}}c|}{");
              }
              if (matrix[i][j] == 0.0) {
                output.write("0");
              } else if (matrix[i][j] == 0.5) {
                output.write("\\textonehalf");
              } else if (matrix[i][j] == 1.0) {
                output.write("1");
              }
              if ((j / enkel) * enkel != j ) {
                output.write("}");
              }
              output.write(" & ");
            }
          }
          output.write(punten[i].getPunten().intValue()
                       + Utilities.kwart(punten[i].getPunten()) + " & ");
          output.write(punten[i].getPartijen() + " & ");
          output.write(punten[i].getWeerstandspunten().intValue()
              + Utilities.kwart(punten[i].getWeerstandspunten()) + " \\\\");
          output.newLine();
          output.write("    \\hline");
          output.newLine();
        }
        output.write("    \\end{tabular}");
        output.newLine();
        output.write("  \\end{center}");
        output.newLine();
        output.write("\\end{landscape}");
        output.newLine();
        output.write("\\newpage");
        output.newLine();
      }
      output.write("\\topmargin =-15.mm");
      output.newLine();

      for (PGN partij: partijen) {
        String zetten = partij.getZetten();
        if (DoosUtils.isNotBlankOrNull(zetten)) {
          output.write("\\begin{chessgame}{" + partij.getTag("White") + "}{"
              + partij.getTag("Black") + "}{" + partij.getTag("Date") + "}{"
              + partij.getTag("Round") + "}{"
              + partij.getTag("Result").replaceAll("1/2", "\\\\textonehalf")
              + "}{");
          String  eco = partij.getTag("ECO");
          if (DoosUtils.isNotBlankOrNull(eco)) {
            output.write(eco);
          }
          output.write("}{"+ partij.getZetten().replaceAll("#", "\\\\#")
              + "}\\end{chessgame}");
        } else {
          output.write("\\begin{chessempty}{" + partij.getTag("White") + "}{"
              + partij.getTag("Black") + "}{" + partij.getTag("Date") + "}{"
              + partij.getTag("Round") + "}{"
              + partij.getTag("Result").replaceAll("1/2", "\\\\textonehalf")
              + "}{}\\end{chessempty}");
        }
        output.newLine();
      }

      output.write("\\end{document}");
      output.newLine();
      output.close();
    } catch (FileNotFoundException ex) {
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
    System.out.println("Klaar.");
  }

  /**
   * Maakt de datum informatie voor de titel pagina.
   */
  protected static String datumInTitel(String startDatum, String eindDatum) {
    StringBuffer  titelDatum  = new StringBuffer();
    Date          datum       = null;
    try {
      datum = Datum.toDate(startDatum, CaissaConstants.PGN_DATUM_FORMAAT);
      titelDatum.append(Datum.fromDate(datum));
    } catch (ParseException e) {
      System.out.println("StartDatum: " + e.getLocalizedMessage()
                         + " [" + startDatum + "]");
    }

    if (!startDatum.equals(eindDatum)) {
      try {
        datum = Datum.toDate(eindDatum, CaissaConstants.PGN_DATUM_FORMAAT);
        titelDatum.append(" - " + Datum.fromDate(datum));
      } catch (ParseException e) {
        System.out.println("EindDatum: " + e.getLocalizedMessage()
                           + " [" + eindDatum + "]");
      }
    }

    return titelDatum.toString();
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    System.out.println("java -jar CaissaTools.jar PgnToLatex [OPTIE...] \\");
    System.out.println("  --bestand=<PGN bestand>");
    System.out.println();
    System.out.println("  --auteur     De auteur of club die de partijen publiceert.");
    System.out.println("  --bestand    Het bestand met de partijen in PGN formaat.");
    System.out.println("  --charsetin  De characterset van <bestand> als deze niet "+ Charset.defaultCharset().name() + " is.");
    System.out.println("  --charsetout De characterset van de uitvoer als deze niet "+ Charset.defaultCharset().name() + " moet zijn.");
    System.out.println("  --datum      De datum waarop de partijen zijn gespeeld.");
    System.out.println("  --enkel      Enkelrondig <J|n>");
    System.out.println("  --halve      Lijst met spelers (gescheiden door een ;) die enkel eerste helft meespelen.");
    System.out.println("               Enkel nodig bij enkel=N.");
    System.out.println("  --logo       Logo op de titel pagina.");
    System.out.println("  --matrix     Uitslagen matrix <J|n>.");
    System.out.println("  --titel      De titel van het document.");
    System.out.println();
    System.out.println("Enkel bestand is verplicht.");
    System.out.println();
  }
}
