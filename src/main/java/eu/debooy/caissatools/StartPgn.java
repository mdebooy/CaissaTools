/**
 * Copyright 2009 Marco de Booij
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
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class StartPgn extends Batchjob {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private StartPgn() {}

  public static void execute(String[] args) {
    Banner.printMarcoBanner(resourceBundle.getString("banner.startpgn"));

    if (!setParameters(args)) {
      return;
    }

    String    date    = parameters.get(CaissaTools.PAR_DATE);
    String    event   = parameters.get(CaissaTools.PAR_EVENT);
    String    site    = parameters.get(CaissaTools.PAR_SITE);
    String[]  speler  = parameters.get(CaissaTools.PAR_SPELERS).split(";");
    String    uitvoer = parameters.get(PAR_UITVOERDIR)
                        + parameters.get(CaissaTools.PAR_BESTAND);

    Arrays.sort(speler, String.CASE_INSENSITIVE_ORDER);
    int noSpelers = speler.length;

    TekstBestand  output  = null;
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoer)
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      for (int i = 0; i < (noSpelers -1); i++) {
        for (int j = i + 1; j < noSpelers; j++) {
          schrijfPartij(output, event, site, date, speler[i], speler[j]);
          schrijfPartij(output, event, site, date, speler[j], speler[i]);
        }
      }
    } catch (BestandException e) {
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

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             uitvoer));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar StartPgn --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm("    --date=<"
                         + resourceBundle.getString("label.date")
                         + "> --event=<"
                         + resourceBundle.getString("label.event")
                         + "> --site=<"
                         + resourceBundle.getString("label.site")
                         + "> \\");
    DoosUtils.naarScherm("    --spelers=<"
                         + resourceBundle.getString("label.speler")
                         + "1>[;<"
                         + resourceBundle.getString("label.speler")
                         + "2>...] \\");
    DoosUtils.naarScherm("    [--uitvoerdir=<"
                         + resourceBundle.getString("label.uitvoerdir") + ">]");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 11),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 11),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_DATE, 11),
                         resourceBundle.getString("help.date"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_EVENT, 11),
                         resourceBundle.getString("help.event"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SITE, 11),
                         resourceBundle.getString("help.site"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SPELERS, 11),
                         resourceBundle.getString("help.spelers"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 11),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(
            resourceBundle.getString("help.paramverplichtbehalve"),
            PAR_UITVOERDIR), 80);
    DoosUtils.naarScherm();
  }

  private static void schrijfPartij(TekstBestand output, String event,
                                    String site, String date, String witspeler,
                                    String zwartspeler)
      throws BestandException {
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENT, event));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_SITE, site));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_DATE, date));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_ROUND, "-"));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_WHITE, witspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_BLACK,
                                      zwartspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_RESULT, "*"));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENTDATE, date));
    output.write("");
    output.write("*");
    output.write("");
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_DATE,
                                          CaissaTools.PAR_EVENT,
                                          CaissaTools.PAR_SITE,
                                          CaissaTools.PAR_SPELERS,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_DATE,
                                         CaissaTools.PAR_EVENT,
                                         CaissaTools.PAR_SITE,
                                         CaissaTools.PAR_SPELERS});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());

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
}
