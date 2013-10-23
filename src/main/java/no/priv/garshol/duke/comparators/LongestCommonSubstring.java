
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * <p>An implementation of the longest common substring comparator. Note
 * that it does not merely find the longest common substring, but does
 * so repeatedly down to a minimal substring length.
 *
 * <p>Described in P. Christen, chapter 5.9. Also in Tolerating spelling
 * errors during patient validation; Friedman C, Sideli R.; Comput
 * Biomed Res. 1992 Oct;25(5):486-509.
 * http://www.cs.utah.edu/contest/2005/spellingErrors.pdf
 *
 * @since 1.2
 */
public class LongestCommonSubstring implements Comparator {
  private int minlen = 2;
  
  public double compare(String s1, String s2) {
    // a couple of quick cutoffs
    if (s1.equals(s2))
      return 1.0;
    if (Math.min(s1.length(), s2.length()) == 0)
      return 0.0;

    return (compare_(s1, s2) + compare_(s2, s1)) / 2.0;
  }

  // the results of the algorithm depends on the order of the input
  // strings.  therefore need a sub-method for this computation
  private double compare_(String s1, String s2) {
    // before we begin, note the length of the shortest string
    int shortlen = Math.min(s1.length(), s2.length());
    int removed = 0; // total length of common substrings
    while (true) {
      // first, we identify the longest common substring
      int longest = 0;
      int longesti = 0;
      int longestj = 0;
    
      int[][] matrix = new int[s1.length()][s2.length()];
      for (int i = 0; i < s1.length(); i++) {
        for (int j = 0; j < s2.length(); j++) {
          if (s1.charAt(i) == s2.charAt(j)) {
            if (i == 0 || j == 0)
              matrix[i][j] = 1;
            else
              matrix[i][j] = matrix[i - 1][j - 1] + 1;

            if (matrix[i][j] > longest) {
              longest = matrix[i][j];
              longesti = i;
              longestj = j;
            }
          } else
            matrix[i][j] = 0;
        }
      }

      longesti++; // this solves an off-by-one problem
      longestj++; // this solves an off-by-one problem

      // at this point we know the length of the longest common
      // substring, and also its location, since it ends at indexes
      // longesti and longestj.

      if (longest < minlen)
        break; // all remaining common substrings are too short, so we stop

      // now we slice away the common substrings
      s1 = s1.substring(0, longesti - longest) + s1.substring(longesti);
      s2 = s2.substring(0, longestj - longest) + s2.substring(longestj);
      removed += longest;
    }
    
    return removed / (double) shortlen;
  }
  
  public boolean isTokenized() {
    return true;
  }

  public void setMinimumLength(int minlen) {
    this.minlen = minlen;
  }

  public int getMinimumLength() {
    return this.minlen;
  }
}