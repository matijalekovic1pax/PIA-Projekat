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
      workspaceType: ['', Validators.required], // 'open', 'office', 'conference'
      capacity: [''] // visible only if office
    });
  }

  ngOnInit() {
    this.spaceService.getAllCities().subscribe(cities => this.cities = cities);
  }

  onTypeChange(type: string, event: any) {
    if (event.target.checked) {
      this.searchForm.patchValue({ workspaceType: type });
      if (type !== 'office') {
        this.searchForm.patchValue({ capacity: '' });
        this.searchForm.get('capacity')?.clearValidators();
      }
      if (type === 'office') {
        this.searchForm.get('capacity')?.setValidators([Validators.required, Validators.min(1)]);
      }
    } else {
      // If unchecking the active one, clear it? Or just behave like radio?
      // "The member selects exactly one checkbox; when one is confirmed, the other two are disabled."
      // This implies radio button UI behavior but using checkboxes visually? Or actual disabling.
      // Let's implement logic: clear if uncheck.
      this.searchForm.patchValue({ workspaceType: '' });
    }

    this.searchForm.get('capacity')?.updateValueAndValidity();
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
}
