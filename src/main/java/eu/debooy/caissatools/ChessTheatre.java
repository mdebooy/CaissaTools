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
import eu.debooy.caissa.exceptions.ZetException;
import eu.debooy.caissa.sorteer.PGNSortByEvent;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ManifestInfo;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class ChessTheatre {
  private static  ManifestInfo    manifestInfo    = new ManifestInfo();
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private ChessTheatre(){}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter  gamedata      = null;
    BufferedWriter  headers       = null;
    BufferedWriter  updates       = null;
    int             maxBestanden  = 50;
    int             minPartijen   = 1;
    String          charsetIn     = Charset.defaultCharset().name();
    String          charsetUit    = Charset.defaultCharset().name();
    String          versie        = manifestInfo.getBuildVersion();
    String          zip           = "";

    Banner.printBanner(resourceBundle.getString("banner.chesstheatre"));

    Arguments       arguments   = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "charsetin", "charsetuit",
                                          "maxBestanden", "minPartijen",
                                          "uitvoerdir", "zip"});
    arguments.setVerplicht(new String[] {"bestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String  bestand = arguments.getArgument("bestand");
    if (bestand.endsWith(".pgn")) {
      bestand = bestand.substring(0, bestand.length() - 4);
    }
    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    if (arguments.hasArgument("maxBestanden")) {
      int hulp  = Integer.valueOf(arguments.getArgument("maxBestanden"));
      if (hulp > 0) {
        maxBestanden  = hulp;
      }
    }
    if (arguments.hasArgument("minPartijen")) {
      int hulp  = Integer.valueOf(arguments.getArgument("minPartijen"));
      if (hulp > 0) {
        minPartijen   = hulp;
      }
    }
    String    uitvoerdir  = arguments.getArgument("uitvoerdir");
    if (null == uitvoerdir) {
      uitvoerdir  = ".";
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    if (arguments.hasArgument("zip")) {
      zip = arguments.getArgument("zip");
    } else {
      zip = bestand;
    }
    if (zip.endsWith(".zip")) {
      zip = zip.substring(0, zip.length() - 4);
    }

    List<PGN> partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn,
                                                     new PGNSortByEvent());
    Collections.sort(partijen);
    int aantalPartijen  = partijen.size() / maxBestanden + 1;
    if (aantalPartijen < minPartijen) {
      aantalPartijen  = minPartijen;
    }
    int   gameFile        = 0;

    File  gamedataFile  = null;
    File  headersFile   = new File(uitvoerdir + File.separator + "headers.xml");
    File  updatesFile   = new File(uitvoerdir + File.separator + "updates.xml");

    try {
      // Maak de headers.xml file
      headers   = Bestand.openUitvoerBestand(headersFile, charsetUit);
      headers.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
      headers.newLine();
      headers.write("<chessgames gamesperfile=\"" + aantalPartijen
                    + "\"  pgnfile=\"" + zip + ".zip\">");
      headers.newLine();
      // Maak de updates.xml file
      updates   = Bestand.openUitvoerBestand(updatesFile, charsetUit);
      updates.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
      updates.newLine();

      int           partijNummer  = 0;
      Iterator<PGN> iter          = partijen.iterator();
      PGN           partij        = iter.next();
      String        vorigEvent    = partij.getTag("Event");
      String        vorigRound    = partij.getTag("Round");

      headers.write("  <tourney event=\"" + vorigEvent + "\">");
      headers.newLine();
      headers.write("    <round roundname=\"" + vorigRound + "\">");
      headers.newLine();

      do {
        if (partijNummer%aantalPartijen == 0) {
          if (null != gamedata) {
            gamedata.write("</gamedata>");
            gamedata.newLine();
            gamedata.write("<!-- Generated by CaissaTools [v" + versie
                           + "] for DGT ChessTheatre -->");
            gamedata.close();
          }
          // Maak de gamedataX.xml file
          gamedataFile  = new File(uitvoerdir + File.separator + "gamedata"
                                   + gameFile + ".xml");
          gamedata      = Bestand.openUitvoerBestand(gamedataFile, charsetUit);
          gamedata.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
          gamedata.newLine();
          gamedata.write("<gamedata>");
          gamedata.newLine();
          gameFile++;
        }
        partijNummer++;
        if (!partij.getTag("Event").equals(vorigEvent)) {
          headers.write("    </round>");
          headers.newLine();
          headers.write("  </tourney>");
          headers.newLine();

          headers.write("  <tourney event=\"" + partij.getTag("Event") + "\">");
          headers.newLine();
          headers.write("    <round roundname=\"" + partij.getTag("Round")
                        + "\">");
          headers.newLine();
        } else {
          if (!partij.getTag("Round").equals(vorigRound)) {
            headers.write("    </round>");
            headers.newLine();
 
            vorigRound  = partij.getTag("Round");
            headers.write("    <round roundname=\"" + partij.getTag("Round")
                          + "\">");
            headers.newLine();
          }
        }

        FEN fen = new FEN();
        if (partij.hasTag("FEN")) {
          fen.setFen(partij.getTag("FEN"));
        }
        headers.write("      <game id=\"" + partijNummer + "\" "
                      + "whiteplayer=\"" + partij.getTag("White") +"\" "
                      + "blackplayer=\"" + partij.getTag("Black") +"\" "
                      + "result=\"" + partij.getTag("Result") +"\" "
                      + "site=\"" + partij.getTag("Site") +"\" "
                      + "tourneydate=\"" + partij.getTag("Date") + "\" />");
        headers.newLine();

        gamedata.write("  <game id=\"" + partijNummer + "\">");
        gamedata.newLine();
        gamedata.write("    <plies type=\"ffenu\">" + fen.getKortePositie()
                       + "</plies>");
        gamedata.newLine();
        gamedata.write("    <comment>" + partij.getTagsAsString());
        gamedata.write("    </comment>");
        gamedata.newLine();
        gamedata.write("    <plies type=\"ffenu\">");
        if (!partij.getZuivereZetten().isEmpty()) {
          try {
            gamedata.write(parseZetten(fen, partij.getZuivereZetten()));
          } catch (FenException e) {
            DoosUtils.foutNaarScherm("Error in " + partij.getTagsAsString());
            DoosUtils.foutNaarScherm(e.getMessage());
          } catch (ZetException e) {
            DoosUtils.foutNaarScherm("Error in " + partij.getTagsAsString());
            DoosUtils.foutNaarScherm(e.getMessage());
          }
        }
        gamedata.write("</plies>");
        gamedata.newLine();
        gamedata.write("  </game>");
        gamedata.newLine();

        if (iter.hasNext()) {
          vorigEvent  = partij.getTag("Event");
          vorigRound  = partij.getTag("Round");
          partij      = iter.next();
        } else {
          partij  = null;
        }
      } while (null != partij);

      gamedata.write("</gamedata>");
      gamedata.newLine();
      gamedata.write("<!-- Generated by CaissaTools [v" + versie
                     + "] for DGT ChessTheatre -->");
      gamedata.close();

      headers.write("    </round>");
      headers.newLine();
      headers.write("  </tourney>");
      headers.newLine();
      headers.write("</chessgames>");
      headers.newLine();
      headers.close();

      updates.write("<updates count=\"0\" />");
      updates.close();
    } catch (FenException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
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
      } catch (IOException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.bestanden") + " "
                         + gameFile);
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar ChessTheatre ["
                         + resourceBundle.getString("label.optie")
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand      ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin    ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit   ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --maxBestanden ",
                         resourceBundle.getString("help.maxbestanden"), 80);
    DoosUtils.naarScherm("  --minPartijen  ",
                         resourceBundle.getString("help.minpartijen"), 80);
    DoosUtils.naarScherm("  --uitvoerdir   ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm("  --zip          ",
                         resourceBundle.getString("help.zip"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             "bestand"), 80);
    DoosUtils.naarScherm();
  }

  /**
   * Zet de PGN zetten om in ChessTheatre zetten.
   * @param zetten de partij in PGN formaat
   * @return de partij in ChessTheatre formaat
   * @throws FenException 
   * @throws PgnException 
   * @throws ZetException 
   */
  private static String parseZetten(FEN fen, String pgnZetten)
      throws FenException, PgnException, ZetException {
    return CaissaUtils.pgnZettenToChessTheatre(fen, pgnZetten);
  }
}
