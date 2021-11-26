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
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_ENKELRONDIG;
import static eu.debooy.caissa.CaissaConstants.JSON_TAG_KALENDER;
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
import eu.debooy.doosutils.html.Utilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * @author Marco de Booij
 */
public final class PgnToHtml extends Batchjob {
  public static final String  HTML_LEGENDA                = "table.legenda.";
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
  public static final String  HTML_TABLE_HEAD_DUBBEL      = "table.head.dubbel";
  public static final String  HTML_TABLE_HEAD_DUBBEL2     =
      "table.head.dubbel2";
  public static final String  HTML_TABLE_HEAD_EIND        = "table.head.eind.";
  public static final String  HTML_TABLE_HEAD_ENKEL       = "table.head.enkel";
  public static final String  HTML_TABLE_HEAD_NAAM        = "table.head.naam";
  public static final String  HTML_TABLE_HEAD_NR          = "table.head.nr";
  public static final String  HTML_TABLE_HEAD_PARTIJEN    =
      "table.head.partijen";
  public static final String  HTML_TABLE_HEAD_PUNTEN      = "table.head.punten";
  public static final String  HTML_TABLE_HEAD_SB          = "table.head.sb";
  public static final String  HTML_TABLE_ROW_BEGIN        = "table.row.begin";
  public static final String  HTML_TABLE_ROW_EIND         = "table.row.eind";
  public static final String  HTML_TABLE_ROW_NAAM         = "table.row.naam";
  public static final String  HTML_TABLE_ROW_NR           = "table.row.nr";
  public static final String  HTML_TABLE_ROW_PARTIJEN     =
      "table.row.partijen";
  public static final String  HTML_TABLE_ROW_PUNTEN       = "table.row.punten";
  public static final String  HTML_TABLE_ROW_SB           = "table.row.sb";
  public static final String  HTML_TABLE_ROW_WIT          = "table.row.wit";
  public static final String  HTML_TABLE_ROW_ZELF         = "table.row.zelf";
  public static final String  HTML_TABLE_ROW_ZWART        = "table.row.zwart";

  public static final String  TAG_NAAM      = "tag.naam";
  public static final String  TAG_NUMMER    = "tag.nummer";
  public static final String  TAG_PARTIJEN  = "tag.partijen";
  public static final String  TAG_PUNTEN    = "tag.punten";
  public static final String  TAG_SB        = "tag.sb";
  public static final String  TAG_WIT       = "tag.wit";
  public static final String  TAG_ZWART     = "tag.zwart";

  public static final String  PROP_INDENT = "indent";

  private static  final ClassLoader     classloader     =
      PgnToHtml.class.getClassLoader();
  private static  final ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static  int               enkel;
  private static  int               kolommen;
  private static  double[][]        matrix;
  private static  TekstBestand      output;
  private static  String            prefix  = "";
  private static  Properties        skelet;
  private static  List<Spelerinfo>  spelers;

  private PgnToHtml() {}

  public static void execute(String[] args) {
    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntohtml"));

    if (!setParameters(args)) {
      return;
    }

    var invoer        = parameters.get(PAR_INVOERDIR)
                         + parameters.get(CaissaTools.PAR_BESTAND)
                         + EXT_PGN;
    var matrixOpStand =
        DoosConstants.WAAR.equalsIgnoreCase(
            parameters.get(CaissaTools.PAR_MATRIXOPSTAND));
    var uitvoerdir    = parameters.get(PAR_UITVOERDIR);

    JsonBestand     competitie;
    Collection<PGN> partijen;
    try {
      competitie  =
          new JsonBestand.Builder()
                         .setBestand(parameters.get(PAR_INVOERDIR)
                                     + parameters.get(CaissaTools.PAR_SCHEMA)
                                     + EXT_JSON)
                         .setCharset(parameters.get(PAR_CHARSETIN))
                         .build();
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (BestandException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    spelers = new ArrayList<>();
    CaissaUtils.vulSpelers(spelers, competitie.getArray(JSON_TAG_SPELERS));

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    if (competitie.containsKey(JSON_TAG_ENKELRONDIG)) {
      enkel = ((boolean) competitie.get(JSON_TAG_ENKELRONDIG)
                  ? CaissaConstants.TOERNOOI_ENKEL
                  : CaissaConstants.TOERNOOI_DUBBEL);
    } else {
      enkel = CaissaConstants.TOERNOOI_MATCH;
    }

    // Maak de Matrix.
    var noSpelers = spelers.size();
    var namen     = new String[noSpelers];
    kolommen      = noSpelers * enkel;
    matrix        = new double[noSpelers][kolommen];

    for (var i = 0; i < noSpelers; i++) {
      namen[i]  = spelers.get(i).getNaam();
    }
    Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);

    // Bepaal de score en SB score.
    CaissaUtils.vulToernooiMatrix(partijen, spelers, matrix, enkel,
                                  matrixOpStand, CaissaConstants.TIEBREAK_SB);

    // Maak het matrix.html bestand.
    maakMatrix();

    // Maak het index.html bestand.
    maakIndex();

    // Maak het uitslagen.html bestand.
    if (enkel != CaissaConstants.TOERNOOI_MATCH) {
      // Sortering terug zetten voor opmaken schema.
      spelers.sort(new Spelerinfo.BySpelerSeqComparator());
      var enkelrondig = (enkel == CaissaConstants.TOERNOOI_ENKEL);
      var schema      =
          CaissaUtils.genereerSpeelschema(spelers, enkelrondig, partijen);
      if (!schema.isEmpty()) {
        var data      =
            CaissaUtils.vulKalender(JSON_TAG_KALENDER_RONDE, spelers.size(),
                                    enkel,
                                    competitie.getArray(JSON_TAG_KALENDER));
        maakUitslagen(schema, data);
      }
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             invoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.uitvoer"),
                             uitvoerdir));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void genereerLegenda() throws BestandException {
    var forfait   = resourceBundle.getString("message.forfait");
    var notRanked = resourceBundle.getString("message.notranked");
    genereerTabelheading();
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
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
    schrijfUitvoer(HTML_TABLE_HEAD, resourceBundle.getString("label.ronde"),
                   ronde, datum);
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
  }

  private static void genereerTabelheading() throws BestandException {
    schrijfUitvoer(HTML_TABLE_BEGIN);
    schrijfUitvoer(HTML_TABLE_COLGROUP);
  }

  private static void genereerUitslagtabel(Set<Partij> schema, String[] data)
      throws BestandException {
    var iter    = schema.iterator();
    var partij  = iter.next();
    var vorige  = Integer.parseInt(partij.getRonde().getRound()
                                                    .split("\\.")[0]);
    genereerRondeheading(vorige, DoosUtils.nullToEmpty(data[vorige]));

    do {
      var ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
      if (ronde != vorige) {
        genereerRondefooting();
        genereerRondeheading(ronde, DoosUtils.nullToEmpty(data[ronde]));
        vorige  = ronde;
      }

      var klasse  =
          (partij.isRanked() && !partij.isBye()) ? "" : " class=\"btncmp\"";
      var uitslag = partij.getUitslag().replace("1/2", "&frac12;")
                          .replace('-', (partij.isForfait() ? 'F' : '-'))
                          .replace('*', '-');
      var wit     = partij.getWitspeler().getVolledigenaam();
      var zwart   = partij.getZwartspeler().getVolledigenaam();
      schrijfUitvoer(HTML_TABLE_BODY, klasse, wit, zwart, uitslag);
      if (iter.hasNext()) {
        partij  = iter.next();
      } else {
        partij  = null;
      }
    } while (null != partij);

    genereerRondefooting();
    genereerLegenda();
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

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToHtml ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString(CaissaTools.LBL_PGNBESTAND)
                         + ">"
                         + " --schema=<"
                         + resourceBundle.getString(CaissaTools.LBL_SCHEMA)
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString(CaissaTools.HLP_BESTAND), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIXOPSTAND, 14),
                         resourceBundle.getString(
                             CaissaTools.HLP_MATRIXOPSTAND), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SCHEMA, 14),
                         resourceBundle.getString(CaissaTools.HLP_SCHEMA), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             CaissaTools.PAR_BESTAND, CaissaTools.PAR_SCHEMA),
                             80);
    DoosUtils.naarScherm();
  }

  private static void maakIndex() {
    var noSpelers = spelers.size();

    skelet  = new Properties();
    Collections.sort(spelers);
    try {
      output    = new TekstBestand.Builder()
                                  .setBestand(parameters.get(PAR_UITVOERDIR)
                                              + "index.html")
                                  .setCharset(parameters.get(PAR_CHARSETUIT))
                                  .setLezen(false).build();
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("index.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      // Start de tabel
      schrijfUitvoer(HTML_TABLE_BEGIN);
      // De colgroup
      schrijfUitvoer(HTML_TABLE_COLGROUP);
      // De thead
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
      // De tbody
      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      var plaats  = 1;
      for (var i = 0; i < noSpelers; i++) {
        if (spelers.get(i).getPartijen() > 0) {
          maakIndexBody(spelers.get(i), plaats);
          plaats++;
        }
      }
      // Alles netjes afsluiten
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

  private static void maakMatrix() {
    List<Spelerinfo>  actief  = new ArrayList<>();
    actief.addAll(spelers);
    CaissaUtils.verwijderNietActief(actief, matrix, enkel);

    var actieveSpelers  = actief.stream()
                                .filter(speler -> (speler.getPartijen() > 0))
                                .count();

    skelet  = new Properties();
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + "matrix.html")
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("matrix.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      // Start de tabel
      schrijfUitvoer(HTML_TABLE_BEGIN);
      // De colgroup
      schrijfUitvoer(HTML_TABLE_COLGROUP_BEGIN);
      for (var i = 0; i < actieveSpelers; i++) {
        if (enkel == 1) {
          schrijfUitvoer(HTML_TABLE_COLGROUP_ENKEL);
        } else {
          schrijfUitvoer(HTML_TABLE_COLGROUP_DUBBEL);
        }
      }
      schrijfUitvoer(HTML_TABLE_COLGROUP_EIND);
      // De thead
      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
      for (var i = 0; i < actieveSpelers; i++) {
        if (enkel == 1) {
          output.write(prefix
              + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_ENKEL),
                                     (i + 1)));
        } else {
          output.write(prefix
              + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_DUBBEL),
                                     (i + 1)));
        }
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
      if (enkel == 2) {
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_EIND + 1));
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN + 2));
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
        for (var i = 0; i < actieveSpelers; i++) {
          output.write(prefix + MessageFormat.format(
                                    skelet.getProperty(HTML_TABLE_HEAD_DUBBEL2),
                                    resourceBundle.getString(TAG_WIT),
                                    resourceBundle.getString(TAG_ZWART)));
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
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);
      // De tbody
      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      var plaats  = 0;
      for (var i = 0; i < actieveSpelers; i++) {
        if (actief.get(i).getPartijen() > 0) {
          maakMatrixBody(actief.get(i), plaats);
          plaats++;
        }
      }
      // Alles netjes afsluiten
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

  private static void maakMatrixBody(Spelerinfo speler, int i)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NR),
                               (i + 1)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NAAM),
                               swapNaam(speler.getNaam())));
    var lijn  = new StringBuilder();
    for (var j = 0; j < kolommen; j++) {
      if ((j / enkel) * enkel == j) {
        lijn.append(prefix).append("      ");
      }
      if (i == j / enkel) {
        lijn.append(skelet.getProperty(HTML_TABLE_ROW_ZELF));
      } else {
        // -1 is een niet gespeelde partij.
        if ((j / enkel) * enkel == j) {
          lijn.append(
              MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_WIT),
                                   getScore(matrix[i][j])));
        } else {
          lijn.append(
              MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_ZWART),
                                   getScore(matrix[i][j])));
        }
      }
      if ((j / enkel) * enkel != j) {
        output.write(lijn.toString());
        lijn  = new StringBuilder();
      }
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

  private static void maakUitslagen(Set<Partij> schema, String[] data) {
    skelet  = new Properties();
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + "uitslagen.html")
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      skelet.load(classloader.getResourceAsStream("uitslagen.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      genereerUitslagtabel(schema, data);
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
          MessageFormat.format(prefix + skelet.getProperty(parameter + k),
          params));
      k++;
    }
  }

  private static boolean setParameters(String[] args) {
    var           arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          CaissaTools.PAR_SCHEMA,
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
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setBestandParameter(arguments, CaissaTools.PAR_SCHEMA, EXT_JSON);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

  private static String swapNaam(String naam) {
    var deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }

    return deel[1].trim() + " " + deel[0].trim();
  }
}
