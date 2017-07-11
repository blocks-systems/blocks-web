package blocks

import grails.validation.Validateable

/**
 * @author emil.wesolowski
 *
 */

class DatatableRequestCommand implements Validateable {

    String sEcho;
    int iColumns;
    String sColumns;
    int iDisplayStart;
    int iDisplayLength;
    List<Integer> amDataProp;
    String sSearch;
    List<String> asSearch;
    boolean bRegex;
    List<Boolean> abRegex;
    List<Boolean> abSearchable;
    int iSortingCols;
    List<Integer> aiSortCol;
    List<String> asSortDir;
    List<Boolean> abSortable;

    @Override
    public String toString() {
        return "DatatableRequestCommand [sEcho=" + sEcho + ", iColumns="
        + iColumns + ", sColumns=" + sColumns + ", iDisplayStart="
        + iDisplayStart + ", iDisplayLength=" + iDisplayLength
        + ", amDataProp=" + amDataProp + ", sSearch=" + sSearch
        + ", asSearch=" + asSearch + ", bRegex=" + bRegex
        + ", abRegex=" + abRegex + ", abSearchable=" + abSearchable
        + ", iSortingCols=" + iSortingCols + ", aiSortCol=" + aiSortCol
        + ", asSortDir=" + asSortDir + ", abSortable=" + abSortable
        + "]";
    }
}
