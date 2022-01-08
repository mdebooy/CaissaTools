/**
 * Copyright (c) 2020 Marco de Booij
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

import eu.debooy.doosutils.test.BatchTest;
import java.io.File;


/**
 * @author Marco de Booij
 */
public final class TestConstants extends BatchTest {
  public static final String  BST_ANALYSE_PGN       = "analyse.pgn";
  public static final String  BST_COMPETITIE_CSV    = "competitie.csv";
  public static final String  BST_COMPETITIE_JSON   = "competitie.json";
  public static final String  BST_COMPETITIE1_PGN   = "competitie1.pgn";
  public static final String  BST_COMPETITIE2_PGN   = "competitie2.pgn";
  public static final String  BST_COMPETITIEH_CSV   = "competitieH.csv";
  public static final String  BST_GESCHIEDENIS_CSV  = "geschiedenis.csv";
  public static final String  BST_INDEX_HTML        = "index.html";
  public static final String  BST_MATRIX_HTML       = "matrix.html";
  public static final String  BST_PARTIJ_PGN        = "partij.pgn";
  public static final String  BST_SCHEMA1_JSON      = "schema1.json";
  public static final String  BST_SCHEMA2_JSON      = "schema2.json";
  public static final String  BST_TEKORT_JSON       = "tekort.json";
  public static final String  BST_TELANG_JSON       = "telang.json";
  public static final String  BST_UITSLAGEN_HTML    = "uitslagen.html";

  public static final String  MSG_ERROR_MESSAGES  = "Error mesages";

  public static final String  PAR_AUTEUR            = "NetBeans";
  public static final String  PAR_BESTAND1          =
      "--bestand=" + getTemp() + File.separator + "competitie1";
  public static final String  PAR_BESTAND2          =
      "--bestand=" + getTemp() + File.separator + "competitie2";
  public static final String  PAR_BESTAND3          =
      "--bestand=" + getTemp() + File.separator + "partij.pgn";
  public static final String  PAR_BESTAND4          =
      "--bestand=" + getTemp() + File.separator + "analyse.pgn";

  public static final String  PAR_GESCHIEDENIS      =
      "--geschiedenisBestand=" + getTemp() + File.separator + "geschiedenis";
  public static final String  PAR_INVOERDIR         = "--invoerdir=";
  public static final String  PAR_MATRIX_OP_STAND   = "--opstand";
  public static final String  PAR_SCHEMA1           =
      "--schema=" + getTemp() + File.separator + "schema1";
  public static final String  PAR_SCHEMA2           = "--schema=schema2";
  public static final String  PAR_SPELERBESTAND     =
      "--spelerBestand=" + getTemp() + File.separator + "competitie";
  public static final String  PAR_TOERNOOIBESTAND1  =
      "--toernooiBestand=" + getTemp() + File.separator + "competitie1";
  public static final String  PAR_TOERNOOIBESTAND2  =
      "--toernooiBestand=" + getTemp() + File.separator + "competitie2";
  public static final String  PAR_UITVOERDIR        = "--uitvoerdir=";

  public static final String  TOT_PARTIJEN  = "150";

  public static final String  TST_TAAL  = "nl";

  protected TestConstants() {}
}
