/*
 * Copyright (c) 2021 Marco de Booij
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

import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.IParameterBundleValidator;
import eu.debooy.doosutils.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public class BestandDefaultParameters implements IParameterBundleValidator {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  @Override
  public List<String> valideer(Map<String, Parameter> params,
                               List<String> argumenten) {
    List<String>  fouten  = new ArrayList<>();

    if (params.containsKey(CaissaTools.PAR_JSON)) {
      valideerMetBestand(CaissaTools.PAR_JSON, params, argumenten, fouten);
    }
    if (params.containsKey(CaissaTools.PAR_SCHEMA)) {
      valideerMetBestand(CaissaTools.PAR_SCHEMA, params, argumenten, fouten);
    }
    if (params.containsKey(CaissaTools.PAR_UITVOER)) {
      valideerMetBestand(CaissaTools.PAR_UITVOER, params, argumenten, fouten);
    }

    return fouten;
  }

  private void valideerMetBestand(String param,
                                  Map<String, Parameter> params,
                                  List<String> argumenten,
                                  List<String> fouten) {
    if (!argumenten.contains(CaissaTools.PAR_BESTAND)) {
      return;
    }

    if (!argumenten.contains(param)) {
      params.get(param).setWaarde(params.get(CaissaTools.PAR_BESTAND)
                                        .getWaarde());
    }

    if (DoosUtils.nullToEmpty(params.get(CaissaTools.PAR_BESTAND)
                                    .getWaarde().toString())
                 .split(";").length !=
        DoosUtils.nullToEmpty(params.get(param).getWaarde().toString())
                 .split(";").length) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BEST_ONGELIJK));
    }
  }
}
