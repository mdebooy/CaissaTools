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
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_SPELERS;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.JsonBestand;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeSet;


/**
 * Versie 526 is de laatste goede.
 * @author Marco de Booij
 */
public final class PgnToLatex extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static  String            auteur;
  private static  String            eindDatum;
  private static  double[][]        matrix;
  private static  TekstBestand      output;
  private static  Collection<PGN>   partijen;
  private static  List<Spelerinfo>  spelers;
  private static  String            startDatum;
  private static  String            titel;
  private static  int               toernooitype;

  private static final String HLINE         = "\\hline";
  private static final String KEYWORDS      = "K";
  private static final String KYW_LOGO      = "L";
  private static final String KYW_MATRIX    = "M";
  private static final String KYW_PARTIJEN  = "P";
  private static final String KYW_PERIODE   = "Q";
  private static final String NORMAAL       = "N";

  PgnToLatex() {}

  private static void bepaalMinMaxDatum(String datum) {
    if (DoosUtils.isNotBlankOrNull(datum)
        && datum.indexOf('?') < 0) {
      if (datum.compareTo(startDatum) < 0 ) {
        startDatum  = datum;
      }
      if (datum.compareTo(eindDatum) > 0 ) {
        eindDatum   = datum;
      }
    }
  }

  protected static String datumInTitel(String startDatum, String eindDatum) {
    var   titelDatum  = new StringBuilder();
    Date  datum       = null;
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
    var           aantalPartijen  = 0;
    List<String>  template        = new ArrayList<>();

    eindDatum   = CaissaConstants.DEF_STARTDATUM;
    output      = null;
    startDatum  = CaissaConstants.DEF_EINDDATUM;

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntolatex"));

    if (!setParameters(args)) {
      return;
    }

    var bestand   = parameters.get(CaissaTools.PAR_BESTAND)
                              .replace(EXT_PGN, "").split(";");
    var schema    = parameters.get(CaissaTools.PAR_SCHEMA)
                              .replace(EXT_JSON, "").split(";");

    auteur        = parameters.get(CaissaTools.PAR_AUTEUR);
    toernooitype  =
        CaissaUtils.getToernooitype(parameters.get(CaissaTools.PAR_ENKEL));
    var metMatrix =
        parameters.get(CaissaTools.PAR_MATRIX).equals(DoosConstants.WAAR);
    titel         = parameters.get(CaissaTools.PAR_TITEL);

    var beginBody = -1;
    var eindeBody = -1;
    TekstBestand  texInvoer;
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

    for (var i = 0; i < bestand.length; i++) {
      partijen  = new TreeSet<>(new PGN.ByEventComparator());
      Map<String, String> texPartij = new HashMap<>();

      JsonBestand competitie;
      try {
        competitie  =
            new JsonBestand.Builder()
                           .setBestand(parameters.get(PAR_INVOERDIR)
                                       + schema[i] + EXT_JSON)
                           .setCharset(parameters.get(PAR_CHARSETIN))
                           .build();
        partijen.addAll(
            CaissaUtils.laadPgnBestand(parameters.get(PAR_INVOERDIR)
                                       + bestand[i] + EXT_PGN,
                                       parameters.get(PAR_CHARSETIN)));
      } catch (BestandException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        return;
      }

      spelers = new ArrayList<>();
      CaissaUtils.vulSpelers(spelers, competitie.getArray(JSON_TAG_SPELERS));

      partijen.forEach(PgnToLatex::verwerkPartij);

      try {
        var noSpelers = spelers.size();
        var kolommen  =
            (toernooitype == CaissaConstants.TOERNOOI_MATCH
                                  ? partijen.size() : noSpelers * toernooitype);
        matrix    = null;
        var namen = new String[noSpelers];
        // Maak de Matrix
        if (metMatrix) {
          var j = 0;
          for (var speler  : spelers) {
            namen[j++]  = speler.getNaam();
          }
          Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);

          // Bepaal de score en weerstandspunten.
          matrix  = new double[noSpelers][kolommen];
          CaissaUtils.vulToernooiMatrix(partijen, spelers, matrix, toernooitype,
                                        parameters
                                          .get(CaissaTools.PAR_MATRIXOPSTAND)
                                          .equals(DoosConstants.WAAR),
                                        CaissaConstants.TIEBREAK_SB);
          matrix    = CaissaUtils.verwijderNietActief(spelers, matrix, toernooitype);
          kolommen  = matrix[0].length;
          noSpelers = spelers.size();
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

        var status  = NORMAAL;
        if (i == 0) {
          for (var j = 0; j < beginBody; j++) {
            status  = schrijf(template.get(j), status, kolommen, noSpelers,
                              texPartij, params);
          }
        }
        for (var j = beginBody; j < eindeBody; j++) {
          status  = schrijf(template.get(j), status, kolommen, noSpelers,
                            texPartij, params);
        }
        if (i == bestand.length - 1) {
          for (var j = eindeBody + 1; j < template.size(); j++) {
            status  = schrijf(template.get(j), status, kolommen, noSpelers,
                              texPartij, params);
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

    for (var tex : bestand) {
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
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SCHEMA, 14),
                         resourceBundle.getString(CaissaTools.HLP_SCHEMA), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TEMPLATE, 14),
                         resourceBundle.getString("help.template"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TITEL, 14),
                         resourceBundle.getString("help.documenttitel"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             CaissaTools.PAR_BESTAND,
                             CaissaTools.PAR_SCHEMA), 80);
    DoosUtils.naarScherm(
        MessageFormat.format(
            resourceBundle.getString("help.paramsverplichtbijbestand"),
                             CaissaTools.PAR_AUTEUR, CaissaTools.PAR_TITEL));
    DoosUtils.naarScherm();
  }

  private static void maakMatrix(int kolommen, int noSpelers)
      throws BestandException {
    var lijn  = new StringBuilder();
    lijn.append("    \\begin{tabular} { | c | l | ");
    for (var i = 0; i < kolommen; i++) {
      lijn.append("c | ");
    }
    lijn.append("r | r | r | }");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    " + HLINE);
    lijn.append("    \\multicolumn{2}{|c|}{} ");
    for (var i = 0; i < (toernooitype == 0 ? kolommen : noSpelers); i++) {
      if (toernooitype < 2) {
        lijn.append(" & ").append((i + 1));
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{").append((i + 1)).append("} ");
      }
    }
    lijn.append("& ").append(resourceBundle.getString("tag.punten"));
    if (toernooitype > 0) {
      lijn.append(" & ").append(resourceBundle.getString("tag.partijen"))
          .append(" & ").append(resourceBundle.getString("tag.sb"));
    }
    lijn.append(" \\\\");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    \\cline{3-" + (2 + kolommen) + "}");
    if (toernooitype == 2) {
      lijn.append("    \\multicolumn{2}{|c|}{} & ");
      for (var i = 0; i < noSpelers; i++) {
        lijn.append(resourceBundle.getString("tag.wit")).append(" & ")
            .append(resourceBundle.getString("tag.zwart")).append(" & ");
      }
      lijn.append("& & \\\\");
      output.write(lijn.toString());
      lijn  = new StringBuilder();
    }
    output.write("    " + HLINE);
    for (var i = 0; i < noSpelers; i++) {
      if (toernooitype == 0) {
        lijn.append("\\multicolumn{2}{|l|}{").append(spelers.get(i).getNaam())
            .append("} & ");
      } else {
        lijn.append((i + 1)).append(" & ").append(spelers.get(i).getNaam())
            .append(" & ");
      }
      for (var j = 0; j < kolommen; j++) {
        if (toernooitype > 0) {
          if (i == j / toernooitype) {
            lijn.append("\\multicolumn{1}"
                        + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ");
            continue;
          }
          if ((j / toernooitype) * toernooitype != j ) {
            lijn.append("\\multicolumn{1}"
                        + "{>{\\columncolor[rgb]{0.8,0.8,0.8}}c|}{");
          }
        }
        if (matrix[i][j] == 0.0) {
          lijn.append("0");
        } else if (matrix[i][j] == 0.5) {
          lijn.append(Utilities.kwart(0.5));
        } else if (matrix[i][j] >= 1.0) {
          lijn.append(((Double)matrix[i][j]).intValue())
              .append(Utilities.kwart(matrix[i][j]));
        }
        if (toernooitype > 0 && (j / toernooitype) * toernooitype != j ) {
          lijn.append("}");
        }
        lijn.append(" & ");
      }
      var pntn  = spelers.get(i).getPunten().intValue();
      var decim = Utilities.kwart(spelers.get(i).getPunten());
      lijn.append(
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "")).append(decim);
      if (toernooitype > 0) {
        var wpntn   = spelers.get(i).getTieBreakScore().intValue();
        var wdecim  = Utilities.kwart(spelers.get(i).getTieBreakScore());
        lijn.append(" & ").append(spelers.get(i).getPartijen()).append(" & ");
        lijn.append(((wpntn == 0 && "".equals(wdecim))
                     || wpntn >= 1 ? wpntn : "")).append(wdecim);
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
    var resultaat = regel;
    for (Entry<String, String> parameter : parameters.entrySet()) {
      resultaat = resultaat.replace("@"+parameter.getKey()+"@",
                                    parameter.getValue());
    }

    return resultaat;
  }

  private static String schrijf(String regel, String status,
                                int kolommen, int noSpelers,
                                Map<String, String> texPartij,
                                Map<String, String> parameters)
      throws BestandException {
    var start = regel.split(" ")[0];
    switch(start) {
      case "%@Include":
        if ("matrix".equalsIgnoreCase(regel.split(" ")[1])
            && null != matrix) {
          maakMatrix(kolommen, noSpelers);
        }
        break;
      case "%@IncludeStart":
        status  = setStatus(regel.split(" ")[1].toLowerCase());
        break;
      case "%@IncludeEind":
        if ("partij".equalsIgnoreCase(regel.split(" ")[1])) {
          verwerkPartijen(partijen, texPartij, output);
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
          case KYW_LOGO:
            if (parameters.containsKey(CaissaTools.PAR_LOGO)) {
              output.write(replaceParameters(regel, parameters));
            }
            break;
          case KYW_MATRIX:
            if (null != matrix) {
              output.write(replaceParameters(regel, parameters));
            }
            break;
          case KYW_PARTIJEN:
            String[]  splits  = regel.substring(1).split("=");
            texPartij.put(splits[0], splits[1]);
            break;
          case KYW_PERIODE:
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
    var           arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_AUTEUR,
                                          CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_DATUM,
                                          CaissaTools.PAR_ENKEL,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_KEYWORDS,
                                          CaissaTools.PAR_LOGO,
                                          CaissaTools.PAR_MATRIX,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          CaissaTools.PAR_SCHEMA,
                                          CaissaTools.PAR_TEMPLATE,
                                          CaissaTools.PAR_TITEL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_SCHEMA});
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
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_KEYWORDS);
    setParameter(arguments, CaissaTools.PAR_LOGO);
    setParameter(arguments, CaissaTools.PAR_MATRIX, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setBestandParameter(arguments, CaissaTools.PAR_SCHEMA);
    setParameter(arguments, CaissaTools.PAR_TEMPLATE);
    setParameter(arguments, CaissaTools.PAR_TITEL);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (parameters.containsKey(CaissaTools.PAR_BESTAND)
        && parameters.get(CaissaTools.PAR_BESTAND).contains(";")) {
      if (!parameters.containsKey(CaissaTools.PAR_AUTEUR)
          || !parameters.containsKey(CaissaTools.PAR_TITEL)) {
        fouten.add(resourceBundle.getString(CaissaTools.ERR_BIJBESTAND));
      }
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
    }

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .split(";").length !=
        DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .split(";").length) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BEST_ONGELIJK));
    }
    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

  private static String setStatus(String keyword) {
    String  status;
    switch(keyword) {
      case "keywords":
        status  = KEYWORDS;
        break;
      case "logo":
        status  = KYW_LOGO;
        break;
      case "matrix":
        status = KYW_MATRIX;
        break;
      case "partij":
        status  = KYW_PARTIJEN;
        break;
      case "periode":
        status  = KYW_PERIODE;
        break;
      default:
        status  = "";
        break;
    }

    return status;
  }

  private static void verwerkPartij(PGN partij) {
    bepaalMinMaxDatum(partij.getTag(CaissaConstants.PGNTAG_EVENTDATE));
    bepaalMinMaxDatum(partij.getTag(CaissaConstants.PGNTAG_DATE));

    if (DoosUtils.isBlankOrNull(auteur)) {
      auteur  = partij.getTag(CaissaConstants.PGNTAG_SITE);
    }
    if (DoosUtils.isBlankOrNull(titel)) {
      titel   = partij.getTag(CaissaConstants.PGNTAG_EVENT);
    }
  }

  private static void verwerkPartijen(Collection<PGN> partijen,
                                      Map<String, String> texPartij,
                                      TekstBestand output) {
    partijen.stream()
            .filter(partij -> !partij.isBye())
            .forEach(partij -> {
      try {
        var fen       = new FEN();
        var regel     = "";
        var resultaat = partij.getTag(CaissaConstants.PGNTAG_RESULT)
                .replace("1/2", Utilities.kwart(0.5));
        var zetten    = partij.getZuivereZetten().replace("#", "\\\\#");
        if (partij.hasTag(CaissaConstants.PGNTAG_FEN)) {
          fen = new FEN(partij.getTag(CaissaConstants.PGNTAG_FEN));
        }
        if (DoosUtils.isNotBlankOrNull(zetten)) {
          if (partij.hasTag(CaissaConstants.PGNTAG_FEN)) {
            regel = texPartij.get("fenpartij");
          } else {
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
            var tag = regel.substring(i+1, j);
            if (partij.hasTag(tag)) {
              switch (tag) {
                case CaissaConstants.PGNTAG_RESULT:
                  regel = regel.replace("@" + tag + "@",
                          partij.getTag(tag)
                                .replace("1/2", Utilities.kwart(0.5)));
                  break;
                case CaissaConstants.PGNTAG_ECO:
                  var extra = "";
                  if (!partij.isRanked()) {
                    extra =
                        " "
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
                  var extra = "";
                  if (!partij.isRanked()) {
                    extra =
                        " "
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
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    });
  }
}
