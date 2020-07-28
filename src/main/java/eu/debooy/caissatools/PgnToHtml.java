/**
 * Copyright 2008 Marco de Booij
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
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * @author Marco de Booij
 */
public final class PgnToHtml extends Batchjob {
  public static final String  HTML_TABLE_COLGROUP       = "table.colgroup.";
  public static final String  HTML_TABLE_COLGROUP_BEGIN =
      "table.colgroup.begin.";
  public static final String  HTML_TABLE_COLGROUP_EIND  =
      "table.colgroup.eind.";
  public static final String  HTML_TABLE_HEAD_BEGIN     = "table.head.begin.";
  public static final String  HTML_TABLE_HEAD_EIND      = "table.head.eind.";
  public static final String  HTML_TABLE_HEAD_PUNTEN    = "table.head.punten";
  public static final String  HTML_TABLE_HEAD_PARTIJEN  = "table.head.partijen";
  public static final String  HTML_TABLE_HEAD_SB        = "table.head.sb";

  public static final String  PROP_INDENT               = "indent";

  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private PgnToHtml() {}

  public static void execute(String[] args) {
    Set<String>   spelers     = new HashSet<>();

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntohtml"));

    if (!setParameters(args)) {
      return;
    }

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    // 1 is default waarde.
    int enkel;
    switch (parameters.get(CaissaTools.PAR_ENKEL)) {
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
    String[]  halve         =
      DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_HALVE)).split(";");
    String    invoer        = parameters.get(PAR_INVOERDIR)
                              + parameters.get(CaissaTools.PAR_BESTAND)
                              + EXT_PGN;
    boolean   matrixOpStand =
        DoosConstants.WAAR.equalsIgnoreCase(
            parameters.get(CaissaTools.PAR_MATRIXOPSTAND));
    String    uitvoerdir    = parameters.get(PAR_UITVOERDIR);

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.forEach(partij -> {
      // Verwerk de spelers
      String  wit   = partij.getTag(CaissaConstants.PGNTAG_WHITE);
      String  zwart = partij.getTag(CaissaConstants.PGNTAG_BLACK);
      if (!"bye".equalsIgnoreCase(wit)
              && DoosUtils.isNotBlankOrNull(wit)) {
        spelers.add(wit);
      }
      if (!"bye".equalsIgnoreCase(zwart)
              && DoosUtils.isNotBlankOrNull(zwart)) {
        spelers.add(zwart);
      }
    });

    // Maak de Matrix.
    int           noSpelers = spelers.size();
    int           kolommen  = noSpelers * enkel;
    Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
    String[]      namen     = new String[noSpelers];
    double[][]    matrix    = new double[noSpelers][noSpelers * enkel];
    Iterator<String>
                speler    = spelers.iterator();
    for (int i = 0; i < noSpelers; i++) {
      namen[i]  = speler.next();
      for (int j = 0; j < kolommen; j++) {
        matrix[i][j] = -1.0;
      }
    }
    Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
    for (int i = 0; i < noSpelers; i++) {
      punten[i] = new Spelerinfo();
      punten[i].setNaam(namen[i]);
    }

    // Bepaal de score en SB score.
    CaissaUtils.vulToernooiMatrix(partijen, punten, halve, matrix, enkel,
                                  matrixOpStand, CaissaConstants.TIEBREAK_SB);

    // Maak het matrix.html bestand.
    maakMatrix(punten, uitvoerdir + "matrix.html",
               parameters.get(PAR_CHARSETUIT), matrix, enkel, noSpelers,
               kolommen);

    // Maak de index.html file
    maakIndex(punten, uitvoerdir + "index.html", parameters.get(PAR_CHARSETUIT),
              noSpelers);

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

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToHtml ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_ENKEL, 14),
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_HALVE, 14),
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIXOPSTAND, 14),
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_BESTAND), 80);
    DoosUtils.naarScherm();
  }

  private static void maakIndex(Spelerinfo[] punten, String bestand,
                                String charsetUit, int noSpelers) {
    Arrays.sort(punten);
    TekstBestand  output    = null;
    Properties    props     = new Properties();
    StringBuilder prefix    = new StringBuilder();
    try {
      output    = new TekstBestand.Builder().setBestand(bestand)
                                            .setCharset(charsetUit)
                                            .setLezen(false).build();
      props.load(PgnToHtml.class.getClassLoader()
                          .getResourceAsStream("index.properties"));

      if (props.containsKey(PROP_INDENT)) {
        int indent = Integer.valueOf(props.getProperty(PROP_INDENT));
        while (prefix.length() < indent) {
          prefix.append(" ");
        }
      }

      // Start de tabel
      output.write(prefix + props.getProperty("table.begin"));
      // De colgroup
      int k = 1;
      while (props.containsKey(HTML_TABLE_COLGROUP + k)) {
        output.write(prefix + props.getProperty(HTML_TABLE_COLGROUP + k));
        k++;
      }
      // De thead
      k = 1;
      while (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + k));
        k++;
      }
      output.write(prefix
          + MessageFormat.format(props.getProperty("table.head.nr"),
                                 resourceBundle.getString("tag.nummer")));
      output.write(prefix
          + MessageFormat.format(props.getProperty("table.head.naam"),
                                 resourceBundle.getString("tag.naam")));
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                 resourceBundle.getString("tag.punten")));
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                 resourceBundle.getString("tag.partijen")));
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB),
                                 resourceBundle.getString("tag.sb")));
      k = 1;
      while (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_EIND + k));
        k++;
      }
      // De tbody
      output.write(prefix + props.getProperty("table.body.begin"));
      for (int i = 0; i < noSpelers; i++) {
        maakIndexBody(output, prefix.toString(), props, punten[i], i + 1);
      }
      // Alles netjes afsluiten
      output.write(prefix + props.getProperty("table.body.eind"));
      output.write(prefix + props.getProperty("table.eind"));
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

  private static void maakIndexBody(TekstBestand output, String prefix,
                                    Properties props, Spelerinfo speler,
                                    int plaats) throws BestandException {
    output.write(prefix + props.getProperty("table.body.start"));
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.nr"), (plaats)));
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.naam"),
                               swapNaam(speler.getNaam())));
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.punten"),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.partijen"),
                               speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.sb"),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    output.write(prefix + props.getProperty("table.body.stop"));
  }

  private static void maakMatrix(Spelerinfo[]  punten, String bestand,
                                 String charsetUit, double[][] matrix,
                                 int enkel, int noSpelers, int kolommen) {
    TekstBestand  output    = null;
    Properties    props     = new Properties();
    StringBuilder prefix    = new StringBuilder();
    try {
      output  = new TekstBestand.Builder().setBestand(bestand)
                                          .setCharset(charsetUit)
                                          .setLezen(false).build();
      props.load(PgnToHtml.class.getClassLoader()
                          .getResourceAsStream("matrix.properties"));

      if (props.containsKey(PROP_INDENT)) {
        int indent = Integer.valueOf(props.getProperty(PROP_INDENT));
        while (prefix.length() < indent) {
          prefix.append(" ");
        }
      }

      // Start de tabel
      output.write(prefix + props.getProperty("table.begin"));
      // De colgroup
      int k = 1;
      while (props.containsKey(HTML_TABLE_COLGROUP_BEGIN + k)) {
        output.write(prefix
                     + props.getProperty(HTML_TABLE_COLGROUP_BEGIN + k));
        k++;
      }
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          output.write(prefix + props.getProperty("table.colgroup.enkel"));
        } else {
          output.write(prefix + props.getProperty("table.colgroup.dubbel"));
        }
      }
      k = 1;
      while (props.containsKey(HTML_TABLE_COLGROUP_EIND + k)) {
        output.write(prefix
                     + props.getProperty(HTML_TABLE_COLGROUP_EIND + k));
        k++;
      }
      // De thead
      k = 1;
      while (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + k));
        k++;
      }
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          output.write(prefix + MessageFormat
                .format(props.getProperty("table.head.enkel"), (i + 1)));
        } else {
          output.write(prefix + MessageFormat
                .format(props.getProperty("table.head.dubbel"), (i + 1)));
        }
      }
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                 resourceBundle.getString("tag.punten")));
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                 resourceBundle.getString("tag.partijen")));
      output.write(prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB),
                                 resourceBundle.getString("tag.sb")));
      if (enkel == 2) {
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_EIND + "1"));
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + "2"));
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + "3"));
        for (int i = 0; i < noSpelers; i++) {
          output.write(prefix
              + MessageFormat.format(props.getProperty("table.head.dubbel2"),
                                     resourceBundle.getString("tag.wit"),
                                     resourceBundle.getString("tag.zwart")));
        }
        output.write(prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                   ""));
        output.write(prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                   ""));
        output.write(prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB), ""));
      }
      k = 1;
      while (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
        output.write(prefix + props.getProperty(HTML_TABLE_HEAD_EIND + k));
        k++;
      }
      // De tbody
      output.write(prefix + props.getProperty("table.body.begin"));
      for (int i = 0; i < noSpelers; i++) {
        maakMatrixBody(output, prefix.toString(), props, punten[i], i,
                       enkel, kolommen, matrix);
      }
      // Alles netjes afsluiten
      output.write(prefix + props.getProperty("table.body.eind"));
      output.write(prefix + props.getProperty("table.eind"));
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

  private static void maakMatrixBody(TekstBestand output, String prefix,
                                     Properties props, Spelerinfo speler,
                                     int i, int enkel, int kolommen,
                                     double[][] matrix)
      throws BestandException {
    output.write(prefix + props.getProperty("table.body.start"));
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.nr"), (i + 1)));
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.naam"),
                               swapNaam(speler.getNaam())));
    StringBuilder lijn  = new StringBuilder();
    for (int j = 0; j < kolommen; j++) {
      if ((j / enkel) * enkel == j) {
        lijn.append(prefix + "      ");
      }
      if (i == j / enkel) {
        lijn.append(props.getProperty("table.body.zelf"));
      } else {
        // -1 is een niet gespeelde partij.
        if ((j / enkel) * enkel == j) {
          lijn.append(
              MessageFormat.format(props.getProperty("table.body.wit"),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))));
        } else {
          lijn.append(
              MessageFormat.format(props.getProperty("table.body.zwart"),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))));
        }
      }
      if ((j / enkel) * enkel != j) {
        output.write(lijn.toString());
        lijn  = new StringBuilder();
      }
    }
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.punten"),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    output.write(prefix + MessageFormat.format(
        props.getProperty("table.body.partijen"), speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(props.getProperty("table.body.sb"),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    output.write(prefix + props.getProperty("table.body.stop"));
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_ENKEL,
                                          CaissaTools.PAR_HALVE,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_HALVE);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
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
}
