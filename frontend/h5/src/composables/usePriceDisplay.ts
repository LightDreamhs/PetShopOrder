export function usePriceDisplay() {
  function formatPrice(price: string | number): string {
    const num = typeof price === 'string' ? parseFloat(price) : price
    return `¥${num.toFixed(2)}`
  }

  function hasDiscount(originalPrice: string, dealPrice: string): boolean {
    return parseFloat(originalPrice) !== parseFloat(dealPrice)
  }

  return { formatPrice, hasDiscount }
}
