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
import eu.debooy.doosutils.Batchjob;
import static eu.debooy.doosutils.Batchjob.setBestandParameter;
import static eu.debooy.doosutils.Batchjob.setDirParameter;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.io.File;
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
public final class SpelerStatistiek extends Batchjob {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final  String    LATEX_HLINE   = "\\hline";
  private static final  String    LATEX_MCOLUMN = " & \\multicolumn{5}{c|}{ ";
  private static final  String    LATEX_NEWLINE = " \\\\";
  private static final  String    SPACES6       = "      ";
  private static final  String[]  UITSLAGEN   =
      new String[] {"1-0", "1/2-1/2", "0-1"};

  private static  String  einddatum   = "0000.00.00";
  private static  String  startdatum  = "9999.99.99";
  private static  int     verwerkt    = 0;

  private SpelerStatistiek() {}

  protected static String datumInTitel(String startdatum, String einddatum) {
    StringBuilder titelDatum  = new StringBuilder();
    Date          datum;
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
    Banner
        .printMarcoBanner(resourceBundle.getString("banner.spelerstatistiek"));

    if (!setParameters(args)) {
      return;
    }

    String  bestand       = parameters.get(CaissaTools.PAR_BESTAND);
    Map<String, int[]>
            items         = new TreeMap<>( );
    String  speler        = parameters.get(CaissaTools.PAR_SPELER);
    String  statistiektag = parameters.get(CaissaTools.PAR_TAG);

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(parameters.get(PAR_INVOERDIR)
                                            + bestand + EXT_PGN,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.forEach(partij -> {
      verwerkPartij(partij, items, speler, statistiektag);
    });

    schrijfLatex(bestand, items, speler);

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             bestand + EXT_TEX));
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

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar SpelerStatistiek ["
                         + getMelding(LBL_OPTIE)
                         + "] \\");
    DoosUtils.naarScherm("    --bestand=<"
                         + resourceBundle.getString("label.pgnbestand")
                         + "> --speler=<"
                         + resourceBundle.getString("label.spelernaam")
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 11),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 11),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 11),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 11),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_LOGO, 11),
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SPELER, 11),
                         resourceBundle.getString("help.statistiekspeler"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TAG, 11),
                         resourceBundle.getString("help.tag"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 11),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             "bestand", "speler"), 80);
    DoosUtils.naarScherm();
  }

  private static void schrijfLatex(String bestand, Map<String, int[]> items,
                                   String speler) {
    TekstBestand  output  = null;
    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(parameters.get(PAR_UITVOERDIR)
                                      + bestand + EXT_TEX)
                          .setCharset(parameters.get(PAR_CHARSETUIT))
                          .setLezen(false).build();

      schrijfLatexHeader(output, speler);

      int[] totaal  = new int[] {0,0,0,0,0,0};
      for (Entry<String, int[]> item : items.entrySet()) {
        int[] statistiek  = item.getValue();
        for (int i = 0; i < 6; i++) {
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
            .isNotBlankOrNull(parameters.containsKey(CaissaTools.PAR_LOGO))) {
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
            .isNotBlankOrNull(parameters.containsKey(CaissaTools.PAR_LOGO))) {
      output.write("    \\vspace{2in}");
      output.write("    \\includegraphics[width=6cm]{"
                   + parameters.get(CaissaTools.PAR_LOGO) + "} \\\\");
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
    StringBuilder lijn  = new StringBuilder();
    lijn.append(swapNaam(sleutel));
    // Als witspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[0], statistiek[1],
                                                  statistiek[2],
                                                  lijn.toString(), output));
    // Als zwartspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[3], statistiek[4],
                                                  statistiek[5],
                                                  lijn.toString(), output));
    // Totaal
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[0] + statistiek[3],
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

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_LOGO,
                                          CaissaTools.PAR_SPELER,
                                          CaissaTools.PAR_TAG,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_SPELER});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setDirParameter(arguments, CaissaTools.PAR_LOGO);
    setDirParameter(arguments, CaissaTools.PAR_SPELER);
    setDirParameter(arguments, CaissaTools.PAR_TAG);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
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
    String  uitslag   = partij.getTag(CaissaConstants.PGNTAG_RESULT);
    String  wit       = partij.getTag(CaissaConstants.PGNTAG_WHITE);
    String  zwart     = partij.getTag(CaissaConstants.PGNTAG_BLACK);
    int i = 0;
    for (String s: UITSLAGEN) {
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
          int punt  = hulpdatum.indexOf('.');
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
