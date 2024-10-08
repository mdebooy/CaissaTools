/**
 * Copyright (c) 2008 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://www.osor.eu/eupl
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
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.html.Utilities;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public final class PgnToHtml extends Batchjob {
  public static final String  HTML_LEGENDA                = "table.legenda.";
  public static final String  HTML_LEGENDA_INHAAL         =
      "table.legenda.inhalen.";
  public static final String  HTML_TABLE_BEGIN            = "table.begin.";
  public static final String  HTML_TABLE_BODY             = "table.body.";
  public static final String  HTML_TABLE_BODY_BEGIN       = "table.body.begin.";
  public static final String  HTML_TABLE_BODY_EIND        = "table.body.eind.";
  public static final String  HTML_TABLE_BODY_FOOTER      =
      "table.body.footer.";
  public static final String  HTML_TABLE_COLGROUP         = "table.colgroup.";
  public static final String  HTML_TABLE_COLGROUP_BEGIN   =
      "table.colgroup.begin.";
  public static final String  HTML_TABLE_COLGROUP_DUBBEL  =
      "table.colgroup.dubbel.";
  public static final String  HTML_TABLE_COLGROUP_EIND    =
      "table.colgroup.eind.";
  public static final String  HTML_TABLE_COLGROUP_ENKEL   =
      "table.colgroup.enkel.";
  public static final String  HTML_TABLE_EIND             = "table.eind.";
  public static final String  HTML_TABLE_HEAD             = "table.head.";
  public static final String  HTML_TABLE_HEAD_BEGIN       = "table.head.begin.";
  public static final String  HTML_TABLE_HEAD_BEGIN_M     = "table.head.begin";
  public static final String  HTML_TABLE_HEAD_BYE         = "table.head.bye";
  public static final String  HTML_TABLE_HEAD_DATUM       = "table.head.datum";
  public static final String  HTML_TABLE_HEAD_DUBBEL      = "table.head.dubbel";
  public static final String  HTML_TABLE_HEAD_DUBBEL2     =
      "table.head.dubbel2";
  public static final String  HTML_TABLE_HEAD_EIND        = "table.head.eind.";
  public static final String  HTML_TABLE_HEAD_ENKEL       = "table.head.enkel";
  public static final String  HTML_TABLE_HEAD_KALENDER    =
      "table.head.kalender";
  public static final String  HTML_TABLE_HEAD_NAAM        = "table.head.naam";
  public static final String  HTML_TABLE_HEAD_NR          = "table.head.nr";
  public static final String  HTML_TABLE_HEAD_PARTIJEN    =
      "table.head.partijen";
  public static final String  HTML_TABLE_HEAD_PUNTEN      = "table.head.punten";
  public static final String  HTML_TABLE_HEAD_RONDE       = "table.head.ronde";
  public static final String  HTML_TABLE_HEAD_SB          = "table.head.sb";
  public static final String  HTML_TABLE_HEAD_SCHEIDING   =
      "table.head.scheiding";
  public static final String  HTML_TABLE_HEAD_WIT         = "table.head.wit";
  public static final String  HTML_TABLE_HEAD_ZWART       = "table.head.zwart";
  public static final String  HTML_TABLE_ROW              = "table.row";
  public static final String  HTML_TABLE_ROW_BEGIN        = "table.row.begin";
  public static final String  HTML_TABLE_ROW_BEGIN_V      =
      "table.row.begin.volgende";
  public static final String  HTML_TABLE_ROW_DATUM        = "table.row.datum";
  public static final String  HTML_TABLE_ROW_EIND         = "table.row.eind";
  public static final String  HTML_TABLE_ROW_GEENINHAAL   =
      "table.row.geen.inhaal";
  public static final String  HTML_TABLE_ROW_NAAM         = "table.row.naam";
  public static final String  HTML_TABLE_ROW_NR           = "table.row.nr";
  public static final String  HTML_TABLE_ROW_PARTIJEN     =
      "table.row.partijen";
  public static final String  HTML_TABLE_ROW_PUNTEN       = "table.row.punten";
  public static final String  HTML_TABLE_ROW_RONDE        = "table.row.ronde";
  public static final String  HTML_TABLE_ROW_SB           = "table.row.sb";
  public static final String  HTML_TABLE_ROW_SCHEIDING    =
      "table.row.scheiding";
  public static final String  HTML_TABLE_ROW_WIT          = "table.row.wit";
  public static final String  HTML_TABLE_ROW_ZELF         = "table.row.zelf";
  public static final String  HTML_TABLE_ROW_ZWART        = "table.row.zwart";

  public static final String  LBL_RONDE     = "label.ronde";
  public static final String  LBL_TESPELEN  = "label.tespelenop";
  public static final String  LBL_WIT       = "label.wit";
  public static final String  LBL_ZWART     = "label.zwart";

  public static final String  MSG_GEENINHAAL  = "message.geen.inhaalpartijen";

  public static final String  TAG_ACTIVITEIT  = "label.activiteit";
  public static final String  TAG_BYE         = "label.bye";
  public static final String  TAG_DATUM       = "label.datum";
  public static final String  TAG_HTML        = "label.html.";
  public static final String  TAG_NAAM        = "tag.naam";
  public static final String  TAG_NUMMER      = "tag.nummer";
  public static final String  TAG_PARTIJEN    = "tag.partijen";
  public static final String  TAG_PUNTEN      = "tag.punten";
  public static final String  TAG_SB          = "tag.sb";
  public static final String  TAG_WIT         = "tag.wit";
  public static final String  TAG_ZWART       = "tag.zwart";

  public static final String  PROP_INDENT = "indent";

  private static  final ClassLoader     classloader     =
      PgnToHtml.class.getClassLoader();
  private static  final ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  Competitie        competitie;
  private static  double[][]        matrix;
  private static  TekstBestand      output;
  private static  String            prefix        = "";
  private static  Properties        skelet;

  protected PgnToHtml() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_PGNTOHTML)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    var invoer        = paramBundle.getBestand(CaissaTools.PAR_BESTAND);
    var matrixOpStand = paramBundle.getBoolean(CaissaTools.PAR_MATRIXOPSTAND);

    Collection<PGN> partijen;
    try {
      competitie  =
          new Competitie(paramBundle.getBestand(CaissaTools.PAR_SCHEMA));
      partijen    =
          CaissaUtils.laadPgnBestand(invoer);
    } catch (CompetitieException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }


    // Maak de Matrix.
    competitie.sorteerOpNaam();

    // Bepaal de score en SB score.
    matrix        = CaissaUtils.vulToernooiMatrix(partijen, competitie,
                                                  matrixOpStand);

    if (Boolean.TRUE.equals(paramBundle.getBoolean(CaissaTools.PAR_AKTIEF))) {
      matrix  = CaissaUtils.verwijderNietActief(matrix, competitie);
    }
    if (matrix.length > 0) {
      // Maak het matrix.html bestand.
      maakMatrix();

      // Maak het index.html bestand.
      maakIndex();
    }

    // Maak het uitslagen.html bestand.
    if (!competitie.isMatch()) {
      var schema      =
          CaissaUtils.genereerSpeelschema(competitie, partijen);
      if (!schema.isEmpty()) {
        maakUitslagen(schema);
      }
    }

    if (!competitie.getKalender().isEmpty()) {
      maakKalender();
    }

    maakInhalen();

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             invoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.uitvoer"),
                             paramBundle.getString(PAR_UITVOERDIR)));
    klaar();
  }

  private static void genereerLegenda(Boolean metInhaaldatum) throws BestandException {
    var forfait     = resourceBundle.getString("message.forfait");
    var notRanked   = resourceBundle.getString("message.notranked");
    var inhaaldatum = resourceBundle.getString("message.met.inhaaldatum");
    genereerTabelheading();
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
    if (Boolean.TRUE.equals(metInhaaldatum)) {
      schrijfUitvoer(HTML_LEGENDA_INHAAL, inhaaldatum);
    }
    schrijfUitvoer(HTML_LEGENDA, notRanked, forfait);
    schrijfUitvoer(HTML_TABLE_BODY_EIND);
    schrijfUitvoer(HTML_TABLE_EIND);
  }

  private static void genereerRondefooting() throws BestandException {
    schrijfUitvoer(HTML_TABLE_BODY_FOOTER);
    schrijfUitvoer(HTML_TABLE_BODY_EIND);
    schrijfUitvoer(HTML_TABLE_EIND);
  }

  private static void genereerRondeheading(int ronde, String datum)
      throws BestandException {
    genereerTabelheading();
    schrijfUitvoer(HTML_TABLE_HEAD, resourceBundle.getString(LBL_RONDE),
                   ronde, datum);
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
  }

  private static void genereerTabelheading() throws BestandException {
    schrijfUitvoer(HTML_TABLE_BEGIN);
    schrijfUitvoer(HTML_TABLE_COLGROUP);
  }

  private static void genereerUitslagtabel(Set<Partij> schema)
      throws BestandException {
    var iter            = schema.iterator();
    var metInhaaldatum  =
            paramBundle.getBoolean(CaissaTools.PAR_METINHAALDATUM);
    var partij          = iter.next();
    var speeldata       = competitie.getSpeeldata();
    var vorige          = Integer.parseInt(partij.getRonde().getRound()
                                                            .split("\\.")[0]);
    genereerRondeheading(vorige,
                         Datum.fromDate(speeldata.get(vorige-1),
                                        CaissaConstants.DEF_DATUMFORMAAT));

    do {
      var ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
      if (ronde != vorige) {
        genereerRondefooting();
        genereerRondeheading(ronde,
                             Datum.fromDate(speeldata.get(ronde-1),
                                            CaissaConstants.DEF_DATUMFORMAAT));
        vorige  = ronde;
      }

      verwerkPartij(partij, metInhaaldatum);

      partij            = iter.hasNext() ? iter.next() : null;
    } while (null != partij);

    genereerRondefooting();
    genereerLegenda(metInhaaldatum);
  }

  private static String getDecimalen(int punten, String decimalen) {
    if ((punten == 0 && "".equals(decimalen)) || punten >= 1) {
      return decimalen;
    } else {
      return "";
    }
  }

  private static String getPunten(int punten, String decimalen) {
    if ((punten == 0 && "".equals(decimalen)) || punten >= 1) {
      return "" + punten;
    } else {
      return decimalen;
    }
  }

  private static String getScore(double score) {
    if (score < 0.0) {
      return "";
    }
    if (score == 0.5) {
      return Utilities.kwart(score);
    }
    return "" + ((Double) score).intValue() + Utilities.kwart(score);
  }

  private static void laadSkelet(String resource) throws IOException {
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream(resource));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.parseInt(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }
  }

  private static void maakIndex() {
    var spelers   = competitie.getDeelnemers();
    var noSpelers = spelers.size();

    Collections.sort(spelers);
    skelet  = new Properties();
    try {
      output    =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                        + DoosUtils.getFileSep() + "index.html")
                          .setLezen(false).build();
      laadSkelet("index.properties");

      schrijfUitvoer(HTML_TABLE_BEGIN);
      schrijfUitvoer(HTML_TABLE_COLGROUP);

      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_NR),
                                 resourceBundle.getString(TAG_NUMMER)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_NAAM),
                                 resourceBundle.getString(TAG_NAAM)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                 resourceBundle.getString(TAG_PUNTEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                 resourceBundle.getString(TAG_PARTIJEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB),
                                 resourceBundle.getString(TAG_SB)));
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);

      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      for (var i = 0; i < noSpelers; i++) {
        maakIndexBody(spelers.get(i), i+1);
      }
      schrijfUitvoer(HTML_TABLE_BODY_EIND);

      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
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

  private static void maakIndexBody(Spelerinfo speler, int plaats)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NR), plaats));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NAAM),
                               swapNaam(speler.getNaam())));
    var pntn  = speler.getPunten().intValue();
    var decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PUNTEN),
                               getPunten(pntn, decim),
                               getDecimalen(pntn, decim)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PARTIJEN),
                               speler.getPartijen()));
    var wpntn   = speler.getTieBreakScore().intValue();
    var wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_SB),
                               getPunten(wpntn, wdecim),
                               getDecimalen(wpntn, wdecim)));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakInhalen() {
    var inhalen = competitie.getInhaalpartijen();
    try {
      output    =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                        + DoosUtils.getFileSep()
                                        + "inhalen.html")
                          .setLezen(false).build();
      laadSkelet("inhalen.properties");

      schrijfUitvoer(HTML_TABLE_BEGIN);
      schrijfUitvoer(HTML_TABLE_COLGROUP);

      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_RONDE),
                               resourceBundle.getString(LBL_RONDE)));
      output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_DATUM),
                               resourceBundle.getString(LBL_TESPELEN)));
      output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_WIT),
                               resourceBundle.getString(LBL_WIT)));
      output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_SCHEIDING));
      output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_ZWART),
                               resourceBundle.getString(LBL_ZWART)));
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);

      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);

      if (inhalen.isEmpty()) {
        output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_GEENINHAAL),
                                 resourceBundle.getString(MSG_GEENINHAAL)));
      } else {
        var datum             =
            ((JSONObject) inhalen.get(0))
                .get(Competitie.JSON_TAG_KALENDER_DATUM).toString();
        for (var i = 0; i < inhalen.size(); i++) {
          maakInhalenBody((JSONObject) inhalen.get(i), datum);
        }
      }

      schrijfUitvoer(HTML_TABLE_BODY_EIND);

      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
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

  private static void maakInhalenBody(JSONObject item, String volgende)
      throws BestandException {
    var datum = item.get("datum").toString();
    var ronde = item.get("ronde").toString();
    var wit   = new Spelerinfo();
    wit.setNaam(item.get("wit").toString());
    var zwart = new Spelerinfo();
    zwart.setNaam(item.get("zwart").toString());

    if (datum.equals(volgende)) {
      output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN_V));
    } else {
      output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    }

    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_RONDE), ronde));
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_DATUM), datum));
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_WIT),
                             wit.getVolledigenaam()));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_SCHEIDING));
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_ZWART),
                             zwart.getVolledigenaam()));

    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakKalender() {
    var     kalender  = competitie.getKalender();
    var     datum     =
        ((JSONObject) kalender.get(0))
            .get(Competitie.JSON_TAG_KALENDER_DATUM).toString();
    var     formatter =
        DateTimeFormatter.ofPattern(CaissaConstants.DEF_DATUMFORMAAT);
    var     speeldag  = LocalDate.parse(datum, formatter).getDayOfWeek();
    var     vandaag   = LocalDate.now();

    String  volgendeSpeeldag;
    if (vandaag.getDayOfWeek().equals(speeldag)) {
      volgendeSpeeldag  =
          vandaag.format(
              DateTimeFormatter.ofPattern(CaissaConstants.DEF_DATUMFORMAAT));
    } else {
      volgendeSpeeldag  = vandaag.with(TemporalAdjusters.next(speeldag))
                                 .format(formatter);
    }

    skelet  = new Properties();
    try {
      output    =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                        + DoosUtils.getFileSep()
                                        + "kalender.html")
                          .setLezen(false).build();
      laadSkelet("kalender.properties");

      schrijfUitvoer(HTML_TABLE_BEGIN);
      schrijfUitvoer(HTML_TABLE_COLGROUP);

      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_KALENDER),
                                 resourceBundle.getString(TAG_DATUM)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_KALENDER),
                                 resourceBundle.getString(TAG_ACTIVITEIT)));
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);

      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      for (var i = 0; i < kalender.size(); i++) {
        maakKalenderBody((JSONObject) kalender.get(i), volgendeSpeeldag);
      }
      schrijfUitvoer(HTML_TABLE_BODY_EIND);

      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
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

  private static void maakKalenderBody(JSONObject item, String volgende)
      throws BestandException {
    var datum = item.get(Competitie.JSON_TAG_KALENDER_DATUM).toString();
    var type  = Competitie.JSON_TAG_KALENDER_RONDE;
    if (item.containsKey(Competitie.JSON_TAG_KALENDER_INHAAL)) {
      type  = Competitie.JSON_TAG_KALENDER_INHAAL;
    }
    if (item.containsKey(Competitie.JSON_TAG_KALENDER_EXTRA)) {
      type  = Competitie.JSON_TAG_KALENDER_EXTRA;
    }
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_BEGIN
                                                  + "." + type),
                             datum.equals(volgende) ? " attentie" : ""));
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_DATUM), datum));
    String  activiteit;
    if (resourceBundle.containsKey(TAG_HTML + type)) {
      activiteit  =
          MessageFormat.format(resourceBundle.getString(TAG_HTML + type),
                               item.get(type).toString());
    } else {
      activiteit  = item.get(type).toString();
    }
    output.write(prefix +
        MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW),
                             activiteit));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakMatrix() {
    var kolommen  = matrix[0].length;
    var spelers   = competitie.getDeelnemers();
    var noSpelers = spelers.size();

    skelet  = new Properties();
    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                        + DoosUtils.getFileSep()
                                        + "matrix.html")
                          .setLezen(false).build();
      laadSkelet("matrix.properties");

      // Start de tabel
      schrijfUitvoer(HTML_TABLE_BEGIN);

      // De colgroup
      String  enkeltekst;
      schrijfUitvoer(HTML_TABLE_COLGROUP_BEGIN);
      if (competitie.isDubbel()) {
        enkeltekst  = HTML_TABLE_COLGROUP_DUBBEL;
      } else {
        enkeltekst  = HTML_TABLE_COLGROUP_ENKEL;
      }
      for (var i = 0; i < noSpelers; i++) {
        schrijfUitvoer(enkeltekst);
      }
      if (competitie.metBye()) {
        schrijfUitvoer(HTML_TABLE_COLGROUP_ENKEL);
      }
      schrijfUitvoer(HTML_TABLE_COLGROUP_EIND);

      // De thead
      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);

      maakMatrixHead(noSpelers);

      schrijfUitvoer(HTML_TABLE_HEAD_EIND);

      // De body
      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      for (var i = 0; i < noSpelers; i++) {
        maakMatrixBody(spelers.get(i), i, kolommen,
                       competitie.isDubbel(), competitie.metBye());
      }
      schrijfUitvoer(HTML_TABLE_BODY_EIND);

      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }

  private static void maakMatrixBody(Spelerinfo speler, int i, int kolommen,
                                     boolean dubbelrondig, boolean metBye)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NR),
                               (i + 1)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NAAM),
                               swapNaam(speler.getNaam())));
    var j     = 0;
    var lijn  = new StringBuilder();
    while (j < kolommen) {
      lijn.append(prefix).append("      ");
      if ((dubbelrondig ? 2 : 1) * i == j) {
        lijn.append(skelet.getProperty(HTML_TABLE_ROW_ZELF));
        if (dubbelrondig) {
          j++;
          lijn.append(skelet.getProperty(HTML_TABLE_ROW_ZELF));
        }
      } else {
        // -1 is een niet gespeelde partij.
        lijn.append(
            MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_WIT),
                                 getScore(matrix[i][j])));
        if (dubbelrondig) {
          j++;
          lijn.append(
              MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_ZWART),
                                   getScore(matrix[i][j])));
        }
      }
      output.write(lijn.toString());
      lijn  = new StringBuilder();
      j++;
    }
    if (metBye) {
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PARTIJEN),
                                 getScore(speler.getByeScore().intValue())));
    }
    var pntn  = speler.getPunten().intValue();
    var decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PUNTEN),
                               getPunten(pntn, decim),
                               getDecimalen(pntn, decim)));
    output.write(prefix + MessageFormat.format(
        skelet.getProperty(HTML_TABLE_ROW_PARTIJEN), speler.getPartijen()));
    var wpntn   = speler.getTieBreakScore().intValue();
    var wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_SB),
                               getPunten(wpntn, wdecim),
                               getDecimalen(wpntn, wdecim)));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakMatrixHead(long noSpelers) throws BestandException {
    String  enkeltekst;
    output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
    if (competitie.isDubbel()) {
      enkeltekst  = skelet.getProperty(HTML_TABLE_HEAD_DUBBEL);
    } else {
      enkeltekst  = skelet.getProperty(HTML_TABLE_HEAD_ENKEL);
    }
    for (var i = 0; i < noSpelers; i++) {
      output.write(prefix + MessageFormat.format(enkeltekst, (i + 1)));
    }
    if (competitie.metBye()) {
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_BYE),
                                 resourceBundle.getString(TAG_BYE)));
    }
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                               resourceBundle.getString(TAG_PUNTEN)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                               resourceBundle.getString(TAG_PARTIJEN)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB),
                               resourceBundle.getString(TAG_SB)));

    if (competitie.isDubbel()) {
      maakMatrixHeadDubbel(noSpelers);
    }
  }

  private static void maakMatrixHeadDubbel(long noSpelers)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_EIND + 1));
    output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN + 2));
    output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
    for (var i = 0; i < noSpelers; i++) {
      output.write(prefix + MessageFormat.format(
                                skelet.getProperty(HTML_TABLE_HEAD_DUBBEL2),
                                resourceBundle.getString(TAG_WIT),
                                resourceBundle.getString(TAG_ZWART)));
    }
    if (competitie.metBye()) {
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_BYE),
                                 ""));
    }
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                               ""));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                               ""));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB), ""));
  }

  private static void maakUitslagen(Set<Partij> schema) {
    skelet  = new Properties();
    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                        + DoosUtils.getFileSep()
                                        + "uitslagen.html")
                          .setLezen(false).build();
      skelet.load(classloader.getResourceAsStream("uitslagen.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.parseInt(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      genereerUitslagtabel(schema);
    } catch (BestandException | IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }

  private static void schrijfUitvoer(String parameter)
      throws BestandException {
    var k = 1;
    while (skelet.containsKey(parameter + k)) {
      output.write(prefix + skelet.getProperty(parameter + k));
      k++;
    }
  }

  private static void schrijfUitvoer(String parameter, Object... params)
      throws BestandException {
    var k = 1;
    while (skelet.containsKey(parameter + k)) {
      output.write(
          prefix + MessageFormat.format(skelet.getProperty(parameter + k),
                                        params));
      k++;
    }
  }

  private static String swapNaam(String naam) {
    var deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }

    return deel[1].trim() + " " + deel[0].trim();
  }

  private static void verwerkPartij(Partij partij, Boolean metInhaaldatum)
      throws BestandException {
    var klasse        =
        (partij.isRanked()
         && (!partij.isBye()
             || competitie.metBye())) ? "" : " class=\"btncmp\"";
    var nietgespeeld  = "-";
    if (Boolean.TRUE.equals(metInhaaldatum) && !partij.isGespeeld()) {
      nietgespeeld    = competitie.getInhaaldatum(partij);
    }
    var uitslagklasse = nietgespeeld.equals("-") ? "aligncenter" : "inhaal";
    var uitslag       =
            competitie.getUitslag(partij)
                      .replace("1/2", "&frac12;")
                      .replace("-", (partij.isForfait() ? "<b>F</b>" : "-"))
                      .replace("*", nietgespeeld);
    var wit           = partij.getWitspeler().getVolledigenaam();
    var zwart         = partij.getZwartspeler().getVolledigenaam();
    schrijfUitvoer(HTML_TABLE_BODY, klasse, wit, zwart, uitslag,
                   uitslagklasse);
  }
}
