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
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
public final class StartPgn {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private StartPgn() {}

  public static void execute(String[] args) {
    List<String>    fouten  = new ArrayList<String>();
    BufferedWriter  output  = null;

    Banner.printBanner(resourceBundle.getString("banner.startpgn"));

    Arguments       arguments   = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.DATE,
                                          CaissaTools.EVENT,
                                          CaissaTools.SITE,
                                          CaissaTools.SPELERS,
                                          CaissaTools.UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND,
                                         CaissaTools.DATE,
                                         CaissaTools.EVENT,
                                         CaissaTools.SITE,
                                         CaissaTools.SPELERS});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand     = arguments.getArgument(CaissaTools.BESTAND);
    if (bestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                       CaissaTools.BESTAND));
    }
    if (!bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand = bestand + CaissaTools.EXTENSIE_PGN;
    }
    String    date        = arguments.getArgument(CaissaTools.DATE);
    String    event       = arguments.getArgument(CaissaTools.EVENT);
    String    site        = arguments.getArgument(CaissaTools.SITE);
    String[]  speler      = arguments.getArgument(CaissaTools.SPELERS)
                                     .split(";");
    String    uitvoerdir  = ".";
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    Arrays.sort(speler, String.CASE_INSENSITIVE_ORDER);
    int noSpelers = speler.length;

    File  pgnFile = new File(uitvoerdir + File.separator + bestand);
    try {
      output  = Bestand.openUitvoerBestand(pgnFile);
      for (int i = 0; i < (noSpelers -1); i++) {
        for (int j = i + 1; j < noSpelers; j++) {
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_EVENT
                                       + " \"" + event + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_SITE
                                       + " \"" + site + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_DATE
                                       + " \"" + date + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_ROUND
                                       + " \"-\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_WHITE
                                       + " \"" + speler[i] + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_BLACK
                                       + " \"" + speler[j] + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_RESULT
                                       + " \"*\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_EVENTDATE
                                       + " \"" + date + "\"]", 2);
          Bestand.schrijfRegel(output, "*", 2);
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_EVENT
                                       + " \"" + event + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_SITE
                                       + " \"" + site + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_DATE
                                       + " \"" + date + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_ROUND
                                       + " \"-\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_WHITE
                                       + " \"" + speler[j] + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_BLACK
                                       + " \"" + speler[i] + "\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_RESULT
                                       + " \"*\"]");
          Bestand.schrijfRegel(output, "[" + CaissaConstants.PGNTAG_EVENTDATE
                                       + " \"" + date + "\"]", 2);
          Bestand.schrijfRegel(output, "*", 2);
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
      } catch (IOException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator + bestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
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
    DoosUtils.naarScherm("  --bestand    ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin  ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --date       ",
                         resourceBundle.getString("help.date"), 80);
    DoosUtils.naarScherm("  --event      ",
                         resourceBundle.getString("help.event"), 80);
    DoosUtils.naarScherm("  --site       ",
                         resourceBundle.getString("help.site"), 80);
    DoosUtils.naarScherm("  --spelers    ",
                         resourceBundle.getString("help.spelers"), 80);
    DoosUtils.naarScherm("  --uitvoerdir ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(
            resourceBundle.getString("help.paramverplichtbehalve"),
                             CaissaTools.UITVOERDIR), 80);
    DoosUtils.naarScherm();
  }
}
