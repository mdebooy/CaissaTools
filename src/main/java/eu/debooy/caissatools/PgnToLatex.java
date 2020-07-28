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
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.io.File;
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
public final class PgnToLatex extends Batchjob {
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

  public static void execute(String[] args) {
    int           aantalPartijen  = 0;
    String        eindDatum       = "0000.00.00";
    String        hulpDatum       = "";
    TekstBestand  output          = null;
    TekstBestand  texInvoer       = null;
    String        startDatum      = "9999.99.99";
    List<String>  template        = new ArrayList<>();

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntolatex"));

    if (!setParameters(args)) {
      return;
    }

    String[]  bestand = parameters.get(CaissaTools.PAR_BESTAND)
                                 .replaceAll(EXT_PGN, "")
                                 .split(";");

    String    auteur        = parameters.get(CaissaTools.PAR_AUTEUR);
    int       toernooitype  =
        CaissaUtils.getToernooitype(parameters.get(CaissaTools.PAR_ENKEL));
    String[]  halve         =
        DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_HALVE)).split(";");
    boolean   metMatrix     =
        parameters.get(CaissaTools.PAR_MATRIX).equals(DoosConstants.WAAR);
    boolean   matrixOpStand =
        parameters.get(CaissaTools.PAR_MATRIXOPSTAND)
                  .equals(DoosConstants.WAAR);
    String    titel         = parameters.get(CaissaTools.PAR_TITEL);

    int beginBody = -1;
    int eindeBody = -1;
    try {
      if (parameters.containsKey(CaissaTools.PAR_TEMPLATE)) {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand(
                                parameters.get(CaissaTools.PAR_TEMPLATE))
                            .setCharset(parameters.get(PAR_CHARSETIN)).build();
      } else {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand("Caissa.tex")
                            .setClassLoader(PgnToLatex.class.getClassLoader())
                            .setCharset(parameters.get(PAR_CHARSETIN)).build();
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
                                   parameters.get(CaissaTools.PAR_TEMPLATE)));
      return;
    }

    try {
      String  regel;
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
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + bestand[0] + EXT_TEX)
                                .setCharset(parameters.get(PAR_CHARSETIN))
                                .setLezen(false).build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (texInvoer != null) {
          texInvoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    for (int i = 0; i < bestand.length; i++) {
      Collection<PGN>     partijen  =
          new TreeSet<>(new PGN.byEventComparator());
      Map<String, String> texPartij = new HashMap<>();
      Set<String>         spelers   = new HashSet<>();

      try {
        partijen.addAll(
            CaissaUtils.laadPgnBestand(parameters.get(PAR_INVOERDIR)
                                       + bestand[i] + EXT_PGN,
                                       parameters.get(PAR_CHARSETIN)));
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }

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
        int           kolommen  =
            (toernooitype == CaissaConstants.TOERNOOI_MATCH
                                  ? partijen.size() : noSpelers * toernooitype);
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
                                          toernooitype, matrixOpStand,
                                          CaissaConstants.TIEBREAK_SB);
          }
        }

        // Zet de te vervangen waardes.
        Map<String, String> params  = new HashMap<>();
        params.put("Auteur", auteur);
        params.put("Datum", parameters.get(CaissaTools.PAR_DATUM));
        if (parameters.containsKey(CaissaTools.PAR_KEYWORDS)) {
          params.put(CaissaTools.PAR_KEYWORDS,
                     parameters.get(CaissaTools.PAR_KEYWORDS));
        }
        if (parameters.containsKey(CaissaTools.PAR_LOGO)) {
          params.put(CaissaTools.PAR_LOGO,
                     parameters.get(CaissaTools.PAR_LOGO));
        }
        if (bestand.length == 1) {
          params.put("Periode", datumInTitel(startDatum, eindDatum));
        }
        params.put("Titel", titel);

        String  status  = NORMAAL;
        if (i == 0) {
          for (int j = 0; j < beginBody; j++) {
            status  = schrijf(template.get(j), status, output, punten,
                              toernooitype, matrix, kolommen, noSpelers,
                              texPartij, partijen, params);
          }
        }
        for (int j = beginBody; j < eindeBody; j++) {
          status  = schrijf(template.get(j), status, output, punten,
                            toernooitype, matrix, kolommen, noSpelers,
                            texPartij, partijen, params);
        }
        if (i == bestand.length - 1) {
          for (int j = eindeBody + 1; j < template.size(); j++) {
            status  = schrijf(template.get(j), status, output, punten,
                              toernooitype, matrix, kolommen, noSpelers,
                              texPartij, partijen, params);
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

    for (String tex : bestand) {
      DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             parameters.get(PAR_UITVOERDIR) + tex + EXT_TEX));
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             aantalPartijen));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToLatex ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_AUTEUR, 14),
                         resourceBundle.getString("help.auteur"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("                  ",
                         resourceBundle.getString("help.bestanden"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_DATUM, 14),
                         resourceBundle.getString("help.speeldatum"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_ENKEL, 14),
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_HALVE, 14),
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_KEYWORDS, 14),
                         resourceBundle.getString("help.keywords"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_LOGO, 14),
                         resourceBundle.getString("help.logo"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIX, 14),
                         resourceBundle.getString("help.matrix"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIXOPSTAND, 14),
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TEMPLATE, 14),
                         resourceBundle.getString("help.template"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TITEL, 14),
                         resourceBundle.getString("help.documenttitel"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_BESTAND), 80);
    DoosUtils.naarScherm(
        MessageFormat.format(
            resourceBundle.getString("help.paramsverplichtbijbestand"),
                             CaissaTools.PAR_AUTEUR, CaissaTools.PAR_TITEL));
    DoosUtils.naarScherm();
  }

  private static void maakMatrix(TekstBestand output, Spelerinfo[] punten,
                                 int toernooitype, double[][] matrix,
                                 int kolommen, int noSpelers)
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
    for (int i = 0; i < (toernooitype == 0 ? kolommen : noSpelers); i++) {
      if (toernooitype < 2) {
        lijn.append(" & " + (i + 1));
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{" + (i + 1) + "} ");
      }
    }
    lijn.append("& " + resourceBundle.getString("tag.punten"));
    if (toernooitype > 0) {
      lijn.append(" & " + resourceBundle.getString("tag.partijen")
                  + " & " + resourceBundle.getString("tag.sb"));
    }
    lijn.append(" \\\\");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    \\cline{3-" + (2 + kolommen) + "}");
    if (toernooitype == 2) {
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
      if (toernooitype == 0) {
        lijn.append("\\multicolumn{2}{|l|}{" + punten[i].getNaam() + "} & ");
      } else {
        lijn.append((i + 1) + " & " + punten[i].getNaam() + " & ");
      }
      for (int j = 0; j < kolommen; j++) {
        if (toernooitype > 0) {
          if (i == j / toernooitype) {
            lijn.append("\\multicolumn{1}"
                        + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ");
            continue;
          } else {
            if ((j / toernooitype) * toernooitype != j ) {
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
        if (toernooitype > 0 && (j / toernooitype) * toernooitype != j ) {
          lijn.append("}");
        }
        lijn.append(" & ");
      }
      int     pntn  = punten[i].getPunten().intValue();
      String  decim = Utilities.kwart(punten[i].getPunten());
      lijn.append(
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "") + decim);
      if (toernooitype > 0) {
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
                                int toernooitype, double[][] matrix,
                                int kolommen, int noSpelers,
                                Map<String, String> texPartij,
                                Collection<PGN> partijen,
                                Map<String, String> parameters)
      throws BestandException {
    String  start = regel.split(" ")[0];
          switch(start) {
          case "%@Include":
            if ("matrix".equalsIgnoreCase(regel.split(" ")[1])
                && null != matrix) {
              maakMatrix(output, punten, toernooitype, matrix, kolommen,
                         noSpelers);
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
              if (parameters.containsKey(CaissaTools.PAR_KEYWORDS)) {
                output.write(replaceParameters(regel, parameters));
              }
              break;
            case LOGO:
              if (parameters.containsKey(CaissaTools.PAR_LOGO)) {
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

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_AUTEUR,
                                          CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_DATUM,
                                          CaissaTools.PAR_ENKEL,
                                          CaissaTools.PAR_HALVE,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_KEYWORDS,
                                          CaissaTools.PAR_LOGO,
                                          CaissaTools.PAR_MATRIX,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          CaissaTools.PAR_TEMPLATE,
                                          CaissaTools.PAR_TITEL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setParameter(arguments, CaissaTools.PAR_AUTEUR);
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_DATUM,
                 Datum.fromDate(new Date(), "dd/MM/yyyy HH:mm:ss"));
    setParameter(arguments, CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_HALVE);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_KEYWORDS);
    setParameter(arguments, CaissaTools.PAR_LOGO);
    setParameter(arguments, CaissaTools.PAR_MATRIX, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setParameter(arguments, CaissaTools.PAR_TEMPLATE);
    setParameter(arguments, CaissaTools.PAR_TITEL);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (parameters.containsKey(CaissaTools.PAR_BESTAND)) {
      if (parameters.containsKey(CaissaTools.PAR_HALVE)) {
        fouten.add(resourceBundle.getString(CaissaTools.ERR_HALVE));
      }
      if (parameters.get(CaissaTools.PAR_BESTAND).contains(";")) {
        if (!parameters.containsKey(CaissaTools.PAR_AUTEUR)
            || !parameters.containsKey(CaissaTools.PAR_TITEL)) {
          fouten.add(resourceBundle.getString(CaissaTools.ERR_BIJBESTAND));
        }
      }
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

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
        if (partij.hasTag("FEN")) {
            // Partij met andere beginstelling.
            try {
              fen = new FEN(partij.getTag("FEN"));
            } catch (FenException e) {
              DoosUtils.foutNaarScherm(partij.toString() + " - "
                                       + e.getLocalizedMessage());
              fen = new FEN();
            }
        } else {
            fen = new FEN();
        }
        if (DoosUtils.isNotBlankOrNull(zetten)) {
          if (partij.hasTag("FEN")) {
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
