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
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collection;
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
    BufferedWriter  output      = null;
    String          bestand     = "";
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();
    String          naarStukken = "";
    String          naarTaal    = Locale.getDefault().getLanguage();
    String          pgn         = "";
    String          vanStukken  = "";
    String          vanTaal     = Locale.getDefault().getLanguage();

    Banner.printBanner(resourceBundle.getString("banner.vertaalpgn"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "charsetin", "charsetuit",
                                          "naartaal", "pgn", "vantaal"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    if (arguments.hasArgument("bestand")) {
      bestand     = arguments.getArgument("bestand");
      if (bestand.endsWith(".pgn")) {
        bestand   = bestand.substring(0, bestand.length() - 4);
      }
    }
    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    if (arguments.hasArgument("naartaal")) {
      naarTaal    = arguments.getArgument("naartaal").toLowerCase();
    }
    if (arguments.hasArgument("pgn")) {
      pgn         = arguments.getArgument("pgn");
    }
    if (arguments.hasArgument("vantaal")) {
      vanTaal     = arguments.getArgument("vantaal").toLowerCase();
    }

    // Laatste test op juistheid
    if (naarTaal.equals(vanTaal)
        || (bestand.isEmpty()  && pgn.isEmpty())
        || (!bestand.isEmpty() && !pgn.isEmpty())) {
      help();
      DoosUtils.naarScherm();
      if (naarTaal.equals(vanTaal)) {
        DoosUtils.foutNaarScherm(
          MessageFormat.format(resourceBundle.getString("error.talen.gelijk"),
                               vanTaal, naarTaal));
      }
      if (bestand.isEmpty()  && pgn.isEmpty()) {
        DoosUtils.foutNaarScherm(resourceBundle.getString("error.geen.invoer"));
      }
      if (!bestand.isEmpty() && !pgn.isEmpty()) {
        DoosUtils
            .foutNaarScherm(resourceBundle.getString("error.bestand.en.pgn"));
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

    Collection<PGN> partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn);

    try {
      output  = Bestand.openUitvoerBestand(bestand + "_" + naarTaal + ".pgn",
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
                         + bestand + "_" + naarTaal + ".pgn");
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
    DoosUtils.naarScherm("  --naartaal   ",
        MessageFormat.format(resourceBundle.getString("help.naartaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm("  --pgn        ",
                         resourceBundle.getString("help.pgnzetten"), 80);
    DoosUtils.naarScherm("  --vantaal    ",
        MessageFormat.format(resourceBundle.getString("help.vantaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.vertaalpgn.extra"), 80);
  }
}
