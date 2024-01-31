/*
 * Copyright (c) 2022 Marco de Booij
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class ToernooioverzichtTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      ToernooioverzichtTest.class.getClassLoader();


  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE2A_PGN,
                                     TestConstants.BST_SCHEMA2_JSON,
                                     TestConstants.BST_TOERNOOI_TEX
                       });
  }

  @Before
  public void beforeTest() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_TOERNOOI_TEX});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    try {
      kopieerBestand(CLASSLOADER, TestConstants.BST_COMPETITIE2A_PGN,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE2A_PGN);
      kopieerBestand(CLASSLOADER, TestConstants.BST_SCHEMA2_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_SCHEMA2_JSON);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }
  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    Toernooioverzicht.execute(args);
    after();

    Assert.assertEquals(1, err.size());
    Assert.assertEquals("PAR-0001", err.get(0).split(" ")[0]);
  }

  @Test
  public void testToernooioverzicht1() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND2A,
                                    TestConstants.PAR_SCHEMA2,
                                    TestConstants.PAR_UITVOER};

    before();
    Toernooioverzicht.execute(args);
    after();

    Assert.assertEquals(0, err.size());
    Assert.assertEquals(getTemp() + File.separator
                          + TestConstants.BST_TOERNOOI_TEX,
                          out.get(13).split(":")[1].trim());
    Assert.assertEquals(TestConstants.TOT_PARTIJEN2,
                 out.get(14).split(":")[1].trim());

    try {
      Assert.assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_TOERNOOI_TEX),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_TOERNOOI2_TEX)));
    } catch (BestandException e) {
      Assert.fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testToernooioverzicht2() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND2A,
                                    TestConstants.PAR_SCHEMA2,
                                    "--metInhaaldatum",
                                    TestConstants.PAR_UITVOER};

    before();
    Toernooioverzicht.execute(args);
    after();

    Assert.assertEquals(0, err.size());
    Assert.assertEquals(getTemp() + File.separator
                          + TestConstants.BST_TOERNOOI_TEX,
                          out.get(13).split(":")[1].trim());
    Assert.assertEquals(TestConstants.TOT_PARTIJEN2,
                          out.get(14).split(":")[1].trim());

    try {
      Assert.assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_TOERNOOI_TEX),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_TOERNOOI2A_TEX)));
    } catch (BestandException e) {
      Assert.fail(e.getLocalizedMessage());
    }
  }
}
