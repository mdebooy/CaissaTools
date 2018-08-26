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
    List<String>    fouten      = new ArrayList<String>();
    BufferedWriter  output      = null;

    Banner.printBanner(resourceBundle.getString("banner.pgntojson"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.JSON,
                                          CaissaTools.PGNVIEWER,
                                          CaissaTools.UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand       = arguments.getArgument(CaissaTools.BESTAND);
    if (bestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                       CaissaTools.BESTAND));
    }
    if (bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand = bestand.substring(0, bestand.length() - 4);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn   = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit  = arguments.getArgument(CaissaTools.CHARDSETUIT);
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument(CaissaTools.INVOERDIR)) {
      invoerdir   = arguments.getArgument(CaissaTools.INVOERDIR);
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    String    jsonBestand = "";
    if (arguments.hasArgument(CaissaTools.JSON)) {
      jsonBestand = arguments.getArgument(CaissaTools.JSON);
    }
    if (DoosUtils.isBlankOrNull(jsonBestand)) {
      jsonBestand = bestand;
    }
    if (!jsonBestand.endsWith(CaissaTools.EXTENSIE_JSON)) {
      jsonBestand = jsonBestand + CaissaTools.EXTENSIE_JSON;
    }
    boolean   pgnviewer   = false;
    if (arguments.hasArgument(CaissaTools.PGNVIEWER)) {
      pgnviewer   =
          DoosConstants.WAAR
              .equalsIgnoreCase(arguments.getArgument(CaissaTools.PGNVIEWER));
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    ObjectMapper    mapper    = new ObjectMapper();
    List<Map<String, String>>
                    lijst     = new ArrayList<Map<String, String>>();
    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand
                                     + CaissaTools.EXTENSIE_PGN,
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
        if (pgnviewer
            && DoosUtils.isNotBlankOrNull(partij.getZuivereZetten())) {
          obj.put("_pgnviewer",
                  CaissaUtils
                      .pgnZettenToChessTheatre(partij.getZuivereZetten()));
        }
        lijst.add(obj);
        partijnr++;
      }

      File  jsonFile  = new File(uitvoerdir + File.separator + jsonBestand);
      output  = Bestand.openUitvoerBestand(jsonFile, charsetUit);
      mapper.writeValue(output, lijst);
    } catch (BestandException | FenException | FileNotFoundException
             | IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
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
                         + invoerdir + File.separator + bestand
                         + CaissaTools.EXTENSIE_PGN);
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
                             CaissaTools.BESTAND), 80);
    DoosUtils.naarScherm();
  }
}
