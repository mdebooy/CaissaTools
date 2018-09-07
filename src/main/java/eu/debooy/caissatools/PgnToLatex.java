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
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;


/**
 * Versie 526 is de laatste goede.
 * @author Marco de Booij
 */
public final class PgnToLatex {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final String HLINE     = "\\hline";
  private static final String KEYWORDS  = "K";
  private static final String LOGO      = "L";
  private static final String MATRIX    = "M";
  private static final String NORMAAL   = "N";
  private static final String PARTIJEN  = "P";
  private static final String PERIODE   = "Q";
  
  PgnToLatex() {}

  public static void execute(String[] args) throws PgnException {
    int           aantalPartijen  = 0;
    String        charsetIn       = Charset.defaultCharset().name();
    String        charsetUit      = Charset.defaultCharset().name();
    String        eindDatum       = "0000.00.00";
    List<String>  fouten          = new ArrayList<String>();
    String        hulpDatum       = "";
    TekstBestand  output          = null;
    TekstBestand  texInvoer       = null;
    String        startDatum      = "9999.99.99";
    List<String>  template        = new ArrayList<String>();

    Banner.printBanner(resourceBundle.getString("banner.pgntolatex"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.AUTEUR,
                                          CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.DATUM,
                                          CaissaTools.ENKEL,
                                          CaissaTools.HALVE,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.KEYWORDS,
                                          CaissaTools.LOGO,
                                          CaissaTools.MATRIX,
                                          CaissaTools.MATRIXOPSTAND,
                                          CaissaTools.TEMPLATE,
                                          CaissaTools.TITEL,
                                          CaissaTools.UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    auteur  = arguments.getArgument(CaissaTools.AUTEUR);
    String[]  bestand = arguments.getArgument(CaissaTools.BESTAND)
                                 .replaceAll(CaissaTools.EXTENSIE_PGN, "")
                                 .split(";");
    for (int i = 0; i < bestand.length; i++) {
      if (bestand[i].contains(File.separator)) {
        fouten.add(
            MessageFormat.format(
                resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                         bestand[i]));
      }
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
    String    datum         = arguments.getArgument(CaissaTools.DATUM);
    if (DoosUtils.isBlankOrNull(datum)) {
      try {
        datum = Datum.fromDate(new Date(), "dd/MM/yyyy HH:mm:ss");
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    // 1 is default waarde.
    int       enkel         = 1;
    if (arguments.hasArgument(CaissaTools.ENKEL)) {
      switch (arguments.getArgument(CaissaTools.ENKEL)) {
      case DoosConstants.WAAR:
        enkel = CaissaConstants.TOERNOOI_ENKEL;
        break;
      case DoosConstants.ONWAAR:
        enkel = CaissaConstants.TOERNOOI_DUBBEL;
        break;
      default:
        enkel = CaissaConstants.TOERNOOI_MATCH;
        break;
      }
    }
    String[]  halve         =
      DoosUtils.nullToEmpty(arguments.getArgument(CaissaTools.HALVE))
               .split(";");
    String    keywords      = arguments.getArgument(CaissaTools.KEYWORDS);
    String    logo          = arguments.getArgument(CaissaTools.LOGO);
    boolean   metMatrix     = true;
    if (arguments.hasArgument(CaissaTools.MATRIX)) {
      metMatrix =
          DoosConstants.WAAR
              .equalsIgnoreCase(arguments.getArgument(CaissaTools.MATRIX));
    }
    boolean   matrixOpStand = false;
    if (arguments.hasArgument(CaissaTools.MATRIXOPSTAND)) {
      matrixOpStand =
          DoosConstants.WAAR.equalsIgnoreCase(
              arguments.getArgument(CaissaTools.MATRIXOPSTAND));
    }
    if (arguments.hasArgument(CaissaTools.TEMPLATE)) {
      File  tex = new File(arguments.getArgument(CaissaTools.TEMPLATE));
      if (!tex.exists()) {
        fouten.add(MessageFormat.format(
            resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
                                     arguments.getArgument(
                                         CaissaTools.TEMPLATE)));
      }
    }
    String  titel       = arguments.getArgument(CaissaTools.TITEL);

    if (bestand.length > 1) {
      if (halve.length > 1) {
        fouten.add(resourceBundle.getString(CaissaTools.ERR_HALVE));
      }
      if (DoosUtils.isBlankOrNull(auteur)
          || DoosUtils.isBlankOrNull(titel)) {
        fouten.add(resourceBundle.getString(CaissaTools.ERR_BIJBESTAND));
      }
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    int beginBody = -1;
    int eindeBody = -1;
    try {
      String  regel;
      if (arguments.hasArgument(CaissaTools.TEMPLATE)) {
        texInvoer = new TekstBestand.Builder()
                                    .setBestand(arguments
                                        .getArgument(CaissaTools.TEMPLATE))
                                    .setCharset(charsetIn).build();
      } else {
        texInvoer = new TekstBestand.Builder()
                                    .setBestand("Caissa.tex")
                                    .setClassLoader(
                                        PgnToLatex.class.getClassLoader())
                                    .setCharset(charsetIn).build();
      }
      while (texInvoer.hasNext()) {
        regel = texInvoer.next();
        if (regel.startsWith("%@IncludeStart Body")) {
          beginBody = template.size();
        }
        if (regel.startsWith("%@IncludeEind Body")) {
          eindeBody = template.size();
        }
        template.add(regel);
      }

      output  = new TekstBestand.Builder()
                                .setBestand(uitvoerdir + File.separator
                                            + bestand[0]
                                            + CaissaTools.EXTENSIE_TEX)
                                .setCharset(charsetUit)
                                .setLezen(false).build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (texInvoer != null) {
          texInvoer.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    for (int i = 0; i < bestand.length; i++) {
      Collection<PGN>     partijen  =
          new TreeSet<PGN>(new PGN.byEventComparator());
      Map<String, String> texPartij = new HashMap<String, String>();
      Set<String>         spelers   = new HashSet<String>();

      partijen.addAll(CaissaUtils.laadPgnBestand(invoerdir + File.separator
                                                   + bestand[i]
                                                   + CaissaTools.EXTENSIE_PGN,
                                                 charsetIn));

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
        int           noSpelers = spelers.size();
        int           kolommen  = (enkel == CaissaConstants.TOERNOOI_MATCH
                                      ? partijen.size() : noSpelers * enkel);
        double[][]    matrix    = null;
        String[]      namen     = new String[noSpelers];
        Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
        // Maak de Matrix
        if (metMatrix) {
          int j = 0;
          for (String speler  : spelers) {
            namen[j++]  = speler;
          }

          // Initialiseer de Spelerinfo array.
          Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
          for (j = 0; j < noSpelers; j++) {
            punten[j] = new Spelerinfo();
            punten[j].setNaam(namen[j]);
          }

          // Bepaal de score en weerstandspunten.
          if (metMatrix) {
            matrix  = new double[noSpelers][kolommen];
            CaissaUtils.vulToernooiMatrix(partijen, punten, halve, matrix,
                                          enkel, matrixOpStand,
                                          CaissaConstants.TIEBREAK_SB);
          }
        }

        // Zet de te vervangen waardes.
        Map<String, String> parameters  = new HashMap<String, String>();
        parameters.put("Auteur", auteur);
        parameters.put("Datum", datum);
        if (DoosUtils.isNotBlankOrNull(keywords)) {
          parameters.put(CaissaTools.KEYWORDS, keywords);
        }
        if (DoosUtils.isNotBlankOrNull(logo)) {
          parameters.put(CaissaTools.LOGO, logo);
        }
        if (bestand.length == 1) {
          parameters.put("Periode", datumInTitel(startDatum, eindDatum));
        }
        parameters.put("Titel", titel);

        String  status  = NORMAAL;
        if (i == 0) {
          for (int j = 0; j < beginBody; j++) {
            status  = schrijf(template.get(j), status, output, punten, enkel,
                              matrix, kolommen, noSpelers, texPartij,
                              partijen, parameters);
          }
        }
        for (int j = beginBody; j < eindeBody; j++) {
          status  = schrijf(template.get(j), status, output, punten, enkel,
                            matrix, kolommen, noSpelers, texPartij,
                            partijen, parameters);
        }
        if (i == bestand.length - 1) {
          for (int j = eindeBody + 1; j < template.size(); j++) {
            status  = schrijf(template.get(j), status, output, punten, enkel,
                              matrix, kolommen, noSpelers, texPartij,
                              partijen, parameters);
          }
        }
        aantalPartijen  += partijen.size();
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    try {
      if (output != null) {
        output.close();
      }
    } catch (BestandException ex) {
      DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
    }

    for (int i = 0; i < bestand.length; i++) {
      DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                           + uitvoerdir + File.separator
                           + bestand[i] + CaissaTools.EXTENSIE_TEX);
    }
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + aantalPartijen);
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
    DoosUtils.naarScherm("  --auteur        ",
                         resourceBundle.getString("help.auteur"), 80);
    DoosUtils.naarScherm("  --bestand       ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("                  ",
                         resourceBundle.getString("help.bestanden"), 80);
    DoosUtils.naarScherm("  --charsetin     ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit    ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --datum         ",
                         resourceBundle.getString("help.speeldatum"), 80);
    DoosUtils.naarScherm("  --enkel         ",
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm("  --halve         ",
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm("  --invoerdir     ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --keywords      ",
                         resourceBundle.getString("help.keywords"), 80);
    DoosUtils.naarScherm("  --logo          ",
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm("  --matrix        ",
                         resourceBundle.getString("help.matrix"), 80);
    DoosUtils.naarScherm("  --matrixopstand ",
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm("  --template      ",
                         resourceBundle.getString("help.template"), 80);
    DoosUtils.naarScherm("  --titel         ",
                         resourceBundle.getString("help.documenttitel"), 80);
    DoosUtils.naarScherm("  --uitvoerdir    ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             CaissaTools.BESTAND), 80);
    DoosUtils.naarScherm(
        MessageFormat.format(
            resourceBundle.getString("help.paramsverplichtbijbestand"),
                             CaissaTools.AUTEUR, CaissaTools.TITEL));
    DoosUtils.naarScherm();
  }

  /**
   * Maak de matrix in het .tex bestand.
   * 
   * @param BufferedWriter output
   * @param Spelerinfo[] punten
   * @param int enkel
   * @param double[][] matrix
   * @param int kolommen
   * @param int noSpelers
   * @throws IOException
   */
  private static void maakMatrix(TekstBestand output, Spelerinfo[] punten,
                                 int enkel, double[][] matrix, int kolommen,
                                 int noSpelers)
      throws BestandException {
    StringBuilder lijn  = new StringBuilder();
    lijn.append("    \\begin{tabular} { | c | l | ");
    for (int i = 0; i < kolommen; i++) {
      lijn.append("c | ");
    }
    lijn.append("r | r | r | }");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    " + HLINE);
    lijn.append("    \\multicolumn{2}{|c|}{} ");
    for (int i = 0; i < (enkel == 0 ? kolommen : noSpelers); i++) {
      if (enkel < 2) {
        lijn.append(" & " + (i + 1));
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{" + (i + 1) + "} ");
      }
    }
    lijn.append("& " + resourceBundle.getString("tag.punten"));
    if (enkel > 0) {
      lijn.append(" & " + resourceBundle.getString("tag.partijen")
                  + " & " + resourceBundle.getString("tag.sb"));
    }
    lijn.append(" \\\\");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    \\cline{3-" + (2 + kolommen) + "}");
    if (enkel == 2) {
      lijn.append("    \\multicolumn{2}{|c|}{} & ");
      for (int i = 0; i < noSpelers; i++) {
        lijn.append(resourceBundle.getString("tag.wit")
                                       + " & "
                                       + resourceBundle.getString("tag.zwart")
                                       + " & ");
      }
      lijn.append("& & \\\\");
      output.write(lijn.toString());
      lijn  = new StringBuilder();
    }
    output.write("    " + HLINE);
    for (int i = 0; i < noSpelers; i++) {
      if (enkel == 0) {
        lijn.append("\\multicolumn{2}{|l|}{" + punten[i].getNaam() + "} & ");
      } else {
        lijn.append((i + 1) + " & " + punten[i].getNaam() + " & ");
      }
      for (int j = 0; j < kolommen; j++) {
        if (enkel > 0) {
          if (i == j / enkel) {
            lijn.append("\\multicolumn{1}"
                        + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ");
            continue;
          } else {
            if ((j / enkel) * enkel != j ) {
              lijn.append("\\multicolumn{1}"
                          + "{>{\\columncolor[rgb]{0.8,0.8,0.8}}c|}{");
            }
          }
        }
        if (matrix[i][j] == 0.0) {
          lijn.append("0");
        } else if (matrix[i][j] == 0.5) {
          lijn.append("\\textonehalf");
        } else if (matrix[i][j] >= 1.0) {
          lijn.append("" + ((Double)matrix[i][j]).intValue()
                      + Utilities.kwart(matrix[i][j]));
        }
        if (enkel > 0 && (j / enkel) * enkel != j ) {
          lijn.append("}");
        }
        lijn.append(" & ");
      }
      int     pntn  = punten[i].getPunten().intValue();
      String  decim = Utilities.kwart(punten[i].getPunten());
      lijn.append(
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "") + decim);
      if (enkel > 0) {
        int     wpntn   = punten[i].getTieBreakScore().intValue();
        String  wdecim  = Utilities.kwart(punten[i].getTieBreakScore());
        lijn.append(" & " + punten[i].getPartijen() + " & ");
        lijn.append(((wpntn == 0 && "".equals(wdecim))
                     || wpntn >= 1 ? wpntn : "") + wdecim);
      }
      lijn.append(" \\\\");
      output.write(lijn.toString());
      lijn  = new StringBuilder();
      output.write("    " + HLINE);
    }
    output.write("    \\end{tabular}");
  }

  /**
   * Vervang de parameters door hun waardes.
   * 
   * @param String regel
   * @param Map<String, String> parameters
   * @return
   */
  private static String replaceParameters(String regel,
                                          Map<String, String> parameters) {
    String resultaat  = regel;
    for (Entry<String, String> parameter : parameters.entrySet()) {
      resultaat = resultaat.replaceAll("@"+parameter.getKey()+"@",
                                       parameter.getValue());
    }

    return resultaat;
  }

  private static String schrijf(String regel, String status,
                                TekstBestand output, Spelerinfo[] punten,
                                int enkel, double[][] matrix, int kolommen,
                                int noSpelers, Map<String, String> texPartij,
                                Collection<PGN> partijen,
                                Map<String, String> parameters)
      throws BestandException {
    String  start = regel.split(" ")[0];
          switch(start) {
          case "%@Include":
            if ("matrix".equals(regel.split(" ")[1].toLowerCase())
                && null != matrix) {
              maakMatrix(output, punten, enkel, matrix, kolommen, noSpelers);
            }
            break;
          case "%@IncludeStart":
            switch(regel.split(" ")[1].toLowerCase()) {
            case "keywords":
              status  = KEYWORDS;
              break;
            case "logo":
              status  = LOGO;
              break;
            case "matrix":
              status = MATRIX;
              break;
            case "partij":
              status  = PARTIJEN;
              break;
            case "periode":
              status  = PERIODE;
              break;
            default:
              break;
            }
            break;
          case "%@IncludeEind":
            switch (regel.split(" ")[1].toLowerCase()) {
            case "partij":
              verwerkPartijen(partijen, texPartij, output);
              break;
            default:
              break;
            }
            status  = NORMAAL;
            break;
          case "%@I18N":
            output.write("% " + resourceBundle.getString(regel.split(" ")[1]
                                                              .toLowerCase()));
            break;
          default:
            switch (status) {
            case KEYWORDS:
              if (parameters.containsKey(CaissaTools.KEYWORDS)) {
                output.write(replaceParameters(regel, parameters));
              }
              break;
            case LOGO:
              if (parameters.containsKey(CaissaTools.LOGO)) {
                output.write(replaceParameters(regel, parameters));
              }
              break;
            case MATRIX:
              if (null != matrix) {
                output.write(replaceParameters(regel, parameters));
              }
              break;
            case PARTIJEN:
              String[]  splits  = regel.substring(1).split("=");
              texPartij.put(splits[0], splits[1]);
              break;
            case PERIODE:
              if (parameters.containsKey("Periode")) {
                output.write(replaceParameters(regel, parameters));
              }
              break;
            default:
              output.write(replaceParameters(regel, parameters));
              break;
            }
            break;
          }

    return status;
  }

  /**
   * Zet de partijen in het .tex bestand.
   * 
   * @param List<PGN> partijen
   * @param String[] texPartij
   * @param BufferedWriter output
   * @throws IOException
   */
  private static void verwerkPartijen(Collection<PGN> partijen,
                                      Map<String, String> texPartij,
                                      TekstBestand output)
      throws BestandException {
    FEN fen = null;
    for (PGN partij: partijen) {
      if (!partij.isBye()) {
        String  regel     = "";
        String  resultaat = partij.getTag(CaissaConstants.PGNTAG_RESULT)
            .replaceAll("1/2", "\\\\textonehalf");
        String  zetten    = partij.getZuivereZetten().replaceAll("#", "\\\\#");
        if (DoosUtils.isNotBlankOrNull(zetten)) {
          if (partij.hasTag("FEN")) {
            // Partij met andere beginstelling.
            try {
              fen = new FEN(partij.getTag("FEN"));
            } catch (FenException e) {
              DoosUtils.foutNaarScherm(partij.toString() + " - "
                                       + e.getMessage());
            }
            regel = texPartij.get("fenpartij");
          } else {
            // 'Gewone' partij.
            if (partij.getZetten().isEmpty()) {
              regel = texPartij.get("legepartij");
            } else {
              regel = texPartij.get("schaakpartij");
            }
          }
        } else {
          // Partij zonder zetten.
          if (!resultaat.equals("*")) {
            regel = texPartij.get("legepartij");
          }
        }
        int i = regel.indexOf('@');
        while (i >= 0) {
          int j = regel.indexOf('@', i+1);
          if (j > i) {
            String  tag = regel.substring(i+1, j);
            if (partij.hasTag(tag)) {
              switch (tag) {
              case CaissaConstants.PGNTAG_RESULT:
                regel = regel.replace("@" + tag + "@",
                    partij.getTag(tag).replaceAll("1/2", "\\\\textonehalf"));
                break;
              case CaissaConstants.PGNTAG_ECO:
                String extra = "";
                if (!partij.isRanked()) {
                  extra = " "
                      + resourceBundle.getString("tekst.buitencompetitie");
                }
                regel = regel.replace("@" + tag + "@",
                                      partij.getTag(tag) + extra);
                break;
              default:
                regel = regel.replace("@" + tag + "@", partij.getTag(tag));
                break;
              }
            } else {
              switch (tag) {
              case CaissaConstants.PGNTAG_ECO:
                String extra = "";
                if (!partij.isRanked()) {
                  extra = " "
                      + resourceBundle.getString("tekst.buitencompetitie");
                }
                regel = regel.replace("@" + tag + "@", extra);
                break;
              case "_EnkelZetten":
                regel = regel.replace("@_EnkelZetten@",
                                      partij.getZuivereZetten()
                                            .replace("#", "\\mate"));
                break;
              case "_Start":
                regel = regel.replace("@_Start@",
                                      fen.getAanZet()+" "+ fen.getZetnummer());
                break;
              case "_Stelling":
                regel = regel.replace("@_Stelling@", fen.getPositie());
                break;
              case "_Zetten":
                regel = regel.replace("@_Zetten@",
                                      partij.getZetten()
                                            .replace("#", "\\mate"));
                break;
              default:
                regel = regel.replace("@" + tag + "@", tag);
                break;
              }
            }
            j = i;
          }
          i = regel.indexOf('@', j+1);
        }
        output.write(regel);
      }
    }
  }
}
