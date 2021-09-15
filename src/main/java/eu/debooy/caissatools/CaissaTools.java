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

import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosUtils;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class CaissaTools {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  public static final String  ERR_BESTANDENPGN    = "error.bestand.en.pgn";
  public static final String  ERR_BEVATDIRECTORY  = "error.bevatdirectory";
  public static final String  ERR_BIJBESTAND      = "error.verplichtbijbestand";
  public static final String  ERR_EINDVOORSTART   = "error.eind.voor.start";
  public static final String  ERR_FOUTEDATUM      = "error.foutedatum";
  public static final String  ERR_FOUTEDATUMIN    = "error.foutedatumin";
  public static final String  ERR_GEENINVOER      = "error.geen.invoer";
  public static final String  ERR_HALVE           = "error.halve.verboden";
  public static final String  ERR_KALENDER        = "error.kalender";

  public static final String  ERR_MAAKNIEUWBESTAND  = "error.maaknieuwbestand";
  public static final String  ERR_MAXVERSCHIL       = "error.maxverschil";
  public static final String  ERR_TALENGELIJK       = "error.talen.gelijk";
  public static final String  ERR_TEMPLATE          = "error.template";
  public static final String  ERR_TSEMAIL           = "error.tsemail";

  public static final String  HLP_BESTAND       = "help.bestand";
  public static final String  HLP_MATRIXOPSTAND = "help.matrixopstand";
  public static final String  HLP_SCHEMA        = "help.competitieschema";

  public static final String  LBL_BESTAND     = "label.bestand";
  public static final String  LBL_PGNBESTAND  = "label.pgnbestand";
  public static final String  LBL_SCHEMA      = "label.competitieschema";

  public static final String  MSG_NIEUWBESTAND  = "message.nieuwbestand";
  public static final String  MSG_STARTTOERNOOI = "message.starttoernooi";

  public static final String  PAR_AUTEUR              = "auteur";
  public static final String  PAR_BERICHT             = "bericht";
  public static final String  PAR_BESTAND             = "bestand";
  public static final String  PAR_CHARSETIN           = "charsetin";
  public static final String  PAR_CHARSETUIT          = "charsetuit";
  public static final String  PAR_DATE                = "date";
  public static final String  PAR_DATUM               = "datum";
  public static final String  PAR_DEFAULTECO          = "defaulteco";
  public static final String  PAR_EINDDATUM           = "eindDatum";
  public static final String  PAR_ENKEL               = "enkel";
  public static final String  PAR_ENKELRONDIG         = "enkelrondig";
  public static final String  PAR_ENKELZETTEN         = "enkelzetten";
  public static final String  PAR_EVENT               = "event";
  public static final String  PAR_EXTRAINFO           = "extraInfo";
  public static final String  PAR_GESCHIEDENISBESTAND = "geschiedenisBestand";
  public static final String  PAR_HALVE               = "halve";
  public static final String  PAR_INCLUDELEGE         = "includelege";
  public static final String  PAR_JSON                = "json";
  public static final String  PAR_KEYWORDS            = "keywords";
  public static final String  PAR_LOGO                = "logo";
  public static final String  PAR_MATRIX              = "matrix";
  public static final String  PAR_MATRIXOPSTAND       = "matrixopstand";
  public static final String  PAR_MAXBESTANDEN        = "maxBestanden";
  public static final String  PAR_MAXVERSCHIL         = "maxVerschil";
  public static final String  PAR_METFEN              = "metFEN";
  public static final String  PAR_METPGNVIEWER        = "metPgnviewer";
  public static final String  PAR_METTRAJECTEN        = "metTrajecten";
  public static final String  PAR_MINPARTIJEN         = "minPartijen";
  public static final String  PAR_NAARTAAL            = "naartaal";
  public static final String  PAR_NIEUWESPELERS       = "nieuweSpelers";
  public static final String  PAR_PERPARTIJ           = "perPartij";
  public static final String  PAR_PGN                 = "pgn";
  public static final String  PAR_SCHEMA              = "schema";
  public static final String  PAR_SITE                = "site";
  public static final String  PAR_SMTPPOORT           = "smtppoort";
  public static final String  PAR_SMTPSERVER          = "smtpserver";
  public static final String  PAR_SPELER              = "speler";
  public static final String  PAR_SPELERBESTAND       = "spelerBestand";
  public static final String  PAR_SPELERS             = "spelers";
  public static final String  PAR_STARTDATUM          = "startDatum";
  public static final String  PAR_STARTELO            = "startELO";
  public static final String  PAR_TAG                 = "tag";
  public static final String  PAR_TEMPLATE            = "template";
  public static final String  PAR_TITEL               = "titel";
  public static final String  PAR_TOERNOOIBESTAND     = "toernooiBestand";
  public static final String  PAR_TSEMAIL             = "tsemail";
  public static final String  PAR_UITVOER             = "uitvoer";
  public static final String  PAR_VANTAAL             = "vantaal";
  public static final String  PAR_VASTEKFACTOR        = "vasteKfactor";
  public static final String  PAR_VOORNICO            = "voorNico";
  public static final String  PAR_ZIP                 = "zip";

  public static final String  TXT_BANNER  = "Caissa Tools";

  public static final String  XML_HEADING =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

  private CaissaTools() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      Banner.printMarcoBanner(TXT_BANNER);
      help();
      return;
    }

    String    commando      = args[0];

    String[]  commandoArgs  = new String[args.length-1];
    System.arraycopy(args, 1, commandoArgs, 0, args.length-1);

    switch (commando.toLowerCase()) {
      case "chesstheatre":
        ChessTheatre.execute(commandoArgs);
        break;
      case "eloberekenaar":
        ELOBerekenaar.execute(commandoArgs);
        break;
      case "pgncleaner":
        PgnCleaner.execute(commandoArgs);
        break;
      case "pgntohtml":
        PgnToHtml.execute(commandoArgs);
        break;
      case "pgntojson":
        PgnToJson.execute(commandoArgs);
        break;
      case "pgntolatex":
        PgnToLatex.execute(commandoArgs);
        break;
      case "spelerstatistiek":
        SpelerStatistiek.execute(commandoArgs);
        break;
      case "startcorrespondentie":
        StartCorrespondentie.execute(commandoArgs);
        break;
      case "startpgn":
        StartPgn.execute(commandoArgs);
        break;
      case "vertaalpgn":
        VertaalPgn.execute(commandoArgs);
        break;
      default:
        Banner.printMarcoBanner(TXT_BANNER);
        help();
        break;
    }
  }

  private static void help() {
    DoosUtils.naarScherm("  ChessTheatre         ",
                         resourceBundle.getString("help.chesstheatre"), 80);
    DoosUtils.naarScherm("  ELOBerekenaar        ",
                         resourceBundle.getString("help.eloberekenaar"), 80);
    DoosUtils.naarScherm("  PgnCleaner           ",
                         resourceBundle.getString("help.pgncleaner"), 80);
    DoosUtils.naarScherm("  PgnToHtml            ",
                         resourceBundle.getString("help.pgntohtml"), 80);
    DoosUtils.naarScherm("  PgnToJson            ",
                         resourceBundle.getString("help.pgntojson"), 80);
    DoosUtils.naarScherm("  PgnToLatex           ",
                         resourceBundle.getString("help.pgntolatex"), 80);
    DoosUtils.naarScherm("  StartCorrespondentie ",
                         resourceBundle.getString("help.startcorrespondentie"),
                         80);
    DoosUtils.naarScherm("  StartPgn             ",
                         resourceBundle.getString("help.startpgn"), 80);
    DoosUtils.naarScherm("  SpelerStatistiek     ",
                         resourceBundle.getString("help.spelerstatistiek"), 80);
    DoosUtils.naarScherm("  VertaalPgn           ",
                         resourceBundle.getString("help.vertaalpgn"), 80);
    DoosUtils.naarScherm("");
    ChessTheatre.help();
    DoosUtils.naarScherm("");
    ELOBerekenaar.help();
    DoosUtils.naarScherm("");
    PgnCleaner.help();
    DoosUtils.naarScherm("");
    PgnToHtml.help();
    DoosUtils.naarScherm("");
    PgnToJson.help();
    DoosUtils.naarScherm("");
    PgnToLatex.help();
    DoosUtils.naarScherm("");
    StartCorrespondentie.help();
    DoosUtils.naarScherm("");
    StartPgn.help();
    DoosUtils.naarScherm("");
    SpelerStatistiek.help();
    DoosUtils.naarScherm("");
    VertaalPgn.help();
  }
}
