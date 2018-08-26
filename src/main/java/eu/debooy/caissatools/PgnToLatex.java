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
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
  
  PgnToLatex() {}

  public static void execute(String[] args) throws PgnException {
    String              charsetIn   = Charset.defaultCharset().name();
    String              charsetUit  = Charset.defaultCharset().name();
    String              eindDatum   = "0000.00.00";
    List<String>        fouten      = new ArrayList<String>();
    String              hulpDatum   = "";
    BufferedWriter      output      = null;
    BufferedReader      texInvoer   = null;
    Map<String, String> texPartij   = new HashMap<String, String>();
    Set<String>         spelers     = new HashSet<String>();
    String              startDatum  = "9999.99.99";
    String              template    = "";

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

    String  auteur  = arguments.getArgument(CaissaTools.AUTEUR);
    String  bestand = arguments.getArgument(CaissaTools.BESTAND);
    if (bestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                       CaissaTools.BESTAND));
    }
    if (bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand   = bestand.substring(0, bestand.length() - 4);
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
      template  = arguments.getArgument(CaissaTools.TEMPLATE);
      File  tex = new File(template);
      if (!tex.exists()) {
        fouten.add(MessageFormat.format(
            resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
                                        template));
      }
    }
    String  titel       = arguments.getArgument(CaissaTools.TITEL);

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    Collection<PGN>
            partijen    = new TreeSet<PGN>(new PGN.byEventComparator());
    partijen.addAll(CaissaUtils.laadPgnBestand(invoerdir + File.separator
                                                 + bestand
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
      output  = Bestand.openUitvoerBestand(uitvoerdir + File.separator
                                             + bestand
                                             + CaissaTools.EXTENSIE_TEX,
                                           charsetUit);

      int           noSpelers = spelers.size();
      int           kolommen  = (enkel == CaissaConstants.TOERNOOI_MATCH
                                    ? partijen.size() : noSpelers * enkel);
      double[][]    matrix    = new double[noSpelers][kolommen];
      String[]      namen     = new String[noSpelers];
      Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
      // Maak de Matrix
      if (metMatrix) {
        int i = 0;
        for (String speler  : spelers) {
          namen[i++]  = speler;
        }

        // Initialiseer de Spelerinfo array.
        Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
        for (i = 0; i < noSpelers; i++) {
          punten[i] = new Spelerinfo();
          punten[i].setNaam(namen[i]);
        }

        // Bepaal de score en weerstandspunten.
        CaissaUtils.vulToernooiMatrix(partijen, punten, halve, matrix, enkel,
                                      matrixOpStand,
                                      CaissaConstants.TIEBREAK_SB);
      }

      // Zet de te vervangen waardes.
      Map<String, String> parameters  = new HashMap<String, String>();
      parameters.put("Auteur", auteur);
      parameters.put("Datum", datum);
      if (DoosUtils.isNotBlankOrNull(keywords)) {
        parameters.put("Keywords", keywords);
      }
      if (DoosUtils.isNotBlankOrNull(logo)) {
        parameters.put("Logo", logo);
      }
      parameters.put("Periode", datumInTitel(startDatum, eindDatum));
      parameters.put("Titel", titel);

      // Maak de .tex file
      if (arguments.hasArgument("template")) {
        texInvoer = Bestand.openInvoerBestand(template, charsetIn);
      } else {
        texInvoer =
            new BufferedReader(
                new InputStreamReader(PgnToLatex.class.getClassLoader()
                    .getResourceAsStream("Caissa.tex"), charsetIn));
      }

      String  regel   = null;
      String  status  = NORMAAL;
      String  type    = "";
      while ((regel = texInvoer.readLine()) != null) {
        if (regel.startsWith("%@Include ")) {
          type  = regel.split(" ")[1].toLowerCase();
          switch(type) {
          case "matrix":
            if (metMatrix) {
              maakMatrix(output, punten, enkel, matrix, kolommen, noSpelers);
            }
            break;
          default:
            break;
          }
        } else if (regel.startsWith("%@IncludeStart ")) {
          type  = regel.split(" ")[1].toLowerCase();
          switch(type) {
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
          default:
            break;
          }
        } else if (regel.startsWith("%@IncludeEind ")) {
          switch (type) {
          case "partij":
            verwerkPartijen(partijen, texPartij, output);
            break;
          default:
            break;
          }
          status  = NORMAAL;
          type    = "";
        } else if (regel.startsWith("%@I18N ")) {
          Bestand.schrijfRegel(output,
                               "% "
                                   + resourceBundle
                                         .getString(regel.split(" ")[1]
                                                         .toLowerCase()));
        } else {
          switch (status) {
          case KEYWORDS:
            if (DoosUtils.isNotBlankOrNull(keywords)) {
              Bestand.schrijfRegel(output, replaceParameters(regel,
                                                             parameters));
            }
            break;
          case LOGO:
            if (DoosUtils.isNotBlankOrNull(logo)) {
              Bestand.schrijfRegel(output, replaceParameters(regel,
                                                             parameters));
            }
            break;
          case MATRIX:
            if (metMatrix) {
              Bestand.schrijfRegel(output, replaceParameters(regel,
                                                             parameters));
            }
            break;
          case PARTIJEN:
            String[]  splits  = regel.substring(1).split("=");
            texPartij.put(splits[0], splits[1]);
            break;
          default:
            Bestand.schrijfRegel(output, replaceParameters(regel, parameters));
            break;
          }
        }
      }
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
      try {
        if (texInvoer != null) {
          texInvoer.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator
                         + bestand + CaissaTools.EXTENSIE_TEX);
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
    DoosUtils.naarScherm("  --auteur        ",
                         resourceBundle.getString("help.auteur"), 80);
    DoosUtils.naarScherm("  --bestand       ",
                         resourceBundle.getString("help.bestand"), 80);
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
    DoosUtils.naarScherm("  --uitvoerdir   ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             CaissaTools.BESTAND), 80);
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
  public static void maakMatrix(BufferedWriter output, Spelerinfo[] punten,
                                int enkel, double[][] matrix, int kolommen,
                                int noSpelers)
      throws IOException {
    Bestand.schrijfRegel(output, "    \\begin{tabular} { | c | l | ", 0);
    for (int i = 0; i < kolommen; i++) {
      Bestand.schrijfRegel(output, "c | ", 0);
    }
    Bestand.schrijfRegel(output, "r | r | r | }");
    Bestand.schrijfRegel(output, "    " + HLINE);
    Bestand.schrijfRegel(output, "    \\multicolumn{2}{|c|}{} ", 0);
    for (int i = 0; i < (enkel == 0 ? kolommen : noSpelers); i++) {
      if (enkel < 2) {
        Bestand.schrijfRegel(output, " & " + (i + 1), 0);
      } else {
        Bestand.schrijfRegel(output,
                             " & \\multicolumn{2}{c|}{" + (i + 1) + "} ", 0);
      }
    }
    Bestand.schrijfRegel(output,
                         "& " + resourceBundle.getString("tag.punten"), 0);
    if (enkel > 0) {
      Bestand.schrijfRegel(output,
                           " & " + resourceBundle.getString("tag.partijen")
                             + " & " + resourceBundle.getString("tag.sb"), 0);
    }
    Bestand.schrijfRegel(output, " \\\\");
    Bestand.schrijfRegel(output, "    \\cline{3-" + (2 + kolommen) + "}");
    if (enkel == 2) {
      Bestand.schrijfRegel(output, "    \\multicolumn{2}{|c|}{} & ", 0);
      for (int i = 0; i < noSpelers; i++) {
        Bestand.schrijfRegel(output, resourceBundle.getString("tag.wit")
                                       + " & "
                                       + resourceBundle.getString("tag.zwart")
                                       + " & ", 0);
      }
      Bestand.schrijfRegel(output, "& & \\\\");
    }
    Bestand.schrijfRegel(output, "    " + HLINE);
    for (int i = 0; i < noSpelers; i++) {
      if (enkel == 0) {
        Bestand.schrijfRegel(output,
                             "\\multicolumn{2}{|l|}{" + punten[i].getNaam()
                               + "} & ", 0);
      } else {
        Bestand.schrijfRegel(output,
                             (i + 1) + " & " + punten[i].getNaam() + " & ", 0);
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
      int     pntn  = punten[i].getPunten().intValue();
      String  decim = Utilities.kwart(punten[i].getPunten());
      Bestand.schrijfRegel(output,
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "") + decim, 0);
      if (enkel > 0) {
        int     wpntn   = punten[i].getTieBreakScore().intValue();
        String  wdecim  = Utilities.kwart(punten[i].getTieBreakScore());
        Bestand.schrijfRegel(output,
                             " & " + punten[i].getPartijen() + " & ", 0);
        Bestand.schrijfRegel(output,
                             ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ?
                                 wpntn : "") + wdecim, 0);
      }
      Bestand.schrijfRegel(output, " \\\\");
      Bestand.schrijfRegel(output, "    " + HLINE);
    }
    Bestand.schrijfRegel(output, "    \\end{tabular}");
  }

  /**
   * Vervang de parameters door hun waardes.
   * 
   * @param String regel
   * @param Map<String, String> parameters
   * @return
   */
  public static String replaceParameters(String regel,
                                         Map<String, String> parameters) {
    String resultaat  = regel;
    for (Entry<String, String> parameter : parameters.entrySet()) {
      resultaat = resultaat.replaceAll("@"+parameter.getKey()+"@",
                                       parameter.getValue());
    }

    return resultaat;
  }

  /**
   * Zet de partijen in het .tex bestand.
   * 
   * @param List<PGN> partijen
   * @param String[] texPartij
   * @param BufferedWriter output
   * @throws IOException
   */
  public static void verwerkPartijen(Collection<PGN> partijen,
                                     Map<String, String> texPartij,
                                     BufferedWriter output)
      throws IOException {
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
        Bestand.schrijfRegel(output, regel);
      }
    }
  }
}
