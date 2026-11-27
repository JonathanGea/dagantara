import { Component } from '@angular/core';
import { Route, Routes } from '@angular/router';
import { ProductPreview, previewProducts } from './features/product-management/product-preview';
import { navigation, productsPage, settingsPage } from './layout/navigation';

@Component({
  template: '<p class="text-sm text-neutral-500">Belum ada data contoh di halaman ini.</p>',
})
class SectionPreview {}

const sections: Route[] = navigation.flatMap((group) =>
  group.pages.map((page) => ({
    path: page.path,
    title: `${page.label} | Dagantara`,
    component: page === productsPage ? ProductPreview : SectionPreview,
    data: { page, group: group.label },
  })),
);

const productDetails: Route[] = previewProducts.map((product) => ({
  path: `${productsPage.path}/${product.slug}`,
  title: `${product.name} | Dagantara`,
  component: ProductPreview,
  data: { page: productsPage, group: navigation[0].label, product },
}));

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: productsPage.path },
  ...productDetails,
  ...sections,
  {
    path: settingsPage.path,
    title: `${settingsPage.label} | Dagantara`,
    component: SectionPreview,
    data: { page: settingsPage },
  },
  { path: '**', redirectTo: productsPage.path },
];
