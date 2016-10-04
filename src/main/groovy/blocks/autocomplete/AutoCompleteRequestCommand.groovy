/**
 *
 */
package blocks.autocomplete;

import grails.validation.Validateable;

/**
 * @author emil.wesolowski
 *
 */

public class AutoCompleteRequestCommand implements Validateable {

    String term;
    Integer limit;
    Integer offset;

    static constraints = {
        term(blank: false, minSize: 3)
        limit nullable: false
        offset nullable: false
    }
}