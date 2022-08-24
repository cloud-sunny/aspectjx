package sun.gradle.plugin.aspectjx

class FilterMatcher {

    static boolean isMatch(String s, String p) {
        int sRight = s.length(), pRight = p.length()
        char star = '*'
        while (sRight > 0 && pRight > 0 && p.charAt(pRight - 1) != star) {
            if (isCharEquals(s.charAt(sRight - 1), p.charAt(pRight - 1))) {
                --sRight
                --pRight
            } else {
                return false
            }
        }
        if (pRight == 0) {
            return sRight == 0
        }
        int sIndex = 0, pIndex = 0
        int sRecord = -1, pRecord = -1

        while (sIndex < sRight && pIndex < pRight) {
            if (p.charAt(pIndex) == star) {
                ++pIndex
                sRecord = sIndex
                pRecord = pIndex
            } else if (isCharEquals(s.charAt(sIndex), p.charAt(pIndex))) {
                ++sIndex
                ++pIndex
            } else if (sRecord != -1 && sRecord + 1 < sRight) {
                ++sRecord
                sIndex = sRecord
                pIndex = pRecord
            } else {
                return false
            }
        }
        return isAllStars(p, pIndex, pRight)
    }

    private static boolean isAllStars(String str, int left, int right) {
        char star = '*'
        for (int i = left; i < right; ++i) {
            if (str.charAt(i) != star) {
                return false
            }
        }
        return true
    }

    private static boolean isCharEquals(char u, char v) {
        char q = '?'
        return u == v || v == q
    }
}