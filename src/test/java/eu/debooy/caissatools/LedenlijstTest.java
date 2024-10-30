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
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class LedenlijstTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      LedenlijstTest.class.getClassLoader();

  private static final  String  BST_LEDENLIJST_JSON  = "leden.json";
  private static final  String  BST_LEDENLIJST_TEX   = "leden.tex";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {BST_LEDENLIJST_JSON,
                                     BST_LEDENLIJST_TEX});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale.Builder()
                                .setLanguage(TestConstants.TST_TAAL)
                                .build());
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    for (var  bestand : new String[] {BST_LEDENLIJST_JSON}) {
      try {
        kopieerBestand(CLASSLOADER, bestand, getTemp()
                        + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    Ledenlijst.execute(args);
    after();

    assertEquals(1, err.size());
    assertEquals("PAR-0001", err.get(0).split(" ")[0]);
  }

  @Test
  public void testLedenlijst() throws BestandException {
    var args  = new String[] {"--" + CaissaTools.PAR_BESTAND,
                              getTemp() + File.separator + BST_LEDENLIJST_JSON};

    before();
    Ledenlijst.execute(args);
    after();

    assertTrue(err.isEmpty());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_LEDENLIJST_TEX),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                        + BST_LEDENLIJST_TEX)));
  }
}
