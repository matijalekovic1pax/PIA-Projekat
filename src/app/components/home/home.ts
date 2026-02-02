import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './home.html'
})
export class HomeComponent implements OnInit {
  topSpaces: any[] = [];
  cities: string[] = [];
  totalSpaces = 0;
  searchForm: FormGroup;
  searchResults: any[] | null = null;
  sortField: string = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private spaceService: SpaceService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.searchForm = this.fb.group({
      name: [''],
      cities: [[]] // Multi-select
    });
  }

  ngOnInit() {
    console.log('[Home] ngOnInit called');
    this.loadTopSpaces();
    this.loadCities();
    this.loadSummary();
  }

  loadTopSpaces() {
    console.log('[Home] loadTopSpaces called');
    this.spaceService.getTopSpaces().subscribe({
      next: (spaces) => {
        console.log('[Home] Top spaces received:', spaces.length);
        this.topSpaces = spaces;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('[Home] Error loading top spaces:', err)
    });
  }

  loadCities() {
    console.log('[Home] loadCities called');
    this.spaceService.getAllCities().subscribe({
      next: (cities) => {
        console.log('[Home] Cities received:', cities.length, cities);
        this.cities = cities;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('[Home] Error loading cities:', err)
    });
  }

  loadSummary() {
    console.log('[Home] loadSummary called');
    this.spaceService.getSpaceSummary().subscribe({
      next: (summary) => {
        console.log('[Home] Summary received:', summary);
        this.totalSpaces = summary.totalSpaces || 0;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('[Home] Error loading summary:', err)
    });
  }

  onCityChange(e: any, city: string) {
    const citiesControl = this.searchForm.get('cities');
    const currentCities: string[] = citiesControl?.value || [];

    if (e.target.checked) {
      citiesControl?.setValue([...currentCities, city]);
    } else {
      citiesControl?.setValue(currentCities.filter(c => c !== city));
    }
  }

  onSearch() {
    const { name, cities } = this.searchForm.value;
    // Pass undefined for optional params to match signature
    this.spaceService.searchSpaces(name, cities).subscribe(results => {
      this.searchResults = results;
      this.sortResults();
      this.cdr.detectChanges();
    });
  }

  sortResults(field?: string) {
    if (field) {
      if (this.sortField === field) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortField = field;
        this.sortDirection = 'asc';
      }
    }

    if (this.searchResults) {
      this.searchResults.sort((a, b) => {
        const valA = a[this.sortField]?.toLowerCase() || '';
        const valB = b[this.sortField]?.toLowerCase() || '';
        if (valA < valB) return this.sortDirection === 'asc' ? -1 : 1;
        if (valA > valB) return this.sortDirection === 'asc' ? 1 : -1;
        return 0;
      });
    }
  }
}
