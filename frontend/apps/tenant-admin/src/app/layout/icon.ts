import { Component, input } from '@angular/core';

const paths = {
  catalog: 'm12 3 9 5-9 5-9-5 9-5Zm-9 9 9 5 9-5M3 16l9 5 9-5',
  inventory: 'M4 7h16v13H4zM3 3h18v4H3zM10 11h4',
  team: 'M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2m18 0v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8M14 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  settings: 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Zm8 3c0-.5 0-1-.15-1.45l1.45-1.25-1.6-2.75-1.8.6a8 8 0 0 0-2.5-1.45L15 3H9l-.4 2.7a8 8 0 0 0-2.5 1.45l-1.8-.6-1.6 2.75 1.45 1.25a7.3 7.3 0 0 0 0 2.9l-1.45 1.25 1.6 2.75 1.8-.6a8 8 0 0 0 2.5 1.45L9 21h6l.4-2.7a8 8 0 0 0 2.5-1.45l1.8.6 1.6-2.75-1.45-1.25c.15-.45.15-.95.15-1.45Z',
  sidebar: 'M3 5h18v14H3zM9 5v14',
  search: 'M16 10.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0Zm-1.6 3.9L20 20',
  menu: 'M4 6h16M4 12h16M4 18h16',
  sun: 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM12 2v2m0 16v2M2 12h2m16 0h2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4m0-14.2-1.4 1.4M6.3 17.7l-1.4 1.4',
  moon: 'M20.9 13.35A8.9 8.9 0 0 1 10.65 3.1 9 9 0 1 0 20.9 13.35Z',
  close: 'm6 6 12 12M18 6 6 18',
  chevron: 'm9 6 6 6-6 6',
  back: 'M19 12H5m7 7-7-7 7-7',
} as const;

@Component({
  selector: 'app-icon',
  host: { 'aria-hidden': 'true' },
  styles: `
    :host { display: inline-flex; flex: none; }
    svg { width: var(--app-icon-size, 16px); height: var(--app-icon-size, 16px); }
  `,
  template: `
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round">
      <path [attr.d]="paths[name()]" />
    </svg>
  `,
})
export class Icon {
  readonly name = input.required<keyof typeof paths>();
  protected readonly paths = paths;
}
