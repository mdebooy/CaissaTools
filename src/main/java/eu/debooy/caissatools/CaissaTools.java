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
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public final class CaissaTools extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  public static final String  ERR_BEST_ONGELIJK = "error.aantal.bestanden";
  public static final String  ERR_BESTANDENPGN  = "error.bestand.en.pgn";
  public static final String  ERR_BIJBESTAND    = "error.verplichtbijbestand";
  public static final String  ERR_EINDVOORSTART = "error.eind.voor.start";
  public static final String  ERR_FOUTEDATUM    = "error.foutedatum";
  public static final String  ERR_FOUTEDATUMIN  = "error.foutedatumin";
  public static final String  ERR_GEENINVOER    = "error.geen.invoer";
  public static final String  ERR_HALVE         = "error.halve.verboden";
  public static final String  ERR_KALENDER      = "error.kalender";

  public static final String  ERR_MAAKNIEUWBESTAND  = "error.maaknieuwbestand";
  public static final String  ERR_MAXVERSCHIL       = "error.maxverschil";
  public static final String  ERR_TALENGELIJK       = "error.talen.gelijk";
  public static final String  ERR_TEMPLATE          = "error.template";
  public static final String  ERR_TSEMAIL           = "error.tsemail";

  public static final String  HLP_BESTAND       = "help.bestand";
  public static final String  HLP_MATRIXOPSTAND = "help.matrixopstand";
  public static final String  HLP_SCHEMA        = "help.competitieschema";
  public static final String  HLP_SUBTITEL      = "help.subtitel";

  public static final String  LBL_BESTAND       = "label.bestand";
  public static final String  LBL_EMAIL         = "label.email";
  public static final String  LBL_JIJ           = "label.jij";
  public static final String  LBL_PARTIJEN      = "label.partijen";
  public static final String  LBL_PGNBESTAND    = "label.pgnbestand";
  public static final String  LBL_SCHEMA        = "label.competitieschema";
  public static final String  LBL_UITVOER       = "label.uitvoer";

  public static final String  MSG_NIEUWBESTAND  = "message.nieuwbestand";
  public static final String  MSG_NIEUWESPELER  = "message.nieuwespeler";
  public static final String  MSG_NIEUWESPELERS = "message.nieuwespelers";
  public static final String  MSG_STARTTOERNOOI = "message.starttoernooi";

  public static final String  PAR_AUTEUR              = "auteur";
  public static final String  PAR_BERICHT             = "bericht";
  public static final String  PAR_BESTAND             = "bestand";
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
  public static final String  PAR_MATRIXEERST         = "matrixeerst";
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
  public static final String  PAR_SCHAAKNOTATIE       = "schaaknotatie.csv";
  public static final String  PAR_SCHEMA              = "schema";
  public static final String  PAR_SITE                = "site";
  public static final String  PAR_SMTPPOORT           = "smtppoort";
  public static final String  PAR_SMTPSERVER          = "smtpserver";
  public static final String  PAR_SPELER              = "speler";
  public static final String  PAR_SPELERBESTAND       = "spelerBestand";
  public static final String  PAR_SPELERS             = "spelers";
  public static final String  PAR_STARTDATUM          = "startDatum";
  public static final String  PAR_STARTELO            = "startELO";
  public static final String  PAR_SUBTITEL            = "subtitel";
  public static final String  PAR_TAG                 = "tag";
  public static final String  PAR_TEMPLATE            = "template";
  public static final String  PAR_TITEL               = "titel";
  public static final String  PAR_TOERNOOIBESTAND     = "toernooiBestand";
  public static final String  PAR_TOERNOOITYPE        = "toernooitype";
  public static final String  PAR_TYPE                = "type";
  public static final String  PAR_TSEMAIL             = "tsemail";
  public static final String  PAR_UITVOER             = "uitvoer";
  public static final String  PAR_VANTAAL             = "vantaal";
  public static final String  PAR_VASTEKFACTOR        = "vasteKfactor";
  public static final String  PAR_VOORNICO            = "voorNico";
  public static final String  PAR_ZIP                 = "zip";

  protected static final  String  TOOL_ANALYSETEX         = "AnalyseToLatex";
  protected static final  String  TOOL_CHESSTHEATRE       = "ChessTheatre";
  protected static final  String  TOOL_ELOBEREKENAAR      = "ELOBerekenaar";
  protected static final  String  TOOL_PGNCLEANER         = "PgnCleaner";
  protected static final  String  TOOL_PGNTOHTML          = "PgnToHtml";
  protected static final  String  TOOL_PGNTOJSON          = "PgnToJson";
  protected static final  String  TOOL_PGNTOLATEX         = "PgnToLatex";
  protected static final  String  TOOL_SPELERSTATISTIEK   = "SpelerStatistiek";
  protected static final  String  TOOL_STARTCORRESP       =
      "StartCorrespondentie";
  protected static final  String  TOOL_STARTPGN           = "StartPgn";
  protected static final  String  TOOL_TOERNOOIOVERZICHT  = "Toernooioverzicht";
  protected static final  String  TOOL_VERTAALPGN         = "VertaalPgn";

  protected static final  List<String>  tools =
      Arrays.asList(TOOL_ANALYSETEX, TOOL_CHESSTHEATRE, TOOL_ELOBEREKENAAR,
                    TOOL_PGNCLEANER, TOOL_PGNTOHTML, TOOL_PGNTOJSON,
                    TOOL_PGNTOLATEX, TOOL_SPELERSTATISTIEK, TOOL_STARTCORRESP,
                    TOOL_STARTPGN, TOOL_TOERNOOIOVERZICHT, TOOL_VERTAALPGN);

  public static final String  TXT_BANNER  = "Caissa Tools";

  public static final String  XML_HEADING =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

  protected CaissaTools() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      Banner.printMarcoBanner(TXT_BANNER);
      help();
      return;
    }

    var commando      = args[0];
    var commandoArgs  = new String[args.length-1];
    System.arraycopy(args, 1, commandoArgs, 0, args.length-1);

    switch (commando.toLowerCase()) {
      case "analysetolatex":
        AnalyseToLatex.execute(commandoArgs);
        break;
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
      case "toernooioverzicht":
        Toernooioverzicht.execute(commandoArgs);
        break;
      case "vertaalpgn":
        VertaalPgn.execute(commandoArgs);
        break;
      default:
        Banner.printMarcoBanner(TXT_BANNER);
        help();
        DoosUtils.foutNaarScherm(
            MessageFormat.format(getMelding(ERR_TOOLONBEKEND), commando));
        DoosUtils.naarScherm();
        break;
    }
  }

  public static void help() {
    tools.forEach(tool -> {
      var parameterBundle = new ParameterBundle.Builder()
                           .setBaseName(tool)
                           .build();
      DoosUtils.naarScherm(parameterBundle.getApplicatie());
      DoosUtils.naarScherm();
      parameterBundle.help();
      DoosUtils.naarScherm(DoosUtils.stringMetLengte("_", 80, "_"));
      DoosUtils.naarScherm();
    });
  }
}
