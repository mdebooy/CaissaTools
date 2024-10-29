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
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Marco de Booij
 */
public class Partijinfo extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  protected Partijinfo() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_PARTIJINFO)
                           .setClassloader(PgnCleaner.class.getClassLoader())
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
        DoosUtils.foutNaarScherm(e.getLocalizedMessage() + "|" + partij.getZuivereZetten() + "|");
      }
    });
  }

  private static void verwerkPartij(PGN partij)
      throws FenException, PgnException {
    var fen         = new FEN();
    var stellingen  = new HashMap<String, Integer>();

    if (partij.hasTag(PGN.PGNTAG_FEN)) {
      fen.setFen(partij.getTag(PGN.PGNTAG_FEN));
    }

    verwerkZetten(partij.getZuivereZetten(), fen, stellingen);

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
        DoosUtils.naarScherm(String.format("%5dx %-59s%s",
                                           stelling.getValue(),
                                           stelling.getKey(),
                                           stelling.getValue() >= 3 && !partij.getTag(PGN.PGNTAG_RESULT).equals(CaissaConstants.PARTIJ_REMISE) ? "!!!" : "")));
  }

  private static void verwerkZetten(String zetten,
                                    FEN fen, Map<String, Integer> stellingen)
      throws PgnException {
    if (zetten.isEmpty()) {
      return;
    }
    var halveZetten   = zetten.split(" ");
    var pgnZet        = "";

    for (var halveZet : halveZetten) {
      if (halveZet.indexOf('.') >= 0) {
        if (halveZet.indexOf('.') == (halveZet.length() - 1)) {
          throw new PgnException(MessageFormat.format(
              resourceBundle.getString(PGN.ERR_HALVEZET),
              halveZet, zetten));
        }
        pgnZet  = halveZet.substring(halveZet.lastIndexOf('.') + 1);
      } else {
        pgnZet  = halveZet;
      }
      fen.doeZet(CaissaUtils.vindZet(fen, pgnZet));
      if (stellingen.containsKey(fen.getKorteFen())) {
        stellingen.put(fen.getKorteFen(),
                       stellingen.get(fen.getKorteFen()) + 1);
      } else {
        stellingen.put(fen.getKorteFen(), 1);
      }
    }
  }
}
