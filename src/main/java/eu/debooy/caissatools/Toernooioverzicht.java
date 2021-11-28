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
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_KALENDER;
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_KALENDER_DATUM;
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_KALENDER_RONDE;
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_SPELERS;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.io.File;
import java.nio.charset.Charset;
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
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static  List<Spelerinfo>  deelnemers;
  private static  JSONArray         jsonKalender;
  private static  String[]          kalender;
  private static  double[][]        matrix;
  private static  TekstBestand      output;
  private static  Set<Partij>       schema;
  private static  List<Spelerinfo>  spelers;
  private static  TekstBestand      texInvoer;
  private static  int               toernooitype;

  private static final String LTX_HLINE       = "\\hline";
  private static final String LTX_END_TABULAR = "\\end{tabular}";
  private static final String LTX_EOL         = "\\\\";
  private static final String KYW_DEELNEMERS  = "D";
  private static final String KYW_KALENDER    = "K";
  private static final String KYW_LOGO        = "L";
  private static final String KYW_MATRIX      = "M";
  private static final String KYW_SUBTITEL    = "S";
  private static final String KYW_TITEL       = "T";
  private static final String KYW_UITSLAGEN   = "U";
  private static final String NORMAAL         = "N";

  private static final String TAG_KALENDER    = "kalender";

  private static final String KLEUR           = "\\columncolor{headingkleur}";
  private static final String KLEURLICHT      = "\\columncolor{headingkleur!25}";
  private static final String RIJKLEUR        = "\\rowcolor{headingkleur}";
  private static final String RIJKLEURLICHT   = "\\rowcolor{headingkleur!25}";
  private static final String RIJKLEURLICHTER = "\\rowcolor{headingkleur!10}";
  private static final String TEKSTKLEUR      = "\\color{headingtekstkleur}";

  Toernooioverzicht() {}

  private static void bepaalTexInvoer()
      throws BestandException {
    if (parameters.containsKey(CaissaTools.PAR_TEMPLATE)) {
      texInvoer =
          new TekstBestand.Builder()
                          .setBestand(
                              parameters.get(CaissaTools.PAR_TEMPLATE))
                          .setCharset(parameters.get(PAR_CHARSETIN)).build();
    } else {
      texInvoer =
          new TekstBestand.Builder()
                          .setBestand("Overzicht.tex")
                          .setClassLoader(classloader)
                          .setCharset(parameters.get(PAR_CHARSETIN)).build();
    }
  }

  public static void execute(String[] args) {
    List<String>  template        = new ArrayList<>();

    Banner.printMarcoBanner(
        resourceBundle.getString("banner.toernooioverzicht"));

    if (!setParameters(args)) {
      return;
    }

    var bestand   = parameters.get(CaissaTools.PAR_BESTAND);

    output        = null;
    toernooitype  =
        CaissaUtils.getToernooitype(parameters.get(CaissaTools.PAR_ENKEL));

    try {
      bepaalTexInvoer();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
                                   parameters.get(CaissaTools.PAR_TEMPLATE)));
      return;
    }

    try {
      while (texInvoer.hasNext()) {
        template.add(texInvoer.next());
      }

      output  = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + bestand + EXT_TEX)
                                .setCharset(parameters.get(PAR_CHARSETIN))
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

    JsonBestand     competitie  = null;
    Collection<PGN> partijen    = new TreeSet<>(new PGN.ByEventComparator());
    try {
      competitie    =
          new JsonBestand.Builder()
                         .setBestand(parameters.get(PAR_INVOERDIR)
                                     + parameters.get(CaissaTools.PAR_SCHEMA)
                                     + EXT_JSON)
                         .setCharset(parameters.get(PAR_CHARSETIN))
                         .build();
      partijen.addAll(
          CaissaUtils.laadPgnBestand(parameters.get(PAR_INVOERDIR)
                                     + bestand + EXT_PGN,
                                     parameters.get(PAR_CHARSETIN)));
      spelers       = new ArrayList<>();
      CaissaUtils.vulSpelers(spelers, competitie.getArray(JSON_TAG_SPELERS));

      deelnemers    = new ArrayList<>();
      deelnemers.addAll(spelers);
      deelnemers.sort(new Spelerinfo.ByNaamComparator());

      jsonKalender  = competitie.getArray(JSON_TAG_KALENDER);
      kalender      =
          CaissaUtils.vulKalender(JSON_TAG_KALENDER_RONDE, spelers.size(),
                                  toernooitype, jsonKalender);
    } catch (PgnException | BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      if (null != competitie) {
        try {
          competitie.close();
        } catch (BestandException e) {
          DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        }
      }
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
                                  parameters
                                    .get(CaissaTools.PAR_MATRIXOPSTAND)
                                    .equals(DoosConstants.WAAR),
                                  CaissaConstants.TIEBREAK_SB);
    matrix    = CaissaUtils.verwijderNietActief(spelers, matrix, toernooitype);
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
      MessageFormat.format(resourceBundle.getString("label.bestand"),
                           parameters.get(PAR_UITVOERDIR) + bestand + EXT_TEX));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
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
                             CaissaTools.PAR_SUBTITEL, CaissaTools.PAR_TITEL));
    DoosUtils.naarScherm();
  }

  private static void maakDeelnemerslijst() throws BestandException {
    for (Spelerinfo deelnemer : deelnemers) {
      output.write("    " + deelnemer.getVolledigenaam() + " & "
              + deelnemer.getTelefoon() + " & "
              + deelnemer.getEmail() + " " + LTX_EOL);
      output.write("    " + LTX_HLINE);
    }
  }

  private static void maakKalender() throws BestandException {
    for (var i = 0; i < jsonKalender.size(); i++) {
      var item  = (JSONObject) jsonKalender.get(i);
      var lijn  = new StringBuilder();
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_EXTRA)) {
        lijn.append(item.get(JSON_TAG_KALENDER_DATUM).toString())
            .append(" & ")
            .append(item.get(CaissaConstants.JSON_TAG_KALENDER_EXTRA)
                        .toString())
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_INHAAL)) {
        output.write(RIJKLEURLICHTER);
        lijn.append(item.get(JSON_TAG_KALENDER_DATUM).toString())
            .append(" & ")
            .append(MessageFormat.format(
                        resourceBundle.getString("label.latex.inhaal"),
                        item.get(CaissaConstants.JSON_TAG_KALENDER_INHAAL)
                            .toString()))
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(JSON_TAG_KALENDER_RONDE)) {
        output.write(RIJKLEURLICHT);
        lijn.append(item.get(JSON_TAG_KALENDER_DATUM).toString())
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
        } else {
          if ((j / toernooitype) * toernooitype != j ) {
            lijn.append("\\multicolumn{1}"
                        + "{>{").append(KLEURLICHT).append("}c|}{");
          }
        }
      }
      if (matrix[rij][j] == 0.0) {
        lijn.append("0");
      } else if (matrix[rij][j] == 0.5) {
        lijn.append(Utilities.kwart(0.5));
      } else if (matrix[rij][j] >= 1.0) {
        lijn.append(((Double)matrix[rij][j]).intValue())
            .append(Utilities.kwart(matrix[rij][j]));
      }
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
                          .replace('-', (partij.isForfait() ? 'F' : '-'))
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
                              getParameter(CaissaTools.PAR_MATRIXEERST)
                                  .equals(DoosConstants.WAAR));
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
        if (parameters.containsKey(CaissaTools.PAR_LOGO)) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_MATRIX:
        if (null != matrix) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_SUBTITEL:
        if (parameters.containsKey(CaissaTools.PAR_SUBTITEL)) {
          output.write(replaceParameters(regel, params));
        }
        break;
      case KYW_TITEL:
        if (parameters.containsKey(CaissaTools.PAR_TITEL)) {
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

  private static boolean setParameters(String[] args) {
    var           arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_ENKEL,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_LOGO,
                                          CaissaTools.PAR_MATRIX,
                                          CaissaTools.PAR_MATRIXEERST,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          CaissaTools.PAR_SCHEMA,
                                          CaissaTools.PAR_SUBTITEL,
                                          CaissaTools.PAR_TEMPLATE,
                                          CaissaTools.PAR_TITEL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_SCHEMA});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_LOGO);
    setParameter(arguments, CaissaTools.PAR_MATRIX, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_MATRIXEERST, DoosConstants.ONWAAR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setBestandParameter(arguments, CaissaTools.PAR_SCHEMA, EXT_JSON);
    setParameter(arguments, CaissaTools.PAR_SUBTITEL);
    setParameter(arguments, CaissaTools.PAR_TEMPLATE);
    setParameter(arguments, CaissaTools.PAR_TITEL);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
    }
    if (parameters.containsKey(CaissaTools.PAR_BESTAND)
        && parameters.get(CaissaTools.PAR_BESTAND).contains(";")) {
      if (!parameters.containsKey(CaissaTools.PAR_SUBTITEL)
          || !parameters.containsKey(CaissaTools.PAR_TITEL)) {
        fouten.add(resourceBundle.getString(CaissaTools.ERR_BIJBESTAND));
      }
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
    if (parameters.containsKey(CaissaTools.PAR_SUBTITEL)) {
      params.put(CaissaTools.PAR_SUBTITEL,
                 parameters.get(CaissaTools.PAR_SUBTITEL));
    }
    if (parameters.containsKey(CaissaTools.PAR_LOGO)) {
      params.put(CaissaTools.PAR_LOGO, parameters.get(CaissaTools.PAR_LOGO));
    }
    if (parameters.containsKey(CaissaTools.PAR_TITEL)) {
      params.put(CaissaTools.PAR_TITEL, parameters.get(CaissaTools.PAR_TITEL));
    }
    params.put("activiteit", resourceBundle.getString("label.activiteit"));
    params.put("datum", resourceBundle.getString("label.datum"));
    params.put("deelnemerslijst",
               resourceBundle.getString("label.deelnemerslijst"));
    params.put("forfait", resourceBundle.getString("message.forfait")
                                        .replace("<b>", "\\textbf{")
                                        .replace("</b>", "}"));
    params.put(TAG_KALENDER, resourceBundle.getString("label.kalender"));
    params.put("notRanked", resourceBundle.getString("message.notranked"));
  }
}
