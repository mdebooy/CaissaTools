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
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PartijinfoTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PartijinfoTest.class.getClassLoader();

  private static final  String  BST_PARTIJINFO_PGN  = "partijinfo.pgn";
  private static final  String  BST_PARTIJINFO_TXT  = "partijinfo.txt";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {BST_PARTIJINFO_PGN});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale.Builder()
                                .setLanguage(TestConstants.TST_TAAL)
                                .build());
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    verwijderBestanden(getTemp() + File.separator,
                       new String[] {BST_PARTIJINFO_PGN});
  }

  @Test
  public void testPartijinfoPgn() {
    var args  = new String[] {"--" + CaissaTools.PAR_BESTAND,
                              getTemp() + File.separator + BST_PARTIJINFO_PGN};

    before();
    Partijinfo.execute(args);
    after();

    try {
      var invoer  = Bestand.openInvoerBestand(CLASSLOADER, BST_PARTIJINFO_TXT);
      var i       = 13;

      while (i < out.size()) {
        assertEquals(invoer.readLine().stripTrailing(),
                     out.get(i).stripTrailing());
        i++;
      }
    } catch (BestandException | IOException e) {
      fail(e.getLocalizedMessage());
    }
  }
}
