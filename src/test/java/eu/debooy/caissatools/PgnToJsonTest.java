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
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToJsonTest extends BatchTest {
  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(TEMP + File.separator + "competitie2.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    BufferedReader  bron  = null;
    BufferedWriter  doel  = null;
    try {
      bron  = Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                        "competitie2.pgn");
      doel  = Bestand.openUitvoerBestand(TEMP + File.separator
                                         + "competitie2.pgn");
      kopieerBestand(bron, doel);
    } finally {
      try {
        if (null != bron) {
          bron.close();
        }
      } catch (IOException e) {
        throw new BestandException(e);
      }
      try {
        if (null != doel) {
          doel.close();
        }
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 25, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
  }

  @Test
  public void testPgnToJson() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie2",
                                        "--json=competitie",
                                        "--invoerdir=" + TEMP,
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("PgnToJson - helptekst", 17, out.size());
    assertEquals("PgnToJson - fouten", 0, 0);
    assertEquals("PgnToJson - 14",
                 TEMP + File.separator + "competitie2.pgn",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToJson - 15", "64",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 16", TEMP + File.separator + "competitie.json",
                 out.get(15).split(":")[1].trim());
    assertTrue("PgnToJson - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie.json"),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      "competitie1.json")));

    Bestand.delete(TEMP + File.separator + "competitie.json");
  }

  @Test
  public void testMetPgnView() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie2",
                                        "--invoerdir=" + TEMP,
                                        "--json=competitie",
                                        "--pgnviewer=J",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("PgnToJson - helptekst", 17, out.size());
    assertEquals("PgnToJson - fouten", 0, 0);
    assertEquals("PgnToJson - 14",
                 TEMP + File.separator + "competitie2.pgn",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToJson - 15", "64",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 16", TEMP + File.separator + "competitie.json",
                 out.get(15).split(":")[1].trim());
    assertTrue("PgnToJson - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie.json"),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      "competitie2.json")));

    Bestand.delete(TEMP + File.separator + "competitie.json");
  }
}
