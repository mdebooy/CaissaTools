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
import eu.debooy.doosutils.IParameterBundleValidator;
import eu.debooy.doosutils.Parameter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * @author Marco de Booij
 */
public class VertaalPgnParameters implements IParameterBundleValidator {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  @Override
  public List<String> valideer(Map<String, Parameter> params,
                               List<String> argumenten) {
    List<String>  fouten  = new ArrayList<>();

    if (argumenten.contains(CaissaTools.PAR_BESTAND)
        && argumenten.contains(CaissaTools.PAR_PGN)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_BESTANDENPGN));
    }

    if (!argumenten.contains(CaissaTools.PAR_BESTAND)
        && !argumenten.contains(CaissaTools.PAR_PGN)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_GEENINVOER));
    }

    if (params.get(CaissaTools.PAR_VANTAAL).getWaarde()
              .equals(params.get(CaissaTools.PAR_NAARTAAL).getWaarde())) {
      fouten.add(MessageFormat
            .format(resourceBundle.getString(CaissaTools.ERR_TALENGELIJK),
                    params.get(CaissaTools.PAR_VANTAAL).getWaarde(),
                    params.get(CaissaTools.PAR_NAARTAAL).getWaarde()));
    }

    return fouten;
  }
}
