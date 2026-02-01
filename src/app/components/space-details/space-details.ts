import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';
import { ReservationComponent } from '../reservation/reservation';
import { FeedbackComponent } from '../feedback/feedback';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-space-details',
  standalone: true,
  imports: [CommonModule, RouterModule, ReservationComponent, FeedbackComponent],
  templateUrl: './space-details.html'
})
export class SpaceDetailsComponent implements OnInit {
  space: any;
  selectedImage: string = '';
  mapUrl: SafeResourceUrl | null = null;
  isMember = false;

  constructor(
    private route: ActivatedRoute,
    private spaceService: SpaceService,
    private sanitizer: DomSanitizer,
    private authService: AuthService
  ) { }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const user = this.authService.getCurrentUser();
      this.isMember = user?.type === 'member';
      this.spaceService.getSpaceDetails(id).subscribe(data => {
        this.space = data;
        this.selectedImage = this.space.mainImage;
        this.mapUrl = this.buildMapUrl();

        // Check cookie for remembered image? "it becomes remembered in the browser's cookie"
        const cookieName = `space_${id}_image`;
        const savedImage = this.getCookie(cookieName);
        if (savedImage && (this.space.galleryImages.includes(savedImage) || savedImage === this.space.mainImage)) {
          this.selectedImage = savedImage;
        }
      });
    }
  }

  selectImage(image: string) {
    this.selectedImage = image;
    // Set cookie
    if (this.space) {
      document.cookie = `space_${this.space.id}_image=${image}; path=/; max-age=86400`; // 1 day
    }
  }

  getCookie(name: string): string | null {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
    return null;
  }

  private buildMapUrl(): SafeResourceUrl | null {
    if (!this.space?.latitude || !this.space?.longitude) {
      return null;
    }

    const lat = this.space.latitude;
    const lon = this.space.longitude;
    const bbox = `${lon - 0.01}%2C${lat - 0.01}%2C${lon + 0.01}%2C${lat + 0.01}`;
    const url = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&marker=${lat}%2C${lon}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}

