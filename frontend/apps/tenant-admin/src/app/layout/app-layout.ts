import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, fromEvent } from 'rxjs';
import type { PreviewProduct } from '../features/product-management/product-preview';
import { Icon } from './icon';
import { navigation, productsPage, settingsPage, NavigationPage } from './navigation';

interface PageContext {
  page: NavigationPage;
  group?: string;
  product?: PreviewProduct;
}

const sidebarPreference = 'dagantara.tenant-admin.sidebar';
const searchablePages = [
  ...navigation.flatMap((group) => group.pages.map((page) => ({ ...page, group: group.label }))),
  { ...settingsPage, group: '' },
];

@Component({
  selector: 'app-layout',
  imports: [NgTemplateOutlet, RouterLink, RouterOutlet, Icon],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css',
  host: {
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'closePopover(true)',
  },
})
export class AppLayout {
  private readonly router = inject(Router);
  private readonly browser = inject(DOCUMENT).defaultView!;
  private readonly mobileQuery = this.browser.matchMedia('(max-width: 760px)');
  private readonly drawer = viewChild<ElementRef<HTMLDialogElement>>('mobileDrawer');
  private readonly sidebarToggle = viewChild<ElementRef<HTMLButtonElement>>('sidebarToggle');
  private popoverTrigger?: HTMLButtonElement;

  protected readonly navigation = navigation;
  protected readonly productsPage = productsPage;
  protected readonly settingsPage = settingsPage;
  protected readonly isMobile = signal(this.mobileQuery.matches);
  protected readonly compact = signal(this.readCompactPreference());
  protected readonly mobileOpen = signal(false);
  protected readonly openGroups = signal<ReadonlySet<string>>(new Set());
  protected readonly popover = signal<string | undefined>(undefined);
  protected readonly active = signal<PageContext | undefined>(undefined);
  protected readonly menuSearch = signal('');
  protected readonly menuQuery = computed(() => this.menuSearch().trim().toLowerCase());
  protected readonly menuResults = computed(() =>
    searchablePages.filter((page) => `${page.label} ${page.group}`.toLowerCase().includes(this.menuQuery())),
  );
  protected readonly title = computed(() => this.active()?.product?.name ?? this.active()?.page.label ?? '');
  protected readonly sidebarLabel = computed(() =>
    this.compact() ? 'Lebarkan sidebar' : 'Ringkas sidebar',
  );

  constructor() {
    this.syncPage();
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      takeUntilDestroyed(),
    ).subscribe(() => this.syncPage());
    fromEvent<MediaQueryListEvent>(this.mobileQuery, 'change').pipe(takeUntilDestroyed()).subscribe(({ matches }) => {
      this.isMobile.set(matches);
      this.mobileOpen.set(false);
      this.menuSearch.set('');
      this.closePopover();
      this.openActiveGroup();
    });
  }

  protected toggleSidebar(): void {
    this.closePopover();
    if (this.isMobile()) {
      const drawer = this.drawer()?.nativeElement;
      this.sidebarToggle()?.nativeElement.focus({ preventScroll: true });
      drawer?.showModal();
      this.mobileOpen.set(drawer?.open ?? false);
      return;
    }

    const compact = !this.compact();
    this.compact.set(compact);
    this.menuSearch.set('');
    if (!compact) this.openActiveGroup();
    try {
      this.browser.localStorage.setItem(sidebarPreference, compact ? 'compact' : 'expanded');
    } catch {
      // The layout still works when storage is unavailable.
    }
  }

  protected closeDrawer(): void {
    this.drawer()?.nativeElement.close();
    this.onDrawerClosed();
  }

  protected onDrawerClosed(): void {
    if (this.drawer()?.nativeElement.open) return;
    this.mobileOpen.set(false);
    this.menuSearch.set('');
    this.closePopover();
  }

  protected closeOnBackdrop(event: MouseEvent): void {
    if (event.target === this.drawer()?.nativeElement) this.closeDrawer();
  }

  protected toggleGroup(group: string, mode: string, event: MouseEvent): void {
    if (mode === 'compact') {
      this.togglePopover(group, event);
      return;
    }

    this.closePopover();
    this.openGroups.update((groups) => {
      const updated = new Set(groups);
      if (updated.has(group)) updated.delete(group);
      else updated.add(group);
      return updated;
    });
  }

  protected togglePopover(id: string, event: MouseEvent): void {
    if (this.popover() === id) {
      this.closePopover();
      return;
    }
    this.popoverTrigger = event.currentTarget as HTMLButtonElement;
    this.popover.set(id);
  }

  protected closePopover(restoreFocus = false): void {
    if (restoreFocus) this.popoverTrigger?.focus();
    this.popoverTrigger = undefined;
    this.popover.set(undefined);
  }

  protected closeOnOutsideClick(event: MouseEvent): void {
    if (event.target instanceof Element && !event.target.closest('[data-sidebar-popover]')) {
      this.closePopover();
    }
  }

  protected selectPage(): void {
    this.menuSearch.set('');
    this.closePopover(true);
    if (this.mobileOpen()) this.closeDrawer();
  }

  protected showGroup(group: string, mode: string): boolean {
    return mode === 'compact' ? this.popover() === group : this.openGroups().has(group);
  }

  private syncPage(): void {
    const data = this.router.routerState.snapshot.root.firstChild?.data as PageContext | undefined;
    this.active.set(data);
    this.openActiveGroup();
    this.selectPage();
  }

  private openActiveGroup(): void {
    const group = this.active()?.group;
    if (group) this.openGroups.update((groups) => new Set([...groups, group]));
  }

  private readCompactPreference(): boolean {
    try {
      return this.browser.localStorage.getItem(sidebarPreference) === 'compact';
    } catch {
      return false;
    }
  }
}
