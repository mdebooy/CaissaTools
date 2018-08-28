/**
 * Copyright 2009 Marco de Booij
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
package eu.debooy.caissatools;

import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosUtils;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class CaissaTools {
  public static final String  AUTEUR                = "auteur";
  public static final String  BESTAND               = "bestand";
  public static final String  CHARDSETIN            = "charsetin";
  public static final String  CHARDSETUIT           = "charsetuit";
  public static final String  DATE                  = "date";
  public static final String  DATUM                 = "datum";
  public static final String  EVENT                 = "event";
  public static final String  EINDDATUM             = "eindDatum";
  public static final String  ENKEL                 = "enkel";
  public static final String  EXTRAINFO             = "extraInfo";
  public static final String  GESCHIEDENISBESTAND   = "geschiedenisBestand";
  public static final String  HALVE                 = "halve";
  public static final String  INVOERDIR             = "invoerdir";
  public static final String  JSON                  = "json";
  public static final String  KEYWORDS              = "keywords";
  public static final String  LOGO                  = "logo";
  public static final String  MATRIX                = "matrix";
  public static final String  MATRIXOPSTAND         = "matrixopstand";
  public static final String  MAXBESTANDEN          = "maxBestanden";
  public static final String  MAXVERSCHIL           = "maxVerschil";
  public static final String  MINPARTIJEN           = "minPartijen";
  public static final String  NAARTAAL              = "naartaal";
  public static final String  PGN                   = "pgn";
  public static final String  PGNVIEWER             = "pgnviewer";
  public static final String  SITE                  = "site";
  public static final String  SPELERBESTAND         = "spelerBestand";
  public static final String  SPELER                = "speler";
  public static final String  SPELERS               = "spelers";
  public static final String  STARTDATUM            = "startDatum";
  public static final String  STARTELO              = "startELO";
  public static final String  TAG                   = "tag";
  public static final String  TEMPLATE              = "template";
  public static final String  TITEL                 = "titel";
  public static final String  TOERNOOIBESTAND       = "toernooiBestand";
  public static final String  UITVOERDIR            = "uitvoerdir";
  public static final String  VANTAAL               = "vantaal";
  public static final String  VASTEKFACTOR          = "vasteKfactor";
  public static final String  ZIP                   = "zip";

  public static final String  ERR_BESTANDENPGN      = "error.bestand.en.pgn";
  public static final String  ERR_BEVATDIRECTORY    = "error.bevatdirectory";
  public static final String  ERR_BIJBESTAND        = "error.verplichtbijbestand";
  public static final String  ERR_EINDVOORSTART     = "error.eind.voor.start";
  public static final String  ERR_FOUTEDATUM        = "error.foutedatum";
  public static final String  ERR_FOUTEDATUMIN      = "error.foutedatumin";
  public static final String  ERR_GEENINVOER        = "error.geen.invoer";
  public static final String  ERR_HALVE             = "error.halve.verboden";
  public static final String  ERR_MAAKNIEUWBESTAND  = "error.maaknieuwbestand";
  public static final String  ERR_MAXVERSCHIL       = "error.maxverschil";
  public static final String  ERR_TALENGELIJK       = "error.talen.gelijk";
  public static final String  ERR_TEMPLATE          = "error.template";

  public static final String  EXTENSIE_CSV          = ".csv";
  public static final String  EXTENSIE_JSON         = ".json";
  public static final String  EXTENSIE_PGN          = ".pgn";
  public static final String  EXTENSIE_TEX          = ".tex";
  public static final String  EXTENSIE_ZIP          = ".zip";

  public static final String  MSG_NIEUWBESTAND      = "message.nieuwbestand";

  public static final String  XML_HEADING           =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private CaissaTools() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      Banner.printBanner("Caissa Tools");
      help();
      return;
    }

    String    commando      = args[0];

    String[]  commandoArgs  = new String[args.length-1];
    System.arraycopy(args, 1, commandoArgs, 0, args.length-1);

    if ("chesstheatre".equalsIgnoreCase(commando)) {
      try {
        ChessTheatre.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("eloberekenaar".equalsIgnoreCase(commando)) {
      try {
        ELOBerekenaar.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("pgntohtml".equalsIgnoreCase(commando)) {
      try {
        PgnToHtml.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("pgntojson".equalsIgnoreCase(commando)) {
      try {
        PgnToJson.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("pgntolatex".equalsIgnoreCase(commando)) {
      try {
        PgnToLatex.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("startpgn".equalsIgnoreCase(commando)) {
      StartPgn.execute(commandoArgs);
      return;
    }
    if ("spelerstatistiek".equalsIgnoreCase(commando)) {
      try {
        SpelerStatistiek.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }
    if ("vertaalpgn".equalsIgnoreCase(commando)) {
      try {
        VertaalPgn.execute(commandoArgs);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getMessage());
      }
      return;
    }

    Banner.printBanner("Caissa Tools");
    help();
  }

  /**
   * Geeft de 'help' pagina.
   */
  private static void help() {
    DoosUtils.naarScherm("  ChessTheatre      ",
                         resourceBundle.getString("help.chesstheatre"), 80);
    DoosUtils.naarScherm("  ELOBerekenaar     ",
                         resourceBundle.getString("help.eloberekenaar"), 80);
    DoosUtils.naarScherm("  PgnToHtml         ",
                         resourceBundle.getString("help.pgntohtml"), 80);
    DoosUtils.naarScherm("  PgnToJson         ",
                         resourceBundle.getString("help.pgntojson"), 80);
    DoosUtils.naarScherm("  PgnToLatex        ",
                         resourceBundle.getString("help.pgntolatex"), 80);
    DoosUtils.naarScherm("  StartPgn          ",
                         resourceBundle.getString("help.startpgn"), 80);
    DoosUtils.naarScherm("  SpelerStatistiek  ",
                         resourceBundle.getString("help.spelerstatistiek"), 80);
    DoosUtils.naarScherm("  VertaalPgn        ",
                         resourceBundle.getString("help.vertaalpgn"), 80);
    DoosUtils.naarScherm("");
    ChessTheatre.help();
    DoosUtils.naarScherm("");
    ELOBerekenaar.help();
    DoosUtils.naarScherm("");
    PgnToHtml.help();
    DoosUtils.naarScherm("");
    PgnToJson.help();
    DoosUtils.naarScherm("");
    PgnToLatex.help();
    DoosUtils.naarScherm("");
    StartPgn.help();
    DoosUtils.naarScherm("");
    SpelerStatistiek.help();
    DoosUtils.naarScherm("");
    VertaalPgn.help();
  }
}
