/**
 * Copyright 2013 Marco de Booij
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
/**
 * 
 */
package eu.debooy.caissatools;

import eu.debooy.caissa.CaissaConstants;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.PgnException;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class VertaalPgn {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private VertaalPgn() {}

  public static void execute(String[] args) throws PgnException {
    String          bestand     = "";
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();
    List<String>    fouten      = new ArrayList<String>();
    String          naarStukken = "";
    String          naarTaal    = Locale.getDefault().getLanguage();
    BufferedWriter  output      = null;
    String          pgn         = "";
    String          vanStukken  = "";
    String          vanTaal     = Locale.getDefault().getLanguage();

    Banner.printBanner(resourceBundle.getString("banner.vertaalpgn"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.NAARTAAL,
                                          CaissaTools.PGN,
                                          CaissaTools.UITVOERDIR,
                                          CaissaTools.VANTAAL});
    if (!arguments.isValid()) {
      help();
      return;
    }

    
    if (arguments.hasArgument(CaissaTools.BESTAND)) {
      bestand     =
          DoosUtils.nullToEmpty(arguments.getArgument(CaissaTools.BESTAND));
      if (bestand.contains(File.separator)) {
        fouten.add(
            MessageFormat.format(
                resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                         CaissaTools.BESTAND));
      }
      if (bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
        bestand = bestand.substring(0, bestand.length() - 4);
      }
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn   = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit  = arguments.getArgument(CaissaTools.CHARDSETUIT);
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
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    if (arguments.hasArgument(CaissaTools.NAARTAAL)) {
      naarTaal    = arguments.getArgument(CaissaTools.NAARTAAL).toLowerCase();
    }
    if (arguments.hasArgument(CaissaTools.PGN)) {
      pgn         = arguments.getArgument(CaissaTools.PGN);
    }
    if (arguments.hasArgument(CaissaTools.VANTAAL)) {
      vanTaal     = arguments.getArgument(CaissaTools.VANTAAL).toLowerCase();
    }

    if (naarTaal.equals(vanTaal)) {
      fouten.add(MessageFormat
            .format(resourceBundle.getString(CaissaTools.ERR_TALENGELIJK),
                    vanTaal, naarTaal));
    }
    if (bestand.isEmpty()  && pgn.isEmpty()) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_GEENINVOER));
    }
    if (!bestand.isEmpty() && !pgn.isEmpty()) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BESTANDENPGN));
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    // Haal de stukcodes op
    naarStukken = CaissaConstants.Stukcodes.valueOf(naarTaal.toUpperCase())
                                 .getStukcodes();
    vanStukken  = CaissaConstants.Stukcodes.valueOf(vanTaal.toUpperCase())
                                 .getStukcodes();
    // Verwerk command line invoer en stop.
    if (!pgn.isEmpty()) {
      PGN partij  = new PGN();
      partij.setZetten(pgn);
      DoosUtils.naarScherm(CaissaUtils.vertaalStukken(partij.getZetten(),
                                                      vanStukken, naarStukken));

      return;
    }

    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand
                                   + CaissaTools.EXTENSIE_PGN, charsetIn);

    try {
      output  = Bestand.openUitvoerBestand(uitvoerdir + File.separator + bestand
                                             + "_" + naarTaal
                                             + CaissaTools.EXTENSIE_PGN,
                                           charsetUit);

      for (PGN partij: partijen) {
        String  zetten  = partij.getZetten();
        partij.setZetten(CaissaUtils.vertaalStukken(zetten,
                                                    vanStukken, naarStukken));
        Bestand.schrijfRegel(output, partij.toString());
      }
      output.close();
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator + bestand + "_"
                         + naarTaal + ".pgn");
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar VertaalPgn ["
                         + resourceBundle.getString("label.optie") + "]");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand    ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin  ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --invoerdir  ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --naartaal   ",
        MessageFormat.format(resourceBundle.getString("help.naartaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm("  --pgn        ",
                         resourceBundle.getString("help.pgnzetten"), 80);
    DoosUtils.naarScherm("  --uitvoerdir ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm("  --vantaal    ",
        MessageFormat.format(resourceBundle.getString("help.vantaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.vertaalpgn.extra"), 80);
  }
}
