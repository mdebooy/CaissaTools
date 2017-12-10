/**
 * Copyright 2017 Marco de Booij
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

import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.errorhandling.exception.FileNotFoundException;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.codehaus.jackson.map.ObjectMapper;


/**
 * @author Marco de Booij
 */
public final class PgnToJson {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private PgnToJson() {}

  public static void execute(String[] args) throws PgnException {
    String          charsetIn   = Charset.defaultCharset().name();
    String          charsetUit  = Charset.defaultCharset().name();
    BufferedWriter  output      = null;

    Banner.printBanner(resourceBundle.getString("banner.pgntojson"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"bestand", "charsetin", "charsetuit",
                                          "invoerdir", "json", "pgnviewer",
                                          "uitvoerdir"});
    arguments.setVerplicht(new String[] {"bestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand   = arguments.getArgument("bestand");
    if (!bestand.endsWith(".pgn")) {
      bestand     = bestand + ".pgn";
    }
    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument("invoerdir")) {
      invoerdir   = arguments.getArgument("invoerdir");
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    jsonBestand = "";
    if (arguments.hasArgument("json")) {
      jsonBestand = arguments.getArgument("json");
    }
    if (DoosUtils.isBlankOrNull(jsonBestand)) {
      jsonBestand = bestand.substring(0, bestand.length() - 4);
    }
    if (!jsonBestand.endsWith(".json")) {
      jsonBestand = jsonBestand + ".json";
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument("uitvoerdir")) {
      uitvoerdir  = arguments.getArgument("uitvoerdir");
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    boolean   pgnviewer   = false;
    if (arguments.hasArgument("pgnviewer")) {
      pgnviewer   =
          DoosConstants.WAAR
                       .equalsIgnoreCase(arguments.getArgument("pgnviewer"));
    }

    ObjectMapper    mapper    = new ObjectMapper();
    List<Map<String, String>>
                    lijst     = new ArrayList<Map<String, String>>();
    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand,
                                     charsetIn);
    try {
      int partijnr  = 1;
      for (PGN partij: partijen) {
        Map<String, String> obj = new LinkedHashMap<String, String>();
        obj.put("_gamekey", "" + partijnr);
        for (Map.Entry<String, String> tag : partij.getTags().entrySet()) {
          obj.put(tag.getKey(), tag.getValue());
        }
        obj.put("_moves", partij.getZetten());
        if (pgnviewer) {
          obj.put("_pgnviewer",
                  CaissaUtils.pgnZettenToChessTheatre(partij.getZetten()));
        }
        lijst.add(obj);
        partijnr++;
      }

      File  jsonFile  = new File(uitvoerdir + File.separator + jsonBestand);
      output  = Bestand.openUitvoerBestand(jsonFile, charsetUit);
      mapper.writeValue(output, lijst);
    } catch (BestandException | FenException | FileNotFoundException
             | IOException e) {
      System.err.println(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + invoerdir + File.separator + bestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir + File.separator + jsonBestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToJson ["
                         + resourceBundle.getString("label.optie")
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand    ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin  ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --invoerdir  ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --json       ",
                         resourceBundle.getString("help.json"), 80);
    DoosUtils.naarScherm("  --pgnviewer  ",
                         resourceBundle.getString("help.pgnviewer"), 80);
    DoosUtils.naarScherm("  --uitvoerdir ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             "bestand"), 80);
    DoosUtils.naarScherm();
  }
}
