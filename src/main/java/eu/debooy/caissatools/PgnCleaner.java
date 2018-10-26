/**
 * Copyright 2018 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
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
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.errorhandling.exception.FileNotFoundException;
import eu.debooy.doosutils.exception.BestandException;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class PgnCleaner {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private PgnCleaner() {}

  public static void execute(String[] args) throws PgnException {
    String        charsetIn   = Charset.defaultCharset().name();
    String        charsetUit  = Charset.defaultCharset().name();

    Banner.printBanner(resourceBundle.getString("banner.pgncleaner"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.ENKELZETTEN,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.UITVOER,
                                          CaissaTools.UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand   = arguments.getArgument(CaissaTools.BESTAND);
    if (!bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand     = bestand + CaissaTools.EXTENSIE_PGN;
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn   = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit  = arguments.getArgument(CaissaTools.CHARDSETUIT);
    }
    boolean   enkelZetten = false;
    if (arguments.hasArgument(CaissaTools.ENKELZETTEN)) {
      enkelZetten = DoosConstants.WAAR
          .equalsIgnoreCase(arguments.getArgument(CaissaTools.ENKELZETTEN));
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument(CaissaTools.INVOERDIR)) {
      invoerdir   = arguments.getArgument(CaissaTools.INVOERDIR);
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    uitvoer;
    if (arguments.hasArgument(CaissaTools.UITVOER)) {
      uitvoer   = arguments.getArgument(CaissaTools.UITVOER);
      if (!uitvoer.endsWith(CaissaTools.EXTENSIE_PGN)) {
        uitvoer = bestand + CaissaTools.EXTENSIE_PGN;
      }
    } else {
      uitvoer   = bestand.replaceAll(CaissaTools.EXTENSIE_PGN + "$",
                                     "_clean" + CaissaTools.EXTENSIE_PGN);
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }

    int             noPartijen  = 0;
    Collection<PGN> partijen    =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand,
                                   charsetIn);
    TekstBestand    output      = null;

    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoerdir
                                            + File.separator + uitvoer)
                                .setCharset(charsetUit)
                                .setLezen(false).build();

      for (PGN partij: partijen) {
        if (enkelZetten) {
          partij.setZetten(partij.getZuivereZetten());
        }

        if (DoosUtils.isNotBlankOrNull(partij.getZuivereZetten())) {
          output.write(partij.getTagsAsString());
          String  zetten  = partij.getZetten() + " "
                            + partij.getTag(CaissaConstants.PGNTAG_RESULT);
          while (zetten.length() > 75) {
            int splits  = zetten.substring(1, 75).lastIndexOf(" ");
            output.write(zetten.substring(0, splits + 1));
            zetten  = zetten.substring(splits + 2);
          }
          output.write(zetten);
          output.write("");
          noPartijen++;
        }
      }
    } catch (BestandException | FileNotFoundException e) {
      System.err.println(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + invoerdir + File.separator + bestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir + File.separator + uitvoer);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + noPartijen);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnCleaner ["
                         + resourceBundle.getString("label.optie")
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand     ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin   ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit  ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --enkelzetten ",
        MessageFormat.format(resourceBundle.getString("help.enkelzetten"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --invoerdir   ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --uitvoer     ",
                         resourceBundle.getString("help.uitvoer"), 80);
    DoosUtils.naarScherm("  --uitvoerdir  ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             CaissaTools.BESTAND), 80);
    DoosUtils.naarScherm();
  }
}
