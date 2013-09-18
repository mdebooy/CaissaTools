/**
 * Copyright 2008 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they will be approved by
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
import eu.debooy.caissa.sorteer.PGNSortByEvent;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class PgnToLatex {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

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

    Banner.printBanner(resourceBundle.getString("banner.pgntolatex"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"auteur", "bestand", "charsetin",
                                          "charsetuit", "datum", "enkel",
                                          "halve", "keywords", "logo", "matrix",
                                          "titel"});
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
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    int       enkel     = 1;
    if (DoosConstants.ONWAAR.equalsIgnoreCase(arguments.getArgument("enkel"))) {
      enkel   = 2;
    }
    String[]  halve     =
      DoosUtils.nullToEmpty(arguments.getArgument("halve")).split(";");
    String    keywords  = arguments.getArgument("keywords");
    String    logo      = arguments.getArgument("logo");
    String    metMatrix = arguments.getArgument("matrix");
    if (DoosUtils.isBlankOrNull(metMatrix)) {
      metMatrix = DoosConstants.WAAR;
    }
    String  titel       = arguments.getArgument("titel");

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn,
                                           new PGNSortByEvent());
    Collections.sort(partijen);

    for (PGN partij: partijen) {
      // Verwerk de spelers
      String  wit   = partij.getTag(CaissaConstants.PGNTAG_WHITE);
      String  zwart = partij.getTag(CaissaConstants.PGNTAG_BLACK);
      if (!"bye".equalsIgnoreCase(wit)
          || DoosUtils.isNotBlankOrNull(wit)) {
        spelers.add(wit);
      }
      if (!"bye".equalsIgnoreCase(zwart)
          || DoosUtils.isNotBlankOrNull(zwart)) {
        spelers.add(zwart);
      }
  
      // Verwerk de 'datums'
      hulpDatum = partij.getTag(CaissaConstants.PGNTAG_EVENTDATE);
      if (DoosUtils.isNotBlankOrNull(hulpDatum)
          && hulpDatum.indexOf('?') < 0) {
        if (hulpDatum.compareTo(startDatum) < 0 ) {
          startDatum  = hulpDatum;
        }
        if (hulpDatum.compareTo(eindDatum) > 0 ) {
          eindDatum   = hulpDatum;
        }
      }
      hulpDatum = partij.getTag(CaissaConstants.PGNTAG_DATE);
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
        auteur  = partij.getTag(CaissaConstants.PGNTAG_SITE);
      }
      if (DoosUtils.isBlankOrNull(titel)) {
        titel   = partij.getTag(CaissaConstants.PGNTAG_EVENT);
      }
    }

    try {
      output  = Bestand.openUitvoerBestand(bestand + ".tex", charsetUit);

      // Maak de Matrix
      int           noSpelers = spelers.size();
      int           kolommen  = noSpelers * enkel;
      Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
      String[]      namen     = new String[noSpelers];
      double[][]    matrix    = new double[noSpelers][noSpelers * enkel];
      int           i         = 0;
      for (String speler  : spelers) {
        namen[i]  = speler;
        for (int j = 0; j < kolommen; j++) {
          matrix[i][j] = -1.0;
        }
        i++;
      }
      Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
      for (i = 0; i < noSpelers; i++) {
        punten[i] = new Spelerinfo();
        punten[i].setNaam(namen[i]);
      }

      // Bepaal de score en weerstandspunten.
      for (PGN partij: partijen) {
        String  wit     = partij.getTag(CaissaConstants.PGNTAG_WHITE);
        String  zwart   = partij.getTag(CaissaConstants.PGNTAG_BLACK);
        if (partij.isRanked()
            && !"bye".equalsIgnoreCase(wit)
            && !"bye".equalsIgnoreCase(zwart)) {
          int     ronde   = 1;
          try {
            ronde = Integer.valueOf(partij.getTag(CaissaConstants.PGNTAG_ROUND))
                           .intValue();
          } catch (NumberFormatException nfe) {
            ronde = 1;
          }
          String  uitslag = partij.getTag(CaissaConstants.PGNTAG_RESULT);
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
            matrix[iWit][iZwart * enkel] =
              Math.max(matrix[iWit][iZwart * enkel], 0.0) + 1.0;
            punten[iZwart].addPartij();
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0);
          } else if ("1/2-1/2".equals(uitslag)) {
            punten[iWit].addPartij();
            punten[iWit].addPunt(0.5);
            matrix[iWit][iZwart * enkel] =
              Math.max(matrix[iWit][iZwart * enkel], 0.0) + 0.5;
            punten[iZwart].addPartij();
            punten[iZwart].addPunt(0.5);
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 0.5;
          } else if ("0-1".equals(uitslag)) {
            punten[iWit].addPartij();
            matrix[iWit][iZwart * enkel] = 0.0;
              Math.max(matrix[iWit][iZwart * enkel], 0.0);
            punten[iZwart].addPartij();
            punten[iZwart].addPunt(1.0);
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 1.0;
          }
        }
      }
      // Bereken Weerstandspunten
      for (i = 0; i < noSpelers; i++) {
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
      for (i = 0; i < noSpelers; i++) {
        stand[Arrays.binarySearch(namen, punten[i].getNaam(),
                                  String.CASE_INSENSITIVE_ORDER)] = i;
      }

      // Maak de Matrix
      for (PGN partij: partijen) {
        String  wit     = partij.getTag(CaissaConstants.PGNTAG_WHITE);
        String  zwart   = partij.getTag(CaissaConstants.PGNTAG_BLACK);
        if (partij.isRanked()
            && !partij.isBye()) {
          int     ronde   = 1;
          try {
            ronde = Integer.valueOf(partij.getTag(CaissaConstants.PGNTAG_ROUND))
                           .intValue();
          } catch (NumberFormatException nfe) {
            ronde = 1;
          }
          String  uitslag = partij.getTag(CaissaConstants.PGNTAG_RESULT);
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
            matrix[iWit][iZwart * enkel] =
              Math.max(matrix[iWit][iZwart * enkel], 0.0) + 1.0;
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0);
          } else if ("1/2-1/2".equals(uitslag)) {
            matrix[iWit][iZwart * enkel] =
              Math.max(matrix[iWit][iZwart * enkel], 0.0) + 0.5;
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 0.5;
          } else if ("0-1".equals(uitslag)) {
            matrix[iWit][iZwart * enkel] =
              Math.max(matrix[iWit][iZwart * enkel], 0.0);
            matrix[iZwart][iWit * enkel + enkel - 1] =
              Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 1.0;
          }
        }
      }

      // Maak de .tex file
      output.write("\\documentclass[dutch,twocolumn,a4paper,10pt]{report}");
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
      output.write("%" + resourceBundle.getString("latex.splitsmelding"));
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
      output.write("\\ifpdf");
      output.newLine();
      output.write("\\pdfinfo{");
      output.newLine();
      output.write("   /Author (" + auteur + ")");
      output.newLine();
      output.write("   /Title  (" + titel + ")");
      output.newLine();
      if (DoosUtils.isNotBlankOrNull(keywords)) {
        output.write("   /Keywords (" + keywords + ")");
        output.newLine();
      }
      output.write("}");
      output.newLine();
      output.write("\\fi");
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
        for (i = 0; i < kolommen; i++) {
          output.write(" c | ");
        }
        output.write("r | r | r | }");
        output.newLine();
        output.write("    \\hline");
        output.newLine();
        output.write("    \\multicolumn{2}{|c|}{} ");
        for (i = 0; i < noSpelers; i++) {
          if (enkel == 1) {
            output.write(" & " + (i + 1));
          } else {
            output.write(" & \\multicolumn{2}{c|}{" + (i + 1) + "} ");
          }
        }
        output.write("& " + resourceBundle.getString("tag.punten")
                     + " & " + resourceBundle.getString("tag.partijen")
                     + " & " + resourceBundle.getString("tag.sb")
                     + " \\\\");
        output.newLine();
        output.write("    \\cline{3-" + (2 + kolommen) + "}");
        output.newLine();
        if (enkel == 2) {
          output.write("    \\multicolumn{2}{|c|}{} & ");
          for (i = 0; i < noSpelers; i++) {
            output.write(resourceBundle.getString("tag.wit") + " & " +
                         resourceBundle.getString("tag.zwart") + " & ");
          }
          output.write("& & \\\\");
          output.newLine();
        }
        output.write("    \\hline");
        output.newLine();
        for (i = 0; i < noSpelers; i++) {
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
              } else if (matrix[i][j] >= 1.0) {
                output.write("" + ((Double)matrix[i][j]).intValue()
                             + Utilities.kwart(matrix[i][j]));
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
        if (!partij.isBye()) {
          String wit    = partij.getTag(CaissaConstants.PGNTAG_WHITE);
          String zwart  = partij.getTag(CaissaConstants.PGNTAG_BLACK);
          String zetten = partij.getZuivereZetten();
          if (DoosUtils.isNotBlankOrNull(zetten)) {
            output.write("\\begin{chessgame}{" + wit + "}{"
                + zwart + "}{" + partij.getTag(CaissaConstants.PGNTAG_DATE)
                + "}{" + partij.getTag(CaissaConstants.PGNTAG_ROUND) + "}{"
                + partij.getTag("Result").replaceAll("1/2", "\\\\textonehalf")
                + "}{");
            String  eco = partij.getTag("ECO");
            if (DoosUtils.isNotBlankOrNull(eco)) {
              output.write(eco);
            }
            if (!partij.isRanked()) {
              output.write(" " +
                           resourceBundle.getString("tekst.buitencompetitie"));
            }
            output.write("}{"+ partij.getZuivereZetten()
                                     .replaceAll("#", "\\\\#"));
            output.write("}\\end{chessgame}");
            output.newLine();
          } else {
            if (!partij.getTag(CaissaConstants.PGNTAG_RESULT).equals("*")) {
              output.write("\\begin{chessempty}{" + wit + "}{"
                  + zwart + "}{" + partij.getTag(CaissaConstants.PGNTAG_DATE)
                  + "}{" + partij.getTag(CaissaConstants.PGNTAG_ROUND) + "}{"
                  + partij.getTag(CaissaConstants.PGNTAG_RESULT)
                          .replaceAll("1/2", "\\\\textonehalf") + "}{");
              if (!partij.isRanked()) {
                output.write(resourceBundle.getString("tekst.buitencompetitie"));
              }
              output.write("}\\end{chessempty}");
              output.newLine();
            }
          }
        }
      }

      output.write("\\end{document}");
      output.newLine();
      output.close();
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + bestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
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
      DoosUtils.foutNaarScherm(resourceBundle.getString("label.startdatum")
                               + " " + e.getLocalizedMessage() + " ["
                               + startDatum + "]");
    }

    if (!startDatum.equals(eindDatum)) {
      try {
        datum = Datum.toDate(eindDatum, CaissaConstants.PGN_DATUM_FORMAAT);
        titelDatum.append(" - ").append(Datum.fromDate(datum));
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(resourceBundle.getString("label.einddatum")
                                 + " " + e.getLocalizedMessage() + " ["
                                 + eindDatum + "]");
      }
    }

    return titelDatum.toString();
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToLatex ["
                         + resourceBundle.getString("label.optie")
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --auteur     ",
                         resourceBundle.getString("help.auteur"), 80);
    DoosUtils.naarScherm("  --bestand    ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin  ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --datum      ",
                         resourceBundle.getString("help.speeldatum"), 80);
    DoosUtils.naarScherm("  --enkel      ",
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm("  --halve      ",
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm("  --keywords   ",
                         resourceBundle.getString("help.keywords"), 80);
    DoosUtils.naarScherm("  --logo       ",
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm("  --matrix     ",
                         resourceBundle.getString("help.matrix"), 80);
    DoosUtils.naarScherm("  --titel      ",
                         resourceBundle.getString("help.documenttitel"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             "bestand"), 80);
    DoosUtils.naarScherm();
  }
}
