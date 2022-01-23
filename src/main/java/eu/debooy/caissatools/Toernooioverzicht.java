/**
 * Copyright 2021 Marco de Booij
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
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import static eu.debooy.doosutils.Batchjob.help;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public final class Toernooioverzicht extends Batchjob {
  private static final  ClassLoader     classloader     =
      Toernooioverzicht.class.getClassLoader();
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  List<Spelerinfo>  deelnemers;
  private static  JSONArray         jsonKalender;
  private static  String[]          kalender;
  private static  double[][]        matrix;
  private static  TekstBestand      output;
  private static  Set<Partij>       schema;
  private static  List<Spelerinfo>  spelers;
  private static  TekstBestand      texInvoer;
  private static  int               toernooitype;

  private static final  String  DEF_TEMPLATE  = "Overzicht.tex";

  private static final  String  LTX_HLINE       = "\\hline";
  private static final  String  LTX_END_TABULAR = "\\end{tabular}";
  private static final  String  LTX_EOL         = "\\\\";

  private static final  String  KYW_DEELNEMERS  = "D";
  private static final  String  KYW_KALENDER    = "K";
  private static final  String  KYW_LOGO        = "L";
  private static final  String  KYW_MATRIX      = "M";
  private static final  String  KYW_SUBTITEL    = "S";
  private static final  String  KYW_TITEL       = "T";
  private static final  String  KYW_UITSLAGEN   = "U";

  private static final  String  NORMAAL         = "N";

  private static final  String  TAG_KALENDER    = "kalender";

  private static final  String  KLEUR           = "\\columncolor{headingkleur}";
  private static final  String  KLEURLICHT      =
      "\\columncolor{headingkleur!25}";
  private static final  String  RIJKLEUR        = "\\rowcolor{headingkleur}";
  private static final  String  RIJKLEURLICHT   = "\\rowcolor{headingkleur!25}";
  private static final  String  RIJKLEURLICHTER = "\\rowcolor{headingkleur!10}";
  private static final  String  TEKSTKLEUR      = "\\color{headingtekstkleur}";

  Toernooioverzicht() {}

  private static void bepaalTexInvoer()
      throws BestandException {
    if (paramBundle.containsParameter(CaissaTools.PAR_TEMPLATE)) {
      texInvoer =
          new TekstBestand.Builder()
                          .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_TEMPLATE))
                          .build();
    } else {
      texInvoer =
          new TekstBestand.Builder()
                          .setBestand(DEF_TEMPLATE)
                          .setClassLoader(classloader)
                          .build();
    }
  }

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_TOERNOOIOVERZICHT)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    output        = null;

    try {
      bepaalTexInvoer();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
              paramBundle.getBestand(CaissaTools.PAR_TEMPLATE)));
      return;
    }

    List<String>  template  = new ArrayList<>();
    try {
      while (texInvoer.hasNext()) {
        template.add(texInvoer.next());
      }

      output  =
          new TekstBestand.Builder()
                          .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_UITVOER))
                          .setLezen(false).build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (null != texInvoer) {
          texInvoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    Collection<PGN> partijen    = new TreeSet<>(new PGN.ByEventComparator());
    try (var competitie    =
          new JsonBestand.Builder()
                         .setBestand(
                            paramBundle.getBestand(CaissaTools.PAR_SCHEMA))
                         .build()) {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(
              paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                     BestandConstants.EXT_PGN)));
      spelers         = new ArrayList<>();
      if (competitie.containsKey(CaissaConstants.JSON_TAG_TOERNOOITYPE)) {
        toernooitype  =
            ((Long) competitie.get(CaissaConstants.JSON_TAG_TOERNOOITYPE))
                .intValue();
      } else {
        toernooitype  = CaissaConstants.TOERNOOI_DUBBEL;
      }
      CaissaUtils.vulSpelers(spelers,
                      competitie.getArray(CaissaConstants.JSON_TAG_SPELERS));

      deelnemers    = new ArrayList<>();
      deelnemers.addAll(spelers);
      deelnemers.sort(new Spelerinfo.ByNaamComparator());

      jsonKalender  = competitie.getArray(CaissaConstants.JSON_TAG_KALENDER);
      kalender      =
          CaissaUtils.vulKalender(CaissaConstants.JSON_TAG_KALENDER_RONDE,
                                  spelers.size(), toernooitype, jsonKalender);
    } catch (PgnException | BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    var noSpelers = spelers.size();
    var kolommen  =
        (toernooitype == CaissaConstants.TOERNOOI_MATCH
                              ? partijen.size() : noSpelers * toernooitype);
    matrix    = null;
    var namen = new String[noSpelers];

    // Maak de Matrix
    var i = 0;
    for (var speler  : spelers) {
      namen[i++]  = speler.getNaam();
    }
    Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);

    if (toernooitype != CaissaConstants.TOERNOOI_MATCH) {
      // Sortering terug zetten voor opmaken schema.
      spelers.sort(new Spelerinfo.BySpelerSeqComparator());
      var enkelrondig = (toernooitype == CaissaConstants.TOERNOOI_ENKEL);
      schema      =
          CaissaUtils.genereerSpeelschema(spelers, enkelrondig, partijen);
    } else {
      schema  = new TreeSet<>();
    }

    // Bepaal de score en weerstandspunten.
    matrix    = new double[noSpelers][kolommen];
    CaissaUtils.vulToernooiMatrix(partijen,
                                  spelers,
                                  matrix, toernooitype,
                                  paramBundle
                                    .getBoolean(CaissaTools.PAR_MATRIXOPSTAND),
                                  CaissaConstants.TIEBREAK_SB);
    if (Boolean.FALSE
               .equals(paramBundle.getBoolean(CaissaTools.PAR_ALLEN))) {
      matrix  = CaissaUtils.verwijderNietActief(spelers, matrix, toernooitype);
    }
    kolommen  = matrix[0].length;
    noSpelers = spelers.size();

    // Zet de te vervangen waardes.
    Map<String, String> params  = new HashMap<>();
    vulParams(params);

    var status  = NORMAAL;
    try {
      for (var j = 0; j < template.size(); j++) {
        status  = schrijf(template.get(j), status, kolommen, noSpelers, params);
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    try {
      if (output != null) {
        output.close();
      }
    } catch (BestandException ex) {
      DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
    }

    DoosUtils.naarScherm(
      MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                           paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                                  BestandConstants.EXT_TEX)));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void maakDeelnemerslijst() throws BestandException {
    for (Spelerinfo deelnemer : deelnemers) {
      output.write("    " + deelnemer.getVolledigenaam() + " & "
              + DoosUtils.nullToEmpty(deelnemer.getTelefoon()) + " & "
              + DoosUtils.nullToEmpty(deelnemer.getEmail()) + " " + LTX_EOL);
      output.write("    " + LTX_HLINE);
    }
  }

  private static void maakKalender() throws BestandException {
    for (var i = 0; i < jsonKalender.size(); i++) {
      var item  = (JSONObject) jsonKalender.get(i);
      var lijn  = new StringBuilder();
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_EXTRA)) {
        lijn.append(item.get(CaissaConstants.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(item.get(CaissaConstants.JSON_TAG_KALENDER_EXTRA)
                        .toString())
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_INHAAL)) {
        output.write(RIJKLEURLICHTER);
        lijn.append(item.get(CaissaConstants.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(MessageFormat.format(
                        resourceBundle.getString("label.latex.inhaal"),
                        item.get(CaissaConstants.JSON_TAG_KALENDER_INHAAL)
                            .toString()))
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_RONDE)) {
        output.write(RIJKLEURLICHT);
        lijn.append(item.get(CaissaConstants.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(MessageFormat.format(
                        resourceBundle.getString("label.latex.ronde"),
                        item.get(CaissaConstants.JSON_TAG_KALENDER_RONDE)
                            .toString()))
            .append(" ").append(LTX_EOL);
      }
      output.write("    " + lijn.toString());
      output.write("    " + LTX_HLINE);
    }
  }

  private static void maakLatexMatrix(int kolommen, int noSpelers,
                                      boolean matrixEerst)
      throws BestandException {
    var lijn  = new StringBuilder();

    // Header
    lijn.append("   \\begin{tabular} { | c l | ");
    if (matrixEerst) {
      matrixEerst(lijn, kolommen, noSpelers);
    } else {
      matrixLaatst(lijn, kolommen, noSpelers);
    }

    // Body
    for (var i = 0; i < noSpelers; i++) {
      lijn  = new StringBuilder();

      if (toernooitype == 0) {
        lijn.append("\\multicolumn{2}{|l|}{")
            .append(spelers.get(i).getVolledigenaam())
            .append("} ");
      } else {
        lijn.append((i + 1)).append(" & ")
            .append(spelers.get(i).getVolledigenaam())
            .append(" ");
      }

      if (matrixEerst) {
        maakLatexMatrixBodyMat(lijn, i, kolommen);
        maakLatexMatrixBodyPnt(lijn, i);
      } else {
        maakLatexMatrixBodyPnt(lijn, i);
        maakLatexMatrixBodyMat(lijn, i, kolommen);
      }

      lijn.append(" ").append(LTX_EOL);
      output.write(lijn.toString());
      output.write("    " + LTX_HLINE);
    }
    output.write("   " + LTX_END_TABULAR);
  }

  private static void maakLatexMatrixBodyMat(StringBuilder lijn, int rij,
                                             int kolommen) {
    for (var j = 0; j < kolommen; j++) {
      lijn.append("& ");
      if (toernooitype > 0) {
        if (rij == j / toernooitype) {
          lijn.append("\\multicolumn{1}"
                      + "{>{").append(KLEUR).append("}c|}{} ");
          continue;
        }
        if ((j / toernooitype) * toernooitype != j ) {
          lijn.append("\\multicolumn{1}"
                      + "{>{").append(KLEURLICHT).append("}c|}{");
        }
      }
      lijn.append(score(matrix[rij][j]));
      if (toernooitype > 0 && (j / toernooitype) * toernooitype != j ) {
        lijn.append("}");
      }
      lijn.append(" ");
    }
  }

  private static void maakLatexMatrixBodyPnt(StringBuilder lijn, int rij) {
      var pntn  = spelers.get(rij).getPunten().intValue();
      var decim = Utilities.kwart(spelers.get(rij).getPunten());
      lijn.append("& ").append(
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "")).append(decim);
      if (toernooitype > 0) {
        var wpntn   = spelers.get(rij).getTieBreakScore().intValue();
        var wdecim  = Utilities.kwart(spelers.get(rij).getTieBreakScore());
        lijn.append(" & ").append(spelers.get(rij).getPartijen()).append(" & ");
        lijn.append(((wpntn == 0 && "".equals(wdecim))
                     || wpntn >= 1 ? wpntn : "")).append(wdecim);
      }
      lijn.append(" ");
  }

  private static void maakLatexMatrixHead1Mat(StringBuilder lijn,
                                              int kolommen) {
    for (var i = 0; i < kolommen; i++) {
      lijn.append("c | ");
    }
  }

  private static void maakLatexMatrixHead2Mat(StringBuilder lijn,
                                              int kolommen, int noSpelers) {
    for (var i = 0; i < (toernooitype == 0 ? kolommen : noSpelers); i++) {
      if (toernooitype < 2) {
        lijn.append("& ").append(TEKSTKLEUR).append((i + 1)).append(" ");
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{").append(TEKSTKLEUR)
            .append((i + 1)).append("} ");
      }
    }
  }

  private static void maakLatexMatrixHead3Mat(StringBuilder lijn,
                                              int noSpelers) {
    for (var i = 0; i < noSpelers; i++) {
      lijn.append("& ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.wit"))
          .append(" & ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.zwart"))
          .append(" ");
    }
  }

  private static void maakLatexMatrixHead2Pnt(StringBuilder lijn) {
    lijn.append("& ").append(TEKSTKLEUR)
        .append(resourceBundle.getString("tag.punten"));
    if (toernooitype > 0) {
      lijn.append(" & ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.partijen"))
          .append(" & ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.sb"));
    }
    lijn.append(" ");
  }

  private static void maakRondeheading(int ronde, String datum)
      throws BestandException {
    output.write("   \\begin{tabular}{ | p{32mm} C{2mm} p{32mm} | C{5mm} | }");
    output.write("    " + LTX_HLINE);
    output.write("    \\rowcolor{headingkleur}");
    output.write("    \\multicolumn{2}{l}{\\color{headingtekstkleur}"
                  + MessageFormat.format(
                        resourceBundle.getString("tabel.ronde"), ronde)
                  + "} & \\multicolumn{2}{r}{\\color{headingtekstkleur}"
                  + datum + "} \\\\");
    output.write("    " + LTX_HLINE);
  }

  private static void maakUitslagentabel()
      throws BestandException {
    var iter    = schema.iterator();
    var partij  = iter.next();
    var vorige  = Integer.parseInt(partij.getRonde().getRound()
                                                    .split("\\.")[0]);
    maakRondeheading(vorige, DoosUtils.nullToEmpty(kalender[vorige]));

    do {
      var ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
      if (ronde != vorige) {
        output.write("    " + LTX_HLINE);
        output.write("   " + LTX_END_TABULAR);
        maakRondeheading(ronde, DoosUtils.nullToEmpty(kalender[ronde]));
        vorige  = ronde;
      }

      var uitslag = partij.getUitslag().replace("1/2", Utilities.kwart(0.5))
                          .replace("-", (partij.isForfait() ? "\\textbf{f}"
                                                            : "-"))
                          .replace('*', '-');
      var wit     = partij.getWitspeler().getVolledigenaam();
      var zwart   = partij.getZwartspeler().getVolledigenaam();
      if (!partij.isRanked()
          || partij.isBye()) {
        output.write("    \\rowcolor{headingkleur!15}");
      }
      output.write("    " + wit + " & - & "
                          + zwart + " & " + uitslag + " " + LTX_EOL);
      if (iter.hasNext()) {
        partij  = iter.next();
      } else {
        partij  = null;
      }
    } while (null != partij);

    output.write("    " + LTX_HLINE);
    output.write("   " + LTX_END_TABULAR);
  }

  private static void matrixEerst(StringBuilder lijn, int kolommen,
                                  int noSpelers) throws BestandException {
    maakLatexMatrixHead1Mat(lijn, kolommen);
    lijn.append("r | r | r | ");
    output.write(lijn.append("}").toString());
    output.write("    " + LTX_HLINE);
    output.write("    " + RIJKLEUR);
    lijn  = new StringBuilder();
    lijn.append("    \\multicolumn{2}{|c|}{} ");
    maakLatexMatrixHead2Mat(lijn, kolommen, noSpelers);
    maakLatexMatrixHead2Pnt(lijn);
    lijn.append(LTX_EOL);
    if (toernooitype == 2) {
      output.write(lijn.toString());
      output.write("    \\cline{3-" + (2 + kolommen) + "}");
      output.write("    " + RIJKLEUR);
      lijn  = new StringBuilder();
      lijn.append("    \\multicolumn{2}{|c|}{} ");
      maakLatexMatrixHead3Mat(lijn, noSpelers);
      lijn.append("& & & ").append(LTX_EOL);
    }
    output.write(lijn.toString());
    output.write("    " + LTX_HLINE);
  }

  private static void matrixLaatst(StringBuilder lijn, int kolommen,
                                   int noSpelers) throws BestandException {
    lijn.append("r | r | r | ");
    maakLatexMatrixHead1Mat(lijn, kolommen);
    output.write(lijn.append("}").toString());
    output.write("    " + LTX_HLINE);
    output.write("    " + RIJKLEUR);
    lijn  = new StringBuilder();
    lijn.append("    \\multicolumn{2}{|c|}{}");
    maakLatexMatrixHead2Pnt(lijn);
    maakLatexMatrixHead2Mat(lijn, kolommen, noSpelers);
    lijn.append(LTX_EOL);
    if (toernooitype == 2) {
      output.write(lijn.toString());
      output.write("    \\cline{6-" + (5 + kolommen) + "}");
      output.write("    " + RIJKLEUR);
      lijn  = new StringBuilder();
      lijn.append("    \\multicolumn{2}{|c|}{} & & & ");
      maakLatexMatrixHead3Mat(lijn, noSpelers);
      lijn.append(LTX_EOL);
    }
    output.write(lijn.toString());
    output.write("    " + LTX_HLINE);
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

  private static String schrijf(String regel, String status, int kolommen,
                                int noSpelers, Map<String, String> parameters)
      throws BestandException {
    var start = regel.split(" ")[0];
    switch(start) {
      case "%@Include":
        switch (regel.split(" ")[1].toLowerCase()) {
          case "deelnemers":
            if (!spelers.isEmpty()) {
              maakDeelnemerslijst();
            }
            break;
          case TAG_KALENDER:
            if (!spelers.isEmpty()) {
              maakKalender();
            }
            break;
          case "matrix":
            if (null != matrix) {
              maakLatexMatrix(kolommen, noSpelers,
                              paramBundle
                                  .getBoolean(CaissaTools.PAR_MATRIXEERST));
            }
            break;
          case "uitslagen":
            if (!schema.isEmpty()) {
              maakUitslagentabel();
            }
            break;
         default:
            break;
        }
        break;
      case "%@IncludeStart":
        status  = setStatus(regel.split(" ")[1].toLowerCase());
        break;
      case "%@IncludeEind":
        status  = NORMAAL;
        break;
      default:
        schrijfUitTemplate(regel, parameters, status);
        break;
    }

    return status;
  }

  private static void schrijfUitTemplate(String regel,
                                         Map<String, String> params,
                                         String status)
      throws BestandException {
    switch (status) {
      case KYW_DEELNEMERS:
        output.write(replaceParameters(regel, params));
        break;
      case KYW_LOGO:
        if (paramBundle.containsParameter(CaissaTools.PAR_LOGO)) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_MATRIX:
        if (null != matrix) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_SUBTITEL:
        if (paramBundle.containsParameter(CaissaTools.PAR_SUBTITEL)) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_TITEL:
        if (paramBundle.containsParameter(CaissaTools.PAR_TITEL)) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_UITSLAGEN:
        if (null != schema) {
          output.write(replaceParameters(regel, params));
        }
        break;
      default:
        output.write(replaceParameters(regel, params));
        break;
      }
  }

  private static String score(double score) {
    if (score == 0.0) {
      return "0";
    }

    if (score < 1.0) {
      return Utilities.kwart(score);
    }

    return "" + ((Double) score).intValue() + Utilities.kwart(score);
  }

  private static String setStatus(String keyword) {
    String  status;
    switch(keyword) {
      case "deelnemers":
        status  = KYW_DEELNEMERS;
        break;
      case TAG_KALENDER:
        status  = KYW_KALENDER;
        break;
      case "logo":
        status  = KYW_LOGO;
        break;
      case "matrix":
        status  = KYW_MATRIX;
        break;
      case "subtitel":
        status  = KYW_SUBTITEL;
        break;
      case "titel":
        status  = KYW_TITEL;
        break;
      case "uitslagen":
        status  = KYW_UITSLAGEN;
        break;
      default:
        status  = "";
        break;
    }

    return status;
  }

  private static void vulParams(Map<String, String> params) {
    if (paramBundle.containsParameter(CaissaTools.PAR_SUBTITEL)) {
      params.put(CaissaTools.PAR_SUBTITEL,
                 paramBundle.getString(CaissaTools.PAR_SUBTITEL));
    }
    if (paramBundle.containsParameter(CaissaTools.PAR_LOGO)) {
      params.put(CaissaTools.PAR_LOGO,
                 paramBundle.getString(CaissaTools.PAR_LOGO));
    }
    if (paramBundle.containsParameter(CaissaTools.PAR_TITEL)) {
      params.put(CaissaTools.PAR_TITEL,
                 paramBundle.getString(CaissaTools.PAR_TITEL));
    }
    params.put("activiteit", resourceBundle.getString("label.activiteit"));
    params.put("datum", resourceBundle.getString("label.datum"));
    params.put("deelnemerslijst",
               resourceBundle.getString("label.deelnemerslijst"));
    params.put("forfait", resourceBundle.getString("message.forfait")
                                        .toLowerCase()
                                        .replace("<b>", "\\textbf{")
                                        .replace("</b>", "}"));
    params.put(TAG_KALENDER, resourceBundle.getString("label.kalender"));
    params.put("notRanked", resourceBundle.getString("message.notranked"));
  }
}
