/**
 * Copyright 2010 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
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
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public final class SpelerStatistiek {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final  String  LATEX_HLINE   = "\\hline";
  private static final  String  LATEX_MCOLUMN = " & \\multicolumn{5}{c|}{ ";
  private static final  String  LATEX_NEWLINE = " \\\\";

  private SpelerStatistiek() {}

  public static void execute(String[] args) throws PgnException {
    TekstBestand  output      = null;
    int           verwerkt    = 0;
    String        charsetIn   = Charset.defaultCharset().name();
    String        charsetUit  = Charset.defaultCharset().name();
    String        eindDatum   = "0000.00.00";
    List<String>  fouten      = new ArrayList<String>();
    String        hulpDatum   = "";
    String        sleutel     = "";
    String        startDatum  = "9999.99.99";
    String[]      uitslagen   = new String[] {"1-0", "1/2-1/2", "0-1"};
    Map<String, int[]>
                  items       = new TreeMap<String, int[]>( );

    Banner.printBanner(resourceBundle.getString("banner.spelerstatistiek"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.LOGO,
                                          CaissaTools.SPELER,
                                          CaissaTools.TAG,
                                          CaissaTools.UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND,
                                         CaissaTools.SPELER});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String  bestand = arguments.getArgument(CaissaTools.BESTAND);
    if (bestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                       CaissaTools.BESTAND));
    }
    if (bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand = bestand.substring(0, bestand.length() - 4);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn   = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit  = arguments.getArgument(CaissaTools.CHARDSETUIT);
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument(CaissaTools.INVOERDIR)) {
      invoerdir   = arguments.getArgument(CaissaTools.INVOERDIR);
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    String  logo          = arguments.getArgument(CaissaTools.LOGO);
    String  speler        = arguments.getArgument(CaissaTools.SPELER);
    String  statistiekTag = arguments.getArgument(CaissaTools.TAG);

    String  datum         = "";
    if (DoosUtils.isBlankOrNull(datum)) {
      try {
        datum = Datum.fromDate(new Date(), "dd/MM/yyyy HH:mm:ss");
      } catch (ParseException e) {
        fouten.add(e.getLocalizedMessage());
      }
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand
                                     + CaissaTools.EXTENSIE_PGN,
                                   charsetIn);

    for (PGN partij: partijen) {
      String  uitslag = partij.getTag(CaissaConstants.PGNTAG_RESULT);
      String  wit     = partij.getTag(CaissaConstants.PGNTAG_WHITE);
      String  zwart   = partij.getTag(CaissaConstants.PGNTAG_BLACK);
      int i = 0;
      for (String s: uitslagen) {
        if (s.equals(uitslag)) {
          break;
        }
        i++;
      }

      if (speler.equals(wit) || speler.equals(zwart)) {
        verwerkt++;
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
        if (DoosUtils.isNotBlankOrNull(statistiekTag)) {
          if ("Date".equals(statistiekTag)) {
            int punt  = hulpDatum.indexOf('.');
            if (punt < 1) {
              sleutel = "????";
            } else {
              sleutel = hulpDatum.substring(0, punt);
            }
          } else { 
            sleutel = DoosUtils.nullToEmpty(partij.getTag(statistiekTag));
          }
        } else {
          if (speler.equals(wit)) {
            sleutel = zwart;
          } else {
            sleutel = wit;
          }
        }
        int[] statistiek  = getStatistiek(sleutel, items);
        if (speler.equals(wit)) {
          statistiek[i]++;
        } else {
          statistiek[5 - i]++;
        }
        items.put(sleutel, statistiek);
      }
    }

    // Maak de .tex file
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoerdir + File.separator
                                            + bestand
                                            + CaissaTools.EXTENSIE_TEX)
                                .setCharset(charsetUit)
                                .setLezen(false).build();
      output.write("\\documentclass[dutch,a4paper,10pt]{report}");
      output.write("");
      output.write("\\usepackage{babel}");
      output.write("\\usepackage{color}");
      output.write("\\usepackage{colortbl}");
      output.write("\\usepackage{longtable}");
      output.write("\\usepackage[T1]{fontenc}");
      output.write("\\usepackage{textcomp}");
      output.write("\\usepackage[pdftex]{graphicx}");
      output.write("\\usepackage{pdflscape}");
      output.write("");
      output.write("\\topmargin =0.mm");
      output.write("\\oddsidemargin =0.mm");
      output.write("\\evensidemargin =0.mm");
      output.write("\\headheight =0.mm");
      output.write("\\headsep =0.mm");
      output.write("\\textheight =265.mm");
      output.write("\\textwidth =165.mm");
      output.write("\\parindent =0.mm");
      output.write("");
      output.write("\\title{" + resourceBundle.getString("label.statistieken")
                   + "}");
      output.write("\\author{" + speler + "}");
      output.write("\\date{\\today{}}");
      output.write("");
      output.write("\\begin{document}");
      if (DoosUtils.isNotBlankOrNull(logo)) {
        output.write("\\DeclareGraphicsExtensions{.pdf,.png,.gif,.jpg}");
      }
      output.write("\\begin{titlepage}");
      output.write("  \\begin{center}");
      output.write("    \\huge "
                   + resourceBundle.getString("label.statistiekenvan")
                   + LATEX_NEWLINE);
      output.write("    \\vspace{1in}");
      output.write("    \\huge " + swapNaam(speler) + LATEX_NEWLINE);
      if (DoosUtils.isNotBlankOrNull(logo)) {
        output.write("    \\vspace{2in}");
        output.write("    \\includegraphics[width=6cm]{"+ logo + "} \\\\");
      }
      output.write("    \\vspace{1in}");
      output.write("    \\large " + datumInTitel(startDatum, eindDatum)
                   + LATEX_NEWLINE);
      output.write("  \\end{center}");
      output.write("\\end{titlepage}");
      output.write("\\begin{landscape}");
      output.write("  \\begin{center}");
      int[] totaal  = new int[] {0,0,0,0,0,0};

      output.write("    \\begin{longtable} { | l | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | }");
      output.write("      " + LATEX_HLINE);
      output.write(LATEX_MCOLUMN
                   + resourceBundle.getString("tekst.wit") + " } "
                   + LATEX_MCOLUMN
                   + resourceBundle.getString("tekst.zwart") + " } "
                   + LATEX_MCOLUMN
                   + resourceBundle.getString("tekst.totaal")
                   + " } \\\\");
      output.write("      \\cline{2-16}");
      String  hoofding  = " & "
                          + resourceBundle.getString("tag.winst") + " & "
                          + resourceBundle.getString("tag.remise") + " & "
                          + resourceBundle.getString("tag.verlies") + " & "
                          + resourceBundle.getString("tag.totaal") + " & "
                          + resourceBundle.getString("tag.procent");
      output.write(hoofding + hoofding + hoofding + LATEX_NEWLINE);
      output.write("      " + LATEX_HLINE);
      output.write("      \\endhead");
      for (Entry<String, int[]> item : items.entrySet()) {
        int[] statistiek  = item.getValue();
        for (int i = 0; i < 6; i++) {
          totaal[i] += statistiek[i];
        }
        printStatistiek(item.getKey(), statistiek, output);
      }
      printStatistiek("Totaal", totaal, output);
      output.write("    \\end{longtable}");
      output.write("  \\end{center}");
      output.write("\\end{landscape}");
      output.write("\\end{document}");
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + bestand + ".tex");
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.verwerkt") + " "
                         + verwerkt);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Maakt de datum informatie voor de titel pagina.
   * 
   * @param startDatum
   * @param eindDatum
   * @return
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
   * Geeft een tabel met resultaten voor de sleutel. De elementen zijn:
   *  wit-wint, remise, zwart-wint, wit-wint, remise, zwart-wint
   * De eerste 3 elementen zijn voor gespeeld met wit en de laatste 3 voor
   * gespeeld met zwart.
   * 
   * @param sleutel
   * @param tabel
   * @return
   */
  protected static int[] getStatistiek(String sleutel,
                                       Map<String, int[]> tabel) {
    if (tabel.containsKey(sleutel)) {
      return tabel.get(sleutel);
    }

    return new int[] {0,0,0,0,0,0};
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar SpelerStatistiek ["
                         + resourceBundle.getString("label.optie")
                         + "] \\");
    DoosUtils.naarScherm("    --bestand=<"
                         + resourceBundle.getString("label.pgnbestand")
                         + "> --speler=<"
                         + resourceBundle.getString("label.spelernaam")
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand    ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin  ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --invoerdir  ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --logo       ",
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm("  --speler     ",
                         resourceBundle.getString("help.statistiekspeler"), 80);
    DoosUtils.naarScherm("  --tag        ",
                         resourceBundle.getString("help.tag"), 80);
    DoosUtils.naarScherm("  --uitvoerdir ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramsverplicht"),
                             "bestand", "speler"), 80);
    DoosUtils.naarScherm();
  }

  /**
   * Print de statistieken per groep.
   * 
   * @param sleutel
   * @param statistiek
   * @param output
   * @throws IOException
   */
  private static void printStatistiek(String sleutel, int[] statistiek,
                                      TekstBestand output)
      throws BestandException {
    StringBuilder lijn  = new StringBuilder();
    lijn.append(swapNaam(sleutel));
    // Als witspeler
    lijn  = new StringBuilder(printStatistiekDeel(statistiek[0], statistiek[1],
                                                  statistiek[2],
                                                  lijn.toString(), output));
    // Als zwartspeler
    lijn  = new StringBuilder(printStatistiekDeel(statistiek[3], statistiek[4],
                                                  statistiek[5],
                                                  lijn.toString(), output));
    // Totaal
    lijn  = new StringBuilder(printStatistiekDeel(statistiek[0] + statistiek[3],
                                                  statistiek[1] + statistiek[4],
                                                  statistiek[2] + statistiek[5],
                                                  lijn.toString(), output));
    lijn.append(LATEX_NEWLINE);
    output.write(lijn.toString());
    output.write("      " + LATEX_HLINE);
  }

  /**
   * Print een gedeelte van de statistieken per groep.
   * 
   * @param winst
   * @param remise
   * @param verlies
   * @param output
   * @throws IOException
   */
  private static String printStatistiekDeel(int winst, int remise, int verlies,
                                            String prefix, TekstBestand output)
      throws BestandException {
    DecimalFormat format    = new DecimalFormat("0.00");
    Double        punten    = Double.valueOf(winst)
                              + Double.valueOf(remise) / 2;
    int           gespeeld  = winst + remise + verlies;
    StringBuilder lijn      = new StringBuilder(prefix);

    if (gespeeld == 0) {
      lijn.append(" & & & & &");
    } else {
      lijn.append(" & " + winst + " & " + remise + " & " + verlies + " & ");
      if (punten != 0.5) {
        lijn.append(punten.intValue());
        output.write(lijn.toString());
        lijn  = new StringBuilder();
      }
      lijn.append(Utilities.kwart(punten) + " & ");
      lijn.append(format.format((punten / gespeeld) * 100) + "\\%");
    }

    return lijn.toString();
  }

  /**
   * Zet de naam in de juiste volgorde. Eerst de voornaam (van achter de komma)
   * en dan de achternaam (van voor de komma).
   * 
   * @param naam
   * @return
   */
  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }
    
    return deel[1].trim() + " " + deel[0].trim();
  }
}
