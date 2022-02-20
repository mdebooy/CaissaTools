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
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public final class SpelerStatistiek extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static final  String    LATEX_HLINE   = "\\hline";
  private static final  String    LATEX_MCOLUMN = " & \\multicolumn{5}{c|}{ ";
  private static final  String    LATEX_NEWLINE = " \\\\";
  private static final  String    SPACES6       = "      ";
  private static final  String[]  UITSLAGEN   =
      new String[] {"1-0", "1/2-1/2", "0-1"};

  private static  String  einddatum   = "0000.00.00";
  private static  String  startdatum  = "9999.99.99";
  private static  int     verwerkt    = 0;

  protected SpelerStatistiek() {}

  protected static String datumInTitel(String startdatum, String einddatum) {
    var   titelDatum  = new StringBuilder();
    Date  datum;
    try {
      datum = Datum.toDate(startdatum, CaissaConstants.PGN_DATUM_FORMAAT);
      titelDatum.append(Datum.fromDate(datum));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(resourceBundle.getString("label.startdatum")
                               + " " + e.getLocalizedMessage() + " ["
                               + startdatum + "]");
    }

    if (!startdatum.equals(einddatum)) {
      try {
        datum = Datum.toDate(einddatum, CaissaConstants.PGN_DATUM_FORMAAT);
        titelDatum.append(" - ").append(Datum.fromDate(datum));
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(resourceBundle.getString("label.einddatum")
                                 + " " + e.getLocalizedMessage() + " ["
                                 + einddatum + "]");
      }
    }

    return titelDatum.toString();
  }

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_SPELERSTATISTIEK)
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    Map<String, int[]>
            items         = new TreeMap<>( );
    var     speler        = paramBundle.getString(CaissaTools.PAR_SPELER);
    var     statistiektag = paramBundle.getString(CaissaTools.PAR_TAG);

    Collection<PGN> partijen;
    try {
      partijen =
          CaissaUtils.laadPgnBestand(paramBundle
                                        .getBestand(CaissaTools.PAR_BESTAND));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.forEach(partij -> verwerkPartij(partij, items, speler,
                                             statistiektag));

    schrijfLatex(items, speler);

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             paramBundle
                                .getBestand(CaissaTools.PAR_BESTAND,
                                            BestandConstants.EXT_TEX)));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.verwerkt"),
                             verwerkt));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
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

  private static void schrijfLatex(Map<String, int[]> items, String speler) {
    TekstBestand  output  = null;
    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle
                                          .getBestand(CaissaTools.PAR_BESTAND,
                                                      BestandConstants.EXT_TEX))
                          .setLezen(false).build();

      schrijfLatexHeader(output, speler);

      var totaal  = new int[] {0,0,0,0,0,0};
      for (Entry<String, int[]> item : items.entrySet()) {
        var statistiek  = item.getValue();
        for (var i = 0; i < 6; i++) {
          totaal[i] += statistiek[i];
        }
        schrijfStatistiek(item.getKey(), statistiek, output);
      }
      schrijfStatistiek("Totaal", totaal, output);

      schrijfLatexFooter(output);

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
  }

  private static void schrijfLatexFooter(TekstBestand output)
      throws BestandException {
    output.write("    \\end{longtable}");
    output.write("  \\end{center}");
    output.write("\\end{landscape}");
    output.write("\\end{document}");
  }

  private static void schrijfLatexHeader(TekstBestand output, String speler)
      throws BestandException {
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
    if (DoosUtils
            .isNotBlankOrNull(paramBundle
                                  .containsParameter(CaissaTools.PAR_LOGO))) {
      output.write("\\DeclareGraphicsExtensions{.pdf,.png,.gif,.jpg}");
    }
    output.write("\\begin{titlepage}");
    output.write("  \\begin{center}");
    output.write("    \\huge "
                 + resourceBundle.getString("label.statistiekenvan")
                 + LATEX_NEWLINE);
    output.write("    \\vspace{1in}");
    output.write("    \\huge " + swapNaam(speler) + LATEX_NEWLINE);
    if (DoosUtils
            .isNotBlankOrNull(paramBundle
                                  .containsParameter(CaissaTools.PAR_LOGO))) {
      output.write("    \\vspace{2in}");
      output.write("    \\includegraphics[width=6cm]{"
                   + paramBundle.getString(CaissaTools.PAR_LOGO) + "} \\\\");
    }
    output.write("    \\vspace{1in}");
    output.write("    \\large " + datumInTitel(startdatum, einddatum)
                 + LATEX_NEWLINE);
    output.write("  \\end{center}");
    output.write("\\end{titlepage}");
    output.write("\\begin{landscape}");
    output.write("  \\begin{center}");

    output.write("    \\begin{longtable} { | l | r | r | r | r | r | r | r | r | r | r | r | r | r | r | r | }");
    output.write(SPACES6 + LATEX_HLINE);
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
    output.write(SPACES6 + LATEX_HLINE);
    output.write("      \\endhead");
  }

  private static void schrijfStatistiek(String sleutel, int[] statistiek,
                                        TekstBestand output)
      throws BestandException {
    var lijn  = new StringBuilder();
    lijn.append(swapNaam(sleutel));
    // Als witspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[0],
                                                    statistiek[1],
                                                    statistiek[2],
                                                    lijn.toString(), output));
    // Als zwartspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[3],
                                                    statistiek[4],
                                                    statistiek[5],
                                                    lijn.toString(), output));
    // Totaal
    lijn  =
        new StringBuilder(schrijfStatistiekDeel(statistiek[0] + statistiek[3],
                                                statistiek[1] + statistiek[4],
                                                statistiek[2] + statistiek[5],
                                                lijn.toString(), output));
    lijn.append(LATEX_NEWLINE);
    output.write(lijn.toString());
    output.write(SPACES6 + LATEX_HLINE);
  }

  private static String schrijfStatistiekDeel(int winst, int remise,
                                              int verlies, String prefix,
                                              TekstBestand output)
      throws BestandException {
    var format    = new DecimalFormat("0.00");
    Double punten = Double.valueOf(winst) + Double.valueOf(remise) / 2;
    var gespeeld  = winst + remise + verlies;
    var lijn      = new StringBuilder(prefix);

    if (gespeeld == 0) {
      lijn.append(" & & & & &");
    } else {
      lijn.append(" & ").append(winst)
          .append(" & ").append(remise)
          .append(" & ").append(verlies).append(" & ");
      if (punten != 0.5) {
        lijn.append(punten.intValue());
        output.write(lijn.toString());
        lijn  = new StringBuilder();
      }
      lijn.append(Utilities.kwart(punten)).append(" & ");
      lijn.append(format.format((punten / gespeeld) * 100)).append("\\%");
    }

    return lijn.toString();
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }

    return deel[1].trim() + " " + deel[0].trim();
  }

  private static void verwerkPartij(PGN partij, Map<String, int[]> items,
                                    String speler, String statistiektag) {
    String  hulpdatum;
    String  sleutel;
    var     uitslag   = partij.getTag(CaissaConstants.PGNTAG_RESULT);
    var     wit       = partij.getTag(CaissaConstants.PGNTAG_WHITE);
    var     zwart     = partij.getTag(CaissaConstants.PGNTAG_BLACK);
    var     i         = 0;
    for (var s: UITSLAGEN) {
      if (s.equals(uitslag)) {
        break;
      }
      i++;
    }

    if (speler.equals(wit) || speler.equals(zwart)) {
      verwerkt++;
      // Verwerk de 'datums'
      hulpdatum = partij.getTag(CaissaConstants.PGNTAG_EVENTDATE);
      if (DoosUtils.isNotBlankOrNull(hulpdatum)
          && hulpdatum.indexOf('?') < 0) {
        if (hulpdatum.compareTo(startdatum) < 0 ) {
          startdatum  = hulpdatum;
        }
        if (hulpdatum.compareTo(einddatum) > 0 ) {
          einddatum   = hulpdatum;
        }
      }
      hulpdatum = partij.getTag(CaissaConstants.PGNTAG_DATE);
      if (DoosUtils.isNotBlankOrNull(hulpdatum)
          && hulpdatum.indexOf('?') < 0) {
        if (hulpdatum.compareTo(startdatum) < 0 ) {
          startdatum  = hulpdatum;
        }
        if (hulpdatum.compareTo(einddatum) > 0 ) {
          einddatum   = hulpdatum;
        }
      }
      if (DoosUtils.isNotBlankOrNull(statistiektag)) {
        if ("Date".equals(statistiektag)) {
          var punt  = hulpdatum.indexOf('.');
          if (punt < 1) {
            sleutel = "????";
          } else {
            sleutel = hulpdatum.substring(0, punt);
          }
        } else {
          sleutel = DoosUtils.nullToEmpty(partij.getTag(statistiektag));
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
}
