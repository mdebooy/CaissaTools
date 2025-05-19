/*
 * Copyright (c) 2024 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
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
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco de Booij
 */
public class Partijinfo extends Batchjob {
  protected Partijinfo() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_PARTIJINFO)
                           .setClassloader(Partijinfo.class.getClassLoader())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    Collection<PGN> partijen;
    try {
      partijen =
          CaissaUtils
              .laadPgnBestand(paramBundle.getBestand(CaissaTools.PAR_BESTAND));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.stream()
            .filter(PGN::isBeeindigd)
            .sorted()
            .forEach(partij -> {
      try {
        verwerkPartij(partij);
      } catch (FenException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage() + "|"
                                  + partij.getZuivereZetten() + "|");
      }
    });
  }

  private static String getOpmerking(int aantal, String uitslag) {
    return aantal >= 3
            && !uitslag.equals(CaissaConstants.PARTIJ_REMISE) ? "!!!" : "";
  }

  private static void verwerkPartij(PGN partij)
      throws FenException, PgnException {
    var fen         = new FEN();
    var stellingen  = new HashMap<String, Integer>();

    if (partij.hasTag(PGN.PGNTAG_FEN)) {
      fen.setFen(partij.getTag(PGN.PGNTAG_FEN));
    }

    CaissaTools.verwerkZetten(partij.getZuivereZetten(), fen, stellingen);

    DoosUtils.naarScherm(
        String.format("%-30s - %-30s : %3s | %-4s | %10s | %5s | %3d | %3d |",
                      partij.getWhite(),
                      partij.getBlack(),
                      partij.getTag(PGN.PGNTAG_RESULT).replace("1/2", "½"),
                      DoosUtils.nullToEmpty(partij.getTag(PGN.PGNTAG_ECO)),
                      partij.getTag(PGN.PGNTAG_DATE),
                      partij.getTag(PGN.PGNTAG_ROUND),
                      partij.getAantalZettenWit(),
                      stellingen.size()));
    stellingen.entrySet()
              .stream()
              .filter(stelling -> stelling.getValue() > 1)
              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
              .forEach(stelling ->
        DoosUtils.naarScherm(
            String.format("%5dx %-59s%s",
                          stelling.getValue(),
                          stelling.getKey(),
                          getOpmerking(stelling.getValue(),
                                       partij.getTag(PGN.PGNTAG_RESULT)))));
  }
}
