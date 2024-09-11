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

import eu.debooy.caissa.Spelerinfo;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.LatexConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public class Ledenlijst extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static final  String  DEF_TEMPLATE    = "Ledenlijst.tex";
  private static final  String  JSON_TAG_LEDEN  = "leden";
  private static final  String  JSON_TAG_NAAM   = "naam";

  private static  Map<String, String> params;

  protected Ledenlijst() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_LEDENLIJST)
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    try (var invoer =
            new JsonBestand.Builder()
                           .setBestand(
                               paramBundle
                           .getBestand(CaissaTools.PAR_BESTAND))
                           .build();
         var output =
            new TekstBestand.Builder()
                            .setLezen(false)
                            .setBestand(
                                paramBundle
                                    .getBestand(CaissaTools.PAR_UITVOER))
                            .setCharset(BestandConstants.UTF8)
                            .build();
         var texInvoer  =
             CaissaTools.getTemplate(paramBundle, DEF_TEMPLATE,
                                     Ledenlijst.class.getClassLoader())) {
      var ledenlijst  = (JSONObject) invoer.read();
      vulParams(ledenlijst.get(JSON_TAG_NAAM).toString());
      var status  = CaissaTools.KYW_NORMAAL;
      while (texInvoer.hasNext()) {
        var regel = texInvoer.next();
        if (regel.equals("%@Include Leden")) {
          schrijfLedenlijst(ledenlijst, output);
        } else {
          status  = schrijfUitTemplate(output, regel, params, status);
        }
      }
    } catch (Exception e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static String formatEmail(String email) {
    if (DoosUtils.isBlankOrNull(email)) {
      return "";
    }

    return email.replace("_", "\\_");
  }

  private static void schrijfLedenlijst(JSONObject ledenlijst,
                                        TekstBestand output)
      throws BestandException {
    var spelers = vulSpelers(ledenlijst);

    Collections.sort(spelers, new Spelerinfo.ByNaamComparator());

    for (var speler : spelers) {
      output.write(String.format("   %s & %s & %s & %s & %s %s",
                                 speler.getVolledigenaam(),
                                 speler.getAdres(),
                                 speler.getPlaats(),
                                 speler.getTelefoon(),
                                 formatEmail(speler.getEmail()),
                                 LatexConstants.LTX_EOL));
    }
  }

  private static String schrijfUitTemplate(TekstBestand output,
                                           String regel,
                                           Map<String, String> params,
                                           String status)
      throws BestandException {
    if (regel.equals("%@IncludeEind Logo")) {
      return CaissaTools.KYW_NORMAAL;
    }
    if (regel.equals("%@IncludeStart Logo")) {
      return CaissaTools.KYW_LOGO;
    }

    if (status.equals(CaissaTools.KYW_LOGO)) {
      if (paramBundle.containsParameter(CaissaTools.PAR_LOGO)) {
        output.write(CaissaTools.replaceParameters(regel, params));
      }
    } else {
      output.write(CaissaTools.replaceParameters(regel, params));
    }

    return status;
  }

  private static void vulParams(String naam) {
    params  = new HashMap<>();
    params.put(CaissaTools.PAR_KLEUR,
               paramBundle.getString(CaissaTools.PAR_KLEUR));
    if (paramBundle.containsArgument(CaissaTools.PAR_LOGO)) {
      params.put(CaissaTools.PAR_LOGO,
                 paramBundle.getString(CaissaTools.PAR_LOGO));
    }
    params.put(CaissaTools.PAR_TEKSTKLEUR,
               paramBundle.getString(CaissaTools.PAR_TEKSTKLEUR));
    params.put(CaissaTools.PAR_TITEL,
               DoosUtils.nullToValue(
                  paramBundle.getString(CaissaTools.PAR_TITEL),
                  naam));
    params.put("adres", resourceBundle.getString(CaissaTools.LBL_ADRES));
    params.put("email", resourceBundle.getString(CaissaTools.LBL_HEAD_EMAIL));
    params.put("ledenlijst",
               resourceBundle.getString(CaissaTools.LBL_LEDENLIJST));
    params.put("naam", resourceBundle.getString(CaissaTools.LBL_NAAM));
    params.put("plaats", resourceBundle.getString(CaissaTools.LBL_PLAATS));
    params.put("telefoon", resourceBundle.getString(CaissaTools.LBL_TELEFOON));
  }

  private static ArrayList<Spelerinfo> vulSpelers(JSONObject ledenlijst) {
    var spelers     = new ArrayList<Spelerinfo>();

    if (!ledenlijst.containsKey(JSON_TAG_LEDEN)) {
      return spelers;
    }
    var spelerId  = 1;
    for (var jSpeler : (JSONArray)  ledenlijst.get(JSON_TAG_LEDEN)) {
      var type  = ((JSONObject) jSpeler).get("type").toString();
      if (type.equals("lid")
          || type.equals("speler")) {
        var speler  = new Spelerinfo((JSONObject) jSpeler);

        if (null == speler.getSpelerId()) {
          speler.setSpelerId(spelerId);
        }
        if (null == speler.getSpelerSeq()) {
          speler.setSpelerSeq(spelerId);
        }
        spelers.add(speler);
        spelerId++;
      }
    }

    return spelers;
  }
}
