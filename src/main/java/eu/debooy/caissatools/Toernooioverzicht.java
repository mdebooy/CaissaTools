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
import eu.debooy.caissa.Competitie;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
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

  private static  Competitie    competitie;
  private static  double[][]    matrix;
  private static  TekstBestand  output;
  private static  Set<Partij>   schema;

  private static final  String  DEF_TEMPLATE  = "Overzicht.tex";

  private static final  String  LTX_HLINE       = "\\hline";
  private static final  String  LTX_END_TABULAR = "\\end{tabular}";
  private static final  String  LTX_EOL         = "\\\\";

  private static final  String  KYW_DEELNEMERS  = "D";
  private static final  String  KYW_INHALEN     = "I";
  private static final  String  KYW_KALENDER    = "K";
  private static final  String  KYW_KLEUREN     = "C";
  private static final  String  KYW_LOGO        = "L";
  private static final  String  KYW_MATRIX      = "M";
  private static final  String  KYW_SKIP        = "X";
  private static final  String  KYW_SUBTITEL    = "S";
  private static final  String  KYW_TITEL       = "T";
  private static final  String  KYW_UITSLAGEN   = "U";

  private static final  String  NORMAAL         = "N";

  private static final  String  KLEUR           = "\\columncolor{headingkleur}";
  private static final  String  KLEURLICHT      =
      "\\columncolor{headingkleur!25}";
  private static final  String  RIJKLEUR        = "\\rowcolor{headingkleur}";
  private static final  String  RIJKLEURLICHT   = "\\rowcolor{headingkleur!25}";
  private static final  String  RIJKLEURLICHTER = "\\rowcolor{headingkleur!10}";
  private static final  String  TEKSTKLEUR      = "\\color{headingtekstkleur}";

  protected Toernooioverzicht() {}

  private static TekstBestand bepaalTexInvoer() throws BestandException {
    if (paramBundle.containsParameter(CaissaTools.PAR_TEMPLATE)) {
      return
          new TekstBestand.Builder()
                          .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_TEMPLATE))
                          .build();
    } else {
      return
          new TekstBestand.Builder()
                          .setBestand(DEF_TEMPLATE)
                          .setClassLoader(classloader)
                          .build();
    }
  }

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_TOERNOOIOVERZICHT)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    output  = null;

    try {
      bepaalTexInvoer();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_TEMPLATE),
              paramBundle.getBestand(CaissaTools.PAR_TEMPLATE)));
      return;
    }

    List<String>  template  = new ArrayList<>();
    try (var texInvoer = bepaalTexInvoer()) {
      while (texInvoer.hasNext()) {
        template.add(texInvoer.next());
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_UITVOER))
                          .setLezen(false).build();
      competitie  =
          new Competitie(paramBundle.getBestand(CaissaTools.PAR_SCHEMA));
      competitie.sorteerOpNaam();
    } catch (BestandException | CompetitieException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    Collection<PGN> partijen  = new TreeSet<>(new PGN.ByEventComparator());
    try {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(
              paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                     BestandConstants.EXT_PGN)));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    var noSpelers = competitie.getDeelnemers().size();
    matrix        = null;

    // Maak de Matrix
    if (!competitie.isMatch()) {
      schema  =
          CaissaUtils.genereerSpeelschema(competitie, partijen);
    } else {
      schema  = new TreeSet<>();
    }

    // Bepaal de score en weerstandspunten.
    var opStand = paramBundle.getBoolean(CaissaTools.PAR_MATRIXOPSTAND);
    matrix      =  CaissaUtils.vulToernooiMatrix(partijen, competitie,
                                                 opStand);
    if (Boolean.TRUE.equals(paramBundle.getBoolean(CaissaTools.PAR_AKTIEF))) {
      matrix    = CaissaUtils.verwijderNietActief(matrix, competitie);
    }
    var kolommen  = null == matrix ? 0 : matrix[0].length;

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

    maakVCards();

    DoosUtils.naarScherm(
      MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                           paramBundle.getBestand(CaissaTools.PAR_UITVOER)));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void gespeeldekleur(Partij partij, char[][] kleuren,
                                     char metWit, char metZwart) {
    if (partij.isBye() || partij.isForfait()) {
      return;
    }

    var wit   = competitie.getSpeler(partij.getWitspeler().getNaam())
                          .getSpelerSeq() - 1;
    var zwart = competitie.getSpeler(partij.getZwartspeler().getNaam())
                          .getSpelerSeq() - 1;
    var ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
    kleuren[wit][ronde-1]   = metWit;
    kleuren[zwart][ronde-1] = metZwart;
  }

  private static void maakDeelnemerslijst() throws BestandException {
    var spelers   = competitie.getSpelers();

    Collections.sort(spelers, new Spelerinfo.ByNaamComparator());

    for (var speler : spelers) {
      if (!speler.getVolledigenaam()
                    .equalsIgnoreCase(CaissaConstants.BYE)) {
        output.write("    " + speler.getVolledigenaam() + " & "
                + DoosUtils.nullToEmpty(speler.getTelefoon()) + " & "
                + DoosUtils.nullToEmpty(speler.getEmail()).replace("_", "\\_")
                + " " + LTX_EOL);
        output.write("    " + LTX_HLINE);
      }
    }
  }

  private static void maakGespeeldeKleuren() throws BestandException {
    if (!competitie.isZwitsers()) {
      return;
    }

    var metWit    = resourceBundle.getString("tag.wit").charAt(0);
    var metZwart  = resourceBundle.getString("tag.zwart").charAt(0);
    var rondes    = competitie.getRondes();
    var spelers   = competitie.getSpelers().size();
    var kleuren   = new char[spelers][rondes];

    for (var i = 0; i < spelers; i++) {
      for (var j = 0; j < rondes; j++) {
        kleuren[i][j] = ' ';
      }
    }

    schema.forEach(partij -> gespeeldekleur(partij, kleuren, metWit, metZwart));

    schrijfCompact();
    var lijn  = new StringBuilder();
    lijn.append("   \\begin{tabular} { | l |");
    for (var i = 0; i < competitie.getRondes(); i++) {
      lijn.append(" c |");
    }
    lijn.append(" c | }");
    output.write(lijn.toString());
    output.write("    " + RIJKLEUR);
    lijn  = new StringBuilder();
    lijn.append("    \\multicolumn{").append(2+rondes)
        .append("}{c}{\\large\\color{headingtekstkleur}")
        .append(resourceBundle.getString("label.kleuren.gespeeld"))
        .append("} ").append(LTX_EOL);
    output.write(lijn.toString());
    output.write("    " + RIJKLEUR);
    lijn  = new StringBuilder();
    lijn.append("    &");
    for (var i = 0; i < competitie.getRondes(); i++) {
      lijn.append(" \\color{headingtekstkleur}").append(i+1).append(" &");
    }
    lijn.append(" ").append(LTX_EOL);
    output.write(lijn.toString());

    schrijfGespeeldeKleurenTabel(kleuren, metWit, metZwart);
  }

  private static void maakInhaaloverzicht() throws BestandException {
    if (competitie.isZwitsers()) {
      return;
    }

    var jsonInhalen = competitie.getInhaalpartijen();

    if (jsonInhalen.isEmpty()) {
      output.write("    \\multicolumn{5}{c}{"
                    + resourceBundle.getString("message.geen.inhaalpartijen")
                    + "}");
      return;
    }

    var wit   = new Spelerinfo();
    var zwart = new Spelerinfo();
    for (var i = 0; i < jsonInhalen.size() ; i++) {
      var item  = (JSONObject) jsonInhalen.get(i);
      wit.setNaam((String) item.get("wit"));
      zwart.setNaam((String) item.get("zwart"));
      output.write(MessageFormat.format("    {0} & {1} & {2} & - & {3} {4}",
                                        item.get("ronde"),
                                        item.get("datum"),
                                        wit.getVolledigenaam(),
                                        zwart.getVolledigenaam(),
                                        LTX_EOL));
    }
  }

  private static void maakKalender() throws BestandException {
    if (competitie.getKalender().isEmpty()) {
      return;
    }

    var jsonKalender  = competitie.getKalender();

    for (var i = 0; i < jsonKalender.size(); i++) {
      var item  = (JSONObject) jsonKalender.get(i);
      var lijn  = new StringBuilder();
      if (item.containsKey(Competitie.JSON_TAG_KALENDER_EXTRA)) {
        lijn.append(item.get(Competitie.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(item.get(Competitie.JSON_TAG_KALENDER_EXTRA)
                        .toString())
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(Competitie.JSON_TAG_KALENDER_INHAAL)) {
        output.write(RIJKLEURLICHTER);
        lijn.append(item.get(Competitie.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(MessageFormat.format(
                        resourceBundle.getString("label.latex.inhaal"),
                        item.get(Competitie.JSON_TAG_KALENDER_INHAAL)
                            .toString()))
            .append(" ").append(LTX_EOL);
      }
      if (item.containsKey(Competitie.JSON_TAG_KALENDER_RONDE)) {
        output.write(RIJKLEURLICHT);
        lijn.append(item.get(Competitie.JSON_TAG_KALENDER_DATUM)
                        .toString())
            .append(" & ")
            .append(MessageFormat.format(
                        resourceBundle.getString("label.latex.ronde"),
                        item.get(Competitie.JSON_TAG_KALENDER_RONDE)
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
    lijn.append("   \\resizebox{\\columnwidth}{!}{\\begin{tabular} { | c l | ");
    if (matrixEerst) {
      matrixEerst(lijn, kolommen, noSpelers);
    } else {
      matrixLaatst(lijn, kolommen, noSpelers);
    }

    // Body
    var deelnemers  = competitie.getDeelnemers();
    for (var i = 0; i < noSpelers; i++) {
      lijn  = new StringBuilder();
      var speler  = deelnemers.get(i);

      if (competitie.isMatch()) {
        lijn.append("\\multicolumn{2}{|l|}{")
            .append(speler.getVolledigenaam())
            .append("} ");
      } else {
        lijn.append((i + 1)).append(" & ")
            .append(speler.getVolledigenaam())
            .append(" ");
      }

      if (matrixEerst) {
        maakLatexMatrixBodyMat(lijn, i, kolommen);
        maakLatexMatrixBodyPnt(lijn, speler);
      } else {
        maakLatexMatrixBodyPnt(lijn, speler);
        maakLatexMatrixBodyMat(lijn, i, kolommen);
      }

      lijn.append(" ").append(LTX_EOL);
      output.write(lijn.toString());
      output.write("    " + LTX_HLINE);
    }
    output.write("   " + LTX_END_TABULAR + "}");
  }

  private static void maakLatexMatrixBodyMat(StringBuilder lijn, int rij,
                                             int kolommen) {
    for (var j = 0; j < kolommen; j++) {
      lijn.append("& ");
      if (!competitie.isMatch()) {
        if (rij == j / competitie.getHeenTerug()) {
          lijn.append("\\multicolumn{1}"
                      + "{>{").append(KLEUR).append("}c|}{} ");
          continue;
        }
        if ((j / competitie.getHeenTerug()) * competitie.getHeenTerug() != j ) {
          lijn.append("\\multicolumn{1}"
                      + "{>{").append(KLEURLICHT).append("}c|}{");
        }
      }
      lijn.append(score(matrix[rij][j]));
      if (competitie.getType() > 0
          && (j / competitie.getHeenTerug())
                * competitie.getHeenTerug() != j ) {
        lijn.append("}");
      }
      lijn.append(" ");
    }
    if (competitie.metBye()) {
      lijn.append("& ")
          .append(competitie.getDeelnemers().get(rij).getByeScore().intValue())
          .append(" ");
    }
  }

  private static void maakLatexMatrixBodyPnt(StringBuilder lijn,
                                             Spelerinfo speler) {
    var pntn  = speler.getPunten().intValue();
      var decim = Utilities.kwart(speler.getPunten());
      lijn.append("& ").append(
          ((pntn == 0 && "".equals(decim)) || pntn >= 1 ?
              pntn : "")).append(decim);
      if (!competitie.isMatch()) {
        var wpntn   = speler.getTieBreakScore().intValue();
        var wdecim  = Utilities.kwart(speler.getTieBreakScore());
        lijn.append(" & ").append(speler.getPartijen()).append(" & ");
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
    if (competitie.metBye()) {
      lijn.append("c | ");
    }
  }

  private static void maakLatexMatrixHead2Mat(StringBuilder lijn,
                                              int kolommen, int noSpelers) {
    for (var i = 0; i < (competitie.isMatch() ? kolommen : noSpelers); i++) {
      if (competitie.isEnkel()) {
        lijn.append("& ").append(TEKSTKLEUR).append((i + 1)).append(" ");
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{").append(TEKSTKLEUR)
            .append((i + 1)).append("} ");
      }
    }
    if (competitie.metBye()) {
      lijn.append("& \\multicolumn{1}{c|}{").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.bye")).append("} ");
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
    if (competitie.metBye()) {
      lijn.append("& ");
    }
  }

  private static void maakLatexMatrixHead2Pnt(StringBuilder lijn) {
    lijn.append("& ").append(TEKSTKLEUR)
        .append(resourceBundle.getString("tag.punten"));
    if (!competitie.isMatch()) {
      lijn.append(" & ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.partijen"))
          .append(" & ").append(TEKSTKLEUR)
          .append(resourceBundle.getString("tag.sb"));
    }
    lijn.append(" ");
  }

  private static void maakRondeheading(int ronde, String datum)
      throws BestandException {
    output.write("   \\begin{tabular}[t]{ | b{31mm} C{2mm} b{31mm} |"
                  + " C{7mm} | }");
    output.write("    " + LTX_HLINE);
    output.write("    " + RIJKLEUR);
    output.write("    \\multicolumn{2}{l}{\\color{headingtekstkleur}"
                  + MessageFormat.format(
                        resourceBundle.getString("tabel.ronde"), ronde)
                  + "} & \\multicolumn{2}{r}{\\color{headingtekstkleur}"
                  + datum + "} \\\\");
    output.write("    " + LTX_HLINE);
  }

  private static void maakUitslagentabel() throws BestandException {
    var iter            = schema.iterator();
    var kalender        = competitie.getSpeeldata();
    var metInhaaldatum  =
            paramBundle.getBoolean(CaissaTools.PAR_METINHAALDATUM);
    var partij          = iter.next();
    var vorige          = Integer.parseInt(partij.getRonde().getRound()
                                                 .split("\\.")[0]);
    maakRondeheading(vorige,
         DoosUtils.nullToEmpty(Datum.fromDate(kalender.get(vorige-1))));

    do {
      var ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
      if (ronde != vorige) {
        output.write("    " + LTX_HLINE);
        output.write("   " + LTX_END_TABULAR);
        maakRondeheading(ronde,
            DoosUtils.nullToEmpty(Datum.fromDate(kalender.get(ronde-1))));
        vorige  = ronde;
      }

      var nietgespeeld  = "-";
      if (Boolean.TRUE.equals(metInhaaldatum) && !partij.isGespeeld()) {
        nietgespeeld    = competitie.getInhaaldatum(partij);
        if (!nietgespeeld.equals("-")) {
          nietgespeeld  = "\\tiny " + nietgespeeld;
        }
      }
      var uitslag       =
              partij.getUitslag().replace("1/2", Utilities.kwart(0.5))
                    .replace("-", (partij.isForfait() ? "\\textbf{f}"
                                                      : "-"))
                    .replace("*", nietgespeeld);
      var wit           = partij.getWitspeler().getVolledigenaam();
      var zwart         = partij.getZwartspeler().getVolledigenaam();
      if (!partij.isRanked()
          || (partij.isBye() && !competitie.metBye())) {
        output.write("    " + RIJKLEURLICHTER);
      }
      output.write(String.format("    %s & - & %s & %s %s",
                                 wit, zwart, uitslag, LTX_EOL));
      partij            = iter.hasNext() ? iter.next() : null;
    } while (null != partij);

    output.write("    " + LTX_HLINE);
    output.write("   " + LTX_END_TABULAR);
  }

  private static void maakVCards() {
    try (var vcards = new TekstBestand.Builder()
                                      .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_UITVOER,
                                                     ".vcf"))
                                      .setLezen(false).build()) {
      var spelers   = competitie.getDeelnemers();

      spelers.sort(new Spelerinfo.ByNaamComparator());
      spelers.forEach(speler -> {
        try {
          vcards.write("BEGIN:VCARD");
          vcards.write("VERSION:2.1");
          vcards.write(String.format("N:%s;%s;;;",
                                     speler.getVoornaam(),
                                     speler.getAchternaam()));
          vcards.write(String.format("FN:%s %s",
                                     speler.getAchternaam().toUpperCase(),
                                     speler.getVoornaam()));
          if (DoosUtils.isNotBlankOrNull(speler.getTelefoon())) {
            schrijfVcardTelefoon(speler.getTelefoon(), vcards);
          }
          if (DoosUtils.isNotBlankOrNull(speler.getEmail())) {
            schrijfVcardEmail(speler.getEmail(), vcards);
          }
          vcards.write("END:VCARD");
        } catch (BestandException e) {
          DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        }
      });
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
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
    if (!competitie.isMatch() && competitie.isDubbel()) {
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
    if (!competitie.isMatch() && competitie.isDubbel()) {
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

  private static String schrijf(String regel, String status, int kolommen,
                                int noSpelers, Map<String, String> parameters)
      throws BestandException {
    var start = regel.split(" ")[0];
    switch(start) {
      case "%@Include":
        switch (regel.split(" ")[1].toLowerCase()) {
          case "compact":
            schrijfCompact();
            break;
          case "deelnemers":
            if (!competitie.getDeelnemers().isEmpty()) {
              maakDeelnemerslijst();
            }
            break;
          case Competitie.JSON_TAG_INHALEN:
            maakInhaaloverzicht();
            break;
          case Competitie.JSON_TAG_KALENDER:
            if (!competitie.getDeelnemers().isEmpty()) {
              maakKalender();
            }
            break;
          case "kleuren":
            maakGespeeldeKleuren();
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

  private static void schrijfCompact() throws BestandException {
    if (paramBundle.getBoolean(CaissaTools.PAR_COMPACT)) {
      output.write("   \\\\[5mm]");
    } else {
      output.write("   \\newpage");
    }
  }

  private static void schrijfGespeeldeKleurenTabel(char[][] kleuren,
                                                   char metWit, char metZwart)
      throws BestandException {
    var lijn        = new StringBuilder();
    var deelnemers  = competitie.getDeelnemers();
    for (var i = 0; i < deelnemers.size(); i++) {
      var evenwicht = 0;
      var seq       = deelnemers.get(i).getSpelerSeq() - 1;
      lijn.append("    ").append(deelnemers.get(i).getVolledigenaam())
          .append(" &");
      for (var j = 0; j < competitie.getRondes(); j++) {
        lijn.append(" ").append(kleuren[seq][j]).append(" &");
        if (kleuren[seq][j] == metWit) {
          evenwicht++;
        }
        if (kleuren[seq][j] == metZwart) {
          evenwicht--;
        }
      }
      lijn.append(" ").append("\\multicolumn{1}{>{")
          .append(KLEURLICHT).append("}c|}{");
      if (evenwicht < 0) {
        lijn.append(metWit);
      }
      if (evenwicht > 0) {
        lijn.append(metZwart);
      }
      lijn.append("} ").append(LTX_EOL);
      output.write(lijn.toString());
      output.write("    " + LTX_HLINE);
      lijn      = new StringBuilder();
    }
  }

  private static void schrijfUitTemplate(String regel,
                                         Map<String, String> params,
                                         String status)
      throws BestandException {
    switch (status) {
      case KYW_DEELNEMERS:
        output.write(CaissaTools.replaceParameters(regel, params));
        break;
      case KYW_KLEUREN:
        output.write(CaissaTools.replaceParameters(regel, params));
        break;
      case KYW_LOGO:
        if (paramBundle.containsParameter(CaissaTools.PAR_LOGO)) {
          output.write(CaissaTools.replaceParameters(regel, params));
        }
        break;
      case KYW_MATRIX:
        if (null != matrix) {
          output.write(CaissaTools.replaceParameters(regel, params));
        }
        break;
      case KYW_SKIP:
        break;
      case KYW_SUBTITEL:
        if (paramBundle.containsParameter(CaissaTools.PAR_SUBTITEL)) {
          output.write(CaissaTools.replaceParameters(regel, params));
        }
        break;
      case KYW_TITEL:
        if (paramBundle.containsParameter(CaissaTools.PAR_TITEL)) {
          output.write(CaissaTools.replaceParameters(regel, params));
        }
        break;
      case KYW_UITSLAGEN:
        if (null != schema) {
          output.write(CaissaTools.replaceParameters(regel, params));
        }
        break;
      default:
        output.write(CaissaTools.replaceParameters(regel, params));
        break;
      }
  }

  private static void schrijfVcardEmail(String emails, TekstBestand vcards)
      throws BestandException {
    for (var email : emails.trim().split(" - ")) {
      vcards.write(String.format("EMAIL;HOME:%s",email));
    }
  }

  private static void schrijfVcardTelefoon(String telefoons,
                                           TekstBestand vcards)
      throws BestandException {
    for (var telefoon : telefoons.trim().split(" - ")) {
      if (!telefoon.startsWith("+")) {
        telefoon  = "+32" + telefoon.substring(1);
      }
      vcards.write(String.format("TEL;CELL:+%s",
                   telefoon.replaceAll("[^\\d]", "")));
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
      case Competitie.JSON_TAG_INHALEN:
        if (competitie.isZwitsers()) {
          status  = KYW_SKIP;
        } else {
          status  = KYW_INHALEN;
        }
        break;
      case Competitie.JSON_TAG_KALENDER:
        status  = KYW_KALENDER;
        break;
      case "kleuren":
        if (competitie.isZwitsers()) {
          status  = KYW_KLEUREN;
        } else {
          status  = KYW_SKIP;
        }
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
    params.put(CaissaTools.PAR_KLEUR,
               paramBundle.getString(CaissaTools.PAR_KLEUR));
    if (paramBundle.containsParameter(CaissaTools.PAR_LOGO)) {
      params.put(CaissaTools.PAR_LOGO,
                 paramBundle.getString(CaissaTools.PAR_LOGO));
    }
    if (paramBundle.containsParameter(CaissaTools.PAR_SUBTITEL)) {
      params.put(CaissaTools.PAR_SUBTITEL,
                 paramBundle.getString(CaissaTools.PAR_SUBTITEL));
    }
    params.put(CaissaTools.PAR_TEKSTKLEUR,
               paramBundle.getString(CaissaTools.PAR_TEKSTKLEUR));
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
    params.put("inhaaldatum", resourceBundle.getString("label.inhaaldatum"));
    params.put(Competitie.JSON_TAG_KALENDER,
               resourceBundle.getString("label.kalender"));
    params.put("notRanked", resourceBundle.getString("message.notranked"));
    params.put("ronde", resourceBundle.getString("label.ronde"));
    params.put("tespelenop", resourceBundle.getString("label.tespelenop"));
    params.put("wit", resourceBundle.getString("label.wit"));
    params.put("zwart", resourceBundle.getString("label.zwart"));
  }
}
