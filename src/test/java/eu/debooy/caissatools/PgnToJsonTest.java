/**
 * Copyright (c) 2018 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
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

import eu.debooy.doosutils.DoosUtils;
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
public class PgnToJsonTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToJsonTest.class.getClassLoader();

  private static final  String  BST_COMPETITIE1_JSON  = "competitie1.json";
  private static final  String  BST_JSON_JSON         = "json.json";
  private static final  String  BST_JSON_PGN          = "json.pgn";
  private static final  String  BST_PARTIJ_JSON       = "partij.json";

  private static final  String  PAR_BESTAND_JSON      =
      "--bestand=" + getTemp() + DoosUtils.getFileSep()+ "json";
  private static final  String  PAR_BESTAND_PARTIJ    =
      "--bestand=" + getTemp() + DoosUtils.getFileSep()+ "partij";
  private static final  String  PAR_INCL_LEGE         = "--metlege";
  private static final  String  PAR_JSON_COMPETITIE1  =
      "--json=" + getTemp() + DoosUtils.getFileSep()+ "competitie1";
  private static final  String  PAR_JSON_JSON         =
      "--json=" + getTemp() + DoosUtils.getFileSep()+ "json";
  private static final  String  PAR_JSON_PARTIJ       =
      "--json=" + getTemp() + DoosUtils.getFileSep()+ "partij";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {BST_COMPETITIE1_JSON,
                                     TestConstants.BST_COMPETITIE2_PGN,
                                     BST_JSON_JSON, BST_JSON_PGN,
                                     BST_PARTIJ_JSON,
                                     TestConstants.BST_PARTIJ_PGN});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_COMPETITIE2_PGN,
                                        BST_JSON_PGN,
                                        TestConstants.BST_PARTIJ_PGN}) {
      try {
        kopieerBestand(CLASSLOADER, bestand,
                       getTemp() + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testCompetitiePgnToJson() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND2,
                                    PAR_INCL_LEGE,
                                    PAR_JSON_COMPETITIE1};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_JSON);
    } catch (BestandException e) {
    }

    before();
    PgnToJson.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE2_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("64", out.get(14).split(":")[1].trim());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_JSON,
                 out.get(15).split(":")[1].trim());
    assertEquals("64", out.get(16).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_JSON),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      BST_COMPETITIE1_JSON)));

    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_JSON);
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    before();
    PgnToJson.execute(args);
    after();

    assertEquals(1, err.size());
  }

  @Test
  public void testMetLegePartijen() throws BestandException {
    String[]  args  = new String[] {PAR_BESTAND_JSON,
                                    PAR_INCL_LEGE,
                                    PAR_JSON_JSON};

    try {
      Bestand.delete(getTemp() + File.separator + BST_JSON_JSON);
    } catch (BestandException e) {
    }

    before();
    PgnToJson.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals("2", out.get(14).split(":")[1].trim());
    assertEquals("2", out.get(16).split(":")[1].trim());

    Bestand.delete(getTemp() + File.separator + BST_JSON_JSON);
  }

  @Test
  public void testPartijToJson() throws BestandException {
    String[]  args  = new String[] {PAR_BESTAND_PARTIJ,
                                    PAR_INCL_LEGE,
                                    PAR_JSON_PARTIJ};

    try {
      Bestand.delete(getTemp() + File.separator + BST_PARTIJ_JSON);
    } catch (BestandException e) {
    }

    before();
    PgnToJson.execute(args);
    after();

    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_PARTIJ_JSON),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      BST_PARTIJ_JSON)));

    Bestand.delete(getTemp() + File.separator + BST_PARTIJ_JSON);
  }

  @Test
  public void testZonderLegePartijen() throws BestandException {
    String[]  args  = new String[] {PAR_BESTAND_JSON,
                                    PAR_JSON_JSON};

    try {
      Bestand.delete(getTemp() + File.separator + BST_JSON_JSON);
    } catch (BestandException e) {
    }

    before();
    PgnToJson.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals("2", out.get(14).split(":")[1].trim());
    assertEquals("1", out.get(16).split(":")[1].trim());

    Bestand.delete(getTemp() + File.separator + BST_JSON_JSON);
  }
}
