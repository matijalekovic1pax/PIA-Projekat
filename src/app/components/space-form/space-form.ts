import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { SpaceService } from '../../services/space.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-space-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './space-form.html'
})
export class SpaceFormComponent implements OnInit {
  spaceForm: FormGroup;
  isEditMode = false;
  spaceId: string | null = null;
  mainImageFile: File | null = null;
  galleryFiles: FileList | null = null;
  loading = false;
  message = '';

  constructor(
    private fb: FormBuilder,
    private spaceService: SpaceService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.spaceForm = this.fb.group({
      name: ['', Validators.required],
      city: ['', Validators.required],
      address: ['', Validators.required],
      pricePerHour: [0, [Validators.required, Validators.min(0)]],
      description: [''],
      openSpaceDeskCount: [5, [Validators.required, Validators.min(5)]],
      noShowLimit: [3, [Validators.required, Validators.min(1)]],
      latitude: [''],
      longitude: [''],
      // Dynamic lists
      offices: this.fb.array([]),
      conferenceRooms: this.fb.array([])
    });
  }

  get offices() {
    return this.spaceForm.get('offices') as FormArray;
  }

  get conferenceRooms() {
    return this.spaceForm.get('conferenceRooms') as FormArray;
  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id && id !== 'new') {
        this.isEditMode = true;
        this.spaceId = id;
        this.loadSpace(id);
      }
    });
  }

  loadSpace(id: string) {
    this.loading = true;
    this.spaceService.getManagerSpace(id).subscribe({
      next: (space) => {
        this.spaceForm.patchValue({
          name: space.name,
          city: space.city,
          address: space.address,
          pricePerHour: space.pricePerHour,
          description: space.description,
          openSpaceDeskCount: space.openSpaceDeskCount,
          noShowLimit: space.noShowLimit,
          latitude: space.latitude,
          longitude: space.longitude
        });

        // Clear and populate arrays
        this.offices.clear();
        space.offices.forEach((o: any) => {
          this.offices.push(this.fb.group({
            name: [o.name, Validators.required],
            deskCount: [o.deskCount, [Validators.required, Validators.min(1)]]
          }));
        });

        this.conferenceRooms.clear();
        space.conferenceRooms.forEach((c: any) => {
          this.conferenceRooms.push(this.fb.group({
            name: [c.name, Validators.required],
            equipment: [c.equipment, Validators.maxLength(300)]
          }));
        });

        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        // Handle unauthorized or not found
      }
    });
  }

  addOffice() {
    this.offices.push(this.fb.group({
      name: ['', Validators.required],
      deskCount: [1, [Validators.required, Validators.min(1)]]
    }));
  }

  removeOffice(index: number) {
    this.offices.removeAt(index);
  }

  addConferenceRoom() {
    this.conferenceRooms.push(this.fb.group({
      name: ['', Validators.required],
      equipment: ['', Validators.maxLength(300)]
    }));
  }

  removeConferenceRoom(index: number) {
    this.conferenceRooms.removeAt(index);
  }

  onMainImageSelected(event: any) {
    if (event.target.files.length > 0) {
      this.mainImageFile = event.target.files[0];
    }
  }

  onGalleryImagesSelected(event: any) {
    if (event.target.files.length > 0) {
      if (event.target.files.length > 5) {
        this.message = 'You can upload up to 5 gallery images.';
        this.galleryFiles = null;
        return;
      }
      this.galleryFiles = event.target.files;
    }
  }

  onSubmit() {
    if (this.spaceForm.invalid) return;

    if (!this.isEditMode && !this.mainImageFile) {
      this.message = 'Main image is required for new spaces.';
      return;
    }

    const lat = this.spaceForm.get('latitude')?.value;
    const lon = this.spaceForm.get('longitude')?.value;
    if ((lat && !lon) || (!lat && lon)) {
      this.message = 'Please provide both latitude and longitude.';
      return;
    }
    if (lat && lon) {
      const latNum = parseFloat(lat);
      const lonNum = parseFloat(lon);
      if (Number.isNaN(latNum) || Number.isNaN(lonNum) || latNum < -90 || latNum > 90 || lonNum < -180 || lonNum > 180) {
        this.message = 'Latitude must be between -90 and 90, longitude between -180 and 180.';
        return;
      }
    }

    this.loading = true;
    const formData = new FormData();

    Object.keys(this.spaceForm.value).forEach(key => {
      if (key === 'offices' || key === 'conferenceRooms') {
        formData.append(key, JSON.stringify(this.spaceForm.value[key]));
      } else {
        formData.append(key, this.spaceForm.value[key]);
      }
    });

    if (this.mainImageFile) {
      formData.append('mainImage', this.mainImageFile);
    }

    if (this.galleryFiles) {
      for (let i = 0; i < this.galleryFiles.length; i++) {
        formData.append('galleryImages', this.galleryFiles[i]);
      }
    }

    if (this.isEditMode && this.spaceId) {
      this.spaceService.updateSpace(this.spaceId, formData).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/manager/dashboard']);
        },
        error: (err) => {
          this.message = err.error?.message || 'Update failed';
          this.loading = false;
        }
      });
    } else {
      this.spaceService.createSpace(formData).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/manager/dashboard']);
        },
        error: (err) => {
          this.message = err.error?.message || 'Creation failed';
          this.loading = false;
        }
      });
    }
  }
}
