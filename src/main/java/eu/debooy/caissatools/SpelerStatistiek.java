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
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public final class SpelerStatistiek {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private SpelerStatistiek() {}

  public static void execute(String[] args) {
    BufferedReader  input       = null;
    BufferedWriter  output      = null;
    int             partijen    = 0;
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();
    String[]        datumDeel   = null;
    String          eindDatum   = "0000.00.00";
    String          hulpDatum   = "";
    String          sleutel     = "";
    String          startDatum  = "9999.99.99";
    String[]        uitslagen   = new String[] {"1-0", "1/2-1/2", "0-1"};
    Map<String, int[]>
                    items       = new TreeMap<String, int[]>( );

    Banner.printBanner(resourceBundle.getString("banner.spelerstatistiek"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"bestand",  "charsetin", "charsetuit",
                                          "logo", "speler", "tag"});
    arguments.setVerplicht(new String[] {"bestand", "speler"});
    if (!arguments.isValid()) {
      help();
      return;
    }

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
    String    datum     = "";
    if (DoosUtils.isBlankOrNull(datum)) {
      try {
        datum = Datum.fromDate(new Date(), "dd/MM/yyyy HH:mm:ss");
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    String  logo          = arguments.getArgument("logo");
    String  speler        = arguments.getArgument("speler");
    String  statistiekTag = arguments.getArgument("tag");

    try {
      input   = Bestand.openInvoerBestand(bestand + ".pgn", charsetIn);
      output  = Bestand.openUitvoerBestand(bestand + ".tex", charsetUit);
      String line = input.readLine();
      // Zoek naar de eerste TAG
      while (null != line && !line.startsWith("[")) {
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
            DoosUtils.foutNaarScherm(e.getLocalizedMessage());
          }
          line = input.readLine();
        }

        String uitslag = partij.getTag("Result");
        while (null != line && !line.endsWith(uitslag)) {
          line = input.readLine();
        }

        // Verwerk de spelers
        String  wit   = partij.getTag("White");
        String  zwart = partij.getTag("Black");
        int i = 0;
        for (String s: uitslagen) {
          if (s.equals(uitslag)) {
            break;
          }
          i++;
        }

        if ("Date".equals(statistiekTag)) {
          datumDeel = hulpDatum.split("\\.");
          if (datumDeel.length == 0) {
            datumDeel  = new String[] {"", "", ""};
          }
        }

        if (speler.equals(wit)) {
          partijen++;
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
          if (DoosUtils.isNotBlankOrNull(statistiekTag)) {
            if ("Date".equals(statistiekTag)) {
              sleutel = datumDeel[0];
            } else { 
              sleutel = DoosUtils.nullToEmpty(partij.getTag(statistiekTag));
            }
          } else {
            sleutel = zwart;
          }
          int[] statistiek  = getStatistiek(sleutel, items);
          statistiek[i]++;
          items.put(sleutel, statistiek);
        } else if (speler.equals(zwart)) {
          partijen++;
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
          if (DoosUtils.isNotBlankOrNull(statistiekTag)) {
            if ("Date".equals(statistiekTag)) {
              sleutel = datumDeel[0];
            } else { 
              sleutel = DoosUtils.nullToEmpty(partij.getTag(statistiekTag));
            }
          } else {
            sleutel = wit;
          }
          int[] statistiek  = getStatistiek(sleutel, items);
          statistiek[5 - i]++;
          items.put(sleutel, statistiek);
        }
        

        // Zoek naar de eerste TAG
        while (null != line && !line.startsWith("[")) {
          line = input.readLine();
        }
      }

      // Maak de .tex file
      output.write("\\documentclass[dutch,a4,10pt]{report}");
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
      output.write("\\usepackage{longtable}");
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
      output.write("\\title{"
                   + resourceBundle.getString("label.statistieken")
                   + "}");
      output.newLine();
      output.write("\\author{" + speler + "}");
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
      output.write("    \\huge "
                   + resourceBundle.getString("label.statistiekenvan")
                   + " \\\\");
      output.newLine();
      output.write("    \\vspace{1in}");
      output.newLine();
      output.write("    \\huge " + swapNaam(speler) + " \\\\");
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
      output.write("\\begin{landscape}");
      output.newLine();
      output.write("  \\begin{center}");
      output.newLine();
      int[] totaal  = new int[] {0,0,0,0,0,0};

      output.write("    \\begin{longtable} { | l | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | }");
      output.newLine();
      output.write("      \\hline");
      output.newLine();
      output.write(" & \\multicolumn{5}{c|}{ "
                   + resourceBundle.getString("tekst.wit") + " } ");
      output.write(" & \\multicolumn{5}{c|}{ "
                   + resourceBundle.getString("tekst.zwart") + " } ");
      output.write(" & \\multicolumn{5}{c|}{ "
                   + resourceBundle.getString("tekst.totaal") + " } \\\\");
      output.newLine();
      output.write("      \\cline{2-16}");
      output.newLine();
      String  hoofding  = " & "
                          + resourceBundle.getString("tag.winst") + " & "
                          + resourceBundle.getString("tag.remise") + " & "
                          + resourceBundle.getString("tag.verlies") + " & "
                          + resourceBundle.getString("tag.totaal") + " & "
                          + resourceBundle.getString("tag.procent");
      output.write(hoofding + hoofding + hoofding + " \\\\");
      output.newLine();
      output.write("      \\hline");
      output.newLine();
      output.write("      \\endhead");
      output.newLine();
      for (String item: items.keySet()) {
        int[] statistiek  = items.get(item);
        for (int i = 0; i < 6; i++) {
          totaal[i] += statistiek[i];
        }
        printStatistiek(item, statistiek, output);
      }
      printStatistiek("Totaal", totaal, output);
      output.write("    \\end{longtable}");
      output.newLine();
      output.write("  \\end{center}");
      output.newLine();
      output.write("\\end{landscape}");
      output.newLine();
      output.write("\\end{document}");
      output.newLine();
      input.close();
      output.close();
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (input != null) {
          input.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + bestand + ".tex");
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen);
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
    DoosUtils.naarScherm("  --logo       ",
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm("  --speler     ",
                         resourceBundle.getString("help.statistiekspeler"), 80);
    DoosUtils.naarScherm("  --tag        ",
                         resourceBundle.getString("help.tag"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramsverplicht"),
                             "bestand", "speler"), 80);
    DoosUtils.naarScherm();
  }

  private static void printStatistiek(String sleutel, int[] statistiek,
                                      BufferedWriter output)
      throws IOException {
    output.write(swapNaam(sleutel));
    // Als witspeler
    printStatistiekDeel(statistiek[0], statistiek[1], statistiek[2], output);
    // Als zwartspeler
    printStatistiekDeel(statistiek[3], statistiek[4], statistiek[5], output);
    // Totaal
    printStatistiekDeel(statistiek[0] + statistiek[3],
                        statistiek[1] + statistiek[4],
                        statistiek[2] + statistiek[5], output);
    output.write(" \\\\");
    output.newLine();
    output.write("      \\hline");
    output.newLine();
  }

  private static void printStatistiekDeel(int winst, int remise, int verlies,
                                          BufferedWriter output)
      throws IOException {
    DecimalFormat format    = new DecimalFormat("0.00");
    Double        punten    = Double.valueOf(winst) + Double.valueOf(remise) / 2;
    int           gespeeld  = winst + remise + verlies;

    if (gespeeld == 0) {
      output.write(" & & & & &");
    } else {
      output.write(" & " + winst + " & " + remise + " & " + verlies + " & " );
      if (punten != 0.5) {
        output.write("" + punten.intValue());
      }
      output.write("" + Utilities.kwart(punten) + " & ");
      output.write("" + format.format((punten / gespeeld) * 100) + "\\%");
    }
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }
    
    return deel[1].trim() + " " + deel[0].trim();
  }
}
