/**
 *
 */
package blocks.autocomplete

/**
 * @author emil.wesolowski
 *
 */
class Item {
    private Long id;
    private String text;

    public Item(final Long id, final String text) {
        super();
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
