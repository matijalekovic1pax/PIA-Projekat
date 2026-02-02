import { Component, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './feedback.html'
})
export class FeedbackComponent implements OnInit {
  @Input() space: any;
  feedbackForm: FormGroup;
  message: string = '';
  currentUserId: number | null = null;

  // Calculate average or display counts? 
  // "The member sees the number of "likes"/"dislikes" and the number of comments they have left"
  // Actually, prompt says: "The member sees the number of "likes"/"dislikes". The member has the option to leave a 'like' or 'dislike'"

  constructor(
    private fb: FormBuilder,
    private spaceService: SpaceService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.feedbackForm = this.fb.group({
      comment: ['', Validators.required],
      isLike: [null] // true/false/null
    });
  }

  ngOnInit() {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || null;
  }

  setRating(isLike: boolean) {
    this.feedbackForm.patchValue({ isLike });
  }

  getRecentComments() {
    if (!this.space?.comments) return [];
    return [...this.space.comments]
      .sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 10);
  }

  isOwnComment(comment: any): boolean {
    return !!this.currentUserId && comment.userId === this.currentUserId;
  }

  onSubmit() {
    if (this.feedbackForm.valid) {
      // Check if isLike is set? Prompt implies they can leave a comment OR like/dislike?
      // "The member should have the option to leave a "like" or "dislike" ... leave a comment"
      // Let's allow either.

      const { comment, isLike } = this.feedbackForm.value;

      this.spaceService.addReview({
        spaceId: this.space.id,
        comment,
        isLike
      }).subscribe({
        next: (updatedSpace) => {
          this.message = 'Review added!';
          this.space = updatedSpace; // Update local view (likes count etc)
          this.feedbackForm.reset();
          this.cdr.detectChanges();
          setTimeout(() => {
            this.message = '';
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          this.message = err.error?.message || 'Failed to add review';
          this.cdr.detectChanges();
        }
      });
    }
  }
}

