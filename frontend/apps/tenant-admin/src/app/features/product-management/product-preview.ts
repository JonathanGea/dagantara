import { Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface PreviewProduct {
  readonly slug: string;
  readonly name: string;
  readonly sku: string;
  readonly category: string;
  readonly published: boolean;
  readonly price: number;
}

export const previewProducts: readonly PreviewProduct[] = [
  { slug: 'kopi-gayo-250g', name: 'Kopi Gayo 250 g', sku: 'KG-250', category: 'Kopi', published: true, price: 85000 },
  { slug: 'kopi-gayo-500g', name: 'Kopi Gayo 500 g', sku: 'KG-500', category: 'Kopi', published: true, price: 158000 },
  { slug: 'paket-seduh', name: 'Paket Seduh', sku: 'PK-SED', category: 'Paket', published: false, price: 135000 },
  { slug: 'filter-kertas', name: 'Filter Kertas', sku: 'AK-FLT', category: 'Aksesori', published: false, price: 32000 },
];

@Component({
  imports: [RouterLink],
  templateUrl: './product-preview.html',
  styleUrl: './product-preview.css',
})
export class ProductPreview {
  readonly product = input<PreviewProduct>();

  protected readonly search = signal('');
  protected readonly visibleProducts = computed(() => {
    const query = this.search().trim().toLowerCase();
    return previewProducts.filter(({ name, sku }) =>
      name.toLowerCase().includes(query) || sku.toLowerCase().includes(query),
    );
  });
  protected readonly currency = new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  });
}
