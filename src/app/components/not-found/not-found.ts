import { Component } from '@angular/core';

@Component({
  selector: 'app-not-found',
  standalone: true,
  template: `
    <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50 px-4">
      <h1 class="text-6xl font-bold text-gray-900">404</h1>
      <p class="mt-4 text-xl text-gray-600">Page not found</p>
      <a href="/" class="mt-8 text-primary-600 hover:text-primary-500 font-medium">Go back home</a>
    </div>
  `
})
export class NotFoundComponent { }
