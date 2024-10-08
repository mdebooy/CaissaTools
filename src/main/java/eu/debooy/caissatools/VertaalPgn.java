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
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class VertaalPgn extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  protected VertaalPgn() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_VERTAALPGN)
                           .setValidator(new VertaalPgnParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    // Haal de stukcodes op
    var naarStukken =
        CaissaConstants.Stukcodes
                       .valueOf(paramBundle.getString(CaissaTools.PAR_NAARTAAL)
                                           .toUpperCase())
                       .getStukcodes();
    var vanStukken  =
        CaissaConstants.Stukcodes
                       .valueOf(paramBundle.getString(CaissaTools.PAR_VANTAAL)
                                           .toUpperCase())
                       .getStukcodes();

    // Verwerk command line invoer en stop.
    if (paramBundle.containsParameter(CaissaTools.PAR_PGN)) {
      var partij  = new PGN();
      partij.setZetten(paramBundle.getString(CaissaTools.PAR_PGN));
      try {
        DoosUtils.naarScherm(CaissaUtils.vertaalStukken(partij.getZetten(),
                vanStukken, naarStukken));
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
      return;
    }

    var deel    = paramBundle.getBestand(CaissaTools.PAR_BESTAND).split("\\.");
    deel[deel.length-2] =
            deel[deel.length-2] + "_"
                  + paramBundle.getString(CaissaTools.PAR_NAARTAAL);

    var uitvoer = String.join(".", deel);

    Collection<PGN> partijen;
    try {
      partijen =
          CaissaUtils
              .laadPgnBestand(paramBundle.getBestand(CaissaTools.PAR_BESTAND));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    try (var output  =
          new TekstBestand.Builder()
                          .setBestand(uitvoer)
                          .setLezen(false).build()) {
      for (var partij: partijen) {
        var zetten  = partij.getZetten();
        partij.setZetten(CaissaUtils.vertaalStukken(zetten,
                                                    vanStukken, naarStukken));
        output.write(partij.toString());
      }
    } catch (BestandException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             uitvoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }
}
