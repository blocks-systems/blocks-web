/**
 *
 */
package blocks.autocomplete

import grails.validation.Validateable

/**
 * @author emil.wesolowski
 *
 */

public class AutoCompleteResponseCommand implements Validateable {

    List<? extends Item> items
    Long total
}
