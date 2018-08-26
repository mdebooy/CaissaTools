/**
 * Copyright 2018 Marco de Booij
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class StartPgnTest extends BatchTest {
  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(temp + File.separator + "competitie1.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    try {
      BufferedReader  bron  =
          Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                    "competitie1.pgn");
      BufferedWriter  doel  = Bestand.openUitvoerBestand(temp + File.separator
                                                         + "competitie1.pgn");
      kopieerBestand(bron, doel);
      bron.close();
      doel.close();
    } catch (IOException e) {
      throw new BestandException(e);
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(StartPgn.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 29, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
  }

  @Test
  public void testStartPgn() throws BestandException {
    String[]  args      = new String[] {"--bestand=start",
                                        "--date=1999.10.01",
                                        "--event=\"Testing 97/98\"",
                                        "--site=\"Caissa Tools\"",
                                        "--spelers=\"Speler, 01;Speler, 02;"+
                                          "Speler, 03;Speler, 04\"",
                                        "--uitvoerdir=" + temp};

    try {
      Bestand.delete(temp + File.separator + "start.pgn");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(StartPgn.class, "execute", args, out, err);

    assertEquals("StartPgn - helptekst", 16, out.size());
    assertEquals("StartPgn - fouten", 0, 0);
    assertEquals("StartPgn - 14",
                 temp + File.separator + "start.pgn",
                 out.get(13).split(":")[1].trim());
    assertEquals("StartPgn - 15", temp,
                 out.get(14).split(":")[1].trim());
    assertTrue("StartPgn - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(temp + File.separator
                                      + "start.pgn"),
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      "start.pgn")));

    Bestand.delete(temp + File.separator + "start.pgn");
  }
}
