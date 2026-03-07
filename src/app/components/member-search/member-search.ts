import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-member-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './member-search.html'
})
export class MemberSearchComponent implements OnInit {
  searchForm: FormGroup;
  searchResults: any[] | null = null;
  cities: string[] = [];

  constructor(private fb: FormBuilder, private spaceService: SpaceService) {
    this.searchForm = this.fb.group({
      name: [''],
      cities: [[]],
      workspaceType: [''], // 'open', 'office', 'conference' - optional
      capacity: [''] // visible only if office
    });
  }

  ngOnInit() {
    this.spaceService.getAllCities().subscribe(cities => this.cities = cities);
    // Load initial results - show all spaces
    this.onSearch();
  }

  onTypeChange(type: string, event: any) {
    if (event.target.checked) {
      this.searchForm.patchValue({ workspaceType: type });
      if (type !== 'office') {
        this.searchForm.patchValue({ capacity: '' });
      }
    } else {
      this.searchForm.patchValue({ workspaceType: '' });
      this.searchForm.patchValue({ capacity: '' });
    }
  }

  isTypeSelected(type: string): boolean {
    return this.searchForm.get('workspaceType')?.value === type;
  }

  isAnyTypeSelected(): boolean {
    return !!this.searchForm.get('workspaceType')?.value;
  }

  onSearch() {
    const { name, cities, workspaceType, capacity } = this.searchForm.value;

    this.spaceService.searchSpaces(name, cities, workspaceType, capacity).subscribe({
      next: (results) => this.searchResults = results,
      error: (err) => console.error(err)
    });
  }

  clearFilters() {
    this.searchForm.reset({
      name: '',
      cities: [],
      workspaceType: '',
      capacity: ''
    });
    this.onSearch();
  }
}
