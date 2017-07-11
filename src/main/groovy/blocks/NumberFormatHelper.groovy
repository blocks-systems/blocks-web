package blocks

import java.text.DecimalFormatSymbols
import java.text.NumberFormat

/**
 * @author emil.wesolowski
 */
class NumberFormatHelper {
    public static final formatBigDecimal(final BigDecimal value, final String language) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale(language))
        DecimalFormatSymbols symbols = numberFormat.getDecimalFormatSymbols()
        symbols.setCurrencySymbol("")
        numberFormat.setDecimalFormatSymbols(symbols)
        return numberFormat.format(value.doubleValue())
    }
}
