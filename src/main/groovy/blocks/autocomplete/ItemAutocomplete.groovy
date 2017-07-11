package blocks.autocomplete

import blocks.autocomplete.Item

/**
 * @author emil.wesolowski
 *
 */
class ItemAutocomplete extends Item {

    private BigDecimal price
    private Long currencyId
    private Long unitOfMeasureId

    public ItemAutocomplete(final Long id, final String text, final BigDecimal price, final Long currencyId, final Long unitOfMeasureId) {
        super(id, text)
        this.price = price
        this.currencyId = currencyId
        this.unitOfMeasureId = unitOfMeasureId
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }
}
