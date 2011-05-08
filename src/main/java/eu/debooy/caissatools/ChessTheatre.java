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
import eu.debooy.doosutils.ManifestInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Marco de Booij
 */
public class ChessTheatre {
  private static  ManifestInfo  manifestInfo  = new ManifestInfo();

  private ChessTheatre(){}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter  gamedata      = null;
    BufferedWriter  headers       = null;
    BufferedWriter  updates       = null;
    int             maxBestanden  = 50;
    int             minPartijen   = 1;
    List<PGN>       partijen      = new ArrayList<PGN>();
    String          charsetIn     = Charset.defaultCharset().name();
    String          charsetUit    = Charset.defaultCharset().name();
    String          versie        = manifestInfo.getBuildVersion();
    String          zip           = "";

    Banner.printBanner("ChessTheatre");

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
    if (arguments.hasArgument("zip")) {
      zip = arguments.getArgument("zip");
    } else {
      zip = bestand;
    }
    if (zip.endsWith(".zip")) {
      zip = zip.substring(0, zip.length() - 4);
    }

    partijen  = CaissaUtils.laadPgnBestand(bestand, charsetIn,
                                           new PGNSortByEvent());
    Collections.sort(partijen);
    int aantalPartijen  = partijen.size() / maxBestanden + 1;
    if (aantalPartijen < minPartijen) {
      aantalPartijen  = minPartijen;
    }
    int gameFile        = 0;

    File    gamedataFile  = null;
    File    headersFile   = new File(uitvoerdir + "/headers.xml");
    File    updatesFile   = new File(uitvoerdir + "/updates.xml");

    try {
      // Maak de headers.xml file
      headers   = new BufferedWriter(
                  new OutputStreamWriter(
                   new FileOutputStream(headersFile), charsetUit));
      headers.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
      headers.newLine();
      headers.write("<chessgames gamesperfile=\"" + aantalPartijen
                    + "\"  pgnfile=\"" + zip + ".zip\">");
      headers.newLine();
      // Maak de updates.xml file
      updates   = new BufferedWriter(
                  new OutputStreamWriter(
                   new FileOutputStream(updatesFile), charsetUit));
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
          gamedataFile  = new File(uitvoerdir + "/gamedata" + gameFile
                                   + ".xml");
          gamedata      = new BufferedWriter(
                          new OutputStreamWriter(
                              new FileOutputStream(gamedataFile), charsetUit));
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
            System.out.println("Error in " + partij.getTagsAsString());
            System.out.println(e.getMessage());
          } catch (ZetException e) {
            System.out.println("Error in " + partij.getTagsAsString());
            System.out.println(e.getMessage());
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
      System.out.println(e.getLocalizedMessage());
    } catch (FileNotFoundException e) {
      System.out.println(e.getLocalizedMessage());
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
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
      } catch (IOException ex) {
        System.out.println(ex.getLocalizedMessage());
      }
    }
    System.out.println("Partijen : " + partijen.size());
    System.out.println("Bestanden: " + gameFile);
    System.out.println("Uitvoer  : " + uitvoerdir);
    System.out.println("Klaar.");
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    System.out.println("java -jar CaissaTools.jar ChessTheatre [OPTIE...] \\");
    System.out.println("  --bestand=<PGN bestand>");
    System.out.println();
    System.out.println("  --bestand      Het bestand met de partijen in PGN formaat.");
    System.out.println("  --charsetin    De characterset van <bestand> als deze niet "
                       + Charset.defaultCharset().name() + " is.");
    System.out.println("  --charsetuit   De characterset van de uitvoer als deze niet "
                       + Charset.defaultCharset().name() + " moet zijn.");
    System.out.println("  --maxBestanden Maximum aantal bestanden voor de partijen.");
    System.out.println("                 Default waarde is 50.");
    System.out.println("  --minPartijen  Minimum aantal partijen per bestand.");
    System.out.println("                 Default waarde is totaal aantal partijen");
    System.out.println("                 gedeeld door maxBestanden.");
    System.out.println("  --uitvoerdir   Directory waar de uitvoer bestanden moeten staan.");
    System.out.println("  --zip          Naam (met eventuele directory) van de zip file.");
    System.out.println("                 Deze zip file wordt niet door dit programma");
    System.out.println("                 gemaakt.");
    System.out.println();
    System.out.println("Enkel bestand is verplicht.");
    System.out.println();
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
