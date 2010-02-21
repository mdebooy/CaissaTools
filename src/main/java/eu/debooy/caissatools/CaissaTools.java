/**
 * Copyright 2009 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/7330l5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package eu.debooy.caissatools;


/**
 * @author Marco de Booij
 */
public class CaissaTools {
  public static void main(String[] args) {
    if (args.length == 0) {
      help();
      return;
    }

    String    commando      = args[0];
    String[]  commandoArgs  = new String[args.length-1];
    System.arraycopy(args, 1, commandoArgs, 0, args.length-1);

    if ("pgntohtml".equalsIgnoreCase(commando)) {
      PgnToHtml.execute(commandoArgs);
      return;
    }
    if ("pgntolatex".equalsIgnoreCase(commando)) {
      PgnToLatex.execute(commandoArgs);
      return;
    }
    if ("startpgn".equalsIgnoreCase(commando)) {
      StartPgn.execute(commandoArgs);
      return;
    }
    help();
  }

  /**
   * Print de Banner.
   */
  private static void banner() {
    System.out.println("+----------+----------+----------+----------+----------+----------+");
    System.out.println("|          |          |");
    System.out.println("|   |\\__   *   __/|   | Caissa Tools");
    System.out.println("|   /  .\\ *** /.   \\  |");
    System.out.println("|  | ( _ \\ * / _ ) |  |");
    System.out.println("+--|    \\_) (_/    |--+----------+----------+----------+----------+");
    System.out.println("|  |    |     |    |  |");
    System.out.println("|  /_____\\   /_____\\  |");
    System.out.println("| [_______]_[_______] | E-Mail : marco.development@debooy.eu");
    System.out.println("|       [_____]       | Website: http://www.debooy.eu");
    System.out.println("+----------+----------+----------+----------+----------+----------+");
    System.out.println("");
  }

  /**
   * Geeft de 'help' pagina.
   */
  private static void help() {
    banner();
    System.out.println("  PgnToHtml  Zet een bestand met PGN partijen uit een toernooi om in");
    System.out.println("             2 HTML Bestanden.");
    System.out.println("  PgnToLatex Zet een bestand met PGN partijen uit een toernooi om in");
    System.out.println("             een .tex bestand.");
    System.out.println("  StartPgn   Maakt een PGN bestand aan met alle partijen die tussen");
    System.out.println("             de opgegeven spelers gespeeld (moeten) worden.");
    System.out.println();
    PgnToHtml.help();
    System.out.println();
    PgnToLatex.help();
    System.out.println();
    StartPgn.help();
  }
}
