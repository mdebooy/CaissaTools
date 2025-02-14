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
public class Stellingen extends Batchjob {
  protected Stellingen() {}

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName("Stellingen")
                           .setClassloader(Stellingen.class.getClassLoader())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    var stellingen  = new HashMap<String, Integer>();

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
        verwerkPartij(partij, stellingen);
      } catch (FenException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage() + "|"
                                  + partij.getZuivereZetten() + "|");
      }
    });

    DoosUtils.naarScherm();
    DoosUtils.naarScherm(String.format("Partijen  : %d", partijen.size()));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(String.format("Stellingen: %d", stellingen.size()));
    if (paramBundle.containsArgument("top")) {
      DoosUtils.naarScherm();
      stellingen.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(DoosUtils.nullToValue(paramBundle.getInteger("top"), 0))
                .forEach(stelling ->
                    DoosUtils.naarScherm(String.format("%-75s | %6d",
                                                       stelling.getKey(),
                                                       stelling.getValue())));
    }
  }

  private static void verwerkPartij(PGN partij, Map<String, Integer> stellingen)
      throws FenException, PgnException {
    var fen         = new FEN();

    if (partij.hasTag(PGN.PGNTAG_FEN)) {
      fen.setFen(partij.getTag(PGN.PGNTAG_FEN));
    }

    CaissaTools.verwerkZetten(partij.getZuivereZetten(), fen, stellingen);

    DoosUtils.naarScherm(
        String.format("%-30s - %-30s : %3s | %-4s | %10s | %5s | %3d |",
                      partij.getWhite(),
                      partij.getBlack(),
                      partij.getTag(PGN.PGNTAG_RESULT).replace("1/2", "½"),
                      DoosUtils.nullToEmpty(partij.getTag(PGN.PGNTAG_ECO)),
                      partij.getTag(PGN.PGNTAG_DATE),
                      partij.getTag(PGN.PGNTAG_ROUND),
                      partij.getAantalZettenWit()));
  }
}
