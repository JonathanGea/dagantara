import { Component } from '@angular/core';
import { AppLayout } from './layout/app-layout';

@Component({
  selector: 'app-root',
  imports: [AppLayout],
  template: '<app-layout />',
})
export class App {}
