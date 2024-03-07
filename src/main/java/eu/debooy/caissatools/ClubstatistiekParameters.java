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

import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.IParameterBundleValidator;
import eu.debooy.doosutils.Parameter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public class ClubstatistiekParameters implements IParameterBundleValidator {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  @Override
  public List<String> valideer(Map<String, Parameter> params,
                               List<String> argumenten) {
    var validator = new BestandDefaultParameters();
    var fouten    = validator.valideer(params, argumenten);

    valideerBestand(params, argumenten, fouten);
    valideerToernooitype(params, fouten);

    return fouten;
  }

  private void valideerBestand(Map<String, Parameter> params,
                               List<String> argumenten, List<String> fouten) {
    if (!argumenten.contains(CaissaTools.PAR_BESTAND)) {
      return;
    }

    if (argumenten.contains(CaissaTools.PAR_VOORRONDE)
        && params.get(CaissaTools.PAR_BESTAND)
                 .getWaarde().toString().split(";").length == 1) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_METVOORRONDE));
      return;
    }

    if (argumenten.contains(CaissaTools.PAR_VOORRONDE)
        && params.get(CaissaTools.PAR_VOORRONDE)
                 .getWaarde().toString().split(";").length == 1) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_METVOORRONDE));
    }
  }

  private void valideerToernooitype(Map<String, Parameter> params, List<String> fouten) {
    var toernooitype  = ((Long) params.get(CaissaTools.PAR_TOERNOOITYPE)
                                      .getWaarde()).intValue();

    if (toernooitype < 1
        || toernooitype > 2) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_TOERNOOITYPE));
    }
  }
}
