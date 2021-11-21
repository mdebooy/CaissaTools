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
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
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
public final class VertaalPgn extends Batchjob {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private VertaalPgn() {}

  public static void execute(String[] args) {
    Banner.printMarcoBanner(resourceBundle.getString("banner.vertaalpgn"));

    if (!setParameters(args)) {
      return;
    }

    // Haal de stukcodes op
    var naarStukken =
        CaissaConstants.Stukcodes
                       .valueOf(parameters.get(CaissaTools.PAR_NAARTAAL)
                                          .toUpperCase())
                       .getStukcodes();
    var vanStukken  =
        CaissaConstants.Stukcodes
                       .valueOf(parameters.get(CaissaTools.PAR_VANTAAL)
                                          .toUpperCase())
                       .getStukcodes();

    // Verwerk command line invoer en stop.
    if (parameters.containsKey(CaissaTools.PAR_PGN)) {
      var partij  = new PGN();
      partij.setZetten(parameters.get(CaissaTools.PAR_PGN));
      try {
        DoosUtils.naarScherm(CaissaUtils.vertaalStukken(partij.getZetten(),
                vanStukken, naarStukken));
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
      return;
    }

    var             invoer    = parameters.get(PAR_INVOERDIR)
                                 + parameters.get(CaissaTools.PAR_BESTAND)
                                 + EXT_PGN;
    TekstBestand    output    = null;
    var             uitvoer   = parameters.get(PAR_UITVOERDIR)
                                 + parameters.get(CaissaTools.PAR_BESTAND) + "_"
                                 + parameters.get(CaissaTools.PAR_NAARTAAL)
                                 + EXT_PGN;

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoer)
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();

      for (var partij: partijen) {
        var zetten  = partij.getZetten();
        partij.setZetten(CaissaUtils.vertaalStukken(zetten,
                                                    vanStukken, naarStukken));
        output.write(partij.toString());
      }
      output.close();
    } catch (BestandException | PgnException e) {
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

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             uitvoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar VertaalPgn ["
                         + getMelding(LBL_OPTIE) + "]");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 11),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 11),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 11),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 11),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_NAARTAAL, 11),
        MessageFormat.format(resourceBundle.getString("help.naartaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_PGN, 11),
                         resourceBundle.getString("help.pgnzetten"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 11),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_VANTAAL, 11),
        MessageFormat.format(resourceBundle.getString("help.vantaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.vertaalpgn.extra"), 80);
  }

  private static boolean setParameters(String[] args) {
    var           arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_NAARTAAL,
                                          CaissaTools.PAR_PGN,
                                          PAR_UITVOERDIR,
                                          CaissaTools.PAR_VANTAAL});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_NAARTAAL,
                 Locale.getDefault().getLanguage());
    setParameter(arguments, CaissaTools.PAR_PGN);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));
    setParameter(arguments, CaissaTools.PAR_VANTAAL,
                 Locale.getDefault().getLanguage());

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (!parameters.containsKey(CaissaTools.PAR_BESTAND)
        && !parameters.containsKey(CaissaTools.PAR_PGN)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_GEENINVOER));
    }
    if (parameters.containsKey(CaissaTools.PAR_BESTAND)
        && parameters.containsKey(CaissaTools.PAR_PGN)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BESTANDENPGN));
    }
    if (getParameter(CaissaTools.PAR_VANTAAL)
            .equals(getParameter(CaissaTools.PAR_NAARTAAL))) {
      fouten.add(MessageFormat
            .format(resourceBundle.getString(CaissaTools.ERR_TALENGELIJK),
                    getParameter(CaissaTools.PAR_VANTAAL),
                    getParameter(CaissaTools.PAR_NAARTAAL)));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }
}
