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
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class ClubstatistiekTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      ClubstatistiekTest.class.getClassLoader();

  private static final  String  BST_STATISTIEK  = "statistiek.json";

  private static final  String  PAR_BESTAND     =
      "--bestand=" + getTemp() + File.separator
        + TestConstants.BST_COMPETITIE_PGN;
  private static final  String  PAR_STATISTIEK  =
      String.format("--%s=%s%s%s", CaissaTools.PAR_STATISTIEK,
                                   getTemp(), File.separator, BST_STATISTIEK);

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE_PGN});
  }

  @Before
  public void beforeTest() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    try {
      kopieerBestand(CLASSLOADER, TestConstants.BST_COMPETITIE_PGN,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE_PGN);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testToernooitype1() {
    String[]  args  = new String[] {PAR_BESTAND, PAR_STATISTIEK,
                                    "--" + CaissaTools.PAR_TOERNOOITYPE + "=0"};

    before();
    Clubstatistiek.execute(args);
    after();

    assertEquals(1, err.size());
    assertTrue(err.get(0).startsWith("CTL-0111 "));
  }

  @Test
  public void testToernooitype2() {
    String[]  args  = new String[] {PAR_BESTAND, PAR_STATISTIEK,
                                    "--" + CaissaTools.PAR_TOERNOOITYPE + "=3"};

    before();
    Clubstatistiek.execute(args);
    after();

    assertEquals(1, err.size());
    assertTrue(err.get(0).startsWith("CTL-0111 "));
  }
}
