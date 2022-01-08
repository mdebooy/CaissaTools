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
public class PgnCleanerTest extends BatchTest {
  private static final  String  CLEAN       =
      "partijClean.pgn";
  private static final  String  METCOMENVAR =
      "partijMetCommentaarEnVarianten.pgn";
  private static final  String  METVAR      =
      "partijMetVarianten.pgn";

  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(getTemp() + File.separator + METCOMENVAR);
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    try {
      kopieerBestand(CLASSLOADER, METCOMENVAR,
                     getTemp() + File.separator + METCOMENVAR);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testClean() throws BestandException {
    var args  = new String[] {"-b", getTemp() + File.separator + METCOMENVAR,
                              "-u", getTemp() + File.separator + CLEAN, "-e"};

    before();
    PgnCleaner.execute(args);
    after();

    assertEquals(0, err.size());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      CLEAN),
            Bestand.openInvoerBestand(getTemp() + File.separator + CLEAN)));

    Bestand.delete(getTemp() + File.separator + CLEAN);
  }

  @Test
  public void testLeeg() {
    var args  = new String[] {};

    before();
    PgnCleaner.execute(args);
    after();

    assertEquals(1, err.size());
  }

  @Test
  public void testMetVarianten() throws BestandException {
    var args  = new String[] {"-b", getTemp() + File.separator + METCOMENVAR,
                              "-u", getTemp() + File.separator + METVAR};

    before();
    PgnCleaner.execute(args);
    after();

    assertEquals(0, err.size());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      METVAR),
            Bestand.openInvoerBestand(getTemp() + File.separator + METVAR)));

    Bestand.delete(getTemp() + File.separator + METVAR);
  }
}
