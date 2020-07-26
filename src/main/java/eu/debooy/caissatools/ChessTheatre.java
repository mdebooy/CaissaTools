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

import eu.debooy.caissa.CaissaConstants;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.caissa.exceptions.ZetException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ManifestInfo;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;


/**
 * @author Marco de Booij
 */
public final class ChessTheatre extends Batchjob {
  private static  ManifestInfo    manifestInfo    = new ManifestInfo();
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final String HTML_ROUND_START  = "<round roundname=\"";
  private static final String HTML_ROUND_EINDE  = "</round";

  private ChessTheatre(){}

  public static void execute(String[] args) {
    TekstBestand  gamedata      = null;
    TekstBestand  headers       = null;
    int           maxBestanden  = 50;
    int           minPartijen   = 1;
    TekstBestand  updates       = null;
    String        versie        = manifestInfo.getBuildVersion();

    Banner.printMarcoBanner(resourceBundle.getString("banner.chesstheatre"));

    if (!setParameters(args)) {
      return;
    }

    if (parameters.containsKey(CaissaTools.PAR_MAXBESTANDEN)) {
      int hulp  =
          Integer.valueOf(parameters.get(CaissaTools.PAR_MAXBESTANDEN));
      if (hulp > 0) {
        maxBestanden  = hulp;
      }
    }
    if (parameters.containsKey(CaissaTools.PAR_MINPARTIJEN)) {
      int hulp  =
          Integer.valueOf(parameters.get(CaissaTools.PAR_MINPARTIJEN));
      if (hulp > 0) {
        minPartijen   = hulp;
      }
    }

    Collection<PGN> partijen    = new TreeSet<>(new PGN.byEventComparator());
    try {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(parameters.get(PAR_INVOERDIR) +
                                     parameters.get(CaissaTools.PAR_BESTAND)
                                     + EXT_PGN,
                      parameters.get(PAR_CHARSETIN)));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getMessage());
      return;
    }

    int aantalPartijen  = partijen.size() / maxBestanden + 1;
    if (aantalPartijen < minPartijen) {
      aantalPartijen  = minPartijen;
    }
    int   gameFile        = 0;

    try {
      // Maak de headers.xml file
      headers   = new TekstBestand.Builder()
                                  .setBestand(parameters.get(PAR_UITVOERDIR)
                                              + "headers.xml")
                                  .setCharset(parameters.get(PAR_CHARSETUIT))
                                  .setLezen(false).build();
      headers.write(CaissaTools.XML_HEADING);
      headers.write("<chessgames gamesperfile=\"" + aantalPartijen
                    + "\"  pgnfile=\"" + parameters.get(CaissaTools.PAR_ZIP)
                    + ".zip\">");
      // Maak de updates.xml file
      updates   = new TekstBestand.Builder()
                                  .setBestand(parameters.get(PAR_UITVOERDIR)
                                              + "updates.xml")
                                  .setCharset(parameters.get(PAR_CHARSETUIT))
                                  .setLezen(false).build();
      updates.write(CaissaTools.XML_HEADING);

      int           partijNummer  = 0;
      Iterator<PGN> iter          = partijen.iterator();
      PGN           partij        = iter.next();
      String        vorigEvent    = partij.getTag(CaissaConstants.PGNTAG_EVENT);
      String        vorigRound    = partij.getTag(CaissaConstants.PGNTAG_ROUND);

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
                              .setBestand(parameters.get(PAR_UITVOERDIR)
                                          + "gamedata" + gameFile + ".xml")
                              .setCharset(parameters.get(PAR_CHARSETUIT))
                              .setLezen(false).build();
          gamedata.write(CaissaTools.XML_HEADING);
          gamedata.write("<gamedata>");
          gameFile++;
        }
        partijNummer++;
        if (!partij.getTag(CaissaConstants.PGNTAG_EVENT).equals(vorigEvent)) {
          headers.write("    " + HTML_ROUND_EINDE);
          headers.write("  </tourney>");

          headers.write("  <tourney event=\""
                        + partij.getTag(CaissaConstants.PGNTAG_EVENT) + "\">");
          headers.write("    " + HTML_ROUND_START
                        + partij.getTag(CaissaConstants.PGNTAG_ROUND)+ "\">");
        } else {
          if (!partij.getTag(CaissaConstants.PGNTAG_ROUND).equals(vorigRound)) {
            headers.write("    " + HTML_ROUND_EINDE);

            vorigRound  = partij.getTag(CaissaConstants.PGNTAG_ROUND);
            headers.write("    " + HTML_ROUND_START
                          + partij.getTag(CaissaConstants.PGNTAG_ROUND)
                          + "\">");
          }
        }

        FEN fen = new FEN();
        if (partij.hasTag(CaissaConstants.PGNTAG_FEN)) {
          fen.setFen(partij.getTag(CaissaConstants.PGNTAG_FEN));
        }
        headers.write("      <game id=\"" + partijNummer + "\" "
                      + "whiteplayer=\""
                        + partij.getTag(CaissaConstants.PGNTAG_WHITE) + "\" "
                      + "blackplayer=\""
                        + partij.getTag(CaissaConstants.PGNTAG_BLACK) + "\" "
                      + "result=\""
                        + partij.getTag(CaissaConstants.PGNTAG_RESULT) + "\" "
                      + "site=\""
                        + partij.getTag(CaissaConstants.PGNTAG_SITE) + "\" "
                      + "tourneydate=\""
                        + partij.getTag(CaissaConstants.PGNTAG_DATE) + "\" />");

        gamedata.write("  <game id=\"" + partijNummer + "\">");
        gamedata.write(
            "    <plies type=\"ffenu\">" + fen.getKortePositie() + "</plies>");
        gamedata.write(
            "    <comment>" + partij.getTagsAsString());
        gamedata.write("    </comment>");
        StringBuilder lijn  = new StringBuilder("    <plies type=\"ffenu\">");
        if (!partij.getZuivereZetten().isEmpty()) {
          try {
            lijn.append(parseZetten(fen,partij.getZuivereZetten()));
          } catch (FenException | PgnException | ZetException e) {
            DoosUtils.foutNaarScherm("Error in " + partij.getTagsAsString());
            DoosUtils.foutNaarScherm(e.getMessage());
          }
        }
        lijn.append("</plies>");
        gamedata.write(lijn.toString());
        gamedata.write("  </game>");

        if (iter.hasNext()) {
          vorigEvent  = partij.getTag(CaissaConstants.PGNTAG_EVENT);
          vorigRound  = partij.getTag(CaissaConstants.PGNTAG_ROUND);
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
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestanden"),
                             gameFile));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.uitvoer"),
                             parameters.get(PAR_UITVOERDIR)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar ChessTheatre ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 13),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 13),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 13),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 13),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MAXBESTANDEN, 13),
                         resourceBundle.getString("help.maxbestanden"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MINPARTIJEN, 13),
                         resourceBundle.getString("help.minpartijen"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 13),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_ZIP, 13),
                         resourceBundle.getString("help.zip"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_BESTAND), 80);
  }

  private static String parseZetten(FEN fen, String pgnZetten)
      throws FenException, PgnException, ZetException {
    return CaissaUtils.pgnZettenToChessTheatre(fen, pgnZetten);
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MAXBESTANDEN,
                                          CaissaTools.PAR_MINPARTIJEN,
                                          PAR_UITVOERDIR,
                                          CaissaTools.PAR_ZIP});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setDirParameter(arguments, CaissaTools.PAR_MAXBESTANDEN);
    setDirParameter(arguments, CaissaTools.PAR_MINPARTIJEN);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));
    setBestandParameter(arguments, CaissaTools.PAR_ZIP, EXT_ZIP);

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_ZIP))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_ZIP));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }
}
