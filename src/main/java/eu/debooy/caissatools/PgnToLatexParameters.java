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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public class PgnToLatexParameters implements IParameterBundleValidator {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  @Override
  public List<String> valideer(Map<String, Parameter> params,
                               List<String> argumenten) {
    List<String>  fouten  = new ArrayList<>();

    if (!argumenten.contains(CaissaTools.PAR_DATUM)) {
      params.get(CaissaTools.PAR_DATUM).setWaarde(new Date());
    }

    if (!argumenten.contains(CaissaTools.PAR_BESTAND)) {
      return fouten;
    }

    if (DoosUtils.nullToEmpty(params.get(CaissaTools.PAR_BESTAND)
                                    .getWaarde().toString())
                 .split(";").length !=
        DoosUtils.nullToEmpty(params.get(CaissaTools.PAR_SCHEMA)
                                    .getWaarde().toString())
                 .split(";").length) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BEST_ONGELIJK));
    }

    if (DoosUtils.nullToEmpty(params.get(CaissaTools.PAR_BESTAND)
                                    .getWaarde().toString()).contains(";")
        && (!argumenten.contains(CaissaTools.PAR_AUTEUR)
          || !argumenten.contains(CaissaTools.PAR_TITEL))) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BIJBESTAND));
    }

    return fouten;
  }
}
