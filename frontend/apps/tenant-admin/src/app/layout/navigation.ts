export interface NavigationPage {
  readonly label: string;
  readonly path: string;
}

export const productsPage: NavigationPage = { label: 'Produk', path: 'katalog/produk' };

export const navigation = [
  {
    label: 'Katalog',
    icon: 'catalog',
    pages: [productsPage, { label: 'Kategori', path: 'katalog/kategori' }],
  },
  {
    label: 'Inventaris',
    icon: 'inventory',
    pages: [
      { label: 'Saldo Stok', path: 'inventaris/saldo-stok' },
      { label: 'Riwayat Stok', path: 'inventaris/riwayat-stok' },
      { label: 'Terima Barang', path: 'inventaris/terima-barang' },
    ],
  },
  {
    label: 'Tim & Akses',
    icon: 'team',
    pages: [
      { label: 'Staf', path: 'tim-akses/staf' },
      { label: 'Role', path: 'tim-akses/role' },
    ],
  },
] as const;

export const settingsPage: NavigationPage = { label: 'Pengaturan Toko', path: 'pengaturan-toko' };
