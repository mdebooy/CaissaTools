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
import java.util.Set;


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
    Set<String>     spelers     = new HashSet<String>();
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
    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    int       enkel     = 0;
    if (DoosConstants.WAAR.equalsIgnoreCase(arguments.getArgument("enkel"))) {
      enkel   = 1;
    }
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
      int           kolommen  = (enkel == 0 ? partijen.size()
                                            : noSpelers * enkel);
      Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
      String[]      namen     = new String[noSpelers];
      double[][]    matrix    = new double[noSpelers][kolommen];
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
          if ((enkel > 0 && ronde > noSpelers)
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
          // Zorgt ervoor dat de index binnen de limieten blijft.
          ronde--;
          if ("1-0".equals(uitslag)) {
            punten[iWit].addPartij();
            punten[iWit].addPunt(1.0);
            punten[iZwart].addPartij();
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0) + 1.0;
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0);
            } else {
              matrix[iWit][iZwart * enkel]  =
                Math.max(matrix[iWit][iZwart * enkel], 0.0) + 1.0;
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0);
            }
          } else if ("1/2-1/2".equals(uitslag)) {
            punten[iWit].addPartij();
            punten[iWit].addPunt(0.5);
            punten[iZwart].addPartij();
            punten[iZwart].addPunt(0.5);
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0) + 0.5;
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0) + 0.5;
            } else {
              matrix[iWit][iZwart * enkel]              =
                Math.max(matrix[iWit][iZwart * enkel], 0.0) + 0.5;
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 0.5;
            }
          } else if ("0-1".equals(uitslag)) {
            punten[iWit].addPartij();
            punten[iZwart].addPartij();
            punten[iZwart].addPunt(1.0);
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0);
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0) + 1.0;
            } else {
              matrix[iWit][iZwart * enkel]              = 0.0;
                Math.max(matrix[iWit][iZwart * enkel], 0.0);
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 1.0;
            }
          }
        }
      }
      // Bereken Weerstandspunten en herinitialiseer de matrix.
      for (i = 0; i < noSpelers; i++) {
        Double weerstandspunten = 0.0;
        for (int j = 0; j < kolommen; j++) {
          if (enkel > 0 && matrix[i][j] > 0.0) {
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

      // Maak de Matrix nogmaals vanwege de sortering die de volgorde van de
      // spelers aanpaste.
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
          if ((enkel > 0 && ronde > noSpelers)
              && (Arrays.binarySearch(halve, wit,
                                      String.CASE_INSENSITIVE_ORDER) > -1
                  || Arrays.binarySearch(halve, zwart,
                                         String.CASE_INSENSITIVE_ORDER) > -1)) {
            continue;
          }
          // Zorgt ervoor dat de index binnen de limieten blijft.
          ronde--;
          int   iWit    =
            stand[Arrays.binarySearch(namen, wit,
                                      String.CASE_INSENSITIVE_ORDER)];
          int   iZwart  =
            stand[Arrays.binarySearch(namen, zwart,
                                      String.CASE_INSENSITIVE_ORDER)];
          if ("1-0".equals(uitslag)) {
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0) + 1.0;
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0);
            } else {
              matrix[iWit][iZwart * enkel]  =
                Math.max(matrix[iWit][iZwart * enkel], 0.0) + 1.0;
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0);
            }
          } else if ("1/2-1/2".equals(uitslag)) {
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0) + 0.5;
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0) + 0.5;
            } else {
              matrix[iWit][iZwart * enkel]              =
                Math.max(matrix[iWit][iZwart * enkel], 0.0) + 0.5;
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 0.5;
            }
          } else if ("0-1".equals(uitslag)) {
            if (enkel == 0) {
              matrix[iWit][ronde]   =
                  Math.max(matrix[iWit][ronde], 0.0);
              matrix[iZwart][ronde] =
                  Math.max(matrix[iZwart][ronde], 0.0) + 1.0;
            } else {
              matrix[iWit][iZwart * enkel]              = 0.0;
                Math.max(matrix[iWit][iZwart * enkel], 0.0);
              matrix[iZwart][iWit * enkel + enkel - 1]  =
                  Math.max(matrix[iZwart][iWit * enkel + enkel - 1], 0.0) + 1.0;
            }
          }
        }
      }

      // Maak de .tex file
      Bestand.schrijfRegel(output, "\\documentclass[dutch,twocolumn,a4paper,10pt]{report}", 2);
      Bestand.schrijfRegel(output, "\\usepackage{skak}");
      Bestand.schrijfRegel(output, "\\usepackage{babel}");
      Bestand.schrijfRegel(output, "\\usepackage{color}");
      Bestand.schrijfRegel(output, "\\usepackage{colortbl}");
      Bestand.schrijfRegel(output, "\\usepackage[T1]{fontenc}");
      Bestand.schrijfRegel(output, "\\usepackage[pdftex]{graphicx}");
      Bestand.schrijfRegel(output, "\\usepackage{pdflscape}", 2);
      Bestand.schrijfRegel(output, "\\topmargin =0.mm");
      Bestand.schrijfRegel(output, "\\oddsidemargin =0.mm");
      Bestand.schrijfRegel(output, "\\evensidemargin =0.mm");
      Bestand.schrijfRegel(output, "\\headheight =0.mm");
      Bestand.schrijfRegel(output, "\\headsep =0.mm");
      Bestand.schrijfRegel(output, "\\textheight =265.mm");
      Bestand.schrijfRegel(output, "\\textwidth =165.mm");
      Bestand.schrijfRegel(output, "\\parindent =0.mm", 2);
      Bestand.schrijfRegel(output, "\\newcommand{\\chessgame}[7]{");
      Bestand.schrijfRegel(output, "  $\\circ$ \\textbf{#1} \\hfill Ronde {#4}\\\\");
      Bestand.schrijfRegel(output, "  $\\bullet$ \\textbf{#2}\\\\");
      Bestand.schrijfRegel(output, "  {#3} \\hfill {#6} \\hfill {#5}\\\\");
      Bestand.schrijfRegel(output, "  \\styleB");
      Bestand.schrijfRegel(output, "  \\newgame");
      Bestand.schrijfRegel(output, "  \\mainline{#7} {\\bf #5}");
      Bestand.schrijfRegel(output, "  \\[\\showboard\\]");
      Bestand.schrijfRegel(output, "  \\begin{center} \\hrule \\end{center}");
      Bestand.schrijfRegel(output, "}", 2);
      Bestand.schrijfRegel(output, "\\newcommand{\\chessempty}[6]{");
      Bestand.schrijfRegel(output, "  $\\circ$ \\textbf{#1} \\hfill Ronde {#4}\\\\");
      Bestand.schrijfRegel(output, "  $\\bullet$ \\textbf{#2}\\\\");
      Bestand.schrijfRegel(output, "  {#3} \\hfill {#6} \\hfill {#5}\\\\");
      Bestand.schrijfRegel(output, "  \\begin{center} \\hrule \\end{center}");
      Bestand.schrijfRegel(output, "}", 2);
      Bestand.schrijfRegel(output, "%" + resourceBundle.getString("latex.splitsmelding"));
      Bestand.schrijfRegel(output, "\\raggedbottom \\topskip 1\\topskip plus1000pt % like "
                   + "\\raggedbottom; moreso \\def\\need#1{\\vskip #1"
                   + "\\penalty0 \\vskip-#1\\relax}", 2);
      Bestand.schrijfRegel(output, "\\title{" + titel + "}");
      Bestand.schrijfRegel(output, "\\author{" + auteur + "}");
      Bestand.schrijfRegel(output, "\\date{" + datum + "}", 2);
      Bestand.schrijfRegel(output, "\\ifpdf");
      Bestand.schrijfRegel(output, "\\pdfinfo{");
      Bestand.schrijfRegel(output, "   /Author (" + auteur + ")");
      Bestand.schrijfRegel(output, "   /Title  (" + titel + ")");
      if (DoosUtils.isNotBlankOrNull(keywords)) {
        Bestand.schrijfRegel(output, "   /Keywords (" + keywords + ")");
      }
      Bestand.schrijfRegel(output, "}");
      Bestand.schrijfRegel(output, "\\fi", 2);
      Bestand.schrijfRegel(output, "\\begin{document}");
      if (DoosUtils.isNotBlankOrNull(logo)) {
        Bestand.schrijfRegel(output, "\\DeclareGraphicsExtensions{.pdf,.png,.gif,.jpg}");
      }
      Bestand.schrijfRegel(output, "\\begin{titlepage}");
      Bestand.schrijfRegel(output, "  \\begin{center}");
      Bestand.schrijfRegel(output, "    \\huge " + titel + " \\\\");
      Bestand.schrijfRegel(output, "    \\vspace{1in}");
      Bestand.schrijfRegel(output, "    \\large " + auteur + " \\\\");
      if (DoosUtils.isNotBlankOrNull(logo)) {
        Bestand.schrijfRegel(output, "    \\vspace{2in}");
        Bestand.schrijfRegel(output, "    \\includegraphics[width=6cm]{"+ logo + "} \\\\");
      }
      Bestand.schrijfRegel(output, "    \\vspace{1in}");
      Bestand.schrijfRegel(output, "    \\large " + datumInTitel(startDatum, eindDatum)
                   + " \\\\");
      Bestand.schrijfRegel(output, "  \\end{center}");
      Bestand.schrijfRegel(output, "\\end{titlepage}");
      Bestand.schrijfRegel(output, "\\topmargin =-15.mm");
      if (DoosConstants.WAAR.equalsIgnoreCase(metMatrix)) {
        Bestand.schrijfRegel(output, "\\begin{landscape}");
        Bestand.schrijfRegel(output, "  \\begin{center}");
        Bestand.schrijfRegel(output, "    \\begin{tabular} { | c | l | ", 0);
        for (i = 0; i < kolommen; i++) {
          Bestand.schrijfRegel(output, " c | ", 0);
        }
        Bestand.schrijfRegel(output, "r | r | r | }");
        Bestand.schrijfRegel(output, "    \\hline");
        Bestand.schrijfRegel(output, "    \\multicolumn{2}{|c|}{} ", 0);
        for (i = 0; i < (enkel == 0 ? kolommen : noSpelers); i++) {
          if (enkel < 2) {
            Bestand.schrijfRegel(output, " & " + (i + 1), 0);
          } else {
            Bestand.schrijfRegel(output, " & \\multicolumn{2}{c|}{" + (i + 1) + "} ", 0);
          }
        }
        Bestand.schrijfRegel(output, "& " + resourceBundle.getString("tag.punten"), 0);
        if (enkel > 0) {
          Bestand.schrijfRegel(output, " & " + resourceBundle.getString("tag.partijen")
                       + " & " + resourceBundle.getString("tag.sb"), 0);
        }
        Bestand.schrijfRegel(output, " \\\\");
        Bestand.schrijfRegel(output, "    \\cline{3-" + (2 + kolommen) + "}");
        if (enkel == 2) {
          Bestand.schrijfRegel(output, "    \\multicolumn{2}{|c|}{} & ", 0);
          for (i = 0; i < noSpelers; i++) {
            Bestand.schrijfRegel(output, resourceBundle.getString("tag.wit") + " & " +
                         resourceBundle.getString("tag.zwart") + " & ", 0);
          }
          Bestand.schrijfRegel(output, "& & \\\\");
        }
        Bestand.schrijfRegel(output, "    \\hline");
        for (i = 0; i < noSpelers; i++) {
          if (enkel == 0) {
            Bestand.schrijfRegel(output, "\\multicolumn{2}{|l|}{" + punten[i].getNaam() + "} & ", 0);
          } else {
            Bestand.schrijfRegel(output, (i + 1) + " & " + punten[i].getNaam() + " & ", 0);
          }
          for (int j = 0; j < kolommen; j++) {
            if (enkel > 0) {
              if (i == j / enkel) {
                Bestand.schrijfRegel(output, "\\multicolumn{1}"
                             + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ", 0);
                continue;
              } else {
                if ((j / enkel) * enkel != j ) {
                  Bestand.schrijfRegel(output, "\\multicolumn{1}"
                               + "{>{\\columncolor[rgb]{0.8,0.8,0.8}}c|}{", 0);
                }
              }
            }
            if (matrix[i][j] == 0.0) {
              Bestand.schrijfRegel(output, "0", 0);
            } else if (matrix[i][j] == 0.5) {
              Bestand.schrijfRegel(output, "\\textonehalf", 0);
            } else if (matrix[i][j] >= 1.0) {
              Bestand.schrijfRegel(output, "" + ((Double)matrix[i][j]).intValue()
                           + Utilities.kwart(matrix[i][j]), 0);
            }
            if (enkel > 0 && (j / enkel) * enkel != j ) {
              Bestand.schrijfRegel(output, "}", 0);
            }
            Bestand.schrijfRegel(output, " & ", 0);
          }
          Bestand.schrijfRegel(output, punten[i].getPunten().intValue()
                       + Utilities.kwart(punten[i].getPunten()), 0);
          if (enkel > 0) {
            Bestand.schrijfRegel(output, " & " + punten[i].getPartijen() + " & ", 0);
            Bestand.schrijfRegel(output, punten[i].getWeerstandspunten().intValue()
                         + Utilities.kwart(punten[i].getWeerstandspunten()), 0);
          }
          Bestand.schrijfRegel(output, " \\\\");
          Bestand.schrijfRegel(output, "    \\hline");
        }
        Bestand.schrijfRegel(output, "    \\end{tabular}");
        Bestand.schrijfRegel(output, "  \\end{center}");
        Bestand.schrijfRegel(output, "\\end{landscape}");
        Bestand.schrijfRegel(output, "\\newpage");
      }

      for (PGN partij: partijen) {
        if (!partij.isBye()) {
          String wit    = partij.getTag(CaissaConstants.PGNTAG_WHITE);
          String zwart  = partij.getTag(CaissaConstants.PGNTAG_BLACK);
          String zetten = partij.getZuivereZetten();
          if (DoosUtils.isNotBlankOrNull(zetten)) {
            Bestand.schrijfRegel(output, "\\begin{chessgame}{" + wit + "}{"
                + zwart + "}{" + partij.getTag(CaissaConstants.PGNTAG_DATE)
                + "}{" + partij.getTag(CaissaConstants.PGNTAG_ROUND) + "}{"
                + partij.getTag("Result").replaceAll("1/2", "\\\\textonehalf")
                + "}{", 0);
            String  eco = partij.getTag("ECO");
            if (DoosUtils.isNotBlankOrNull(eco)) {
              Bestand.schrijfRegel(output, eco, 0);
            }
            if (!partij.isRanked()) {
              Bestand.schrijfRegel(output, " " +
                           resourceBundle.getString("tekst.buitencompetitie"), 0);
            }
            Bestand.schrijfRegel(output, "}{"+ partij.getZuivereZetten()
                                     .replaceAll("#", "\\\\#"), 0);
            Bestand.schrijfRegel(output, "}\\end{chessgame}");
          } else {
            if (!partij.getTag(CaissaConstants.PGNTAG_RESULT).equals("*")) {
              Bestand.schrijfRegel(output, "\\begin{chessempty}{" + wit + "}{"
                  + zwart + "}{" + partij.getTag(CaissaConstants.PGNTAG_DATE)
                  + "}{" + partij.getTag(CaissaConstants.PGNTAG_ROUND) + "}{"
                  + partij.getTag(CaissaConstants.PGNTAG_RESULT)
                          .replaceAll("1/2", "\\\\textonehalf") + "}{", 0);
              if (!partij.isRanked()) {
                Bestand.schrijfRegel(output, resourceBundle.getString("tekst.buitencompetitie"), 0);
              }
              Bestand.schrijfRegel(output, "}\\end{chessempty}");
            }
          }
        }
      }

      Bestand.schrijfRegel(output, "\\end{document}");
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
                         + bestand + ".tex");
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Maakt de datum informatie voor de titel pagina.
   */
  protected static String datumInTitel(String startDatum, String eindDatum) {
    StringBuilder titelDatum  = new StringBuilder();
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
