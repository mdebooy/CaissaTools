/**
 * Copyright 2011 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/7330l5
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
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ManifestInfo;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;


/**
 * @author Marco de Booij
 */
public final class ChessTheatre extends Batchjob {
  private static final  ManifestInfo    manifestInfo    = new ManifestInfo();
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static final String HTML_ROUND_START  = "<round roundname=\"";
  private static final String HTML_ROUND_EINDE  = "</round";

  protected ChessTheatre(){}

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_CHESSTHEATRE)
                           .setClassloader(ChessTheatre.class.getClassLoader())
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    TekstBestand  gamedata      = null;
    TekstBestand  headers       = null;
    var           maxBestanden  = 50;
    var           minPartijen   = 1;
    TekstBestand  updates       = null;
    var           versie        = manifestInfo.getBuildVersion();

    if (paramBundle.containsArgument(CaissaTools.PAR_MAXBESTANDEN)) {
      var hulp  = paramBundle.getLong(CaissaTools.PAR_MAXBESTANDEN).intValue();
      if (hulp > 0) {
        maxBestanden  = hulp;
      }
    }
    if (paramBundle.containsArgument(CaissaTools.PAR_MINPARTIJEN)) {
      var hulp  = paramBundle.getLong(CaissaTools.PAR_MINPARTIJEN).intValue();
      if (hulp > 0) {
        minPartijen   = hulp;
      }
    }

    Collection<PGN> partijen    = new TreeSet<>(new PGN.ByEventComparator());
    try {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(
              paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                     BestandConstants.EXT_PGN),
              paramBundle.getString(PAR_CHARSETIN)));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    var aantalPartijen  = partijen.size() / maxBestanden + 1;
    if (aantalPartijen < minPartijen) {
      aantalPartijen  = minPartijen;
    }
    var   gameFile        = 0;

    try {
      // Maak de headers.xml file
      headers   =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                      + "headers.xml")
                          .setCharset(paramBundle.getString(PAR_CHARSETUIT))
                          .setLezen(false).build();
      headers.write(CaissaTools.XML_HEADING);
      headers.write("<chessgames gamesperfile=\"" + aantalPartijen
                    + "\"  pgnfile=\""
                    + paramBundle.getBestand(CaissaTools.PAR_ZIP,
                                             BestandConstants.EXT_ZIP)
                    + "\">");
      // Maak de updates.xml file
      updates   =
          new TekstBestand.Builder()
                          .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                      + "updates.xml")
                          .setCharset(paramBundle.getString(PAR_CHARSETUIT))
                          .setLezen(false).build();
      updates.write(CaissaTools.XML_HEADING);

      var partijNummer  = 0;
      var iter          = partijen.iterator();
      var partij        = iter.next();
      var vorigEvent    = partij.getTag(PGN.PGNTAG_EVENT);
      var vorigRound    = partij.getTag(PGN.PGNTAG_ROUND);

      headers.write("  <tourney event=\"" + vorigEvent + "\">");
      headers.write("    " + HTML_ROUND_START + vorigRound + "\">");

      do {
        if (partijNummer%aantalPartijen == 0) {
          if (null != gamedata) {
            gamedata.write("</gamedata>");
            gamedata.write("<!-- Generated by CaissaTools [v" + versie +
                          "] for DGT ChessTheatre -->");
            gamedata.close();
          }
          // Maak de gamedataX.xml file
          gamedata  =
              new TekstBestand.Builder()
                              .setBestand(paramBundle.getString(PAR_UITVOERDIR)
                                          + "gamedata" + gameFile + ".xml")
                              .setCharset(paramBundle.getString(PAR_CHARSETUIT))
                              .setLezen(false).build();
          gamedata.write(CaissaTools.XML_HEADING);
          gamedata.write("<gamedata>");
          gameFile++;
        }
        partijNummer++;
        if (!partij.getTag(PGN.PGNTAG_EVENT).equals(vorigEvent)) {
          headers.write("    " + HTML_ROUND_EINDE);
          headers.write("  </tourney>");

          headers.write("  <tourney event=\""
                        + partij.getTag(PGN.PGNTAG_EVENT) + "\">");
          headers.write("    " + HTML_ROUND_START
                        + partij.getTag(PGN.PGNTAG_ROUND)+ "\">");
        } else {
          if (!partij.getTag(PGN.PGNTAG_ROUND).equals(vorigRound)) {
            headers.write("    " + HTML_ROUND_EINDE);

            vorigRound  = partij.getTag(PGN.PGNTAG_ROUND);
            headers.write("    " + HTML_ROUND_START
                          + partij.getTag(PGN.PGNTAG_ROUND)
                          + "\">");
          }
        }

        var fen = new FEN();
        if (partij.hasTag(PGN.PGNTAG_FEN)) {
          fen.setFen(partij.getTag(PGN.PGNTAG_FEN));
        }
        headers.write("      <game id=\"" + partijNummer + "\" "
                      + "whiteplayer=\""
                        + partij.getTag(PGN.PGNTAG_WHITE) + "\" "
                      + "blackplayer=\""
                        + partij.getTag(PGN.PGNTAG_BLACK) + "\" "
                      + "result=\""
                        + partij.getTag(PGN.PGNTAG_RESULT) + "\" "
                      + "site=\""
                        + partij.getTag(PGN.PGNTAG_SITE) + "\" "
                      + "tourneydate=\""
                        + partij.getTag(PGN.PGNTAG_DATE) + "\" />");

        gamedata.write("  <game id=\"" + partijNummer + "\">");
        gamedata.write(
            "    <plies type=\"ffenu\">" + fen.getKortePositie() + "</plies>");
        gamedata.write(
            "    <comment>" + partij.getTagsAsString());
        gamedata.write("    </comment>");
        var lijn  = new StringBuilder("    <plies type=\"ffenu\">");
        if (!partij.getZuivereZetten().isEmpty()) {
          try {
            lijn.append(parseZetten(fen,partij.getZuivereZetten()));
          } catch (PgnException e) {
            DoosUtils.foutNaarScherm("Error in " + partij.getTagsAsString());
            DoosUtils.foutNaarScherm(e.getLocalizedMessage());
          }
        }
        lijn.append("</plies>");
        gamedata.write(lijn.toString());
        gamedata.write("  </game>");

        if (iter.hasNext()) {
          vorigEvent  = partij.getTag(PGN.PGNTAG_EVENT);
          vorigRound  = partij.getTag(PGN.PGNTAG_ROUND);
          partij      = iter.next();
        } else {
          partij  = null;
        }
      } while (null != partij);

      gamedata.write("</gamedata>");
      gamedata.write("<!-- Generated by CaissaTools [v" + versie
                     + "] for DGT ChessTheatre -->");

      headers.write("    " + HTML_ROUND_EINDE);
      headers.write("  </tourney>");
      headers.write("</chessgames>");

      updates.write("<updates count=\"0\" />");
    } catch (FenException | BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (gamedata != null) {
          gamedata.close();
        }
        if (headers != null) {
          headers.close();
        }
        if (updates != null) {
          updates.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestanden"),
                             gameFile));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.uitvoer"),
                             paramBundle.getString(PAR_UITVOERDIR)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static String parseZetten(FEN fen, String pgnZetten)
      throws PgnException {
    return CaissaUtils.pgnZettenToChessTheatre(fen, pgnZetten);
  }
}
