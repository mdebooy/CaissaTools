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

import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
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
public class StartPgnTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      StartPgnTest.class.getClassLoader();

  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(TEMP + File.separator + "competitie1.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    TekstBestand  bron  = null;
    TekstBestand  doel  = null;
    try {
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand("competitie1.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "competitie1.pgn")
                              .setLezen(false).build();
      doel.add(bron);
    } finally {
      if (null != bron) {
        bron.close();
      }
      if (null != doel) {
        doel.close();
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(StartPgn.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 29, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testStartPgn() throws BestandException {
    String[]  args      = new String[] {"--bestand=start",
                                        "--date=1999.10.01",
                                        "--event=\"Testing 97/98\"",
                                        "--site=\"Caissa Tools\"",
                                        "--spelers=\"Speler, 01;Speler, 02;" +
                                          "Speler, 03;Speler, 04\"",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "start.pgn");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(StartPgn.class, "execute", args, out, err);

    assertEquals("StartPgn - helptekst", 17, out.size());
    assertEquals("StartPgn - fouten", 0, err.size());
    assertEquals("StartPgn - 14",
                 TEMP + File.separator + "start.pgn",
                 out.get(13).split(":")[1].trim());
    assertTrue("StartPgn - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      "start.pgn"),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "start.pgn")));

    Bestand.delete(TEMP + File.separator + "start.pgn");
  }
}
