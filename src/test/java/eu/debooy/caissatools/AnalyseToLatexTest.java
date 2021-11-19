/*
 * Copyright (c) 2021 Marco de Booij
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

import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.DoosUtilsTestConstants;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class AnalyseToLatexTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      AnalyseToLatexTest.class.getClassLoader();

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {TestConstants.BST_ANALYSE_PGN});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_ANALYSE_PGN}) {
      try {
        kopieerBestand(CLASSLOADER, bestand, TEMP + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(AnalyseToLatex.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder parameters - helptekst", 26, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

//  @Test
//  public void testAnalyseToLatex() throws BestandException {
//    String[]  args      = new String[] {TestConstants.PAR_AUTEUR,
//                                        TestConstants.PAR_BESTAND4,
//                                        TestConstants.PAR_INVOERDIR + TEMP,
//                                        TestConstants.PAR_UITVOERDIR + "/homes/booymar/Schaken/"};
//
//
//    VangOutEnErr.execute(AnalyseToLatex.class,
//                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);
//debug();
//    assertEquals("PgnToLatex - helptekst", 18, out.size());
//    assertEquals("PgnToLatex - fouten", 0, err.size());
//  }
}
