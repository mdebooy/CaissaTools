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

import static eu.debooy.caissatools.StartPgnTest.CLASSLOADER;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class StartCorrespondentieTest extends BatchTest {
  private static final  String  CORRESPONDENTIE_JSON  = "correspondentie.json";
  private static final  String  CORRESPONDENTIE_PGN   = "correspondentie.pgn";

  @AfterClass
  public static void afterClass() throws BestandException {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {CORRESPONDENTIE_JSON,
                                     CORRESPONDENTIE_PGN});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    try {
      kopieerBestand(CLASSLOADER, CORRESPONDENTIE_JSON,
                     getTemp() + File.separator + CORRESPONDENTIE_JSON);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testCorrespondentie() throws BestandException {
    String[]  args  =
        new String[] {"-b", getTemp() + File.separator + CORRESPONDENTIE_PGN};

    before();
    StartCorrespondentie.execute(args);
    after();

    assertEquals(0, err.size());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      CORRESPONDENTIE_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                        + CORRESPONDENTIE_PGN)));
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    StartCorrespondentie.execute(args);
    after();

    assertEquals(1, err.size());
    assertEquals("PAR-0001", err.get(0).split(" ")[0]);
  }
}
